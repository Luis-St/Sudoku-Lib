package net.luis.sudoku.generation;

import net.luis.sudoku.grid.*;
import net.luis.sudoku.rng.DeterministicRandom;
import net.luis.sudoku.solver.BacktrackingSolver;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link SolutionFiller}.
 */
class SolutionFillerTest {
	
	private static RegionPartition classic(GridSize size) {
		return ClassicRegionPartition.of(size);
	}
	
	/**
	 * Builds a non-classic, contiguous jigsaw partition for the 6x6 grid. None of its six regions is a classic
	 * 3x2 box, so a filler that silently assumed boxes would produce a different grid or fail to honour the layout.
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
	
	/**
	 * Builds a deliberately unfillable 4x4 partition: the classic box layout with the two middle cells of the top
	 * row (indices 1 and 2) swapped between the top-left and top-right boxes. The bottom two boxes stay classic.
	 * <p>
	 *     That single swap makes the top two rows impossible to complete, so the whole grid admits no solution. The
	 *     {@code unfillablePartition_*} tests cross-check that claim against {@link BacktrackingSolver#solve(Puzzle)}
	 *     on the empty puzzle, exactly as required, so the fixture cannot silently rot into a fillable one.
	 * </p>
	 */
	private static RegionPartition unfillableFour() {
		List<Region> regions = List.of(
			new Region(0, 2, 4, 5),
			new Region(1, 3, 6, 7),
			new Region(8, 9, 12, 13),
			new Region(10, 11, 14, 15)
		);
		return new RegionPartition(GridSize.FOUR, regions);
	}
	
	private static boolean isSolved(GridSize size, RegionPartition partition, int[] values) {
		return Puzzle.ofGivens(size, Variant.CLASSIC, partition, values).isSolved();
	}
	
	@Test
	void fill_everyClassicSize_producesACompleteValidSolution() {
		assertAll(Arrays.stream(GridSize.values()).map(size -> () -> {
			RegionPartition partition = classic(size);
			Optional<int[]> solution = SolutionFiller.fill(partition, new DeterministicRandom(42L));
			
			assertTrue(solution.isPresent(), "No fill for " + size);
			int[] values = solution.orElseThrow();
			assertEquals(size.cellCount(), values.length, "Wrong length for " + size);
			assertTrue(isSolved(size, partition, values), "Fill for " + size + " is not a solved grid");
		}));
	}
	
	@Test
	void fill_sameSeed_producesByteIdenticalGrids() {
		assertAll(Arrays.stream(GridSize.values()).map(size -> () -> {
			RegionPartition partition = classic(size);
			int[] first = SolutionFiller.fill(partition, new DeterministicRandom(7L)).orElseThrow();
			int[] second = SolutionFiller.fill(partition, new DeterministicRandom(7L)).orElseThrow();
			
			assertArrayEquals(first, second, "Same seed diverged for " + size);
		}));
	}
	
	@Test
	void fill_differentSeeds_produceDifferentGrids() {
		RegionPartition partition = classic(GridSize.NINE);
		long[][] seedPairs = { { 1L, 2L }, { 100L, 200L }, { -5L, 5L }, { 12345L, 67890L } };
		
		assertAll(Arrays.stream(seedPairs).map(pair -> () -> {
			int[] first = SolutionFiller.fill(partition, new DeterministicRandom(pair[0])).orElseThrow();
			int[] second = SolutionFiller.fill(partition, new DeterministicRandom(pair[1])).orElseThrow();
			
			assertFalse(Arrays.equals(first, second), "Seeds " + pair[0] + " and " + pair[1] + " gave the same grid");
		}));
	}
	
	@Test
	void fill_nonClassicPartition_producesACompleteValidSolution() {
		RegionPartition partition = jigsawSix();
		Optional<int[]> solution = SolutionFiller.fill(partition, new DeterministicRandom(99L));
		
		assertTrue(solution.isPresent());
		assertTrue(isSolved(GridSize.SIX, partition, solution.orElseThrow()));
	}
	
	@Test
	void fill_unfillablePartition_returnsEmpty() {
		RegionPartition partition = unfillableFour();
		
		assertAll(
			() -> assertTrue(SolutionFiller.fill(partition, new DeterministicRandom(1L)).isEmpty(), "Filler completed an unfillable partition"),
			() -> assertTrue(SolutionFiller.fill(partition, new DeterministicRandom(123456L)).isEmpty(), "Filler completed an unfillable partition for another seed")
		);
	}
	
