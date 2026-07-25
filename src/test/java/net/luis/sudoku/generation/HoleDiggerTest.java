package net.luis.sudoku.generation;

import net.luis.sudoku.grid.*;
import net.luis.sudoku.rng.DeterministicRandom;
import net.luis.sudoku.solver.BacktrackingSolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link HoleDigger}.
 */
class HoleDiggerTest {
	
	private static final RegionPartition FOUR_CLASSIC = ClassicRegionPartition.of(GridSize.FOUR);
	private static final RegionPartition NINE_CLASSIC = ClassicRegionPartition.of(GridSize.NINE);
	
	/**
	 * A hand-verified solved 4x4 grid, row-major:
	 * <pre>
	 * 1 2 | 3 4
	 * 3 4 | 1 2
	 * ----+----
	 * 2 1 | 4 3
	 * 4 3 | 2 1
	 * </pre>
	 */
	private static final int[] SOLVED_FOUR = {
		1, 2, 3, 4,
		3, 4, 1, 2,
		2, 1, 4, 3,
		4, 3, 2, 1
	};
	
	/**
	 * A full 4x4 grid whose last row is {@code 4 3 1 2}: every row and every box holds 1..4, but column 2 holds
	 * {@code 3 1 4 1} and column 3 holds {@code 4 2 3 2}, so the grid is complete yet invalid.
	 */
	private static final int[] COMPLETE_INVALID_FOUR = {
		1, 2, 3, 4,
		3, 4, 1, 2,
		2, 1, 4, 3,
		4, 3, 1, 2
	};
	
	private static RegionPartition classicOf(GridSize size) {
		return ClassicRegionPartition.of(size);
	}
	
	private static int[] solutionOf(GridSize size) {
		Puzzle empty = Puzzle.empty(size, Variant.CLASSIC, classicOf(size));
		return BacktrackingSolver.solve(empty).orElseThrow(() -> new AssertionError("Empty " + size + " grid must be solvable"));
	}
	
	private static int uniqueSolutionCount(GridSize size, RegionPartition partition, int[] givens) {
		Puzzle puzzle = Puzzle.ofGivens(size, Variant.CLASSIC, partition, givens);
		return BacktrackingSolver.countSolutions(puzzle, 2);
	}
	
	private static int holeCount(int[] givens) {
		int holes = 0;
		for (int value : givens) {
			if (value == 0) {
				holes++;
			}
		}
		return holes;
	}
	
	private static void assertGivensMatchSolution(GridSize size, int[] givens, int[] solution) {
		for (int cellIndex = 0; cellIndex < givens.length; cellIndex++) {
			int given = givens[cellIndex];
			if (given != 0) {
				assertEquals(solution[cellIndex], given, "given at cell " + cellIndex + " must equal the solution");
			}
		}
	}
	
	private void assertDiggableSize(GridSize size) {
		this.assertDiggableSize(size, Integer.MAX_VALUE);
	}
	
	private void assertDiggableSize(GridSize size, int maxHoles) {
		RegionPartition partition = classicOf(size);
		int[] solution = solutionOf(size);
		int[] guard = solution.clone();
		long start = System.nanoTime();
		int[] givens = HoleDigger.dig(partition, solution, new DeterministicRandom(0xC0FFEEL), maxHoles);
		long millis = (System.nanoTime() - start) / 1_000_000L;
		int holes = holeCount(givens);
		System.out.println("dig-timing size=" + size + " cellCount=" + size.cellCount() + " holes=" + holes + " millis=" + millis);
		assertAll(
			() -> assertEquals(size.cellCount(), givens.length, "givens length must match the grid"),
			() -> assertArrayEquals(guard, solution, "the solution array must not be mutated"),
			() -> assertTrue(holes >= 1, "at least one hole must be dug for " + size),
			() -> assertEquals(1, uniqueSolutionCount(size, partition, givens), "the result must have exactly one solution"),
			() -> assertGivensMatchSolution(size, givens, solution)
		);
	}
	
	@Test
	void dig_sizeFour_producesUniqueGivensMatchingSolution() {
		this.assertDiggableSize(GridSize.FOUR);
	}
	
	@Test
	void dig_sizeSix_producesUniqueGivensMatchingSolution() {
		this.assertDiggableSize(GridSize.SIX);
	}
	
	@Test
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	void dig_sizeNine_producesUniqueGivensMatchingSolution() {
		this.assertDiggableSize(GridSize.NINE);
	}
	
	@Test
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	void dig_sizeTwelve_producesUniqueGivensMatchingSolution() {
		// Bounded to match how PuzzleGenerator digs 12x12: an unbounded dig of a near-minimal 12x12 grid can
		// take many seconds to prove unique, which is neither necessary nor deterministic to depend on here.
		this.assertDiggableSize(GridSize.TWELVE, 105);
	}
	
	@Test
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	void dig_sizeSixteen_producesUniqueGivensMatchingSolution() {
		// Bounded to match how PuzzleGenerator digs 16x16: an unbounded dig here runs for minutes because
		// proving a near-minimal 256-cell grid unique branches deeply even with the MRV solver.
		this.assertDiggableSize(GridSize.SIXTEEN, 140);
	}
	
