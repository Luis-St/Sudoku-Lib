package net.luis.sudoku.generation;

import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.grid.RegionPartition;
import net.luis.sudoku.rng.DeterministicRandom;
import net.luis.sudoku.solver.BacktrackingSolver;

import java.util.Optional;

/**
 * Produces one complete, valid solution grid for a {@link RegionPartition} by seeded randomized backtracking.
 * <p>
 *     This is the randomized sibling of {@link BacktrackingSolver}: it shares the same
 *     minimum-remaining-values cell selection and the same {@code int} bitmask bookkeeping per row, per column and
 *     per region, and it works purely against the generalized {@link RegionPartition} so that a classic box layout
 *     and a chaos (jigsaw) layout run through exactly the same code path. The one and only difference is what makes
 *     it a generator rather than a solver: the <em>digit try order</em> at each cell is randomized. The candidate
 *     digits of the chosen cell are gathered from the bitmask and then shuffled with
 *     {@link DeterministicRandom#shuffle(int[])}, which is exactly what lets two seeds grow two different grids from
 *     the same partition. The minimum-remaining-values choice is essential for the larger jigsaw layouts, where a
 *     plain row-major scan backtracks catastrophically; a forced (single-candidate) cell shuffles a one-element
 *     array and so consumes no draws, keeping the choice deterministic.
 * </p>
 * <p>
 *     The fill is deterministic in the strict sense: the same partition and the same seed produce a byte-identical
 *     grid and consume an identical number of draws from the supplied {@link DeterministicRandom}. The caller owns
 *     the random stream; this class only draws from it. Because the digit order is the sole source of randomness,
 *     the number of draws is a pure function of the search path the seed steers the backtracker down.
 * </p>
 * <p>
 *     A partition that admits no completion at all yields an empty optional rather than an exception. That is
 *     possible for a chaos partition, and later phases rely on distinguishing "unfillable" from "error", so the
 *     empty result must never be turned into a throw.
 * </p>
 * <p>
 *     The search is <strong>bounded</strong>. One attempt may visit at most {@link #NODE_BUDGET} nodes, after which
 *     it is abandoned and restarted with a fresh digit order, up to {@link #MAX_RESTARTS} times. Without that bound
 *     the fill is not merely slow on unlucky seeds, it is unbounded: a 16x16 sweep was observed sitting on a single
 *     seed for 34 minutes at full CPU, still inside the fill, and the generator runs unattended in a server-side
 *     pool where that costs a worker thread permanently. Both constants carry their measurement and their reasoning.
 * </p>
 *
 * @see net.luis.sudoku.solver.BacktrackingSolver
 * @see RegionPartition
 */
public final class SolutionFiller {

	/**
	 * The number of search nodes one attempt may visit before it is abandoned and restarted with a fresh digit order.
	 * <p>
	 *     Ten million nodes is roughly 1.2 seconds. The number comes from measuring the node count of one fill of an
	 *     empty classic partition over 500 seeds at 16x16, 1000 at 12x12 and 3000 at 9x9, which is the only shape
	 *     {@link RegionGenerator} ever fills and therefore where the cost lives:
	 * </p>
	 * <pre>
	 *     size    p50    p90        p95         p99             max
	 *     9x9      82     87         88          92             105
	 *     12x12   152    203        237         423         104,216
	 *     16x16   296    540    129,200  87,430,180     755,870,274
	 * </pre>
	 * <p>
	 *     The 16x16 row is the whole problem: the runtime of a randomized most-constrained-cell search on an empty
	 *     16x16 grid is heavy-tailed, so the median is 296 nodes while 1.8 percent of seeds need more than ten
	 *     million and the worst seed measured needed 756 million. Two seeds in a 32-seed sweep never finished at all.
	 *     A restart is the standard cure for exactly that distribution, because a fresh digit order is an independent
	 *     draw from it rather than a continuation of an unlucky one.
	 * </p>
	 * <p>
	 *     The obvious budget, "high enough that no currently-succeeding seed reaches it", does not exist here. It
	 *     would have to sit above 756 million nodes, which is about 90 seconds per attempt and leaves an unattended
	 *     generator just as wedged as it was before. This budget is therefore chosen the other way round: low enough
	 *     to bound the damage, high enough that only the 1.8 percent of 16x16 seeds already costing more than a
	 *     second take a different path than they did before. Nothing at 12x12 or below reaches it (the worst seed
	 *     measured there was 104,216 nodes, two orders of magnitude short), so every 4x4, 6x6 and 9x9 grid this
	 *     library has ever produced is byte-identical, and the golden fixtures are unaffected. That is the reason
	 *     {@code GenVersion} does not move.
	 * </p>
	 */
	private static final long NODE_BUDGET = 10_000_000L;
	/**
	 * The number of times an abandoned attempt is retried with a fresh digit order before the fill gives up.
	 * <p>
	 *     Each restart is an independent draw, and 1.8 percent of 16x16 draws exceed {@link #NODE_BUDGET}, so nine
	 *     attempts all failing has a probability near 1e-15. That margin is what makes the {@code orElseThrow} at
	 *     {@link RegionGenerator}'s call site safe: a classic layout is always fillable, so an empty optional there
	 *     is a real error, and the budget must never manufacture one. The alternative, letting the last attempt run
	 *     unbounded so completeness is guaranteed on paper, was rejected because it reinstates the hang for exactly
	 *     the seeds this exists to bound.
	 * </p>
	 */
	private static final int MAX_RESTARTS = 8;

