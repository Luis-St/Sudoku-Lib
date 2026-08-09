package net.luis.sudoku.solver;

/**
 * The naked-pair technique: two cells of a unit sharing the identical two-candidate set eliminate those two digits from the rest of the unit.
 * <p>
 *     If two cells of a unit can only hold the same two digits, those two digits are used up by that pair within the unit, so no other cell of the unit may hold either of them. This is the smallest naked subset and the easiest of the three to see.
 * </p>
 *
 * @see TechniqueStrategy
 * @see NakedSubset
 * @see Technique#NAKED_PAIR
 */
public final class NakedPair extends NakedSubset {
	
	/**
	 * Constructs the naked-pair strategy. The strategy is stateless and holds no grid.
	 */
	public NakedPair() {
		super(Technique.NAKED_PAIR, 2);
	}
}
