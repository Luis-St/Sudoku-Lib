package net.luis.sudoku.solver;

import java.util.Objects;

/**
 * A reference to one unit of the grid, by kind and index.
 * <p>
 *     Indices are the same ones {@link CandidateGrid#rowCells(int)}, {@link CandidateGrid#columnCells(int)} and
 *     {@link CandidateGrid#regionCells(int)} take, so a consumer can resolve a reference back to its cells without
 *     the explanation having to carry them.
 * </p>
 *
 * @param kind Which kind of unit this refers to
 * @param index The zero-based index of the unit within its kind
 *
 * @see Explanation
 */
public record UnitRef(UnitKind kind, int index) {
	
	/**
	 * Constructs a unit reference.
	 *
	 * @throws NullPointerException If the kind is null
	 * @throws IllegalArgumentException If the index is negative
	 */
	public UnitRef {
		Objects.requireNonNull(kind, "Unit kind must not be null");
		
		if (index < 0) {
			throw new IllegalArgumentException("Unit index must not be negative, but was " + index);
		}
	}
	
	/**
	 * Creates a reference to the given row.
	 *
	 * @param index The row index
	 * @return The reference
	 */
	public static UnitRef row(int index) {
		return new UnitRef(UnitKind.ROW, index);
	}
	
	/**
	 * Creates a reference to the given column.
	 *
	 * @param index The column index
	 * @return The reference
	 */
	public static UnitRef column(int index) {
		return new UnitRef(UnitKind.COLUMN, index);
	}
	
	/**
	 * Creates a reference to the given region.
	 *
	 * @param index The region index
	 * @return The reference
	 */
	public static UnitRef region(int index) {
		return new UnitRef(UnitKind.REGION, index);
	}
	
	/**
	 * Resolves this reference to the cells of the unit it names.
	 *
	 * @param grid The grid to resolve against
	 * @return The row-major cell indices of the unit
	 * @throws NullPointerException If the grid is null
	 */
	public int[] cells(CandidateGrid grid) {
		Objects.requireNonNull(grid, "Grid must not be null");
		
		return switch (this.kind) {
			case ROW -> grid.rowCells(this.index);
			case COLUMN -> grid.columnCells(this.index);
			case REGION -> grid.regionCells(this.index);
		};
	}
}
