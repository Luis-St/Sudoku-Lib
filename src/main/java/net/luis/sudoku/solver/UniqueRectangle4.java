package net.luis.sudoku.solver;

import java.util.List;
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
	Optional<Deduction> test(CandidateGrid grid, int[] corners, int pair, Explanation.Builder explanation) {
		int[] roof = this.roofOf(grid, corners, pair);
		if (roof == null) {
			return Optional.empty();
		}
		
		int first = Integer.numberOfTrailingZeros(pair);
		int second = Integer.numberOfTrailingZeros(pair & (pair - 1));
		for (int locked : new int[] { first, second }) {
			int[] lockingUnit = this.lockingUnit(grid, roof, locked);
			if (lockingUnit == null) {
				continue;
			}
			
			EliminationBuilder builder = new EliminationBuilder();
			int removed = locked == first ? second : first;
			builder.add(grid, roof[0], removed);
			builder.add(grid, roof[1], removed);
			
			Optional<Deduction> found = builder.build(Technique.UNIQUE_RECTANGLE_4);
			// Only a rectangle that removes something is the deduction being returned, so only that one is worth
			// explaining: any earlier one was looked at and rejected.
			if (found.isPresent()) {
				if (explanation != null) {
					this.explainRectangle(grid, corners, pair, explanation);
					// The locked digit is certainly used by one of the roof corners, so the other one taking the
					// pair's other digit would leave all four corners filled from the pair alone.
					explanation.focusDigit(locked)
						.focusUnits(locked, List.of(Explanations.refOf(grid, lockingUnit, roof[0])))
						.implication(locked, List.of(PatternCell.of(roof[0], CellRole.LINK_ON, locked), PatternCell.of(roof[1], CellRole.LINK_ON, locked)));
				}
				return found;
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Returns the first unit holding both roof corners that confines the digit to exactly those two cells, or null if
	 * no unit does.
	 */
	private int[] lockingUnit(CandidateGrid grid, int[] roof, int digit) {
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
				return unit;
			}
		}
		return null;
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
