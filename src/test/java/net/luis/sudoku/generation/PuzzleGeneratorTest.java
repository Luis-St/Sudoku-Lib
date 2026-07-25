package net.luis.sudoku.generation;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.difficulty.DifficultyRater;
import net.luis.sudoku.grid.*;
import net.luis.sudoku.key.PuzzleKey;
import net.luis.sudoku.solver.BacktrackingSolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link PuzzleGenerator}.
 * <p>
 *     The full-matrix tests cover every {@link GridSize} at every {@link Difficulty}. Generation is fast at all
 *     five sizes because the solver selects cells by a minimum-remaining-values heuristic and the generator caps
 *     the hole count per size, so even 16x16 completes in milliseconds.
 * </p>
 */
class PuzzleGeneratorTest {
	
	private static final long FIXED_SEED = 0x5EED_C0DE_1234_5678L;
	private static final GridSize[] ALL_SIZES = GridSize.values();
	
	private static int solutionCount(Puzzle puzzle) {
		return BacktrackingSolver.countSolutions(puzzle, 2);
	}
	
	private static int givenCount(Puzzle puzzle) {
		int count = 0;
		for (int cellIndex = 0; cellIndex < puzzle.size().cellCount(); cellIndex++) {
			if (puzzle.valueAt(cellIndex) != 0) {
				count++;
			}
		}
		return count;
	}
	
	private static void assertUniquelySolvableToCarriedSolution(GeneratedPuzzle generated, String label) {
		assertAll(label,
			() -> assertEquals(generated.puzzle().size().cellCount(), generated.solution().length, "Wrong solution length"),
			() -> assertEquals(1, solutionCount(generated.puzzle()), "Puzzle is not uniquely solvable"),
			() -> assertArrayEquals(generated.solution(), BacktrackingSolver.solve(generated.puzzle()).orElseThrow(),
				"The only solution is not the carried solution")
		);
	}
	
	private static void assertGivensMarkedAndHolesEmptyInValidGrid(Puzzle puzzle, String label) {
		assertTrue(puzzle.isValid(), "Generated puzzle carries a conflict for " + label);
		for (int cellIndex = 0; cellIndex < puzzle.size().cellCount(); cellIndex++) {
			Cell cell = puzzle.cell(cellIndex);
			if (cell.value() == 0) {
				assertTrue(cell.isEmpty(), "Hole is not empty at cell " + cellIndex + " for " + label);
				assertFalse(cell.isGiven(), "Empty cell is marked as a given at " + cellIndex + " for " + label);
			} else {
				assertTrue(cell.isGiven(), "Clue is not marked as a given at cell " + cellIndex + " for " + label);
			}
		}
	}
	
	@Test
	@Timeout(value = 120, unit = TimeUnit.SECONDS)
	void generate_everySizeAndDifficulty_yieldsAUniquePuzzleWhoseSolutionIsTheCarriedOne() {
		for (GridSize size : ALL_SIZES) {
			for (Difficulty difficulty : Difficulty.values()) {
				PuzzleKey key = PuzzleKey.of(size, Variant.CLASSIC, difficulty, FIXED_SEED);
				
				GeneratedPuzzle generated = PuzzleGenerator.generate(key);
				
				assertEquals(key, generated.key(), "The key is not carried through for " + size + "/" + difficulty);
				assertUniquelySolvableToCarriedSolution(generated, size + "/" + difficulty);
			}
		}
	}
	
	@Test
	@Timeout(value = 120, unit = TimeUnit.SECONDS)
	void generate_everySizeAndDifficulty_marksGivensAndLeavesHolesEmptyInAValidGrid() {
		for (GridSize size : ALL_SIZES) {
			for (Difficulty difficulty : Difficulty.values()) {
				PuzzleKey key = PuzzleKey.of(size, Variant.CLASSIC, difficulty, FIXED_SEED);
				
				Puzzle puzzle = PuzzleGenerator.generate(key).puzzle();
				
				assertGivensMarkedAndHolesEmptyInValidGrid(puzzle, size + "/" + difficulty);
			}
		}
	}
	
	@Test
	void generate_everyGivenCellMatchesTheSolution() {
		GeneratedPuzzle generated = PuzzleGenerator.generate(PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.THREE, FIXED_SEED));
		Puzzle puzzle = generated.puzzle();
		
