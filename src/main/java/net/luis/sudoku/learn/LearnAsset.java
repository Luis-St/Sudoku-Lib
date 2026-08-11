package net.luis.sudoku.learn;

import net.luis.sudoku.solver.Technique;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One technique's bundled content: the worked examples the wiki shows and the exercises its training sets.
 * <p>
 *     This is the shape of the files {@link LearnAssetExporter} writes and an app reads back, and it lives beside the
 *     writer on purpose. The two halves of a format that is generated on one machine and parsed on another are the
 *     easiest thing in a project to let drift apart, and the failure is silent until a device refuses to open the
 *     learn area.
 * </p>
 * <p>
 *     Examples and exercises are kept apart rather than in one list because they are used for entirely different
 *     things: the examples are watched, the exercises are solved, and a caller always wants one or the other.
 * </p>
 *
 * @param technique The technique this content teaches
 * @param examples The worked examples, {@link LearnContent#EXAMPLES_PER_TECHNIQUE} of them in a complete file
 * @param exercises The training exercises, level by level, {@link LearnContent#EXERCISES_PER_TECHNIQUE} in a complete
 *        file
 *
 * @see LearnAssetExporter
 * @see LearnPuzzleCodec
 */
public record LearnAsset(Technique technique, List<LearnPuzzle> examples, List<LearnPuzzle> exercises) {

	/**
	 * Constructs an asset, copying both lists so the record stays an immutable value.
	 *
	 * @throws NullPointerException If the technique or either list is null, or a list holds a null element
	 */
	public LearnAsset {
		Objects.requireNonNull(technique, "Technique must not be null");
		Objects.requireNonNull(examples, "Examples must not be null");
		Objects.requireNonNull(exercises, "Exercises must not be null");

		examples = List.copyOf(examples);
		exercises = List.copyOf(exercises);
	}

	/**
	 * Returns the file name a technique's content is written under, which is the name an app asks its asset manager
	 * for.
	 *
	 * @param technique The technique
	 * @return The file name
	 * @throws NullPointerException If the technique is null
	 */
	public static String fileNameOf(Technique technique) {
		Objects.requireNonNull(technique, "Technique must not be null");

		return technique.name().toLowerCase() + ".json";
	}

	/**
	 * Writes the content as the JSON one file holds.
	 *
	 * @param asset The content
	 * @return The JSON text
	 * @throws NullPointerException If the content is null
	 */
	public static String write(LearnAsset asset) {
		Objects.requireNonNull(asset, "Asset must not be null");

		return "{\n\"technique\":\"" + asset.technique().name() + "\",\n"
			+ "\"level\":" + asset.technique().level() + ",\n"
			+ "\"examples\":" + LearnPuzzleCodec.writeAll(asset.examples()) + ",\n"
			+ "\"exercises\":" + LearnPuzzleCodec.writeAll(asset.exercises()) + "\n}\n";
	}

	/**
	 * Reads back what {@link #write(LearnAsset)} wrote.
	 * <p>
	 *     The two arrays are told apart by where they start, which is all the parser needs: every puzzle is written as
	 *     one object and the {@code exercises} key follows the {@code examples} array.
	 * </p>
	 *
	 * @param json The JSON text of one file
	 * @return The content
	 * @throws NullPointerException If the text is null
	 * @throws IllegalArgumentException If the text is not in the written shape
	 */
	public static LearnAsset read(String json) {
		Objects.requireNonNull(json, "Json must not be null");

		int examplesAt = json.indexOf("\"examples\":");
		int exercisesAt = json.indexOf("\"exercises\":");
		if (examplesAt < 0 || exercisesAt < 0 || examplesAt > exercisesAt) {
			throw new IllegalArgumentException("Not a learn asset: no examples and exercises in that order");
		}

		List<LearnPuzzle> examples = LearnPuzzleCodec.readAll(json.substring(examplesAt, exercisesAt));
		List<LearnPuzzle> exercises = LearnPuzzleCodec.readAll(json.substring(exercisesAt));
		if (examples.isEmpty() && exercises.isEmpty()) {
			throw new IllegalArgumentException("Not a learn asset: it holds no puzzles");
		}

		List<LearnPuzzle> all = new ArrayList<>(examples);
		all.addAll(exercises);
		return new LearnAsset(all.get(0).technique(), examples, exercises);
	}

	/**
	 * Returns the exercise at one place in the training.
	 *
	 * @param level The one-based level, {@code 1..}{@link LearnContent#LEVELS}
	 * @param index The zero-based sub-level within it
	 * @return The exercise
	 * @throws IllegalArgumentException If the level or the index is outside the training
	 * @throws IndexOutOfBoundsException If this content does not hold that exercise, which means the file is
	 *         incomplete
	 */
	public LearnPuzzle exercise(int level, int index) {
		if (level < 1 || level > LearnContent.LEVELS) {
			throw new IllegalArgumentException("Level " + level + " is not in 1.." + LearnContent.LEVELS);
		}
		if (index < 0 || index >= LearnContent.SUB_LEVELS) {
			throw new IllegalArgumentException("Sub level " + index + " is not in 0.." + (LearnContent.SUB_LEVELS - 1));
		}

		return this.exercises.get((level - 1) * LearnContent.SUB_LEVELS + index);
	}

	/**
	 * Checks whether this content holds everything a technique needs.
	 *
	 * @return True if every example and every exercise is there
	 */
	public boolean isComplete() {
		return this.examples.size() == LearnContent.EXAMPLES_PER_TECHNIQUE
			&& this.exercises.size() == LearnContent.EXERCISES_PER_TECHNIQUE;
	}
}
