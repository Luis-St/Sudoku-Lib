package net.luis.sudoku.solver;

import net.luis.sudoku.grid.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link BacktrackingSolver}.
 */
class BacktrackingSolverTest {
	
	private static final String[] UNIQUE_NINE_GIVENS = {
		"530070000",
		"600195000",
		"098000060",
		"800060003",
		"400803001",
		"700020006",
		"060000280",
		"000419005",
		"000080079"
	};
	private static final String[] UNIQUE_NINE_SOLUTION = {
		"534678912",
		"672195348",
		"198342567",
		"859761423",
		"426853791",
		"713924856",
		"961537284",
		"287419635",
		"345286179"
	};
	/**
	 * The solved grid above with an unavoidable rectangle removed: the cells (3,5), (3,8), (4,5) and (4,8) hold
	 * 1, 3, 3 and 1, so both assignments complete the grid and the puzzle has exactly two solutions.
	 */
	private static final int[][] TWO_SOLUTION_HOLES = { { 3, 5 }, { 3, 8 }, { 4, 5 }, { 4, 8 } };
	
	private static int[] parse(String[] rows) {
		int n = rows.length;
		int[] values = new int[n * n];
		for (int row = 0; row < n; row++) {
			for (int column = 0; column < n; column++) {
				values[row * n + column] = rows[row].charAt(column) - '0';
			}
		}
		return values;
	}
	
	private static Puzzle uniqueNinePuzzle() {
		return Puzzle.classicOfGivens(GridSize.NINE, parse(UNIQUE_NINE_GIVENS));
	}
	
	private static Puzzle twoSolutionNinePuzzle() {
		int[] givens = parse(UNIQUE_NINE_SOLUTION);
		for (int[] hole : TWO_SOLUTION_HOLES) {
			givens[hole[0] * 9 + hole[1]] = 0;
		}
		return Puzzle.classicOfGivens(GridSize.NINE, givens);
	}
	
	/**
	 * Builds a non-classic, contiguous jigsaw partition for the 6x6 grid. None of its six regions is a classic
	 * 3x2 box, so a solver that silently assumed boxes would produce a different grid.
	 */
	private static RegionPartition jigsawSix() {
		int[][] layout = {
			{ 0, 0, 0, 1, 1, 1 },
			{ 0, 0, 2, 2, 1, 1 },
			{ 3, 0, 2, 2, 1, 4 },
			{ 3, 3, 2, 2, 4, 4 },
			{ 3, 3, 5, 5, 5, 4 },
			{ 3, 5, 5, 5, 4, 4 }
		};
		int[][] cells = new int[6][6];
		int[] filled = new int[6];
		for (int row = 0; row < 6; row++) {
			for (int column = 0; column < 6; column++) {
				int regionIndex = layout[row][column];
				cells[regionIndex][filled[regionIndex]++] = row * 6 + column;
			}
		}
		List<Region> regions = new ArrayList<>(6);
		for (int regionIndex = 0; regionIndex < 6; regionIndex++) {
			regions.add(new Region(cells[regionIndex]));
		}
		return new RegionPartition(GridSize.SIX, regions);
	}
	
	private static boolean isSolvedGrid(GridSize size, Variant variant, RegionPartition partition, int[] values) {
		Puzzle puzzle = Puzzle.empty(size, variant, partition);
		for (int cellIndex = 0; cellIndex < values.length; cellIndex++) {
			puzzle.setValue(cellIndex, values[cellIndex]);
		}
		return puzzle.isSolved();
	}
	
	/**
	 * Builds a puzzle without a single duplicate, whose cell (0,8) still has every candidate eliminated: its row
	 * already holds 1 to 8 and its column already holds 9.
	 */
	private static Puzzle strangledCellPuzzle() {
		int[] givens = new int[81];
		for (int column = 0; column < 8; column++) {
			givens[column] = column + 1;
		}
		givens[9 + 8] = 9;
		Puzzle puzzle = Puzzle.classicOfGivens(GridSize.NINE, givens);
		assertTrue(puzzle.isValid(), "The fixture must not contain a direct duplicate");
		return puzzle;
	}
	
	private static Puzzle duplicatePuzzle() {
		int[] givens = parse(UNIQUE_NINE_GIVENS);
		givens[2] = 5;
		Puzzle puzzle = Puzzle.classicOfGivens(GridSize.NINE, givens);
		assertFalse(puzzle.isValid(), "The fixture must contain a direct duplicate");
		return puzzle;
	}
	
