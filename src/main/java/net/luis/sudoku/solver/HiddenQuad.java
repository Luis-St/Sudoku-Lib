package net.luis.sudoku.solver;

/**
 * The hidden-quad technique: four digits confined to the same four cells of a unit eliminate every other candidate from those four cells.
 * <p>
 *     The largest hidden subset the solver searches for. Like the naked quad it is rare, and it is normally only found once the easier subsets have already thinned the unit down.
 * </p>
 *
 * @see TechniqueStrategy
 * @see HiddenSubset
 * @see Technique#HIDDEN_QUAD
 */
public final class HiddenQuad extends HiddenSubset {
	
	/**
	 * Constructs the hidden-quad strategy. The strategy is stateless and holds no grid.
	 */
	public HiddenQuad() {
		super(Technique.HIDDEN_QUAD, 4);
	}
}
