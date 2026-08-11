package net.luis.sudoku.solver;

import java.util.List;
import java.util.Optional;

/**
 * Unique rectangle type 2: both roof corners carry the same single extra candidate, which must be used by one of them.
 * <p>
 *     The two floor corners hold nothing but the shared pair. If neither roof corner took its extra digit, all four
 *     corners would be down to the pair and the rectangle would become the deadly pattern. So one of the two roof
 *     corners holds the extra digit, and no cell seeing both of them can.
 * </p>
 *
 * @see TechniqueStrategy
 * @see UniqueRectangle
 * @see Technique#UNIQUE_RECTANGLE_2
 */
public final class UniqueRectangle2 extends UniqueRectangle {
	
	/**
	 * Constructs the type 2 strategy. The strategy is stateless and holds no grid.
	 */
	public UniqueRectangle2() {
		super(Technique.UNIQUE_RECTANGLE_2);
	}
	
	@Override
	Optional<Deduction> test(CandidateGrid grid, int[] corners, int pair, Explanation.Builder explanation) {
		int[] roof = this.roofOf(grid, corners, pair);
		if (roof == null) {
			return Optional.empty();
		}
		
		int extraA = grid.candidates(roof[0]) & ~pair;
		int extraB = grid.candidates(roof[1]) & ~pair;
		if (extraA != extraB || Integer.bitCount(extraA) != 1) {
			return Optional.empty();
		}
		
		int digit = Integer.numberOfTrailingZeros(extraA);
		Optional<Deduction> deduction = ConjugateLinks.eliminateSeenByBoth(grid, digit, roof[0], roof[1], Technique.UNIQUE_RECTANGLE_2, corners);
		// Only a rectangle that removes something is the deduction being returned, so only that one is worth
		// explaining: any earlier one was looked at and rejected.
		if (deduction.isPresent() && explanation != null) {
			this.explainRectangle(grid, corners, pair, explanation);
			// If neither roof corner used the extra digit, all four corners would be down to the pair and the
			// rectangle would be deadly, so one of the two has to hold it.
			explanation.focusDigit(digit)
				.implication(digit, List.of(PatternCell.of(roof[0], CellRole.LINK_ON, digit), PatternCell.of(roof[1], CellRole.LINK_ON, digit)));
		}
		return deduction;
	}
}
