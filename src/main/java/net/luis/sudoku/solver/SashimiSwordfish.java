package net.luis.sudoku.solver;

/**
 * The sashimi Swordfish: a finned Swordfish in which a base line is down to a single cover candidate.
 * <p>
 *     The fish it is built on is incomplete, which is exactly what makes it harder to see than the plain finned form: one of its corners is missing and only the fin keeps the argument alive. The elimination rule is unchanged.
 * </p>
 *
 * @see TechniqueStrategy
 * @see BasicFish
 * @see Technique#SASHIMI_SWORDFISH
 */
public final class SashimiSwordfish extends BasicFish {
	
	/**
	 * Constructs the sashimi Swordfish strategy. The strategy is stateless and holds no grid.
	 */
	public SashimiSwordfish() {
		super(Technique.SASHIMI_SWORDFISH, 3, FinMode.SASHIMI);
	}
}
