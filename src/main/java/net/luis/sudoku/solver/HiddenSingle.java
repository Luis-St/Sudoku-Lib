package net.luis.sudoku.solver;

import java.util.Optional;

/**
 * The hidden-single technique: within a unit, a digit that is a candidate of exactly one cell must be placed there.
 * <p>
 *     The scan is deterministic — every unit is visited in {@link CandidateGrid#allUnits()} order (rows, then columns,
 *     then regions), digits in ascending order, and the first digit confined to a single cell of a unit is placed. The
 *     solver only reaches this technique once no {@link NakedSingle} is available.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Technique#HIDDEN_SINGLE
 */
public final class HiddenSingle implements TechniqueStrategy {
	
	/**
	 * Constructs the hidden-single strategy. The strategy is stateless and holds no grid.
	 */
	public HiddenSingle() {}
	
	@Override
	public Technique technique() {
		return Technique.HIDDEN_SINGLE;
	}
	
	/**
	 * Scans every unit for a digit that is a candidate of exactly one of its cells and returns that forced placement.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first hidden single, or empty if no digit is confined to a single cell of any unit
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		int n = grid.n();
		for (int[] unit : grid.allUnits()) {
			for (int digit = 1; digit <= n; digit++) {
				int seen = 0;
				int target = -1;
				for (int cell : unit) {
					if (grid.hasCandidate(cell, digit)) {
						seen++;
						target = cell;
					}
				}
				if (seen == 1) {
					return Optional.of(new Deduction.Placement(Technique.HIDDEN_SINGLE, target, digit));
				}
			}
		}
		return Optional.empty();
	}
}