	@Test
	void dig_handWrittenFour_producesUniqueSolution() {
		int[] givens = HoleDigger.dig(FOUR_CLASSIC, SOLVED_FOUR, new DeterministicRandom(7L));
		assertAll(
			() -> assertArrayEquals(new int[] { 1, 2, 3, 4, 3, 4, 1, 2, 2, 1, 4, 3, 4, 3, 2, 1 }, SOLVED_FOUR, "the solution array must not be mutated"),
			() -> assertTrue(holeCount(givens) >= 1, "at least one hole must be dug"),
			() -> assertEquals(1, uniqueSolutionCount(GridSize.FOUR, FOUR_CLASSIC, givens), "the result must have exactly one solution"),
			() -> assertGivensMatchSolution(GridSize.FOUR, givens, SOLVED_FOUR)
		);
	}
	
	@Test
	void dig_sameSeed_producesByteIdenticalGivens() {
		int[] solution = solutionOf(GridSize.NINE);
		int[] first = HoleDigger.dig(NINE_CLASSIC, solution, new DeterministicRandom(42L));
		int[] second = HoleDigger.dig(NINE_CLASSIC, solution, new DeterministicRandom(42L));
		assertArrayEquals(first, second, "the same seed must produce byte-identical givens");
	}
	
	@Test
	void dig_differentSeeds_producesDifferentGivens() {
		int[] solution = solutionOf(GridSize.NINE);
		int[] first = HoleDigger.dig(NINE_CLASSIC, solution, new DeterministicRandom(1L));
		int[] second = HoleDigger.dig(NINE_CLASSIC, solution, new DeterministicRandom(2L));
		assertFalse(Arrays.equals(first, second), "different seeds must produce different givens");
	}
	
	@Test
	void dig_maxHolesZero_returnsSolutionUnchanged() {
		int[] solution = solutionOf(GridSize.NINE);
		int[] givens = HoleDigger.dig(NINE_CLASSIC, solution, new DeterministicRandom(3L), 0);
		assertAll(
			() -> assertArrayEquals(solution, givens, "maxHoles == 0 must return the full solution"),
			() -> assertEquals(0, holeCount(givens), "maxHoles == 0 must leave no holes")
		);
	}
	
	@Test
	void dig_maxHolesFive_digsAtMostFiveHoles() {
		int[] solution = solutionOf(GridSize.NINE);
		int[] givens = HoleDigger.dig(NINE_CLASSIC, solution, new DeterministicRandom(4L), 5);
		assertTrue(holeCount(givens) <= 5, "maxHoles == 5 must dig at most 5 holes");
	}
	
	@Test
	void dig_maxHolesBounded_stillUniqueAndMatchesSolution() {
		int[] solution = solutionOf(GridSize.NINE);
		int[] givens = HoleDigger.dig(NINE_CLASSIC, solution, new DeterministicRandom(4L), 5);
		assertAll(
			() -> assertEquals(1, uniqueSolutionCount(GridSize.NINE, NINE_CLASSIC, givens), "a bounded dig must stay uniquely solvable"),
			() -> assertGivensMatchSolution(GridSize.NINE, givens, solution)
		);
	}
	
	@Test
	void dig_wrongLength_throwsIllegalArgument() {
		int[] tooShort = new int[GridSize.NINE.cellCount() - 1];
		Arrays.fill(tooShort, 1);
		assertThrows(IllegalArgumentException.class, () -> HoleDigger.dig(NINE_CLASSIC, tooShort, new DeterministicRandom(0L)));
	}
	
	@Test
	void dig_valueOutsideRange_throwsIllegalArgument() {
		int[] solution = SOLVED_FOUR.clone();
		solution[0] = 5;
		assertThrows(IllegalArgumentException.class, () -> HoleDigger.dig(FOUR_CLASSIC, solution, new DeterministicRandom(0L)));
	}
	
	@Test
	void dig_zeroValue_throwsIllegalArgument() {
		int[] solution = SOLVED_FOUR.clone();
		solution[0] = 0;
		assertThrows(IllegalArgumentException.class, () -> HoleDigger.dig(FOUR_CLASSIC, solution, new DeterministicRandom(0L)));
	}
	
	@Test
	void dig_completeButInvalidGrid_throwsIllegalArgument() {
		assertThrows(IllegalArgumentException.class, () -> HoleDigger.dig(FOUR_CLASSIC, COMPLETE_INVALID_FOUR, new DeterministicRandom(0L)));
	}
	
	@Test
	void dig_negativeMaxHoles_throwsIllegalArgument() {
		assertThrows(IllegalArgumentException.class, () -> HoleDigger.dig(FOUR_CLASSIC, SOLVED_FOUR, new DeterministicRandom(0L), -1));
	}
	
	@Test
	void dig_completeInvalidConstant_isReallyCompleteButInvalid() {
		Puzzle puzzle = Puzzle.ofGivens(GridSize.FOUR, Variant.CLASSIC, FOUR_CLASSIC, COMPLETE_INVALID_FOUR);
		assertAll(
			() -> assertTrue(puzzle.isComplete(), "the invalid grid must still be complete"),
			() -> assertFalse(puzzle.isValid(), "the invalid grid must carry a conflict")
		);
	}
}
