package net.luis.sudoku.solver;

/**
 * The X-Chain: an alternating chain of strong and weak links in a single digit.
 * <p>
 *     Every node of the chain is the same digit, so the only conclusion it can reach is the first of the three: one of the two ends holds the digit, and no cell seeing both of them can. The Skyscraper, the 2-String Kite and the Crane are the shortest X-Chains, singled out as their own techniques because their shapes are recognizable; this is the unrestricted form, where the chain has to be traced link by link.
 * </p>
 *
 * @see TechniqueStrategy
 * @see AlternatingChain
 * @see Technique#X_CHAIN
 */
public final class XChain extends AlternatingChain {
	
	/**
	 * Constructs the X-Chain strategy. The strategy is stateless and holds no grid.
	 */
	public XChain() {
		super(Technique.X_CHAIN, true, false, false);
	}
}