	@Test
	void solve_uniqueNinePuzzle_returnsTheKnownSolution() {
		Optional<int[]> solution = BacktrackingSolver.solve(uniqueNinePuzzle());
		
		assertTrue(solution.isPresent());
		assertArrayEquals(parse(UNIQUE_NINE_SOLUTION), solution.orElseThrow());
	}
	
	@Test
	void solve_uniqueNinePuzzle_keepsEveryGiven() {
		int[] givens = parse(UNIQUE_NINE_GIVENS);
		int[] solution = BacktrackingSolver.solve(uniqueNinePuzzle()).orElseThrow();
		
		for (int cellIndex = 0; cellIndex < givens.length; cellIndex++) {
			if (givens[cellIndex] != 0) {
				assertEquals(givens[cellIndex], solution[cellIndex], "Cell " + cellIndex + " lost its given");
			}
		}
		assertTrue(isSolvedGrid(GridSize.NINE, Variant.CLASSIC, ClassicRegionPartition.of(GridSize.NINE), solution));
	}
	
	@Test
	void countSolutions_uniqueNinePuzzle_returnsOne() {
		assertEquals(1, BacktrackingSolver.countSolutions(uniqueNinePuzzle(), 2));
	}
	
	@Test
	void countSolutions_puzzleWithTwoSolutions_returnsTwo() {
		assertEquals(2, BacktrackingSolver.countSolutions(twoSolutionNinePuzzle(), 2));
	}
	
	@Test
	void countSolutions_puzzleWithTwoSolutionsAndHigherCap_stillReturnsTwo() {
		assertEquals(2, BacktrackingSolver.countSolutions(twoSolutionNinePuzzle(), 25));
	}
	
	@Test
	void countSolutions_puzzleWithTwoSolutionsAndCapOne_returnsOne() {
		assertEquals(1, BacktrackingSolver.countSolutions(twoSolutionNinePuzzle(), 1));
	}
	
	@Test
	@Timeout(value = 10, unit = TimeUnit.SECONDS)
	void countSolutions_emptyNineGrid_abortsAtTheCap() {
		Puzzle puzzle = Puzzle.empty(GridSize.NINE, Variant.CLASSIC, ClassicRegionPartition.of(GridSize.NINE));
		
		assertEquals(2, BacktrackingSolver.countSolutions(puzzle, 2));
	}
	
	@Test
	@Timeout(value = 10, unit = TimeUnit.SECONDS)
	void countSolutions_nearlyEmptyNineGridWithHighCap_abortsAtTheCap() {
		Puzzle puzzle = Puzzle.empty(GridSize.NINE, Variant.CLASSIC, ClassicRegionPartition.of(GridSize.NINE));
		puzzle.setValue(0, 1);
		
		assertEquals(7, BacktrackingSolver.countSolutions(puzzle, 7));
	}
	
	@Test
	void solve_emptyFourGrid_returnsSolvedGrid() {
		RegionPartition partition = ClassicRegionPartition.of(GridSize.FOUR);
		Puzzle puzzle = Puzzle.empty(GridSize.FOUR, Variant.CLASSIC, partition);
		
		int[] solution = BacktrackingSolver.solve(puzzle).orElseThrow();
		
		assertArrayEquals(parse(new String[] { "1234", "3412", "2143", "4321" }), solution);
		assertTrue(isSolvedGrid(GridSize.FOUR, Variant.CLASSIC, partition, solution));
	}
	
	@Test
	void countSolutions_emptyFourGrid_returnsTwo() {
		Puzzle puzzle = Puzzle.empty(GridSize.FOUR, Variant.CLASSIC, ClassicRegionPartition.of(GridSize.FOUR));
		
		assertEquals(2, BacktrackingSolver.countSolutions(puzzle, 2));
	}
	
	@Test
	void solve_fullySolvedGrid_returnsTheIdenticalGrid() {
		int[] solved = parse(UNIQUE_NINE_SOLUTION);
		Puzzle puzzle = Puzzle.classicOfGivens(GridSize.NINE, solved);
		
		Optional<int[]> solution = BacktrackingSolver.solve(puzzle);
		
		assertTrue(solution.isPresent());
		assertArrayEquals(solved, solution.orElseThrow());
	}
	
