package net.luis.sudoku.solver;

/**
 * The finned X-Wing: an X-Wing whose base rows carry a few extra candidates, all inside one region.
 * <p>
 *     Either the X-Wing holds, or the digit sits on one of the fins. Both cases place the digit inside the fin's region, so the X-Wing's eliminations survive for the cover cells in that region. Both the finned and the sashimi form are accepted here, since at this size the two are equally hard to read.
 * </p>
 *
 * @see TechniqueStrategy
 * @see BasicFish
 * @see Technique#FINNED_X_WING
 */
public final class FinnedXWing extends BasicFish {
	
	/**
	 * Constructs the finned X-Wing strategy. The strategy is stateless and holds no grid.
	 */
	public FinnedXWing() {
		super(Technique.FINNED_X_WING, 2, FinMode.ANY_FINNED);
	}
}
