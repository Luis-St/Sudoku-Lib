package net.luis.sudoku.grid;

import java.util.Arrays;
import java.util.Objects;

/**
 * A mutable puzzle: a grid size, a variant, a region partition and the {@link Cell cells} that fill the grid.
 * <p>
 *     The cells are stored row-major, so cell index {@code row * n + column} addresses the cell at that
 *     coordinate, exactly as {@link GridSize#indexOf(int, int)} computes it. All conflict detection runs over the
 *     generalized {@link RegionPartition}, which is what lets the classic and the chaos variant share one code
 *     path at every supported size.
 * </p>
 * <p>
 *     Conflicts are found with {@code int} bitmasks over each row, column and region rather than with sets, so
 *     the result never depends on a hash order and detection stays allocation-light.
 * </p>
 *
 * @see Cell
 * @see RegionPartition
 */
public final class Puzzle {
	
	private final GridSize size;
	private final Variant variant;
	private final RegionPartition partition;
	private final Cell[] cells;
	
	/**
	 * Constructs a puzzle from the given cells.
	 * <p>
	 *     The array is copied, so the caller may reuse it afterwards, but the cells themselves are adopted by
	 *     reference and stay shared with the caller.
	 * </p>
	 *
	 * @param size The grid size
	 * @param variant The region layout variant
	 * @param partition The partition describing the regions of the grid
	 * @param cells The cells of the grid in row-major order
	 * @throws IllegalArgumentException If the variant is unsupported at the size, if the partition belongs to a
	 * 		different size, or if the cell count does not match the grid
	 * @throws NullPointerException If any cell is null
	 */
	public Puzzle(GridSize size, Variant variant, RegionPartition partition, Cell[] cells) {
		variant.checkSupportedAt(size);
		if (partition.size() != size) {
			throw new IllegalArgumentException("Partition belongs to grid size " + partition.size() + ", expected " + size);
		}
		if (cells.length != size.cellCount()) {
			throw new IllegalArgumentException("Expected " + size.cellCount() + " cells for grid size " + size + ", got " + cells.length);
		}
		
		Cell[] copy = new Cell[cells.length];
		for (int cellIndex = 0; cellIndex < cells.length; cellIndex++) {
			copy[cellIndex] = Objects.requireNonNull(cells[cellIndex], "Cell " + cellIndex + " must not be null");
		}
		
		this.size = size;
		this.variant = variant;
		this.partition = partition;
		this.cells = copy;
	}
	
	/**
	 * Creates a puzzle whose cells are all empty and none of which is a given.
	 *
	 * @param size The grid size
	 * @param variant The region layout variant
	 * @param partition The partition describing the regions of the grid
	 * @return The new empty puzzle
	 * @throws IllegalArgumentException If the variant is unsupported at the size or the partition belongs to a
	 * 		different size
	 */
	public static Puzzle empty(GridSize size, Variant variant, RegionPartition partition) {
		Cell[] cells = new Cell[size.cellCount()];
		for (int cellIndex = 0; cellIndex < cells.length; cellIndex++) {
			cells[cellIndex] = new Cell();
		}
		return new Puzzle(size, variant, partition, cells);
	}
	
	/**
	 * Creates a puzzle from a row-major array of clue digits.
	 *
	 * @param size The grid size
	 * @param variant The region layout variant
	 * @param partition The partition describing the regions of the grid
	 * @param givens The clue digits in row-major order, where {@code 0} marks an empty cell
	 * @return The new puzzle, in which every non-zero entry became a given cell
	 * @throws IllegalArgumentException If the variant is unsupported at the size, if the partition belongs to a
	 * 		different size, if the array length does not match the grid, or if a value is not in {@code 0..n}
	 */
	public static Puzzle ofGivens(GridSize size, Variant variant, RegionPartition partition, int[] givens) {
		if (givens.length != size.cellCount()) {
			throw new IllegalArgumentException("Expected " + size.cellCount() + " givens for grid size " + size + ", got " + givens.length);
		}
		
		Cell[] cells = new Cell[givens.length];
		for (int cellIndex = 0; cellIndex < givens.length; cellIndex++) {
			int digit = givens[cellIndex];
			if (digit == 0) {
				cells[cellIndex] = new Cell();
				continue;
			}
			
			size.checkDigit(digit);
			cells[cellIndex] = Cell.given(digit);
		}
		return new Puzzle(size, variant, partition, cells);
	}
	
	/**
	 * Creates a classic puzzle from a row-major array of clue digits, using the cached box partition of the size.
	 *
	 * @param size The grid size
	 * @param givens The clue digits in row-major order, where {@code 0} marks an empty cell
	 * @return The new classic puzzle
	 * @throws IllegalArgumentException If the array length does not match the grid or a value is not in {@code 0..n}
	 */
	public static Puzzle classicOfGivens(GridSize size, int[] givens) {
		return ofGivens(size, Variant.CLASSIC, ClassicRegionPartition.of(size), givens);
	}
	
