package net.luis.sudoku.solver;

/**
 * The three kinds of unit a technique can argue over.
 * <p>
 *     A unit is a set of cells that must hold every digit exactly once. Which kind a unit is matters to an
 *     {@link Explanation} in a way it does not matter to the solver: "this row" and "this box" are the words a
 *     player thinks in, and a highlight that cannot say which of the two it means cannot teach a technique.
 * </p>
 *
 * @see UnitRef
 */
public enum UnitKind {

	/**
	 * A horizontal line of the grid.
	 */
	ROW,
	/**
	 * A vertical line of the grid.
	 */
	COLUMN,
	/**
	 * A region: a box in a classic grid, an irregular block in a chaos grid.
	 */
	REGION
}