	@Test
	void countSolutions_fullySolvedGrid_returnsOne() {
		Puzzle puzzle = Puzzle.classicOfGivens(GridSize.NINE, parse(UNIQUE_NINE_SOLUTION));
		
		assertEquals(1, BacktrackingSolver.countSolutions(puzzle, 2));
	}
	
	@Test
	void solve_cellWithoutAnyCandidate_returnsEmpty() {
		assertTrue(BacktrackingSolver.solve(strangledCellPuzzle()).isEmpty());
	}
	
	@Test
	void countSolutions_cellWithoutAnyCandidate_returnsZero() {
		assertEquals(0, BacktrackingSolver.countSolutions(strangledCellPuzzle(), 2));
	}
	
	@Test
	void solve_puzzleWithDuplicateInRow_returnsEmpty() {
		assertTrue(BacktrackingSolver.solve(duplicatePuzzle()).isEmpty());
	}
	
	@Test
	void countSolutions_puzzleWithDuplicateInRow_returnsZero() {
		assertEquals(0, BacktrackingSolver.countSolutions(duplicatePuzzle(), 2));
	}
	
	@Test
	void solve_puzzleWithDuplicateInRegion_returnsEmpty() {
		int[] givens = new int[81];
		givens[0] = 5;
		givens[10] = 5;
		Puzzle puzzle = Puzzle.classicOfGivens(GridSize.NINE, givens);
		
		assertTrue(BacktrackingSolver.solve(puzzle).isEmpty());
		assertEquals(0, BacktrackingSolver.countSolutions(puzzle, 2));
	}
	
	@Test
	void solve_anyPuzzle_doesNotMutateThePuzzle() {
		Puzzle puzzle = uniqueNinePuzzle();
		int[] before = puzzle.values();
		
		BacktrackingSolver.solve(puzzle);
		
		assertArrayEquals(before, puzzle.values());
		assertArrayEquals(parse(UNIQUE_NINE_GIVENS), puzzle.values());
	}
	
	@Test
	void countSolutions_anyPuzzle_doesNotMutateThePuzzle() {
		Puzzle puzzle = twoSolutionNinePuzzle();
		int[] before = puzzle.values();
		
		BacktrackingSolver.countSolutions(puzzle, 2);
		
		assertArrayEquals(before, puzzle.values());
	}
	
	@Test
	void solve_emptyGrid_doesNotMutateThePuzzle() {
		Puzzle puzzle = Puzzle.empty(GridSize.SIX, Variant.CLASSIC, ClassicRegionPartition.of(GridSize.SIX));
		
		BacktrackingSolver.solve(puzzle);
		
		assertArrayEquals(new int[36], puzzle.values());
	}
	
	@Test
	void solve_returnedArray_isNotSharedBetweenCalls() {
		Puzzle puzzle = uniqueNinePuzzle();
		
		int[] first = BacktrackingSolver.solve(puzzle).orElseThrow();
		int[] second = BacktrackingSolver.solve(puzzle).orElseThrow();
		
		assertNotSame(first, second);
		assertArrayEquals(first, second);
	}
	
	@Test
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	void solve_theSamePuzzleFiftyTimes_returnsByteIdenticalArrays() {
		Puzzle puzzle = Puzzle.empty(GridSize.NINE, Variant.CLASSIC, ClassicRegionPartition.of(GridSize.NINE));
		int[] expected = BacktrackingSolver.solve(puzzle).orElseThrow();
		
		for (int run = 0; run < 50; run++) {
			assertArrayEquals(expected, BacktrackingSolver.solve(puzzle).orElseThrow(), "Run " + run + " differed");
		}
	}
	
	@Test
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	void solve_theSameGivenPuzzleFiftyTimes_returnsByteIdenticalArrays() {
		int[] expected = BacktrackingSolver.solve(twoSolutionNinePuzzle()).orElseThrow();
		
		for (int run = 0; run < 50; run++) {
			assertArrayEquals(expected, BacktrackingSolver.solve(twoSolutionNinePuzzle()).orElseThrow(), "Run " + run + " differed");
		}
	}
	