	/**
	 * Returns the grid size of this puzzle.
	 *
	 * @return The grid size
	 */
	public GridSize size() {
		return this.size;
	}
	
	/**
	 * Returns the region layout variant of this puzzle.
	 *
	 * @return The variant
	 */
	public Variant variant() {
		return this.variant;
	}
	
	/**
	 * Returns the partition describing the regions of this puzzle.
	 *
	 * @return The region partition
	 */
	public RegionPartition partition() {
		return this.partition;
	}
	
	/**
	 * Returns the cell at the given row-major index. The cell is the live instance, not a copy.
	 *
	 * @param cellIndex The row-major cell index
	 * @return The cell at that index
	 * @throws IndexOutOfBoundsException If the cell index is outside the grid
	 */
	public Cell cell(int cellIndex) {
		this.size.checkCellIndex(cellIndex);
		return this.cells[cellIndex];
	}
	
	/**
	 * Returns the cell at the given coordinate. The cell is the live instance, not a copy.
	 *
	 * @param row The zero-based row
	 * @param column The zero-based column
	 * @return The cell at that coordinate
	 * @throws IndexOutOfBoundsException If the row or the column is outside the grid
	 */
	public Cell cell(int row, int column) {
		return this.cells[this.size.indexOf(row, column)];
	}
	
	/**
	 * Returns the digit held by the cell at the given row-major index.
	 *
	 * @param cellIndex The row-major cell index
	 * @return The digit, or {@code 0} if the cell is empty
	 * @throws IndexOutOfBoundsException If the cell index is outside the grid
	 */
	public int valueAt(int cellIndex) {
		return this.cell(cellIndex).value();
	}
	
	/**
	 * Returns the digit held by the cell at the given coordinate.
	 *
	 * @param row The zero-based row
	 * @param column The zero-based column
	 * @return The digit, or {@code 0} if the cell is empty
	 * @throws IndexOutOfBoundsException If the row or the column is outside the grid
	 */
	public int valueAt(int row, int column) {
		return this.cell(row, column).value();
	}
	
	/**
	 * Sets the digit of the cell at the given row-major index.
	 *
	 * @param cellIndex The row-major cell index
	 * @param digit The digit to place, or {@code 0} to clear the cell
	 * @throws IndexOutOfBoundsException If the cell index is outside the grid
	 * @throws IllegalArgumentException If the digit is neither {@code 0} nor in {@code 1..n}
	 * @throws IllegalStateException If the addressed cell is a given
	 */
	public void setValue(int cellIndex, int digit) {
		this.size.checkCellIndex(cellIndex);
		if (digit != 0) {
			this.size.checkDigit(digit);
		}
		this.cells[cellIndex].setValue(digit);
	}
	
	/**
	 * Returns a snapshot of every cell's digit in row-major order.
	 *
	 * @return A new array of the digits, where {@code 0} marks an empty cell
	 */
	public int[] values() {
		int[] values = new int[this.cells.length];
		for (int cellIndex = 0; cellIndex < this.cells.length; cellIndex++) {
			values[cellIndex] = this.cells[cellIndex].value();
		}
		return values;
	}
	
	/**
	 * Returns the row-major cell indices of the given row in ascending order.
	 *
	 * @param row The zero-based row
	 * @return A new array of the cell indices
	 * @throws IndexOutOfBoundsException If the row is outside the grid
	 */
	public int[] rowCells(int row) {
		int n = this.size.n();
		int first = this.size.indexOf(row, 0);
		int[] indices = new int[n];
		for (int column = 0; column < n; column++) {
			indices[column] = first + column;
		}
		return indices;
	}
	
	/**
	 * Returns the row-major cell indices of the given column in ascending order.
	 *
	 * @param column The zero-based column
	 * @return A new array of the cell indices
	 * @throws IndexOutOfBoundsException If the column is outside the grid
	 */
	public int[] columnCells(int column) {
		int n = this.size.n();
		int first = this.size.indexOf(0, column);
		int[] indices = new int[n];
		for (int row = 0; row < n; row++) {
			indices[row] = first + row * n;
		}
		return indices;
	}
	
	/**
	 * Returns the row-major cell indices of the given region in ascending order.
	 *
	 * @param regionIndex The zero-based region index
	 * @return A new array of the cell indices
	 * @throws IndexOutOfBoundsException If the region index is outside the partition
	 */
	public int[] regionCells(int regionIndex) {
		return this.partition.region(regionIndex).cells();
	}
	
