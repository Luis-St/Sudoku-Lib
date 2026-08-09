package net.luis.sudoku.solver;

/**
 * The finned Swordfish: a Swordfish whose base lines carry a few extra candidates, all inside one region.
 * <p>
 *     The fin argument of the finned X-Wing applied one size up. Every base line still keeps at least two candidates inside the cover set, so the underlying Swordfish is complete and only the eliminations are narrowed to the fin's region.
 * </p>
 *
 * @see TechniqueStrategy
 * @see BasicFish
 * @see Technique#FINNED_SWORDFISH
 */
public final class FinnedSwordfish extends BasicFish {
	
	/**
	 * Constructs the finned Swordfish strategy. The strategy is stateless and holds no grid.
	 */
	public FinnedSwordfish() {
		super(Technique.FINNED_SWORDFISH, 3, FinMode.FINNED);
	}
}
