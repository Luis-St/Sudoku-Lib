package net.luis.sudoku.generation;

import net.luis.sudoku.grid.*;
import net.luis.sudoku.rng.DeterministicRandom;

import java.util.*;

/**
 * Builds the irregular but contiguous region partition of a chaos (jigsaw) puzzle by perturbing the classic layout
 * while keeping a known solution valid.
 * <p>
 *     This is spec §3.3 step 1 for {@link Variant#CHAOS}. Proving an arbitrary contiguous partition fillable (spec's
 *     second acceptance check) is cheap when it succeeds but astronomically expensive to <i>dis</i>prove for the many
 *     jigsaw layouts that admit no completion, so this generator sidesteps the problem entirely: it keeps a fixed,
 *     fully solved grid valid at every step, which makes every partition it emits fillable <b>by construction</b>.
 * </p>
 * <p>
 *     A random complete solution for the classic box layout is generated first with the {@link SolutionFiller}. The
 *     partition then starts as the classic boxes and is jumbled with many random <b>swaps</b>: one boundary cell moves
 *     from region {@code A} into an adjacent region {@code B} and a different boundary cell moves from {@code B} back
 *     into {@code A}. A swap is kept only if both regions stay contiguous <i>and</i> both still hold the digits
 *     {@code 1..n} exactly once under the fixed solution — so that solution remains a valid completion of the new
 *     partition, and rows and columns are untouched throughout. Because the two moved cells sit at different points on
 *     the shared border, the swap grows an interlocking jagged edge rather than collapsing back; sizes are preserved
 *     exactly since each region loses one cell and gains one.
 * </p>
 * <p>
 *     The only remaining acceptance check is the cheap, local one (spec §3.3 step 1): no region may be exactly a full
 *     row or full column, which adds no information beyond the line constraint. A partition that trips it is retried
 *     deterministically within {@link #MAX_ATTEMPTS}, consuming fresh draws from the same random stream so the whole
 *     procedure stays a bounded, deterministic function of the seed (spec §3.2).
 * </p>
 *
 * @see PuzzleGenerator
 * @see SolutionFiller
 */
public final class RegionGenerator {
	
	/**
	 * How many accepted swaps to make per cell before returning a partition. Enough to erase the box structure while
	 * keeping the shapes compact.
	 */
	private static final int SWAPS_PER_CELL = 6;
	/**
	 * The maximum number of jumble-and-check attempts before giving up. Every jumbled partition is fillable by
	 * construction and only rarely degenerate, so a valid partition is almost always produced on the first attempt;
	 * the bound only keeps the loop finite.
	 */
	public static final int MAX_ATTEMPTS = 200;
	
	private RegionGenerator() {}
	
	/**
	 * Generates a chaos region partition for the given size using the given random stream.
	 *
	 * @param size The grid size; must support {@link Variant#CHAOS}
	 * @param random The seeded random source, consumed to build the guiding solution and jumble the partition
	 * @return A contiguous, non-degenerate, fillable region partition
	 * @throws IllegalArgumentException If the size does not support chaos (that is {@link GridSize#FOUR})
	 * @throws NullPointerException If the size or the random source is null
	 * @throws IllegalStateException If no acceptable partition is found within {@link #MAX_ATTEMPTS} attempts
	 */
	public static RegionPartition generateChaos(GridSize size, DeterministicRandom random) {
		return generateChaosLayout(size, random).partition();
	}
	
