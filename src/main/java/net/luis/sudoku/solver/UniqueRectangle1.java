package net.luis.sudoku.solver;

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
	Optional<Deduction> test(CandidateGrid grid, int[] corners, int pair) {
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
		return builder.build(Technique.UNIQUE_RECTANGLE_1);
	}
}
