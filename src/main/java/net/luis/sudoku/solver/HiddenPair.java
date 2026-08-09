package net.luis.sudoku.solver;

/**
 * The hidden-pair technique: two digits confined to the same two cells of a unit eliminate every other candidate from those two cells.
 * <p>
 *     If two digits can only go in the same two cells of a unit, those cells must hold exactly those two digits between them, so every other candidate in them is impossible. The pair is hidden behind precisely those extra candidates.
 * </p>
 *
 * @see TechniqueStrategy
 * @see HiddenSubset
 * @see Technique#HIDDEN_PAIR
 */
public final class HiddenPair extends HiddenSubset {
	
	/**
	 * Constructs the hidden-pair strategy. The strategy is stateless and holds no grid.
	 */
	public HiddenPair() {
		super(Technique.HIDDEN_PAIR, 2);
	}
}
