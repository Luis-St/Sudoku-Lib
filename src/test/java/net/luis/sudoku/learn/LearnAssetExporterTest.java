package net.luis.sudoku.learn;

import net.luis.sudoku.solver.Technique;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link LearnAssetExporter}.
 * <p>
 *     The completeness check is what these are really about. Resuming an export on whether a file <i>exists</i> once
 *     shipped three techniques with an unfinished carousel: each of them had run out of budget on its examples, had
 *     still written a file, and was therefore skipped by every later resume. The contents are what decides, so the
 *     contents are what is tested.
 * </p>
 */
class LearnAssetExporterTest {

	private static final LearnPuzzleGenerator.Budget BUDGET = new LearnPuzzleGenerator.Budget(400, 60_000L);

	/**
	 * Writes a file in the exported shape with the given number of examples and exercises, reusing one cheap puzzle
	 * for every entry: the completeness check counts puzzles and never looks at what is in them.
	 */
	private static Path write(Path directory, Technique technique, int examples, int exercises) throws IOException {
		LearnPuzzle puzzle = LearnPuzzleGenerator.generate(technique, 1L, BUDGET).orElseThrow();
		List<LearnPuzzle> exampleList = new ArrayList<>();
		List<LearnPuzzle> exerciseList = new ArrayList<>();
		for (int index = 0; index < examples; index++) {
			exampleList.add(puzzle);
		}
		for (int index = 0; index < exercises; index++) {
			exerciseList.add(puzzle);
		}

		Path file = LearnAssetExporter.fileOf(directory, technique);
		String json = "{\n\"technique\":\"" + technique.name() + "\",\n"
			+ "\"level\":" + technique.level() + ",\n"
			+ "\"examples\":" + LearnPuzzleCodec.writeAll(exampleList) + ",\n"
			+ "\"exercises\":" + LearnPuzzleCodec.writeAll(exerciseList) + "\n}\n";
		Files.writeString(file, json, StandardCharsets.UTF_8);
		return file;
	}

	@Test
	void isCompleteAcceptsAFullFile(@TempDir Path directory) throws IOException {
		Path file = write(directory, Technique.NAKED_SINGLE, LearnContent.EXAMPLES_PER_TECHNIQUE, LearnContent.EXERCISES_PER_TECHNIQUE);

		assertTrue(LearnAssetExporter.isComplete(file));
	}

	@Test
	void isCompleteRejectsAFileMissingExamples(@TempDir Path directory) throws IOException {
		Path file = write(directory, Technique.NAKED_SINGLE, 1, LearnContent.EXERCISES_PER_TECHNIQUE);

		assertFalse(LearnAssetExporter.isComplete(file));
	}

	@Test
	void isCompleteRejectsAFileMissingExercises(@TempDir Path directory) throws IOException {
		Path file = write(directory, Technique.NAKED_SINGLE, LearnContent.EXAMPLES_PER_TECHNIQUE, 0);

		assertFalse(LearnAssetExporter.isComplete(file));
	}

	@Test
	void isCompleteRejectsAMissingFile(@TempDir Path directory) {
		assertFalse(LearnAssetExporter.isComplete(LearnAssetExporter.fileOf(directory, Technique.NAKED_SINGLE)));
	}

	@Test
	void isCompleteRejectsAFileThatIsNotAnExport(@TempDir Path directory) throws IOException {
		Path file = LearnAssetExporter.fileOf(directory, Technique.NAKED_SINGLE);
		Files.writeString(file, "{}", StandardCharsets.UTF_8);

		assertFalse(LearnAssetExporter.isComplete(file));
	}

	@Test
	void isCompleteRejectsNull() {
		assertThrows(NullPointerException.class, () -> LearnAssetExporter.isComplete(null));
	}

	@Test
	void exportWritesEveryExampleAndExercise(@TempDir Path directory) throws IOException {
		Path file = LearnAssetExporter.fileOf(directory, Technique.NAKED_SINGLE);

		LearnAssetExporter.Result result = LearnAssetExporter.export(file, Technique.NAKED_SINGLE, BUDGET);

		assertEquals(LearnContent.EXAMPLES_PER_TECHNIQUE, result.examples());
		assertEquals(LearnContent.EXERCISES_PER_TECHNIQUE, result.exercises());
		assertTrue(result.isComplete());
		assertTrue(LearnAssetExporter.isComplete(file));
	}
}
