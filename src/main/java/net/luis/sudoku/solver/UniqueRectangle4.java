package net.luis.sudoku.solver;

import java.util.Optional;

/**
 * Unique rectangle type 4: one of the two shared digits is locked to the roof corners, so the other cannot sit there.
 * <p>
 *     The two floor corners hold nothing but the shared pair {@code {a, b}}. If, in a unit containing both roof
 *     corners, the digit {@code b} can only go in those two corners, then {@code b} is certainly used by one of them.
 *     Were the other roof corner to take {@code a}, all four corners would be filled from the pair alone and the
 *     rectangle would become the deadly pattern — so {@code a} can be removed from both roof corners.
 * </p>
 *
 * @see TechniqueStrategy
 * @see UniqueRectangle
 * @see Technique#UNIQUE_RECTANGLE_4
 */
public final class UniqueRectangle4 extends UniqueRectangle {
	
	/**
	 * Constructs the type 4 strategy. The strategy is stateless and holds no grid.
	 */
	public UniqueRectangle4() {
		super(Technique.UNIQUE_RECTANGLE_4);
	}
	
	@Override
	Optional<Deduction> test(CandidateGrid grid, int[] corners, int pair) {
		int[] roof = this.roofOf(grid, corners, pair);
		if (roof == null) {
			return Optional.empty();
		}
		
		int first = Integer.numberOfTrailingZeros(pair);
		int second = Integer.numberOfTrailingZeros(pair & (pair - 1));
		for (int locked : new int[] { first, second }) {
			if (!this.isLockedToRoof(grid, roof, locked)) {
				continue;
			}
			
			EliminationBuilder builder = new EliminationBuilder();
			int removed = locked == first ? second : first;
			builder.add(grid, roof[0], removed);
			builder.add(grid, roof[1], removed);
			
			Optional<Deduction> found = builder.build(Technique.UNIQUE_RECTANGLE_4);
			if (found.isPresent()) {
				return found;
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Checks whether some unit holding both roof corners confines the digit to exactly those two cells.
	 */
	private boolean isLockedToRoof(CandidateGrid grid, int[] roof, int digit) {
		for (int[] unit : grid.allUnits()) {
			if (!this.contains(unit, roof[0]) || !this.contains(unit, roof[1])) {
				continue;
			}
			
			int seen = 0;
			for (int cell : unit) {
				if (grid.hasCandidate(cell, digit)) {
					seen++;
				}
			}
			
			if (seen == 2) {
				return true;
			}
		}
		return false;
	}
	
	private boolean contains(int[] unit, int cell) {
		for (int member : unit) {
			if (member == cell) {
				return true;
			}
		}
		return false;
	}
}
