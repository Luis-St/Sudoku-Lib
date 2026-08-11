package net.luis.sudoku.solver;

import java.util.List;
import java.util.Optional;

/**
 * Unique rectangle type 1: three corners hold nothing but the two shared digits, so the fourth must hold one of its
 * extra candidates.
 * <p>
 *     If the fourth corner also settled on one of the two shared digits, the rectangle would become the deadly
 *     pattern and the puzzle would have two solutions. It cannot, so both shared digits can be removed from that
 *     corner outright. This is the simplest and by far the most common of the four types.
 * </p>
 *
 * @see TechniqueStrategy
 * @see UniqueRectangle
 * @see Technique#UNIQUE_RECTANGLE_1
 */
public final class UniqueRectangle1 extends UniqueRectangle {
	
	/**
	 * Constructs the type 1 strategy. The strategy is stateless and holds no grid.
	 */
	public UniqueRectangle1() {
		super(Technique.UNIQUE_RECTANGLE_1);
	}
	
	@Override
	Optional<Deduction> test(CandidateGrid grid, int[] corners, int pair, Explanation.Builder explanation) {
		int target = -1;
		for (int corner : corners) {
			if ((grid.candidates(corner) & ~pair) == 0) {
				continue;
			}
			
			if (target != -1) {
				return Optional.empty();
			}
			target = corner;
		}
		
		if (target == -1) {
			return Optional.empty();
		}
		
		EliminationBuilder builder = new EliminationBuilder();
		builder.addAll(grid, target, pair);
		Optional<Deduction> deduction = builder.build(Technique.UNIQUE_RECTANGLE_1);
		// Only a rectangle that removes something is the deduction being returned, so only that one is worth
		// explaining: any earlier one was looked at and rejected.
		if (deduction.isPresent() && explanation != null) {
			this.explainRectangle(grid, corners, pair, explanation);
			// Three corners are down to the pair already, so this one taking either of them would complete the
			// deadly pattern: it has to use one of its own extra candidates instead.
			explanation.implication(0, List.of(new PatternCell(target, CellRole.ROOF, grid.candidates(target) & ~pair)));
		}
		return deduction;
	}
}
