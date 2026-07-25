package net.luis.sudoku.solver;

import net.luis.sudoku.grid.*;

import java.util.Optional;

/**
 * An exhaustive backtracking solver for every supported {@link GridSize} and both {@link Variant variants}.
 * <p>
 *     The solver works purely against the generalized {@link RegionPartition}, so a classic box layout and a
 *     chaos (jigsaw) layout run through exactly the same code path. A cell's region is resolved once per solve
 *     through {@link RegionPartition#regionOf(int)}; boxes are never assumed anywhere.
 * </p>
 * <p>
 *     Both entry points are deterministic by construction, which is what lets the generator and the rater be
 *     byte-identical on every JVM. The next cell to fill is chosen by a <em>minimum-remaining-values</em>
 *     heuristic — the empty cell with the fewest remaining candidates, ties broken by the lowest cell index —
 *     and the digits within a cell are always tried in ascending order. The heuristic is essential for
 *     proving uniqueness of sparse large grids in bounded time, and it is fully deterministic: the tie-break
 *     is a plain index comparison, never a hash order or a randomization. {@link #countSolutions(Puzzle, int)}
 *     in particular returns the same count regardless of the order cells are visited in, so the hole digger's
 *     accept/reject decisions — and therefore the puzzles the generator emits — are byte-identical to what a
 *     naive row-major scan would produce, only found far faster. There is no randomization, no hash-ordered
 *     iteration and no parallelism here; the seeded randomized filler used during generation is a separate
 *     class.
 * </p>
 * <p>
 *     Candidates are tracked with {@code int} bitmasks per row, per column and per region, where bit {@code d}
 *     stands for digit {@code d}. That keeps the search allocation-free after setup and makes the candidate set
 *     of a cell a single bitwise expression. Recursion depth is bounded by the cell count, so at most 256 frames
 *     for a 16x16 grid.
 * </p>
 * <p>
 *     Neither method mutates the puzzle it is given; both operate on a snapshot taken through
 *     {@link Puzzle#values()}.
 * </p>
 *
 * @see Puzzle
 * @see RegionPartition
 */
public final class BacktrackingSolver {
	
	private BacktrackingSolver() {}
	
	/**
	 * Solves the given puzzle and returns the first solution in the solver's deterministic search order.
	 * <p>
	 *     The returned array is a fresh row-major array of every cell's value, including the values the puzzle
	 *     already carried. The puzzle itself is left untouched.
	 * </p>
	 *
	 * @param puzzle The puzzle to solve
	 * @return The solution, or an empty optional if the puzzle has no solution at all, which includes the case
	 *         where the puzzle already contains a conflict
	 */
	public static Optional<int[]> solve(Puzzle puzzle) {
		Search search = new Search(puzzle);
		if (!search.initialize()) {
			return Optional.empty();
		}
		if (!search.solve()) {
			return Optional.empty();
		}
		return Optional.of(search.values());
	}
	
	/**
	 * Counts the solutions of the given puzzle, stopping as soon as the cap is reached.
	 * <p>
	 *     The result is therefore {@code min(actualSolutionCount, cap)}, and the search is abandoned the moment
	 *     the cap is hit rather than enumerating the remaining solutions. Passing a cap of {@code 2} is the
	 *     uniqueness test the hole digger uses. The puzzle itself is left untouched.
	 * </p>
	 *
	 * @param puzzle The puzzle to count the solutions of
	 * @param cap The number of solutions to stop at
	 * @return The number of solutions found, never more than the cap, and {@code 0} if the puzzle already
	 *         contains a conflict
	 * @throws IllegalArgumentException If the cap is less than one
	 */
	public static int countSolutions(Puzzle puzzle, int cap) {
		if (cap < 1) {
			throw new IllegalArgumentException("The solution cap must be at least 1, but is " + cap);
		}
		Search search = new Search(puzzle);
		if (!search.initialize()) {
			return 0;
		}
		return search.count(cap);
	}
	
