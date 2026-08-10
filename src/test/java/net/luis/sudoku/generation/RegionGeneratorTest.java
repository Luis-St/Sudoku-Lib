package net.luis.sudoku.generation;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.grid.*;
import net.luis.sudoku.key.KeyDerivation;
import net.luis.sudoku.key.PuzzleKey;
import net.luis.sudoku.rng.DeterministicRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RegionGeneratorTest {
	
	private static final GridSize[] CHAOS_SIZES = { GridSize.SIX, GridSize.NINE, GridSize.TWELVE, GridSize.SIXTEEN };
	
	private static RegionPartition generate(GridSize size, long seed) {
		return RegionGenerator.generateChaos(size, new DeterministicRandom(seed));
	}
	
	private static boolean isConnected(GridSize size, Region region) {
		int n = size.n();
		int[] cells = region.cells();
		boolean[] inRegion = new boolean[size.cellCount()];
		for (int cell : cells) {
			inRegion[cell] = true;
		}
		boolean[] visited = new boolean[size.cellCount()];
		int[] stack = new int[cells.length];
		int top = 0;
		stack[top++] = cells[0];
		visited[cells[0]] = true;
		int reached = 1;
		while (top > 0) {
			int cell = stack[--top];
			int row = cell / n;
			int column = cell % n;
			int[] neighbors = { row > 0 ? cell - n : -1, row < n - 1 ? cell + n : -1, column > 0 ? cell - 1 : -1, column < n - 1 ? cell + 1 : -1 };
			for (int neighbor : neighbors) {
				if (neighbor >= 0 && inRegion[neighbor] && !visited[neighbor]) {
					visited[neighbor] = true;
					stack[top++] = neighbor;
					reached++;
				}
			}
		}
		return reached == cells.length;
	}
	
	/** True when the region exactly fills its bounding box - a full row and a full column are the 1-wide cases. */
	private static boolean isRectangular(GridSize size, Region region) {
		int n = size.n();
		int minRow = Integer.MAX_VALUE;
		int maxRow = Integer.MIN_VALUE;
		int minColumn = Integer.MAX_VALUE;
		int maxColumn = Integer.MIN_VALUE;
		for (int position = 0; position < region.size(); position++) {
			int cell = region.cell(position);
			minRow = Math.min(minRow, cell / n);
			maxRow = Math.max(maxRow, cell / n);
			minColumn = Math.min(minColumn, cell % n);
			maxColumn = Math.max(maxColumn, cell % n);
		}
		return (maxRow - minRow + 1) * (maxColumn - minColumn + 1) == region.size();
	}
	
	private static boolean solves(GridSize size, RegionPartition partition, int[] solution) {
		Puzzle puzzle = Puzzle.ofGivens(size, Variant.CHAOS, partition, solution);
		return puzzle.isSolved();
	}
	
	@Test
	@Timeout(value = 120, unit = TimeUnit.SECONDS)
	void generateChaos_everySupportedSize_hasNRegionsOfNContiguousCellsCoveringEveryCellOnce() {
		for (GridSize size : CHAOS_SIZES) {
			RegionPartition partition = generate(size, 12345L);
			int n = size.n();
			int[] regionOfCount = new int[size.cellCount()];
			
			assertEquals(n, partition.regionCount(), "Wrong region count for " + size);
			for (Region region : partition.regions()) {
				assertEquals(n, region.size(), "Region does not have n cells for " + size);
				assertTrue(isConnected(size, region), "Region is not contiguous for " + size);
				for (int cell : region.cells()) {
					regionOfCount[cell]++;
				}
			}
			for (int cell = 0; cell < size.cellCount(); cell++) {
				assertEquals(1, regionOfCount[cell], "Cell " + cell + " is not covered exactly once for " + size);
			}
		}
	}
	
	@Test
	@Timeout(value = 120, unit = TimeUnit.SECONDS)
	void generateChaos_everySupportedSize_hasNoRectangularRegionAndIsFillable() {
		for (GridSize size : CHAOS_SIZES) {
			RegionGenerator.ChaosLayout layout = RegionGenerator.generateChaosLayout(size, new DeterministicRandom(777L));
			
			for (Region region : layout.partition().regions()) {
				// Covers full rows and columns (the 1-wide rectangles) as well as classic box shapes.
				assertFalse(isRectangular(size, region), "A region is a plain rectangle for " + size);
			}
			// Fillability is guaranteed by construction: the layout carries a solution valid under the partition.
			// Confirm that solution really is a completion — every region holds 1..n exactly once, and rows/columns
			// stay valid — rather than solving the empty grid, which a jigsaw can make ruinously slow.
			assertTrue(solves(size, layout.partition(), layout.solution()),
				"Carried solution does not fill the chaos partition for " + size);
		}
	}
	
	@Test
	@Timeout(value = 120, unit = TimeUnit.SECONDS)
	void generateChaos_isNotTheClassicBoxLayout() {
		for (GridSize size : CHAOS_SIZES) {
			assertNotEquals(ClassicRegionPartition.of(size), generate(size, 42L),
				"Chaos partition is identical to the classic box layout for " + size);
		}
	}
	
	@Test
	void generateChaos_sameSeed_isDeterministic() {
		for (GridSize size : CHAOS_SIZES) {
			assertEquals(generate(size, 2024L), generate(size, 2024L), "Chaos generation is not deterministic for " + size);
		}
	}
	
	@Test
	void generateChaos_differentSeeds_yieldDifferentPartitions() {
		assertNotEquals(generate(GridSize.NINE, 1L), generate(GridSize.NINE, 2L));
	}
	
	@Test
	void generateChaos_fourByFour_throwsBecauseChaosIsUnsupported() {
		assertThrows(IllegalArgumentException.class, () -> generate(GridSize.FOUR, 1L));
	}
	
	/**
	 * The end-to-end regression for the unbounded fill. Seeds 8 and 22 at 16x16 chaos band five are the two seeds
	 * from a 32-seed sweep whose layout generation never returned: one of them was left running for 34 minutes at
	 * full CPU, still inside {@code SolutionFiller}, before it was killed. Both complete in about 1.3 seconds now.
	 * The timeout is what makes this a test rather than a hang, and it is deliberately far above the measured cost
	 * so that a slow machine does not turn it into a flake.
	 */
	@Test
	@Timeout(value = 60, unit = TimeUnit.SECONDS)
	void generateChaosLayout_seedsThatPreviouslyNeverReturned_complete() {
		assertAll(java.util.stream.LongStream.of(8L, 22L).mapToObj(seed -> () -> {
			PuzzleKey key = PuzzleKey.of(GridSize.SIXTEEN, Variant.CHAOS, Difficulty.FIVE, seed);
			RegionGenerator.ChaosLayout layout = RegionGenerator.generateChaosLayout(GridSize.SIXTEEN, KeyDerivation.randomFor(key));
			
			assertNotNull(layout, "No layout for seed " + seed);
			assertTrue(Puzzle.ofGivens(GridSize.SIXTEEN, Variant.CHAOS, layout.partition(), layout.solution()).isSolved(), "The layout for seed " + seed + " does not carry a solved grid");
		}));
	}
	
	@Test
	void generateChaos_nullArguments_throw() {
		assertAll(
			() -> assertThrows(NullPointerException.class, () -> RegionGenerator.generateChaos(null, new DeterministicRandom(1L))),
			() -> assertThrows(NullPointerException.class, () -> RegionGenerator.generateChaos(GridSize.NINE, null))
		);
	}
}
