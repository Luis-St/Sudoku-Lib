package net.luis.sudoku.solver;

/**
 * The naked-quad technique: four cells of a unit confined to a common four-candidate set eliminate those four digits from the rest of the unit.
 * <p>
 *     The quad is the largest naked subset worth searching for: beyond four cells the complementary hidden subset is always smaller and therefore easier to see instead. Its cells may each carry as few as two of the four digits.
 * </p>
 *
 * @see TechniqueStrategy
 * @see NakedSubset
 * @see Technique#NAKED_QUAD
 */
public final class NakedQuad extends NakedSubset {
	
	/**
	 * Constructs the naked-quad strategy. The strategy is stateless and holds no grid.
	 */
	public NakedQuad() {
		super(Technique.NAKED_QUAD, 4);
	}
}