	private SolutionFiller() {}
	
	/**
	 * Fills an empty grid of the given partition into one complete, valid solution.
	 * <p>
	 *     The returned array is a fresh row-major array in which every value lies in {@code 1..n} and every row,
	 *     column and region holds {@code 1..n} exactly once. The digits tried at each cell are shuffled with the
	 *     supplied random, so two different seeds produce two different grids while a repeated seed reproduces the
	 *     grid byte for byte and consumes the same number of draws.
	 * </p>
	 * <p>
	 *     This always terminates: the search is capped at {@link #NODE_BUDGET} nodes per attempt and
	 *     {@link #MAX_RESTARTS} restarts, so the worst case is bounded work rather than an open-ended one.
	 * </p>
	 *
	 * @param partition The partition describing the regions to fill
	 * @param random The seeded source of randomness driving the digit order; the caller owns the stream
	 * @return A complete row-major solution, or an empty optional if the partition admits no completion at all
	 */
	public static Optional<int[]> fill(RegionPartition partition, DeterministicRandom random) {
		return fill(partition, random, NODE_BUDGET, MAX_RESTARTS);
	}

	/**
	 * The budgeted implementation behind {@link #fill(RegionPartition, DeterministicRandom)}, visible to the tests so
	 * that the restart path can be forced with a budget small enough to hit on an ordinary grid. Production code must
	 * call the two-argument overload, because the two constants are part of what makes generation reproducible.
	 *
	 * @param partition The partition describing the regions to fill
	 * @param random The seeded source of randomness driving the digit order; the caller owns the stream
	 * @param nodeBudget The number of search nodes one attempt may visit before it is abandoned
	 * @param maxRestarts The number of times an abandoned attempt may be retried with a fresh digit order
	 * @return A complete row-major solution, or an empty optional if the partition admits no completion at all
	 */
	static Optional<int[]> fill(RegionPartition partition, DeterministicRandom random, long nodeBudget, int maxRestarts) {
		for (int attempt = 0; attempt <= maxRestarts; attempt++) {
			// The stream is deliberately not reset between attempts. A restart has to draw a *different* digit
			// order to be worth anything, and re-seeding would replay the identical search that just failed.
			Fill fill = new Fill(partition, random, nodeBudget);
			if (fill.fill()) {
				return Optional.of(fill.values());
			}

			// A search that finished inside its budget has *proved* the partition admits no completion, and no
			// amount of reshuffling changes a proof. Restarting only makes sense when the budget cut the search
			// short, so the two outcomes must stay distinguishable: treating them alike would burn every restart
			// on the genuinely unfillable partitions the later phases rely on being told about promptly.
			if (!fill.exhausted()) {
				return Optional.empty();
			}
		}
		return Optional.empty();
	}

	private static final class Fill {

