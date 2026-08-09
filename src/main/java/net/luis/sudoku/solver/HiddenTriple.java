package net.luis.sudoku.solver;

/**
 * The hidden-triple technique: three digits confined to the same three cells of a unit eliminate every other candidate from those three cells.
 * <p>
 *     As with the naked form, no digit has to appear in all three cells; the union of their candidate positions being exactly three cells is enough. Hidden triples are routinely buried under several unrelated candidates, which is what makes them harder than the naked triple.
 * </p>
 *
 * @see TechniqueStrategy
 * @see HiddenSubset
 * @see Technique#HIDDEN_TRIPLE
 */
public final class HiddenTriple extends HiddenSubset {
	
	/**
	 * Constructs the hidden-triple strategy. The strategy is stateless and holds no grid.
	 */
	public HiddenTriple() {
		super(Technique.HIDDEN_TRIPLE, 3);
	}
}
