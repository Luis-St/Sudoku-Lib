package net.luis.sudoku.solver;

import java.util.Optional;

/**
 * The 2-String Kite: a conjugate pair of a digit in a row and one in a column, whose near ends share a region.
 * <p>
 *     The two near ends lie in the same region, so at most one of them holds the digit. If the row's near end does
 *     not, the row's far end does; if the column's near end does not, the column's far end does. One of the two far
 *     ends therefore always holds it, and no cell seeing both of them can.
 * </p>
 * <p>
 *     It is the {@link Skyscraper} with one of its two links turned through ninety degrees — same length, same
 *     conclusion, joined through a region instead of a line.
 * </p>
 * <p>
 *     The scan is deterministic — digits ascending, rows ascending, then columns ascending — and the first
 *     configuration that actually removes a candidate is returned.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Technique#TWO_STRING_KITE
 */
public final class TwoStringKite implements TechniqueStrategy {
	
	/**
	 * Constructs the 2-String Kite strategy. The strategy is stateless and holds no grid.
	 */
	public TwoStringKite() {}
	
	@Override
	public Technique technique() {
		return Technique.TWO_STRING_KITE;
	}
	
	/**
	 * Scans every row-column pair of conjugate pairs for the kite shape.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first 2-String Kite elimination, or empty if the pattern makes no progress anywhere
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		return this.scan(grid, null);
	}
	
	/**
	 * Explains the kite as the two-link chain it is: the digit, the row and the column it is confined to, the link
	 * along each of them, and the pair of far ends one of which must therefore hold it.
	 *
	 * @param grid The working grid; never mutated
	 * @return The eliminations and their explanation, or empty if the pattern makes no progress anywhere
	 */
	@Override
	public Optional<ExplainedDeduction> findExplained(CandidateGrid grid) {
		Explanation.Builder builder = Explanation.builder(Technique.TWO_STRING_KITE);
		return this.scan(grid, builder).map(deduction -> new ExplainedDeduction(deduction, builder.conclusion(deduction).build()));
	}
	
	private Optional<Deduction> scan(CandidateGrid grid, Explanation.Builder explanation) {
		int n = grid.n();
		for (int digit = 1; digit <= n; digit++) {
			for (int row = 0; row < n; row++) {
				int[] rowPair = this.pairOf(grid, grid.rowCells(row), digit);
				if (rowPair == null) {
					continue;
				}
				
				for (int column = 0; column < n; column++) {
					int[] columnPair = this.pairOf(grid, grid.columnCells(column), digit);
					if (columnPair == null) {
						continue;
					}
					
					Optional<Deduction> found = this.test(grid, digit, rowPair, columnPair, explanation);
					if (found.isPresent()) {
						return found;
					}
				}
			}
		}
		return Optional.empty();
	}
	
	private Optional<Deduction> test(CandidateGrid grid, int digit, int[] rowPair, int[] columnPair, Explanation.Builder explanation) {
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 2; j++) {
				int rowNear = rowPair[i];
				int columnNear = columnPair[j];
				int rowFar = rowPair[1 - i];
				int columnFar = columnPair[1 - j];
				
				// The two near ends must be distinct cells of one region; the far ends must be distinct too, and
				// neither may coincide with a near end, or the kite degenerates into a single conjugate pair.
				if (rowNear == columnNear || grid.regionOf(rowNear) != grid.regionOf(columnNear)) {
					continue;
				}
				if (rowFar == columnFar || rowFar == columnNear || columnFar == rowNear) {
					continue;
				}
				
				Optional<Deduction> found = ConjugateLinks.eliminateSeenByBoth(grid, digit, rowFar, columnFar, Technique.TWO_STRING_KITE, rowNear, columnNear);
				// Only a configuration that removes something is the deduction being returned, so only that one is
				// worth explaining: any earlier one was looked at and rejected.
				if (found.isPresent()) {
					if (explanation != null) {
						// End to end: along the row to the near end, across the shared region, and out along the column.
						Explanations.chain(grid, digit, new int[] { rowFar, rowNear, columnNear, columnFar }, explanation);
					}
					return found;
				}
			}
		}
		return Optional.empty();
	}
	
	private int[] pairOf(CandidateGrid grid, int[] lineCells, int digit) {
		int first = -1;
		int second = -1;
		int seen = 0;
		for (int cell : lineCells) {
			if (grid.hasCandidate(cell, digit)) {
				if (seen == 0) {
					first = cell;
				} else {
					second = cell;
				}
				seen++;
			}
		}
		return seen == 2 ? new int[] { first, second } : null;
	}
}