	private static final class Search {
		
		private final int n;
		private final int cellCount;
		private final int fullMask;
		private final int[] values;
		private final int[] regionOf;
		private final int[] rowMasks;
		private final int[] columnMasks;
		private final int[] regionMasks;
		private int found;
		
		private Search(Puzzle puzzle) {
			GridSize size = puzzle.size();
			RegionPartition partition = puzzle.partition();
			this.n = size.n();
			this.cellCount = size.cellCount();
			this.fullMask = ((1 << this.n) - 1) << 1;
			this.values = puzzle.values();
			this.regionOf = new int[this.cellCount];
			for (int cellIndex = 0; cellIndex < this.cellCount; cellIndex++) {
				this.regionOf[cellIndex] = partition.regionOf(cellIndex);
			}
			this.rowMasks = new int[this.n];
			this.columnMasks = new int[this.n];
			this.regionMasks = new int[partition.regionCount()];
		}
		
		private int[] values() {
			return this.values;
		}
		
		private boolean initialize() {
			for (int cellIndex = 0; cellIndex < this.cellCount; cellIndex++) {
				int value = this.values[cellIndex];
				if (value == 0) {
					continue;
				}
				int bit = 1 << value;
				int row = cellIndex / this.n;
				int column = cellIndex % this.n;
				int region = this.regionOf[cellIndex];
				if ((this.rowMasks[row] & bit) != 0 || (this.columnMasks[column] & bit) != 0 || (this.regionMasks[region] & bit) != 0) {
					return false;
				}
				this.rowMasks[row] |= bit;
				this.columnMasks[column] |= bit;
				this.regionMasks[region] |= bit;
			}
			return true;
		}
		
		private boolean solve() {
			int cellIndex = this.selectCell();
			if (cellIndex == this.cellCount) {
				return true;
			}
			int row = cellIndex / this.n;
			int column = cellIndex % this.n;
			int region = this.regionOf[cellIndex];
			int candidates = this.candidates(row, column, region);
			while (candidates != 0) {
				int bit = candidates & -candidates;
				candidates &= candidates - 1;
				this.place(cellIndex, bit, row, column, region);
				if (this.solve()) {
					return true;
				}
				this.remove(cellIndex, bit, row, column, region);
			}
			return false;
		}
		
		private int count(int cap) {
			int cellIndex = this.selectCell();
			if (cellIndex == this.cellCount) {
				this.found++;
				return this.found;
			}
			int row = cellIndex / this.n;
			int column = cellIndex % this.n;
			int region = this.regionOf[cellIndex];
			int candidates = this.candidates(row, column, region);
			while (candidates != 0 && this.found < cap) {
				int bit = candidates & -candidates;
				candidates &= candidates - 1;
				this.place(cellIndex, bit, row, column, region);
				this.count(cap);
				this.remove(cellIndex, bit, row, column, region);
			}
			return this.found;
		}
		
		/**
		 * Picks the next cell to branch on: the empty cell with the fewest remaining candidates, breaking ties
		 * by the lowest cell index. A cell with no candidates is returned immediately, since it is a guaranteed
		 * dead end and no other cell can prune the search sooner. The index comparison keeps the choice
		 * deterministic on every JVM.
		 *
		 * @return The chosen cell index, or {@link #cellCount} when every cell is already filled
		 */
		private int selectCell() {
			int best = this.cellCount;
			int bestCount = Integer.MAX_VALUE;
			for (int cellIndex = 0; cellIndex < this.cellCount; cellIndex++) {
				if (this.values[cellIndex] != 0) {
					continue;
				}
				int row = cellIndex / this.n;
				int column = cellIndex % this.n;
				int region = this.regionOf[cellIndex];
				int count = Integer.bitCount(this.candidates(row, column, region));
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
		
		private int candidates(int row, int column, int region) {
			return this.fullMask & ~(this.rowMasks[row] | this.columnMasks[column] | this.regionMasks[region]);
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
