package net.luis.sudoku.solver;

/**
 * The XY-Chain: an alternating chain running through bi-value cells.
 * <p>
 *     Each cell of the chain holds exactly two candidates, so entering it on one digit leaves it on the other, and the chain threads from cell to cell on shared digits. When both ends offer the same digit, that digit is removed from every cell seeing both. The XY-Wing is the three-cell case of this chain.
 * </p>
 *
 * @see TechniqueStrategy
 * @see AlternatingChain
 * @see Technique#XY_CHAIN
 */
public final class XyChain extends AlternatingChain {
	
	/**
	 * Constructs the XY-Chain strategy. The strategy is stateless and holds no grid.
	 */
	public XyChain() {
		super(Technique.XY_CHAIN, false, true, false);
	}
}