		private final int n;
		private final int cellCount;
		private final int fullMask;
		private final int[] values;
		private final int[] regionOf;
		private final int[] rowMasks;
		private final int[] columnMasks;
		private final int[] regionMasks;
		private final DeterministicRandom random;
		private final long nodeBudget;
		private long nodes;
		private boolean exhausted;

		private Fill(RegionPartition partition, DeterministicRandom random, long nodeBudget) {
			GridSize size = partition.size();
			this.n = size.n();
			this.cellCount = size.cellCount();
			this.fullMask = ((1 << this.n) - 1) << 1;
			this.values = new int[this.cellCount];
			this.regionOf = new int[this.cellCount];
			for (int cellIndex = 0; cellIndex < this.cellCount; cellIndex++) {
				this.regionOf[cellIndex] = partition.regionOf(cellIndex);
			}
			this.rowMasks = new int[this.n];
			this.columnMasks = new int[this.n];
			this.regionMasks = new int[partition.regionCount()];
			this.random = random;
			this.nodeBudget = nodeBudget;
		}

		private int[] values() {
			return this.values;
		}

		/**
		 * Whether this attempt stopped because it ran out of budget rather than because it searched the tree out.
		 */
		private boolean exhausted() {
			return this.exhausted;
		}

		private boolean fill() {
			if (++this.nodes > this.nodeBudget) {
				this.exhausted = true;
				return false;
			}

			int cellIndex = this.selectCell();
			if (cellIndex == -1) {
				return true;
			}
			
			int row = cellIndex / this.n;
			int column = cellIndex % this.n;
			int region = this.regionOf[cellIndex];
			int[] candidates = this.shuffledCandidates(row, column, region);
			for (int digit : candidates) {
				int bit = 1 << digit;
				this.place(cellIndex, bit, row, column, region);
				if (this.fill()) {
					return true;
				}
				this.remove(cellIndex, bit, row, column, region);

				// Unwind the whole recursion once the budget is gone instead of falling through to the next digit,
				// which would let every frame on the stack keep trying and turn one over-budget attempt into
				// another full sweep of the tree.
				if (this.exhausted) {
					return false;
				}
			}
			return false;
		}
		
		/**
		 * Returns the empty cell with the fewest candidates, ties broken by the lowest index, or {@code -1} when the
		 * grid is full. Choosing the most constrained cell keeps the randomized fill from backtracking catastrophically
		 * on the larger jigsaw layouts, while the lowest-index tie-break keeps the choice deterministic.
		 */
		private int selectCell() {
			int best = -1;
			int bestCount = Integer.MAX_VALUE;
			for (int cellIndex = 0; cellIndex < this.cellCount; cellIndex++) {
				if (this.values[cellIndex] != 0) {
					continue;
				}
				
				int row = cellIndex / this.n;
				int column = cellIndex % this.n;
				int region = this.regionOf[cellIndex];
				int mask = this.fullMask & ~(this.rowMasks[row] | this.columnMasks[column] | this.regionMasks[region]);
				int count = Integer.bitCount(mask);
				if (count < bestCount) {
					bestCount = count;
					best = cellIndex;
					if (count <= 1) {
						break;
					}
				}
			}
			return best;
		}
		
		private int[] shuffledCandidates(int row, int column, int region) {
			int mask = this.fullMask & ~(this.rowMasks[row] | this.columnMasks[column] | this.regionMasks[region]);
			int[] digits = new int[Integer.bitCount(mask)];
			int count = 0;
			while (mask != 0) {
				int bit = mask & -mask;
				mask &= mask - 1;
				digits[count++] = Integer.numberOfTrailingZeros(bit);
			}
			
			this.random.shuffle(digits);
			return digits;
		}
		
		private void place(int cellIndex, int bit, int row, int column, int region) {
			this.values[cellIndex] = Integer.numberOfTrailingZeros(bit);
			this.rowMasks[row] |= bit;
			this.columnMasks[column] |= bit;
			this.regionMasks[region] |= bit;
		}
		
		private void remove(int cellIndex, int bit, int row, int column, int region) {
			this.values[cellIndex] = 0;
			this.rowMasks[row] &= ~bit;
			this.columnMasks[column] &= ~bit;
			this.regionMasks[region] &= ~bit;
		}
	}
}
