package net.luis.sudoku.solver;

/**
 * The Swordfish: the three-line generalization of the X-Wing.
 * <p>
 *     Three rows whose candidates for a digit lie within the same three columns use those three columns up, so the digit can be removed from them in every other row. The rows need not each carry all three columns, which is what makes a Swordfish considerably harder to see than an X-Wing.
 * </p>
 *
 * @see TechniqueStrategy
 * @see BasicFish
 * @see Technique#SWORDFISH
 */
public final class Swordfish extends BasicFish {
	
	/**
	 * Constructs the Swordfish strategy. The strategy is stateless and holds no grid.
	 */
	public Swordfish() {
		super(Technique.SWORDFISH, 3, FinMode.NONE);
	}
}
