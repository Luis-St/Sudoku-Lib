package net.luis.sudoku.solver;

/**
 * The Jellyfish: the four-line generalization of the X-Wing and Swordfish.
 * <p>
 *     Four base lines confined to four cover lines lock the digit into their intersection. This is the largest fish worth searching for: a fish of five lines always has a smaller complementary fish in the other orientation.
 * </p>
 *
 * @see TechniqueStrategy
 * @see BasicFish
 * @see Technique#JELLYFISH
 */
public final class Jellyfish extends BasicFish {
	
	/**
	 * Constructs the Jellyfish strategy. The strategy is stateless and holds no grid.
	 */
	public Jellyfish() {
		super(Technique.JELLYFISH, 4, FinMode.NONE);
	}
}
