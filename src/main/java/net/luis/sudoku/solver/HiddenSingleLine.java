package net.luis.sudoku.solver;

import java.util.Optional;

/**
 * The hidden-single technique restricted to lines: within a row or a column, a digit that is a candidate of exactly
 * one cell must be placed there.
 * <p>
 *     The argument is identical to {@link HiddenSingleRegion}, but a line is spread across the whole grid rather than
 *     compact, so finding one takes deliberate scanning rather than a glance. That difference in human effort is why
 *     it sits a full difficulty level higher.
 * </p>
 * <p>
 *     The scan is deterministic — every row in ascending order first, then every column, digits ascending — and the
 *     first digit confined to a single cell of a line is placed.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Technique#HIDDEN_SINGLE_LINE
 */
public final class HiddenSingleLine implements TechniqueStrategy {
	
	/**
	 * Constructs the line hidden-single strategy. The strategy is stateless and holds no grid.
	 */
	public HiddenSingleLine() {}
	
	@Override
	public Technique technique() {
		return Technique.HIDDEN_SINGLE_LINE;
	}
	
	/**
	 * Scans every row and column for a digit that is a candidate of exactly one of its cells and returns that forced
	 * placement.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first line hidden single, or empty if no digit is confined to a single cell of any line
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		Optional<Deduction> rows = HiddenSingles.scan(grid, grid.rows(), Technique.HIDDEN_SINGLE_LINE);
		if (rows.isPresent()) {
			return rows;
		}
		return HiddenSingles.scan(grid, grid.columns(), Technique.HIDDEN_SINGLE_LINE);
	}

	/**
	 * Explains the single as cross-hatching along the line, in the same rows-before-columns order the scan uses.
	 *
	 * @param grid The working grid; never mutated
	 * @return The placement and its explanation, or empty if there is none
	 */
	@Override
	public Optional<ExplainedDeduction> findExplained(CandidateGrid grid) {
		Optional<ExplainedDeduction> rows = HiddenSingles.scanExplained(grid, grid.rows(), Technique.HIDDEN_SINGLE_LINE);
		if (rows.isPresent()) {
			return rows;
		}
		return HiddenSingles.scanExplained(grid, grid.columns(), Technique.HIDDEN_SINGLE_LINE);
	}
}
