package net.luis.sudoku.solver;

/**
 * The plain alternating inference chain: the same machinery with no restriction on which candidates may take part.
 * <p>
 *     Dropping both the single-digit and the bi-value restriction lets the chain switch digits inside a multi-value cell and jump between digits freely, which is what makes it strictly stronger than the X-Chain and the XY-Chain, and correspondingly harder to find. All three end relations are available to it.
 * </p>
 *
 * @see TechniqueStrategy
 * @see AlternatingChain
 * @see Technique#AIC
 */
public final class Aic extends AlternatingChain {
	
	/**
	 * Constructs the AIC strategy. The strategy is stateless and holds no grid.
	 */
	public Aic() {
		super(Technique.AIC, false, false, false);
	}
}
