package net.luis.sudoku.solver;

/**
 * The X-Wing: a digit confined to the same two columns across two rows, or the mirror, is locked into the rectangle they form.
 * <p>
 *     Whichever way the digit falls in the two rows, both columns carry it inside the rectangle, so it can be removed from those two columns in every other row. This is the smallest fish and the first technique that argues across units.
 * </p>
 *
 * @see TechniqueStrategy
 * @see BasicFish
 * @see Technique#X_WING
 */
public final class XWing extends BasicFish {
	
	/**
	 * Constructs the X-Wing strategy. The strategy is stateless and holds no grid.
	 */
	public XWing() {
		super(Technique.X_WING, 2, FinMode.NONE);
	}
}
