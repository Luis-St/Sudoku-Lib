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
 *     into {@code A}. Since every region already holds the digits {@code 1..n} exactly once, the fixed solution stays a
 *     valid completion precisely when the two moved cells carry the <i>same</i> digit, so the swap is drawn that way
 *     directly; it is then kept only if both regions also stay contiguous. Rows and columns are untouched throughout.
 *     Because the two moved cells sit at different points on the shared border, the swap grows an interlocking jagged
 *     edge rather than collapsing back; sizes are preserved exactly since each region loses one cell and gains one.
 * </p>
 * <p>
 *     The only remaining acceptance check is the cheap, local one (spec §3.3 step 1): no region may be a plain
 *     rectangle. That covers a full row or column, which adds no information beyond the line constraint, and equally
 *     the classic box shape a chaos puzzle exists to replace. A partition that trips it is retried deterministically
 *     within {@link #MAX_ATTEMPTS}, consuming fresh draws from the same random stream so the whole procedure stays a
 *     bounded, deterministic function of the seed (spec §3.2).
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
	 * How many draws to spend per wanted swap before giving up on reaching the target. Most draws still fail - the two
	 * cells have to sit in different regions, share a digit and leave both regions contiguous - so this has to be
	 * comfortably above the reciprocal of that acceptance rate, or the jumble stops early with box-shaped regions.
	 */
	private static final int MAX_TRIES_PER_SWAP = 60;
	/**
	 * How many rectangles {@link #repair} will break before giving up on a jumbled partition. Repairing one region can
	 * in principle square off another, so the bound is a generous multiple of the region count rather than exactly it.
	 */
	private static final int MAX_REPAIR_ROUNDS = 64;
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
			int[] solution = SolutionFiller.fill(ClassicRegionPartition.of(size), random).orElseThrow(() -> new IllegalStateException("The classic layout must always be fillable"));
			int[] regionOf = jumble(size, solution, random);
			if (!repair(size, solution, regionOf, random)) {
				continue;
			}
			return new ChaosLayout(toPartition(size, regionOf), solution);
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
		for (int cell = 0; cell < cellCount; cell++) {
			regionOf[cell] = boxes.regionOf(cell);
		}
		
		int target = SWAPS_PER_CELL * cellCount;
		int accepted = 0;
		int attempts = 0;
		int maxAttempts = target * MAX_TRIES_PER_SWAP;
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
			
			// Every region already holds 1..n exactly once, so a swap can only keep both valid when the two moved cells
			// carry the *same* digit - any other pair would duplicate one digit and drop another. Searching for that
			// digit directly, rather than drawing a boundary cell and rejecting the ~(n-1)/n that mismatch, is what
			// makes the accepted-swap target actually reachable at 12x12 and 16x16; a rejection-sampling search left
			// most regions still shaped like the classic boxes it is supposed to erase.
			int fromB = boundaryCell(size, regionOf, regionB, regionA, solution, solution[fromA], random, neighborBuffer);
			if (fromB == -1) {
				continue;
			}
			
			regionOf[fromA] = regionB;
			regionOf[fromB] = regionA;
			if (isConnected(size, regionOf, regionA) && isConnected(size, regionOf, regionB)) {
				accepted++;
			} else {
				regionOf[fromA] = regionA;
				regionOf[fromB] = regionB;
			}
		}
		return regionOf;
	}
	
	/**
	 * Replaces every rectangular region left by {@link #jumble} with a jagged one, in place, reporting whether it
	 * succeeded.
	 * <p>
	 *     The jumble alone does not get there. Along a given border there is usually exactly one digit shared between
	 *     the two facing edges, so the only move available to a region is one particular swap - and the only move after
	 *     that is its own inverse. The walk therefore oscillates instead of wandering, and a region that started as a
	 *     classic box has an even chance of ending back as one however long the walk runs. Raising the swap target does
	 *     not help; the equilibrium is the problem, not the mixing time.
	 * </p>
	 * <p>
	 *     So the rectangles are removed deliberately rather than waited out: each one takes the single swap that breaks
	 *     its own shape without turning its partner into a rectangle, and is then left alone. Each repair is a swap of
	 *     the same kind the jumble makes, so the carried solution stays valid by exactly the same argument.
	 * </p>
	 */
	private static boolean repair(GridSize size, int[] solution, int[] regionOf, DeterministicRandom random) {
		for (int round = 0; round < MAX_REPAIR_ROUNDS; round++) {
			int rectangular = firstRectangularRegion(size, regionOf);
			if (rectangular == -1) {
				return true;
			}
			if (!breakRectangle(size, solution, regionOf, rectangular, random)) {
				return false;
			}
		}
		return firstRectangularRegion(size, regionOf) == -1;
	}
	
	/** The lowest-numbered region that exactly fills its bounding box, or {@code -1} if none does. */
	private static int firstRectangularRegion(GridSize size, int[] regionOf) {
		int n = size.n();
		int[] minRow = new int[n];
		int[] maxRow = new int[n];
		int[] minColumn = new int[n];
		int[] maxColumn = new int[n];
		int[] count = new int[n];
		Arrays.fill(minRow, Integer.MAX_VALUE);
		Arrays.fill(minColumn, Integer.MAX_VALUE);
		Arrays.fill(maxRow, Integer.MIN_VALUE);
		Arrays.fill(maxColumn, Integer.MIN_VALUE);
		for (int cell = 0; cell < regionOf.length; cell++) {
			int region = regionOf[cell];
			int row = cell / n;
			int column = cell % n;
			minRow[region] = Math.min(minRow[region], row);
			maxRow[region] = Math.max(maxRow[region], row);
			minColumn[region] = Math.min(minColumn[region], column);
			maxColumn[region] = Math.max(maxColumn[region], column);
			count[region]++;
		}
		for (int region = 0; region < n; region++) {
			if ((maxRow[region] - minRow[region] + 1) * (maxColumn[region] - minColumn[region] + 1) == count[region]) {
				return region;
			}
		}
		return -1;
	}
	
	/**
	 * Applies one valid swap that leaves {@code region} non-rectangular, chosen uniformly among those that exist, and
	 * reports whether one was found. Because each region holds every digit exactly once, the partner cell for a given
	 * cell and neighbouring region is unique, so the candidates are simply the region's own cells crossed with the
	 * regions they touch.
	 */
	private static boolean breakRectangle(GridSize size, int[] solution, int[] regionOf, int region, DeterministicRandom random) {
		int n = size.n();
		int[] cellOfDigit = cellOfDigitPerRegion(size, solution, regionOf);
		int[] neighborBuffer = new int[4];
		int count = 0;
		int chosenFrom = -1;
		int chosenTo = -1;
		for (int cell = 0; cell < regionOf.length; cell++) {
			if (regionOf[cell] != region) {
				continue;
			}
			int neighborCount = orthogonalNeighbors(size, cell, neighborBuffer);
			for (int index = 0; index < neighborCount; index++) {
				int other = regionOf[neighborBuffer[index]];
				if (other == region) {
					continue;
				}
				int partner = cellOfDigit[other * (n + 1) + solution[cell]];
				if (!isSwapAcceptable(size, regionOf, cell, partner, region, other)) {
					continue;
				}
				count++;
				if (random.nextInt(count) == 0) {
					chosenFrom = cell;
					chosenTo = partner;
				}
			}
		}
		if (chosenFrom == -1) {
			return false;
		}
		int partnerRegion = regionOf[chosenTo];
		regionOf[chosenFrom] = partnerRegion;
		regionOf[chosenTo] = region;
		return true;
	}
	
	/**
	 * Tries the swap of {@code cell} and {@code partner} and reports whether it leaves both regions contiguous and
	 * neither of them rectangular, restoring the assignment either way.
	 */
	private static boolean isSwapAcceptable(GridSize size, int[] regionOf, int cell, int partner, int region, int other) {
		regionOf[cell] = other;
		regionOf[partner] = region;
		boolean acceptable = isConnected(size, regionOf, region) && isConnected(size, regionOf, other)
			&& !isRectangular(size, regionOf, region) && !isRectangular(size, regionOf, other);
		regionOf[cell] = region;
		regionOf[partner] = other;
		return acceptable;
	}
	
	/** Flattened {@code [region][digit] -> cell} lookup; every region holds every digit exactly once, so it is total. */
	private static int[] cellOfDigitPerRegion(GridSize size, int[] solution, int[] regionOf) {
		int n = size.n();
		int[] cellOfDigit = new int[n * (n + 1)];
		for (int cell = 0; cell < regionOf.length; cell++) {
			cellOfDigit[regionOf[cell] * (n + 1) + solution[cell]] = cell;
		}
		return cellOfDigit;
	}
	
	/**
	 * Picks a random cell of {@code region} that borders {@code adjacentRegion} and whose solution digit is
	 * {@code digit}, or {@code -1} if no such cell exists. The choice consumes draws so the whole procedure stays
	 * deterministic.
	 */
	private static int boundaryCell(GridSize size, int[] regionOf, int region, int adjacentRegion, int[] solution, int digit, DeterministicRandom random, int[] neighborBuffer) {
		int cellCount = size.cellCount();
		int count = 0;
		int chosen = -1;
		for (int cell = 0; cell < cellCount; cell++) {
			if (regionOf[cell] != region || solution[cell] != digit) {
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
	
	/**
	 * Whether a region exactly fills its own bounding box, which makes it a plain rectangle rather than a jigsaw shape.
	 * <p>
	 *     This is the single acceptance check of spec §3.3 step 1, generalised: a full row is a {@code 1 x n} rectangle
	 *     and a full column an {@code n x 1} one, so rejecting rectangles rejects those too. It additionally rejects the
	 *     classic box shape (and any other {@code rows x columns == n} block), which a chaos puzzle must not contain -
	 *     a rectangular region is indistinguishable from the classic layout the variant exists to replace.
	 * </p>
	 */
	private static boolean isRectangular(GridSize size, int[] regionOf, int region) {
		int n = size.n();
		int minRow = Integer.MAX_VALUE;
		int maxRow = Integer.MIN_VALUE;
		int minColumn = Integer.MAX_VALUE;
		int maxColumn = Integer.MIN_VALUE;
		int count = 0;
		for (int cell = 0; cell < regionOf.length; cell++) {
			if (regionOf[cell] != region) {
				continue;
			}
			int row = cell / n;
			int column = cell % n;
			minRow = Math.min(minRow, row);
			maxRow = Math.max(maxRow, row);
			minColumn = Math.min(minColumn, column);
			maxColumn = Math.max(maxColumn, column);
			count++;
		}
		// The cells are distinct, so filling the bounding box is equivalent to the areas matching.
		return (maxRow - minRow + 1) * (maxColumn - minColumn + 1) == count;
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