	/**
	 * Generates a chaos region partition and a complete solution valid under it, using the given random stream.
	 *
	 * @param size The grid size; must support {@link Variant#CHAOS}
	 * @param random The seeded random source, consumed to build the guiding solution and jumble the partition
	 * @return The partition and a solution that fills it
	 * @throws IllegalArgumentException If the size does not support chaos (that is {@link GridSize#FOUR})
	 * @throws NullPointerException If the size or the random source is null
	 * @throws IllegalStateException If no acceptable partition is found within {@link #MAX_ATTEMPTS} attempts
	 */
	public static ChaosLayout generateChaosLayout(GridSize size, DeterministicRandom random) {
		Objects.requireNonNull(size, "Size must not be null");
		Objects.requireNonNull(random, "Random must not be null");
		Variant.CHAOS.checkSupportedAt(size);
		for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
			int[] solution = SolutionFiller.fill(ClassicRegionPartition.of(size), random).orElseThrow(
				() -> new IllegalStateException("The classic layout must always be fillable"));
			int[] regionOf = jumble(size, solution, random);
			RegionPartition partition = toPartition(size, regionOf);
			if (isDegenerate(size, partition)) {
				continue;
			}
			return new ChaosLayout(partition, solution);
		}
		throw new IllegalStateException("Failed to build a valid chaos partition for " + size + " within " + MAX_ATTEMPTS + " attempts");
	}
	
	/**
	 * Starts from the classic box assignment and applies random swaps that preserve region size, region contiguity and
	 * the validity of the given solution, until enough have been accepted, returning the region-of-cell array.
	 */
	private static int[] jumble(GridSize size, int[] solution, DeterministicRandom random) {
		int cellCount = size.cellCount();
		RegionPartition boxes = ClassicRegionPartition.of(size);
		int[] regionOf = new int[cellCount];
		int[] regionDigits = new int[size.n()];
		for (int cell = 0; cell < cellCount; cell++) {
			int region = boxes.regionOf(cell);
			regionOf[cell] = region;
			regionDigits[region] |= 1 << solution[cell];
		}
		int target = SWAPS_PER_CELL * cellCount;
		int accepted = 0;
		int attempts = 0;
		int maxAttempts = target * 12;
		int[] neighborBuffer = new int[4];
		while (accepted < target && attempts++ < maxAttempts) {
			int fromA = random.nextInt(cellCount);
			int neighborCount = orthogonalNeighbors(size, fromA, neighborBuffer);
			int across = neighborBuffer[random.nextInt(neighborCount)];
			int regionA = regionOf[fromA];
			int regionB = regionOf[across];
			if (regionA == regionB) {
				continue;
			}
			int fromB = boundaryCell(size, regionOf, regionB, regionA, random, neighborBuffer);
			if (fromB == -1) {
				continue;
			}
			int bitA = 1 << solution[fromA];
			int bitB = 1 << solution[fromB];
			if (((regionDigits[regionA] & ~bitA) & bitB) != 0 || ((regionDigits[regionB] & ~bitB) & bitA) != 0) {
				continue;
			}
			regionOf[fromA] = regionB;
			regionOf[fromB] = regionA;
			if (isConnected(size, regionOf, regionA) && isConnected(size, regionOf, regionB)) {
				regionDigits[regionA] = (regionDigits[regionA] & ~bitA) | bitB;
				regionDigits[regionB] = (regionDigits[regionB] & ~bitB) | bitA;
				accepted++;
			} else {
				regionOf[fromA] = regionA;
				regionOf[fromB] = regionB;
			}
		}
		return regionOf;
	}
	
	/**
	 * Picks a random cell of {@code region} that borders {@code adjacentRegion}, or {@code -1} if the two regions do
	 * not touch. The choice consumes draws so the whole procedure stays deterministic.
	 */
	private static int boundaryCell(GridSize size, int[] regionOf, int region, int adjacentRegion, DeterministicRandom random, int[] neighborBuffer) {
		int cellCount = size.cellCount();
		int count = 0;
		int chosen = -1;
		for (int cell = 0; cell < cellCount; cell++) {
			if (regionOf[cell] != region) {
				continue;
			}
			int neighborCount = orthogonalNeighbors(size, cell, neighborBuffer);
			boolean borders = false;
			for (int index = 0; index < neighborCount; index++) {
				if (regionOf[neighborBuffer[index]] == adjacentRegion) {
					borders = true;
					break;
				}
			}
			if (borders) {
				count++;
				if (random.nextInt(count) == 0) {
					chosen = cell;
				}
			}
		}
		return chosen;
	}
	
	/**
	 * Writes the in-bounds orthogonal neighbours of a cell into the buffer and returns how many there are. Every
	 * interior cell has four, edge cells three and corner cells two, so there is always at least one.
	 */
	private static int orthogonalNeighbors(GridSize size, int cell, int[] buffer) {
		int n = size.n();
		int row = cell / n;
		int column = cell % n;
		int count = 0;
		if (row > 0) {
			buffer[count++] = cell - n;
		}
		if (row < n - 1) {
			buffer[count++] = cell + n;
		}
		if (column > 0) {
			buffer[count++] = cell - 1;
		}
		if (column < n - 1) {
			buffer[count++] = cell + 1;
		}
		return count;
	}
	
	/**
	 * Checks that all cells currently assigned to the given region form a single orthogonally connected block.
	 */
	private static boolean isConnected(GridSize size, int[] regionOf, int region) {
		int total = 0;
		int start = -1;
		for (int cell = 0; cell < regionOf.length; cell++) {
			if (regionOf[cell] == region) {
				total++;
				if (start == -1) {
					start = cell;
				}
			}
		}
		if (start == -1) {
			return false;
		}
		boolean[] visited = new boolean[regionOf.length];
		int[] stack = new int[total];
		int top = 0;
		stack[top++] = start;
		visited[start] = true;
		int reached = 1;
		int[] neighborBuffer = new int[4];
		while (top > 0) {
			int cell = stack[--top];
			int neighborCount = orthogonalNeighbors(size, cell, neighborBuffer);
			for (int index = 0; index < neighborCount; index++) {
				int neighbor = neighborBuffer[index];
				if (regionOf[neighbor] == region && !visited[neighbor]) {
					visited[neighbor] = true;
					stack[top++] = neighbor;
					reached++;
				}
			}
		}
		return reached == total;
	}
	
	private static RegionPartition toPartition(GridSize size, int[] regionOf) {
		int n = size.n();
		int[][] cells = new int[n][n];
		int[] filled = new int[n];
		for (int cell = 0; cell < regionOf.length; cell++) {
			int region = regionOf[cell];
			cells[region][filled[region]++] = cell;
		}
		List<Region> regions = new ArrayList<>(n);
		for (int region = 0; region < n; region++) {
			regions.add(new Region(cells[region]));
		}
		return new RegionPartition(size, regions);
	}
	
	private static boolean isDegenerate(GridSize size, RegionPartition partition) {
		int n = size.n();
		for (Region region : partition.regions()) {
			boolean sameRow = true;
			boolean sameColumn = true;
			int firstRow = region.cell(0) / n;
			int firstColumn = region.cell(0) % n;
			for (int position = 1; position < region.size(); position++) {
				sameRow &= region.cell(position) / n == firstRow;
				sameColumn &= region.cell(position) % n == firstColumn;
			}
			if (sameRow || sameColumn) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * A chaos partition together with the complete solution it was built to admit.
	 * <p>
	 *     The solution is a load-bearing by-product, not a convenience: an empty jigsaw grid can be fillable yet
	 *     ruinously slow to solve from scratch, so the generator keeps and hands back the solution it already knows
	 *     rather than making a caller rediscover it. Downstream generation digs holes out of this solution and never
	 *     solves the empty grid.
	 * </p>
	 *
	 * @param partition The generated chaos partition
	 * @param solution A complete row-major solution valid under {@code partition}
	 */
	public record ChaosLayout(RegionPartition partition, int[] solution) {}
}