	@Test
	@Timeout(value = 60, unit = TimeUnit.SECONDS)
	void solve_emptyGridOfEverySize_returnsSolvedGrid() {
		for (GridSize size : GridSize.values()) {
			RegionPartition partition = ClassicRegionPartition.of(size);
			Puzzle puzzle = Puzzle.empty(size, Variant.CLASSIC, partition);
			
			int[] solution = BacktrackingSolver.solve(puzzle).orElseThrow(() -> new AssertionError("No solution for " + size));
			
			assertEquals(size.cellCount(), solution.length, "Wrong length for " + size);
			assertTrue(isSolvedGrid(size, Variant.CLASSIC, partition, solution), "Not solved for " + size);
		}
	}
	
	@Test
	@Timeout(value = 60, unit = TimeUnit.SECONDS)
	void countSolutions_emptyGridOfEverySize_returnsTwo() {
		for (GridSize size : GridSize.values()) {
			Puzzle puzzle = Puzzle.empty(size, Variant.CLASSIC, ClassicRegionPartition.of(size));
			
			assertEquals(2, BacktrackingSolver.countSolutions(puzzle, 2), "Wrong count for " + size);
		}
	}
	
	@Test
	void solve_jigsawPartition_honoursTheCustomRegionsInsteadOfBoxes() {
		RegionPartition partition = jigsawSix();
		Puzzle puzzle = Puzzle.empty(GridSize.SIX, Variant.CHAOS, partition);
		
		int[] solution = BacktrackingSolver.solve(puzzle).orElseThrow();
		
		assertArrayEquals(parse(new String[] { "123456", "451623", "264315", "645231", "316542", "532164" }), solution);
		assertTrue(isSolvedGrid(GridSize.SIX, Variant.CHAOS, partition, solution), "The jigsaw regions were not honoured");
		assertFalse(isSolvedGrid(GridSize.SIX, Variant.CLASSIC, ClassicRegionPartition.of(GridSize.SIX), solution), "The solver fell back to classic boxes");
	}
	
	@Test
	void solve_jigsawPartition_fillsEveryRegionWithEveryDigit() {
		RegionPartition partition = jigsawSix();
		Puzzle puzzle = Puzzle.empty(GridSize.SIX, Variant.CHAOS, partition);
		
		int[] solution = BacktrackingSolver.solve(puzzle).orElseThrow();
		
		for (int regionIndex = 0; regionIndex < partition.regionCount(); regionIndex++) {
			Region region = partition.region(regionIndex);
			int mask = 0;
			for (int position = 0; position < region.size(); position++) {
				mask |= 1 << solution[region.cell(position)];
			}
			assertEquals(0b1111110, mask, "Region " + regionIndex + " does not hold every digit exactly once");
		}
	}
	
	@Test
	void countSolutions_jigsawPartition_returnsTwoForAnEmptyGrid() {
		Puzzle puzzle = Puzzle.empty(GridSize.SIX, Variant.CHAOS, jigsawSix());
		
		assertEquals(2, BacktrackingSolver.countSolutions(puzzle, 2));
	}
	
	@Test
	void solve_jigsawPartitionUnsolvableUnderItsRegions_returnsEmpty() {
		RegionPartition partition = jigsawSix();
		int[] givens = new int[36];
		Region region = partition.region(0);
		givens[region.cell(0)] = 4;
		givens[region.cell(5)] = 4;
		Puzzle puzzle = Puzzle.ofGivens(GridSize.SIX, Variant.CHAOS, partition, givens);
		
		assertTrue(BacktrackingSolver.solve(puzzle).isEmpty());
		assertEquals(0, BacktrackingSolver.countSolutions(puzzle, 2));
	}
	
	@Test
	void countSolutions_capBelowOne_throwsIllegalArgumentException() {
		Puzzle puzzle = uniqueNinePuzzle();
		
		assertThrows(IllegalArgumentException.class, () -> BacktrackingSolver.countSolutions(puzzle, 0));
		assertThrows(IllegalArgumentException.class, () -> BacktrackingSolver.countSolutions(puzzle, -1));
		assertThrows(IllegalArgumentException.class, () -> BacktrackingSolver.countSolutions(puzzle, Integer.MIN_VALUE));
	}
	
	@Test
	void countSolutions_capOfOne_returnsOneForASolvablePuzzle() {
		assertEquals(1, BacktrackingSolver.countSolutions(uniqueNinePuzzle(), 1));
	}
}
