package net.luis.sudoku.solver;

/**
 * The naked-triple technique: three cells of a unit confined to a common three-candidate set eliminate those three digits from the rest of the unit.
 * <p>
 *     The three cells need not each hold all three digits; two of them are enough per cell, as long as the union across the three is exactly three digits. That partial-overlap form is what makes a triple markedly harder to spot than a pair.
 * </p>
 *
 * @see TechniqueStrategy
 * @see NakedSubset
 * @see Technique#NAKED_TRIPLE
 */
public final class NakedTriple extends NakedSubset {
	
	/**
	 * Constructs the naked-triple strategy. The strategy is stateless and holds no grid.
	 */
	public NakedTriple() {
		super(Technique.NAKED_TRIPLE, 3);
	}
}
