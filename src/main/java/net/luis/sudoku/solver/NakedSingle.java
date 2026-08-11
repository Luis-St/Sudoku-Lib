package net.luis.sudoku.solver;

import java.util.List;
import java.util.Optional;

/**
 * The naked-single technique: a cell with exactly one remaining candidate must hold that digit.
 * <p>
 *     The scan is deterministic — cells are visited in ascending row-major order and the first empty cell whose
 *     candidate set has collapsed to a single digit is placed. This is the easiest technique and the only one the
 *     solver reaches for before {@link HiddenSingleRegion}.
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

	/**
	 * Explains the single by showing what emptied the cell: its three units, and the peer that rules out each of the
	 * digits it no longer has.
	 *
	 * @param grid The working grid; never mutated
	 * @return The placement and its explanation, or empty if no cell has collapsed to a single candidate
	 */
	@Override
	public Optional<ExplainedDeduction> findExplained(CandidateGrid grid) {
		return this.find(grid).map(deduction -> {
			Deduction.Placement placement = (Deduction.Placement) deduction;
			int cell = placement.cell();
			return new ExplainedDeduction(deduction, Explanation.builder(Technique.NAKED_SINGLE)
				.focusUnits(0, Explanations.unitsOf(grid, cell))
				.pattern(placement.digit(), List.of(PatternCell.of(cell, CellRole.PATTERN, placement.digit())))
				.implication(0, Explanations.blockersFor(grid, cell, placement.digit()))
				.conclusion(deduction)
				.build());
		});
	}
}
