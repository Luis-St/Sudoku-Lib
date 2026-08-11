package net.luis.sudoku.learn;

import net.luis.sudoku.solver.Technique;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Generates the learn area's bundled content and writes it as one JSON file per technique.
 * <p>
 *     This runs at development time, never on a device. Finding the rarer positions takes minutes, which is fine
 *     offline and impossible in an app, so the results are generated once, committed, and shipped as assets. That is
 *     also what makes the exercises identical for every player, which the progression depends on.
 * </p>
 * <p>
 *     The exporter is deliberately restartable and idempotent. Seeds come from {@link LearnContent#seedFor}, so a
 *     technique regenerated after an unrelated change produces the same puzzles it did before, and a run that is
 *     interrupted can be resumed by skipping the files that already exist. A technique that cannot be generated
 *     within its budget is reported and skipped rather than failing the whole run, because losing forty finished
 *     techniques to one stubborn one helps nobody.
 * </p>
 *
 * @see LearnPuzzleGenerator
 * @see LearnPuzzleCodec
 */
public final class LearnAssetExporter {

	private LearnAssetExporter() {}

	/**
	 * What one technique's export produced.
	 *
	 * @param technique The technique
	 * @param examples How many worked examples were found, of {@link LearnContent#EXAMPLES_PER_TECHNIQUE}
	 * @param exercises How many training exercises were found, of {@link LearnContent#EXERCISES_PER_TECHNIQUE}
	 * @param millis How long it took
	 */
	public record Result(Technique technique, int examples, int exercises, long millis) {

		/**
		 * Constructs a result.
		 *
		 * @throws NullPointerException If the technique is null
		 */
		public Result {
			Objects.requireNonNull(technique, "Technique must not be null");
		}

		/**
		 * Checks whether this technique got everything it needs.
		 *
		 * @return True if every example and every exercise was found
		 */
		public boolean isComplete() {
			return this.examples == LearnContent.EXAMPLES_PER_TECHNIQUE && this.exercises == LearnContent.EXERCISES_PER_TECHNIQUE;
		}

		@Override
		public String toString() {
			return String.format("%-26s %d/%d examples  %d/%d exercises  %6dms%s", this.technique,
				this.examples, LearnContent.EXAMPLES_PER_TECHNIQUE, this.exercises, LearnContent.EXERCISES_PER_TECHNIQUE,
				this.millis, this.isComplete() ? "" : "  INCOMPLETE");
		}
	}

	/**
	 * Generates and writes the content of every taught technique.
	 *
	 * @param directory The directory to write into, created if it does not exist
	 * @param budget How much work each single puzzle may cost
	 * @param skipExisting True to leave techniques whose file is already <i>complete</i> untouched, so a run can be
	 *        resumed
	 * @return One result per technique, in {@link LearnTechniques#taught()} order
	 * @throws NullPointerException If the directory or the budget is null
	 * @throws IOException If the directory or a file cannot be written
	 */
	public static List<Result> exportAll(Path directory, LearnPuzzleGenerator.Budget budget, boolean skipExisting) throws IOException {
		Objects.requireNonNull(directory, "Directory must not be null");
		Objects.requireNonNull(budget, "Budget must not be null");

		Files.createDirectories(directory);
		List<Result> results = new ArrayList<>();
		for (Technique technique : LearnContent.techniques()) {
			Path file = fileOf(directory, technique);
			if (skipExisting && isComplete(file)) {
				continue;
			}

			results.add(export(file, technique, budget));
		}
		return List.copyOf(results);
	}

	/**
	 * Generates and writes one technique's content.
	 *
	 * @param file The file to write
	 * @param technique The technique
	 * @param budget How much work each single puzzle may cost
	 * @return What was produced
	 * @throws NullPointerException If any argument is null
	 * @throws IOException If the file cannot be written
	 */
	public static Result export(Path file, Technique technique, LearnPuzzleGenerator.Budget budget) throws IOException {
		Objects.requireNonNull(file, "File must not be null");
		Objects.requireNonNull(technique, "Technique must not be null");
		Objects.requireNonNull(budget, "Budget must not be null");

		long start = System.currentTimeMillis();
		List<LearnPuzzle> examples = LearnPuzzleGenerator.generateSet(technique, LearnContent.EXAMPLES_PER_TECHNIQUE, LearnContent.seedFor(technique, 0, 0), budget);

		// The exercises are generated one at a time rather than as a set: they are met one at a time too, so two of
		// them sharing a layout costs nothing, whereas five examples side by side in a carousel must all differ.
		List<LearnPuzzle> exercises = new ArrayList<>(LearnContent.EXERCISES_PER_TECHNIQUE);
		for (int level = 1; level <= LearnContent.LEVELS; level++) {
			for (int index = 0; index < LearnContent.SUB_LEVELS; index++) {
				Optional<LearnPuzzle> puzzle = LearnPuzzleGenerator.generate(technique, LearnContent.seedFor(technique, level, index), budget);
				puzzle.ifPresent(exercises::add);
			}
		}

		List<LearnPuzzle> all = new ArrayList<>(examples);
		all.addAll(exercises);
		Files.writeString(file, write(technique, examples, exercises), StandardCharsets.UTF_8);

		// Reading every puzzle straight back is the only check that catches a format change before the asset reaches
		// a device, where it would show as a learn area that simply refuses to open.
		for (LearnPuzzle puzzle : all) {
			LearnPuzzleCodec.read(LearnPuzzleCodec.write(puzzle));
		}
		return new Result(technique, examples.size(), exercises.size(), System.currentTimeMillis() - start);
	}

	/**
	 * Checks whether a technique's file is already there <i>and</i> holds everything it should.
	 * <p>
	 *     Resuming on existence alone is what let three techniques ship with an unfinished carousel: a technique that
	 *     ran out of budget on its examples still wrote a file, and every later resume skipped it because the file was
	 *     there. What matters is the contents, so that is what is counted.
	 * </p>
	 *
	 * @param file The file
	 * @return True if the file holds every example and every exercise
	 */
	public static boolean isComplete(Path file) {
		Objects.requireNonNull(file, "File must not be null");

		if (!Files.exists(file)) {
			return false;
		}

		try {
			String json = Files.readString(file, StandardCharsets.UTF_8);
			int examples = json.indexOf("\"examples\":");
			int exercises = json.indexOf("\"exercises\":");
			if (examples < 0 || exercises < 0 || examples > exercises) {
				return false;
			}
			return count(json.substring(examples, exercises)) == LearnContent.EXAMPLES_PER_TECHNIQUE
				&& count(json.substring(exercises)) == LearnContent.EXERCISES_PER_TECHNIQUE;
		} catch (IOException e) {
			// A file that cannot be read is not a file worth keeping, so the technique is generated again.
			return false;
		}
	}

	/**
	 * Counts the puzzles in one array of the written format, each of which opens with its technique.
	 */
	private static int count(String json) {
		int count = 0;
		int at = json.indexOf("{\"technique\":");
		while (at >= 0) {
			count++;
			at = json.indexOf("{\"technique\":", at + 1);
		}
		return count;
	}

	/**
	 * Returns the file a technique's content is written to.
	 *
	 * @param directory The asset directory
	 * @param technique The technique
	 * @return The file
	 * @throws NullPointerException If either argument is null
	 */
	public static Path fileOf(Path directory, Technique technique) {
		Objects.requireNonNull(directory, "Directory must not be null");
		Objects.requireNonNull(technique, "Technique must not be null");

		return directory.resolve(technique.name().toLowerCase() + ".json");
	}

	/**
	 * Writes one technique's file: its examples and its exercises, kept apart because they are used for different
	 * things and the app asks for one or the other, never both.
	 */
	private static String write(Technique technique, List<LearnPuzzle> examples, List<LearnPuzzle> exercises) {
		return "{\n\"technique\":\"" + technique.name() + "\",\n"
			+ "\"level\":" + technique.level() + ",\n"
			+ "\"examples\":" + LearnPuzzleCodec.writeAll(examples) + ",\n"
			+ "\"exercises\":" + LearnPuzzleCodec.writeAll(exercises) + "\n}\n";
	}

	/**
	 * Runs the export from the command line.
	 * <p>
	 *     Usage: {@code LearnAssetExporter <directory> [--resume]}. The directory is normally the Android module's
	 *     {@code src/main/assets/learn}.
	 * </p>
	 *
	 * @param args The output directory, and optionally {@code --resume}
	 * @throws IOException If the output cannot be written
	 */
	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.err.println("Usage: LearnAssetExporter <directory> [--resume]");
			return;
		}

		boolean resume = args.length > 1 && "--resume".equals(args[1]);
		Path directory = Path.of(args[0]);
		System.out.println("Exporting " + LearnContent.techniques().size() + " techniques, " + LearnContent.totalPuzzles() + " puzzles, into " + directory.toAbsolutePath());

		long start = System.currentTimeMillis();
		int incomplete = 0;
		Files.createDirectories(directory);
		for (Technique technique : LearnContent.techniques()) {
			Path file = fileOf(directory, technique);
			if (resume && isComplete(file)) {
				System.out.println(technique + " already complete, skipped");
				continue;
			}

			Result result = export(file, technique, LearnPuzzleGenerator.Budget.offline());
			System.out.println(result);
			if (!result.isComplete()) {
				incomplete++;
			}
		}

		System.out.println("Done in " + (System.currentTimeMillis() - start) / 1000 + "s, " + incomplete + " techniques incomplete");
	}
}