		for (int cellIndex = 0; cellIndex < puzzle.size().cellCount(); cellIndex++) {
			int value = puzzle.valueAt(cellIndex);
			if (value != 0) {
				assertEquals(generated.solutionAt(cellIndex), value, "Given disagrees with the solution at cell " + cellIndex);
			}
		}
	}
	
	@Test
	void generate_digsAtLeastOneHole() {
		GeneratedPuzzle generated = PuzzleGenerator.generate(PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.ONE, FIXED_SEED));
		
		assertTrue(givenCount(generated.puzzle()) < GridSize.NINE.cellCount(),
			"No hole was dug: the puzzle is the full solution");
	}
	
	@Test
	@Timeout(value = 120, unit = TimeUnit.SECONDS)
	void generate_chaosVariant_everySupportedSize_yieldsAUniqueWellFormedPuzzle() {
		for (GridSize size : new GridSize[] { GridSize.SIX, GridSize.NINE, GridSize.TWELVE, GridSize.SIXTEEN }) {
			GeneratedPuzzle generated = PuzzleGenerator.generate(PuzzleKey.of(size, Variant.CHAOS, Difficulty.THREE, FIXED_SEED));
			
			assertAll(size.toString(),
				() -> assertEquals(Variant.CHAOS, generated.puzzle().variant()),
				() -> assertUniquelySolvableToCarriedSolution(generated, size + "/CHAOS"),
				() -> assertGivensMarkedAndHolesEmptyInValidGrid(generated.puzzle(), size + "/CHAOS"),
				// the chaos partition must not be the classic box layout
				() -> assertNotEquals(ClassicRegionPartition.of(size), generated.puzzle().partition(),
					"Chaos partition is identical to the classic box layout")
			);
		}
	}
	
	@Test
	void generate_chaosVariant_theSameKeyTwice_yieldsEqualPuzzles() {
		PuzzleKey key = PuzzleKey.of(GridSize.NINE, Variant.CHAOS, Difficulty.TWO, 314L);
		
		assertEquals(PuzzleGenerator.generate(key), PuzzleGenerator.generate(key));
	}
	
	@Test
	void generate_nullKey_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> PuzzleGenerator.generate(null));
	}
	
	@Test
	void generate_theSameKeyTwice_yieldsEqualPuzzles() {
		PuzzleKey key = PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.TWO, FIXED_SEED);
		
		GeneratedPuzzle first = PuzzleGenerator.generate(key);
		GeneratedPuzzle second = PuzzleGenerator.generate(key);
		
		assertAll(
			() -> assertEquals(first, second, "The same key produced two different results"),
			() -> assertArrayEquals(first.puzzle().values(), second.puzzle().values(), "Givens differ"),
			() -> assertArrayEquals(first.solution(), second.solution(), "Solutions differ")
		);
	}
	
	@Test
	void generate_differentSeedsAtTheSameSizeAndDifficulty_yieldDifferentPuzzles() {
		PuzzleKey first = PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.ONE, 1L);
		PuzzleKey second = PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.ONE, 2L);
		
		int[] firstGivens = PuzzleGenerator.generate(first).puzzle().values();
		int[] secondGivens = PuzzleGenerator.generate(second).puzzle().values();
		
		assertFalse(Arrays.equals(firstGivens, secondGivens), "Two seeds produced identical givens");
	}
	
	@Test
	void generate_theSameSeedAtTwoDifferentDifficulties_yieldsUnrelatedPuzzles() {
		PuzzleKey easy = PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.ONE, FIXED_SEED);
		PuzzleKey hard = PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.FIVE, FIXED_SEED);
		
		GeneratedPuzzle easyPuzzle = PuzzleGenerator.generate(easy);
		GeneratedPuzzle hardPuzzle = PuzzleGenerator.generate(hard);
		
		assertAll(
			() -> assertFalse(Arrays.equals(easyPuzzle.solution(), hardPuzzle.solution()),
				"Two difficulties share the same full solution"),
			() -> assertFalse(Arrays.equals(easyPuzzle.puzzle().values(), hardPuzzle.puzzle().values()),
				"Two difficulties share the same givens")
		);
	}
	
	@Test
	void generate_requestedBandThatIsReliablyReachable_isRatedAsRequested() {
		DifficultyRater rater = new DifficultyRater();
		// ONE (singles), FIVE (needs guessing) and LISA (the size ceiling, which is FIVE at 9x9) are reliably
		// reachable within the attempt bound at 9x9; the middle bands may fall back to the closest candidate.
		for (long seed = 0; seed < 8; seed++) {
			assertEquals(Difficulty.ONE, rater.rate(PuzzleGenerator.generate(PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.ONE, seed)).puzzle()),
				"Requested ONE was not rated ONE at seed " + seed);
			assertEquals(Difficulty.FIVE, rater.rate(PuzzleGenerator.generate(PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.FIVE, seed)).puzzle()),
				"Requested FIVE was not rated FIVE at seed " + seed);
			assertEquals(Difficulty.FIVE, rater.rate(PuzzleGenerator.generate(PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.LISA, seed)).puzzle()),
				"Requested LISA (ceiling FIVE) was not rated FIVE at seed " + seed);
		}
	}
	
	@Test
	void generate_everyRequestedDifficulty_returnsAUniquelySolvablePuzzle() {
		// Even when the exact band is unreachable and the closest candidate is returned, the result must still be a
		// valid, uniquely solvable puzzle.
		for (Difficulty difficulty : Difficulty.values()) {
			GeneratedPuzzle generated = PuzzleGenerator.generate(PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, difficulty, 123L));
			
			assertEquals(1, solutionCount(generated.puzzle()), "Not uniquely solvable for " + difficulty);
			assertArrayEquals(generated.solution(), BacktrackingSolver.solve(generated.puzzle()).orElseThrow(),
				"Carried solution is not the unique solution for " + difficulty);
		}
	}
}