	@Test
	void fill_unfillablePartition_agreesWithTheSolverOnTheEmptyPuzzle() {
		RegionPartition partition = unfillableFour();
		Puzzle empty = Puzzle.empty(GridSize.FOUR, Variant.CLASSIC, partition);
		
		assertTrue(BacktrackingSolver.solve(empty).isEmpty(), "Fixture is not actually unfillable");
	}
	
	/**
	 * Catches the return of the unbounded search. Seed 120 on the classic 16x16 partition needs more than the
	 * production node budget on its first attempt, so reaching a grid at all proves the restart path ran and
	 * recovered. The companion test below is what stops this from passing vacuously: it pins that the very same
	 * seed does <em>not</em> finish inside one budget, so if the budget were ever raised until seed 120 fitted
	 * under it, that test fails and this one stops meaning anything.
	 */
	@Test
	void fill_seedExceedingTheNodeBudget_restartsAndReturnsACompleteGrid() {
		RegionPartition partition = classic(GridSize.SIXTEEN);
		Optional<int[]> solution = SolutionFiller.fill(partition, new DeterministicRandom(120L));

		assertTrue(solution.isPresent(), "The restart path gave up on a fillable partition");
		assertTrue(isSolved(GridSize.SIXTEEN, partition, solution.orElseThrow()), "The restart path returned an incomplete grid");
	}

	/**
	 * The non-vacuity guard for the test above: one attempt at the production budget really is not enough for
	 * seed 120, so the empty result here is the budget being spent rather than the partition being unfillable.
	 */
	@Test
	void fill_seedExceedingTheNodeBudgetWithoutRestarts_returnsEmpty() {
		RegionPartition partition = classic(GridSize.SIXTEEN);

		assertTrue(SolutionFiller.fill(partition, new DeterministicRandom(120L), 10_000_000L, 0).isEmpty(), "Seed 120 no longer exceeds one node budget, so the restart regression test is vacuous");
	}

	/**
	 * Drives the restart path on a budget small enough to hit constantly, so the mechanics are covered without
	 * paying for the ten million node budget. Seed 1 blows a thousand-node budget on its first attempt.
	 */
	@Test
	void fill_injectedBudgetTooSmallForOneAttempt_restartsAndReturnsACompleteGrid() {
		RegionPartition partition = classic(GridSize.SIXTEEN);

		assertTrue(SolutionFiller.fill(partition, new DeterministicRandom(1L), 1_000L, 0).isEmpty(), "Seed 1 fits inside a thousand nodes, so this test no longer forces a restart");

		Optional<int[]> solution = SolutionFiller.fill(partition, new DeterministicRandom(1L), 1_000L, 8);
		assertTrue(solution.isPresent(), "Restarts did not recover from a tiny budget");
		assertTrue(isSolved(GridSize.SIXTEEN, partition, solution.orElseThrow()), "The restart path returned an incomplete grid");
	}

	/**
	 * A search that proves the partition unfillable must not be retried. Restarting a proof cannot change it, and
	 * spending every restart on it would make an unfillable chaos partition cost nine full searches instead of one.
	 * A budget of one node would exhaust on any partition, so passing a generous one is what makes this meaningful.
	 */
	@Test
	void fill_unfillablePartition_doesNotSpendItsRestarts() {
		RegionPartition partition = unfillableFour();
		DeterministicRandom random = new DeterministicRandom(1L);

		assertTrue(SolutionFiller.fill(partition, random, 10_000_000L, 8).isEmpty(), "Filler completed an unfillable partition");

		// One attempt on a 4x4 draws a handful of values. Nine would draw noticeably more, so the stream position
		// is the cheapest available witness that only one search ran.
		DeterministicRandom single = new DeterministicRandom(1L);
		SolutionFiller.fill(partition, single, 10_000_000L, 0);
		assertEquals(single.nextLong(), random.nextLong(), "The unfillable partition was searched more than once");
	}

	@Test
	void fill_sameSeed_consumesTheSameNumberOfDraws() {
		assertAll(Arrays.stream(GridSize.values()).map(size -> () -> {
			RegionPartition partition = classic(size);
			DeterministicRandom first = new DeterministicRandom(2024L);
			DeterministicRandom second = new DeterministicRandom(2024L);
			SolutionFiller.fill(partition, first);
			SolutionFiller.fill(partition, second);
			
			assertEquals(first.nextLong(), second.nextLong(), "Draw counts diverged for " + size);
		}));
	}
}
