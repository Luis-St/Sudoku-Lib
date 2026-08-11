package net.luis.sudoku.learn;

import net.luis.sudoku.solver.Technique;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link LearnPuzzleCodec}.
 * <p>
 *     The round trip is the whole point: the exercises are generated on a desktop and read back on a phone, so a
 *     writer and reader that disagree would ship an asset nobody notices is broken until it fails to load.
 * </p>
 */
class LearnPuzzleCodecTest {

	private static final LearnPuzzleGenerator.Budget BUDGET = new LearnPuzzleGenerator.Budget(400, 60_000L);

	private static LearnPuzzle generate(Technique technique) {
		return LearnPuzzleGenerator.generate(technique, 1L, BUDGET).orElseThrow();
	}

	@Test
	void writesAndReadsBackAnIdenticalPuzzle() {
		for (Technique technique : List.of(Technique.NAKED_SINGLE, Technique.HIDDEN_SINGLE_REGION, Technique.POINTING, Technique.NAKED_PAIR)) {
			LearnPuzzle puzzle = generate(technique);

			LearnPuzzle restored = LearnPuzzleCodec.read(LearnPuzzleCodec.write(puzzle));

			assertEquals(puzzle, restored, "Round trip changed the exercise for " + technique);
			assertEquals(puzzle.technique(), restored.technique());
			assertEquals(puzzle.targetCell(), restored.targetCell());
			assertEquals(puzzle.targetDigit(), restored.targetDigit());
			assertArrayEquals(puzzle.board(), restored.board());
			assertArrayEquals(puzzle.solution(), restored.solution());
			assertArrayEquals(puzzle.pencilMarks(), restored.pencilMarks());
			assertEquals(puzzle.explanation(), restored.explanation());
			assertEquals(puzzle.layoutKey(), restored.layoutKey());
		}
	}

	@Test
	void writesAListAsAJsonArray() {
		List<LearnPuzzle> puzzles = LearnPuzzleGenerator.generateSet(Technique.HIDDEN_SINGLE_REGION, 3, 1L, BUDGET);

		String json = LearnPuzzleCodec.writeAll(puzzles);

		assertTrue(json.startsWith("["));
		assertTrue(json.trim().endsWith("]"));
		assertEquals(puzzles.size(), json.split("\"technique\":").length - 1);
	}

	@Test
	void readRejectsTextThatIsNotAnExercise() {
		assertThrows(IllegalArgumentException.class, () -> LearnPuzzleCodec.read("{}"));
		assertThrows(IllegalArgumentException.class, () -> LearnPuzzleCodec.read("{\"technique\":\"NAKED_SINGLE\"}"));
	}

	@Test
	void rejectsNulls() {
		assertThrows(NullPointerException.class, () -> LearnPuzzleCodec.write(null));
		assertThrows(NullPointerException.class, () -> LearnPuzzleCodec.read(null));
		assertThrows(NullPointerException.class, () -> LearnPuzzleCodec.writeAll(null));
	}
}
