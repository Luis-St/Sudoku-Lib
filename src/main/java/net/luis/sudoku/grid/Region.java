package net.luis.sudoku.grid;

import java.util.Arrays;

/**
 * An immutable set of cell indices that must contain every digit {@code 1..n} exactly once.
 * <p>
 *     A region is an arbitrary set of cells, not necessarily a rectangle. That generalization is what lets
 *     every size and both {@link Variant variants} share a single code path.
 * </p>
 * <p>
 *     The cells are stored in an ascending {@code int[]} rather than a {@code Set<Integer>}, both to avoid
 *     autoboxing in the solver's hot loop and to guarantee an iteration order that never depends on a hash.
 * </p>
 *
 * @see RegionPartition
 */
public record Region(int... cells) {
	
	/**
	 * Constructs a region from the given cell indices.
	 * <p>
	 *     The indices are copied and sorted ascending, so the caller may pass them in any order and may reuse
	 *     the array afterwards.
	 * </p>
	 *
	 * @param cells The row-major cell indices belonging to this region
	 * @throws IllegalArgumentException If the region is empty, contains a negative index or contains a duplicate
	 */
	public Region(int... cells) {
		if (cells.length == 0) {
			throw new IllegalArgumentException("A region must contain at least one cell");
		}
		
		int[] sorted = cells.clone();
		Arrays.sort(sorted);
		if (sorted[0] < 0) {
			throw new IllegalArgumentException("Cell index " + sorted[0] + " is negative");
		}
		
		for (int i = 1; i < sorted.length; i++) {
			if (sorted[i] == sorted[i - 1]) {
				throw new IllegalArgumentException("Cell index " + sorted[i] + " appears more than once in the region");
			}
		}
		this.cells = sorted;
	}
	
	/**
	 * Returns the number of cells in this region.
	 *
	 * @return The cell count
	 */
	public int size() {
		return this.cells.length;
	}
	
	/**
	 * Returns the cell index at the given position in ascending order.
	 * <p>
	 *     Prefer this over {@link #cells()} in hot loops, as it does not copy.
	 * </p>
	 *
	 * @param position The zero-based position within the region
	 * @return The row-major cell index at that position
	 * @throws IndexOutOfBoundsException If the position is outside the region
	 */
	public int cell(int position) {
		return this.cells[position];
	}
	
	/**
	 * Returns the cell indices of this region in ascending order.
	 *
	 * @return A copy of the cell indices
	 */
	@Override
	public int[] cells() {
		return this.cells.clone();
	}
	
	/**
	 * Checks whether the given cell belongs to this region.
	 *
	 * @param cellIndex The row-major cell index to look for
	 * @return True if the cell belongs to this region, false otherwise
	 */
	public boolean contains(int cellIndex) {
		return Arrays.binarySearch(this.cells, cellIndex) >= 0;
	}
	
	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		return object instanceof Region region && Arrays.equals(this.cells, region.cells);
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(this.cells);
	}
	
	@Override
	public String toString() {
		return "Region" + Arrays.toString(this.cells);
	}
}
