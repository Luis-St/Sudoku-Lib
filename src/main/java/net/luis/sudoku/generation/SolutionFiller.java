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
 *
 * @see net.luis.sudoku.solver.BacktrackingSolver
 * @see RegionPartition
 */
public final class SolutionFiller {
	
	private SolutionFiller() {}
	
	/**
	 * Fills an empty grid of the given partition into one complete, valid solution.
	 * <p>
	 *     The returned array is a fresh row-major array in which every value lies in {@code 1..n} and every row,
	 *     column and region holds {@code 1..n} exactly once. The digits tried at each cell are shuffled with the
	 *     supplied random, so two different seeds produce two different grids while a repeated seed reproduces the
	 *     grid byte for byte and consumes the same number of draws.
	 * </p>
	 *
	 * @param partition The partition describing the regions to fill
	 * @param random The seeded source of randomness driving the digit order; the caller owns the stream
	 * @return A complete row-major solution, or an empty optional if the partition admits no completion at all
	 */
	public static Optional<int[]> fill(RegionPartition partition, DeterministicRandom random) {
		Fill fill = new Fill(partition, random);
		if (!fill.fill()) {
			return Optional.empty();
		}
		return Optional.of(fill.values());
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
		
		private Fill(RegionPartition partition, DeterministicRandom random) {
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
		}
		
		private int[] values() {
			return this.values;
		}
		
		private boolean fill() {
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
