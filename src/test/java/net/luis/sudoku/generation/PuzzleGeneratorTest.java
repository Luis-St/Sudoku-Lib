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
		// Bands 1 to 4 and 7 land on every one of the first 32 seeds at 9x9 (./gradlew bench). The others are
		// reachable but not certain on a given seed, and may fall back to the closest candidate, so pinning them
		// here would make this test a flake rather than a check.
		Difficulty[] reliable = { Difficulty.ONE, Difficulty.TWO, Difficulty.THREE, Difficulty.FOUR, Difficulty.SEVEN };
		for (long seed = 0; seed < 8; seed++) {
			for (Difficulty requested : reliable) {
				PuzzleKey key = PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, requested, seed);
				
				assertEquals(requested, rater.rate(PuzzleGenerator.generate(key).puzzle()),
					"Requested " + requested + " was not rated " + requested + " at seed " + seed);
			}
		}
	}
	
	@Test
	void generate_bandTheSizeCannotProduce_snapsToTheNearestSupportedBand() {
		DifficultyRater rater = new DifficultyRater();
		// A 4x4 grid supports band 1 alone, so every harder request must come back as a band-1 puzzle rather than
		// as something the size cannot actually make.
		for (Difficulty requested : Difficulty.values()) {
			PuzzleKey key = PuzzleKey.of(GridSize.FOUR, Variant.CLASSIC, requested, 0L);
			
			assertEquals(Difficulty.ONE, rater.rate(PuzzleGenerator.generate(key).puzzle()), requested.toString());
		}
	}
	
	@Test
	void partitionFor_classicKey_everySize_isTheCachedBoxLayout() {
		for (GridSize size : ALL_SIZES) {
			PuzzleKey key = PuzzleKey.of(size, Variant.CLASSIC, Difficulty.THREE, FIXED_SEED);
			
			assertSame(ClassicRegionPartition.of(size), PuzzleGenerator.partitionFor(key), size.toString());
		}
	}
	
	@Test
	void partitionFor_classicKey_isThePartitionTheGeneratedPuzzleCarries() {
		PuzzleKey key = PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.TWO, FIXED_SEED);
		
		assertEquals(PuzzleGenerator.generate(key).puzzle().partition(), PuzzleGenerator.partitionFor(key));
	}
	
	@Test
	void partitionFor_chaosKey_twoEqualKeys_yieldEqualPartitions() {
		PuzzleKey first = PuzzleKey.of(GridSize.NINE, Variant.CHAOS, Difficulty.TWO, 314L);
		PuzzleKey second = PuzzleKey.of(GridSize.NINE, Variant.CHAOS, Difficulty.TWO, 314L);
		
		assertEquals(PuzzleGenerator.partitionFor(first), PuzzleGenerator.partitionFor(second));
	}
	
	@Test
	void partitionFor_chaosKey_differentSeeds_yieldDifferentPartitions() {
		PuzzleKey first = PuzzleKey.of(GridSize.NINE, Variant.CHAOS, Difficulty.TWO, 1L);
		PuzzleKey second = PuzzleKey.of(GridSize.NINE, Variant.CHAOS, Difficulty.TWO, 2L);
		
		assertNotEquals(PuzzleGenerator.partitionFor(first), PuzzleGenerator.partitionFor(second));
	}
	
	@Test
	@Timeout(value = 120, unit = TimeUnit.SECONDS)
	void partitionFor_chaosKey_isThePartitionTheGeneratedPuzzleCarries() {
		for (GridSize size : new GridSize[] { GridSize.SIX, GridSize.NINE, GridSize.TWELVE }) {
			PuzzleKey key = PuzzleKey.of(size, Variant.CHAOS, Difficulty.THREE, FIXED_SEED);
			
			RegionPartition partition = PuzzleGenerator.partitionFor(key);
			RegionPartition generated = PuzzleGenerator.generate(key).puzzle().partition();
			
			assertEquals(size, partition.size(), "Wrong grid size for " + size);
			assertEquals(generated.regionCount(), partition.regionCount(), "Wrong region count for " + size);
			for (int regionIndex = 0; regionIndex < generated.regionCount(); regionIndex++) {
				assertArrayEquals(generated.region(regionIndex).cells(), partition.region(regionIndex).cells(),
					"Region " + regionIndex + " differs for " + size);
			}
			assertNotEquals(ClassicRegionPartition.of(size), partition, "Chaos partition is the classic box layout for " + size);
		}
	}
	
	@Test
	void partitionFor_nullKey_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> PuzzleGenerator.partitionFor(null));
	}
	
	@Test
	void fromGivens_generatedClassicGivens_rebuildTheSamePuzzleAndSolution() {
		PuzzleKey key = PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.THREE, FIXED_SEED);
		GeneratedPuzzle generated = PuzzleGenerator.generate(key);
		
		GeneratedPuzzle rebuilt = PuzzleGenerator.fromGivens(key, generated.puzzle().values());
		
		assertAll(
			() -> assertEquals(key, rebuilt.key(), "The key is not carried through"),
			() -> assertArrayEquals(generated.puzzle().values(), rebuilt.puzzle().values(), "Givens differ"),
			() -> assertEquals(generated.puzzle().partition(), rebuilt.puzzle().partition(), "Partitions differ"),
			() -> assertEquals(generated.puzzle(), rebuilt.puzzle(), "Puzzles differ"),
			() -> assertArrayEquals(generated.solution(), rebuilt.solution(), "The derived solution is not the original one")
		);
	}
	
	@Test
	void fromGivens_generatedChaosGivens_rebuildTheSamePuzzleAndSolution() {
		PuzzleKey key = PuzzleKey.of(GridSize.NINE, Variant.CHAOS, Difficulty.THREE, FIXED_SEED);
		GeneratedPuzzle generated = PuzzleGenerator.generate(key);
		
		GeneratedPuzzle rebuilt = PuzzleGenerator.fromGivens(key, generated.puzzle().values());
		
		assertAll(
			() -> assertEquals(Variant.CHAOS, rebuilt.puzzle().variant(), "The variant is not carried through"),
			() -> assertArrayEquals(generated.puzzle().values(), rebuilt.puzzle().values(), "Givens differ"),
			() -> assertEquals(generated.puzzle().partition(), rebuilt.puzzle().partition(), "Partitions differ"),
			() -> assertEquals(generated.puzzle(), rebuilt.puzzle(), "Puzzles differ"),
			() -> assertArrayEquals(generated.solution(), rebuilt.solution(), "The derived solution is not the original one")
		);
	}
	
	@Test
	void fromGivens_marksEveryClueAsAGivenAndLeavesTheHolesEmpty() {
		PuzzleKey key = PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.TWO, 99L);
		int[] givens = PuzzleGenerator.generate(key).puzzle().values();
		
		Puzzle rebuilt = PuzzleGenerator.fromGivens(key, givens).puzzle();
		
		assertGivensMarkedAndHolesEmptyInValidGrid(rebuilt, "rebuilt 9x9 classic");
	}
	
	@Test
	void fromGivens_aCompleteSolution_isAcceptedAsAUniquePuzzle() {
		PuzzleKey key = PuzzleKey.of(GridSize.FOUR, Variant.CLASSIC, Difficulty.ONE, 5L);
		GeneratedPuzzle generated = PuzzleGenerator.generate(key);
		
		GeneratedPuzzle rebuilt = PuzzleGenerator.fromGivens(key, generated.solution());
		
		assertAll(
			() -> assertTrue(rebuilt.puzzle().isSolved(), "A complete solution did not rebuild into a solved grid"),
			() -> assertArrayEquals(generated.solution(), rebuilt.solution(), "The derived solution differs")
		);
	}
	
	@Test
	void fromGivens_wrongLength_throwsIllegalArgumentException() {
		PuzzleKey key = PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.ONE, FIXED_SEED);
		
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> PuzzleGenerator.fromGivens(key, new int[80])),
			() -> assertThrows(IllegalArgumentException.class, () -> PuzzleGenerator.fromGivens(key, new int[82])),
			() -> assertThrows(IllegalArgumentException.class, () -> PuzzleGenerator.fromGivens(key, new int[0]))
		);
	}
	
	@Test
	void fromGivens_illegalDigit_throwsIllegalArgumentException() {
		PuzzleKey key = PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.ONE, FIXED_SEED);
		int[] givens = new int[81];
		givens[0] = 10;
		
		assertThrows(IllegalArgumentException.class, () -> PuzzleGenerator.fromGivens(key, givens));
	}
	
	@Test
	void fromGivens_givensWithoutAUniqueSolution_throwsIllegalArgumentException() {
		PuzzleKey key = PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.ONE, FIXED_SEED);
		int[] almostEmpty = new int[81];
		almostEmpty[0] = 1;
		
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> PuzzleGenerator.fromGivens(key, new int[81])),
			() -> assertThrows(IllegalArgumentException.class, () -> PuzzleGenerator.fromGivens(key, almostEmpty))
		);
	}
	
	@Test
	void fromGivens_givensThatContradictThemselves_throwsIllegalArgumentException() {
		PuzzleKey key = PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.ONE, FIXED_SEED);
		GeneratedPuzzle generated = PuzzleGenerator.generate(key);
		int[] givens = generated.puzzle().values();
		// Filling one hole with anything but the solution's digit leaves a grid with no solution at all.
		int hole = 0;
		while (givens[hole] != 0) {
			hole++;
		}
		givens[hole] = generated.solutionAt(hole) % 9 + 1;
		
		assertThrows(IllegalArgumentException.class, () -> PuzzleGenerator.fromGivens(key, givens));
	}
	
	@Test
	void fromGivens_nullKey_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> PuzzleGenerator.fromGivens(null, new int[81]));
	}
	
	@Test
	void fromGivens_nullGivens_throwsNullPointerException() {
		PuzzleKey key = PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.ONE, FIXED_SEED);
		
		assertThrows(NullPointerException.class, () -> PuzzleGenerator.fromGivens(key, null));
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
