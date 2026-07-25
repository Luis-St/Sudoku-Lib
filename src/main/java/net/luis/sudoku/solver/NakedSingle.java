package net.luis.sudoku.solver;

import java.util.Optional;

/**
 * The naked-single technique: a cell with exactly one remaining candidate must hold that digit.
 * <p>
 *     The scan is deterministic — cells are visited in ascending row-major order and the first empty cell whose
 *     candidate set has collapsed to a single digit is placed. This is the easiest technique and the only one the
 *     solver reaches for before {@link HiddenSingle}.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Technique#NAKED_SINGLE
 */
public final class NakedSingle implements TechniqueStrategy {
	
	/**
	 * Constructs the naked-single strategy. The strategy is stateless and holds no grid.
	 */
	public NakedSingle() {}
	
	@Override
	public Technique technique() {
		return Technique.NAKED_SINGLE;
	}
	
	/**
	 * Scans for the first empty cell with exactly one candidate and returns its forced placement.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first naked single, or empty if no cell has collapsed to a single candidate
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		for (int cell = 0; cell < grid.cellCount(); cell++) {
			if (grid.isEmpty(cell) && grid.candidateCount(cell) == 1) {
				int digit = grid.candidateDigits(cell)[0];
				return Optional.of(new Deduction.Placement(Technique.NAKED_SINGLE, cell, digit));
			}
		}
		return Optional.empty();
	}
}
