package net.luis.sudoku.solver;

/**
 * The grouped alternating inference chain: an AIC in which a whole group of candidates may act as a single node.
 * <p>
 *     Where a digit's candidates inside one region all sit on the same row or column, that group behaves exactly like one candidate for linking purposes: it is true if any of its cells holds the digit. Allowing such groups as chain nodes reaches eliminations no ungrouped chain does, at the cost of a much larger graph to search.
 * </p>
 *
 * @see TechniqueStrategy
 * @see AlternatingChain
 * @see Technique#GROUPED_AIC
 */
public final class GroupedAic extends AlternatingChain {
	
	/**
	 * Constructs the grouped AIC strategy. The strategy is stateless and holds no grid.
	 */
	public GroupedAic() {
		super(Technique.GROUPED_AIC, false, false, true);
	}
}