	/**
	 * Checks whether every cell of this puzzle holds a digit.
	 *
	 * @return True if no cell is empty, false otherwise
	 */
	public boolean isComplete() {
		for (Cell cell : this.cells) {
			if (cell.isEmpty()) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Checks whether the cell at the given index collides with another cell.
	 * <p>
	 *     A filled cell is in conflict when its digit appears a second time in its row, its column or its region.
	 *     Given cells take part in conflicts like any other cell.
	 * </p>
	 *
	 * @param cellIndex The row-major cell index
	 * @return True if the cell holds a digit that another cell of one of its units holds too, false otherwise
	 * @throws IndexOutOfBoundsException If the cell index is outside the grid
	 */
	public boolean hasConflictAt(int cellIndex) {
		this.size.checkCellIndex(cellIndex);
		int value = this.cells[cellIndex].value();
		if (value == 0) {
			return false;
		}
		
		int n = this.size.n();
		int row = this.size.rowOf(cellIndex);
		int column = this.size.columnOf(cellIndex);
		int rowStart = row * n;
		for (int index = 0; index < n; index++) {
			if (this.isDuplicate(rowStart + index, cellIndex, value) || this.isDuplicate(index * n + column, cellIndex, value)) {
				return true;
			}
		}
		
		Region region = this.partition.regionContaining(cellIndex);
		for (int position = 0; position < region.size(); position++) {
			if (this.isDuplicate(region.cell(position), cellIndex, value)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns every cell that is in conflict, as defined by {@link #hasConflictAt(int)}.
	 *
	 * @return A new array of the conflicting cell indices in ascending order, empty if there are none
	 */
	public int[] conflictingCells() {
		boolean[] conflicted = new boolean[this.cells.length];
		int n = this.size.n();
		for (int row = 0; row < n; row++) {
			this.markUnitConflicts(this.rowCells(row), conflicted);
		}
		for (int column = 0; column < n; column++) {
			this.markUnitConflicts(this.columnCells(column), conflicted);
		}
		for (int regionIndex = 0; regionIndex < n; regionIndex++) {
			this.markUnitConflicts(this.regionCells(regionIndex), conflicted);
		}
		
		int count = 0;
		for (boolean flag : conflicted) {
			if (flag) {
				count++;
			}
		}
		
		int[] indices = new int[count];
		int position = 0;
		for (int cellIndex = 0; cellIndex < conflicted.length; cellIndex++) {
			if (conflicted[cellIndex]) {
				indices[position++] = cellIndex;
			}
		}
		return indices;
	}
	
	/**
	 * Checks whether this puzzle is free of conflicts. An empty grid is valid, as is any partially filled grid
	 * whose digits do not collide.
	 *
	 * @return True if no cell is in conflict, false otherwise
	 */
	public boolean isValid() {
		int n = this.size.n();
		for (int index = 0; index < n; index++) {
			if (this.hasDuplicate(this.rowCells(index)) || this.hasDuplicate(this.columnCells(index)) || this.hasDuplicate(this.regionCells(index))) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Checks whether this puzzle is completely and correctly filled.
	 *
	 * @return True if {@link #isComplete()} and {@link #isValid()} both hold, false otherwise
	 */
	public boolean isSolved() {
		return this.isComplete() && this.isValid();
	}
	
	/**
	 * Creates a deep copy of this puzzle, in which every cell is an independent copy of the original one.
	 * <p>
	 *     The grid size, the variant and the partition are shared, as all three are immutable.
	 * </p>
	 *
	 * @return The copied puzzle
	 */
	public Puzzle copy() {
		Cell[] copies = new Cell[this.cells.length];
		for (int cellIndex = 0; cellIndex < this.cells.length; cellIndex++) {
			copies[cellIndex] = this.cells[cellIndex].copy();
		}
		return new Puzzle(this.size, this.variant, this.partition, copies);
	}
	
	private boolean isDuplicate(int otherIndex, int cellIndex, int value) {
		return otherIndex != cellIndex && this.cells[otherIndex].value() == value;
	}
	
	private boolean hasDuplicate(int[] unit) {
		int seen = 0;
		for (int cellIndex : unit) {
			int value = this.cells[cellIndex].value();
			if (value == 0) {
				continue;
			}
			
			int bit = 1 << value;
			if ((seen & bit) != 0) {
				return true;
			}
			seen |= bit;
		}
		return false;
	}
	
	private void markUnitConflicts(int[] unit, boolean[] conflicted) {
		int seen = 0;
		int duplicated = 0;
		for (int cellIndex : unit) {
			int value = this.cells[cellIndex].value();
			if (value == 0) {
				continue;
			}
			
			int bit = 1 << value;
			if ((seen & bit) != 0) {
				duplicated |= bit;
			}
			seen |= bit;
		}
		
		if (duplicated == 0) {
			return;
		}
		
		for (int cellIndex : unit) {
			int value = this.cells[cellIndex].value();
			if (value != 0 && (duplicated & (1 << value)) != 0) {
				conflicted[cellIndex] = true;
			}
		}
	}
	
	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		return object instanceof Puzzle puzzle && this.size == puzzle.size && this.variant == puzzle.variant && this.partition.equals(puzzle.partition) && Arrays.equals(this.cells, puzzle.cells);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.size, this.variant, this.partition, Arrays.hashCode(this.cells));
	}
	
	@Override
	public String toString() {
		return "Puzzle[size=" + this.size + ", variant=" + this.variant + ", values=" + Arrays.toString(this.values()) + "]";
	}
}
