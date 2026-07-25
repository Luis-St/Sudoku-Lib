package net.luis.sudoku.grid;

/**
 * The supported grid edge lengths and the classic box dimensions that belong to each of them.
 * <p>
 *     A grid of size {@code n} has {@code n * n} cells, is partitioned into {@code n} regions of exactly
 *     {@code n} cells each, and is filled with the digits {@code 1..n}. For the classic variant the regions
 *     are rectangular boxes of {@link #boxWidth()} by {@link #boxHeight()} cells.
 * </p>
 *
 * @see Variant
 * @see ClassicRegionPartition
 */
public enum GridSize {
	
	/**
	 * A 4x4 grid with 2x2 boxes. Trivial, and the only size for which {@link Variant#CHAOS} is not supported.
	 */
	FOUR(4, 2, 2),
	/**
	 * A 6x6 grid with 3x2 boxes. The smallest size that supports {@link Variant#CHAOS}.
	 */
	SIX(6, 3, 2),
	/**
	 * A 9x9 grid with 3x3 boxes. The default size.
	 */
	NINE(9, 3, 3),
	/**
	 * A 12x12 grid with 4x3 boxes.
	 */
	TWELVE(12, 4, 3),
	/**
	 * A 16x16 grid with 4x4 boxes.
	 */
	SIXTEEN(16, 4, 4);
	
	private final int n;
	private final int boxWidth;
	private final int boxHeight;
	
	GridSize(int n, int boxWidth, int boxHeight) {
		this.n = n;
		this.boxWidth = boxWidth;
		this.boxHeight = boxHeight;
	}
	
	/**
	 * Returns the grid size matching the given edge length.
	 *
	 * @param n The edge length to look up
	 * @return The matching grid size
	 * @throws IllegalArgumentException If no supported grid size has the given edge length
	 */
	public static GridSize ofEdgeLength(int n) {
		for (GridSize size : values()) {
			if (size.n == n) {
				return size;
			}
		}
		throw new IllegalArgumentException("No supported grid size with edge length " + n);
	}
	
	/**
	 * Returns the edge length of the grid, which is also the number of regions, the number of cells per region
	 * and the highest digit that may be placed.
	 *
	 * @return The edge length
	 */
	public int n() {
		return this.n;
	}
	
	/**
	 * Returns the total number of cells in the grid.
	 *
	 * @return The cell count, {@code n * n}
	 */
	public int cellCount() {
		return this.n * this.n;
	}
	
	/**
	 * Returns the width of a classic box in cells.
	 *
	 * @return The box width
	 */
	public int boxWidth() {
		return this.boxWidth;
	}
	
	/**
	 * Returns the height of a classic box in cells.
	 *
	 * @return The box height
	 */
	public int boxHeight() {
		return this.boxHeight;
	}
	
	/**
	 * Converts a row and column into a row-major cell index.
	 *
	 * @param row The zero-based row
	 * @param column The zero-based column
	 * @return The row-major cell index
	 * @throws IndexOutOfBoundsException If the row or the column is outside the grid
	 */
	public int indexOf(int row, int column) {
		this.checkCoordinate(row, "Row");
		this.checkCoordinate(column, "Column");
		return row * this.n + column;
	}
	
	/**
	 * Returns the zero-based row of the given row-major cell index.
	 *
	 * @param cellIndex The row-major cell index
	 * @return The row
	 * @throws IndexOutOfBoundsException If the cell index is outside the grid
	 */
	public int rowOf(int cellIndex) {
		this.checkCellIndex(cellIndex);
		return cellIndex / this.n;
	}
	
	/**
	 * Returns the zero-based column of the given row-major cell index.
	 *
	 * @param cellIndex The row-major cell index
	 * @return The column
	 * @throws IndexOutOfBoundsException If the cell index is outside the grid
	 */
	public int columnOf(int cellIndex) {
		this.checkCellIndex(cellIndex);
		return cellIndex % this.n;
	}
	
	/**
	 * Checks whether the given digit is placeable on this grid.
	 *
	 * @param digit The digit to check
	 * @return True if the digit is in {@code 1..n}, false otherwise
	 */
	public boolean isValidDigit(int digit) {
		return digit >= 1 && digit <= this.n;
	}
	
	/**
	 * Throws if the given cell index is outside this grid.
	 *
	 * @param cellIndex The row-major cell index to check
	 * @throws IndexOutOfBoundsException If the cell index is negative or not less than {@link #cellCount()}
	 */
	public void checkCellIndex(int cellIndex) {
		if (cellIndex < 0 || cellIndex >= this.cellCount()) {
			throw new IndexOutOfBoundsException("Cell index " + cellIndex + " is outside a " + this.n + "x" + this.n + " grid");
		}
	}
	
	/**
	 * Throws if the given digit is not placeable on this grid.
	 *
	 * @param digit The digit to check
	 * @throws IllegalArgumentException If the digit is not in {@code 1..n}
	 */
	public void checkDigit(int digit) {
		if (!this.isValidDigit(digit)) {
			throw new IllegalArgumentException("Digit " + digit + " is not in 1.." + this.n);
		}
	}
	
	private void checkCoordinate(int coordinate, String name) {
		if (coordinate < 0 || coordinate >= this.n) {
			throw new IndexOutOfBoundsException(name + " " + coordinate + " is outside a " + this.n + "x" + this.n + " grid");
		}
	}
}
