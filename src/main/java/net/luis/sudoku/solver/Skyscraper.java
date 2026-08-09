package net.luis.sudoku.solver;

import java.util.Optional;

/**
 * The Skyscraper: two parallel lines in which a digit is confined to two cells each, sharing one of their two
 * crossing lines.
 * <p>
 *     Say the digit is confined to columns {@code c} and {@code a} in one row, and to columns {@code c} and {@code b}
 *     in another. Column {@code c} can carry the digit only once, so at least one of the two rows must place it away
 *     from {@code c} — at {@code a} or at {@code b}. Whatever happens, the digit sits in one of those two far cells,
 *     so no cell seeing both of them can hold it.
 * </p>
 * <p>
 *     The Skyscraper is the two-link single-digit chain whose links run in the same direction; the
 *     {@link TwoStringKite} is the same length with one link turned, and the {@link Crane} is the three-link form.
 * </p>
 * <p>
 *     The scan is deterministic — digits ascending, the row form before the column form, line pairs ascending — and
 *     the first configuration that actually removes a candidate is returned.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Technique#SKYSCRAPER
 */
public final class Skyscraper implements TechniqueStrategy {
	
	/**
	 * Constructs the Skyscraper strategy. The strategy is stateless and holds no grid.
	 */
	public Skyscraper() {}
	
	@Override
	public Technique technique() {
		return Technique.SKYSCRAPER;
	}
	
	/**
	 * Scans for a Skyscraper, row form before column form.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first Skyscraper elimination, or empty if the pattern makes no progress anywhere
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		Optional<Deduction> rows = this.scan(grid, true);
		if (rows.isPresent()) {
			return rows;
		}
		return this.scan(grid, false);
	}
	
	private Optional<Deduction> scan(CandidateGrid grid, boolean rowForm) {
		int n = grid.n();
		for (int digit = 1; digit <= n; digit++) {
			for (int first = 0; first < n; first++) {
				int[] firstCells = this.pairOf(grid, rowForm, first, digit);
				if (firstCells == null) {
					continue;
				}
				
				for (int second = first + 1; second < n; second++) {
					int[] secondCells = this.pairOf(grid, rowForm, second, digit);
					if (secondCells == null) {
						continue;
					}
					
					Optional<Deduction> found = this.test(grid, rowForm, digit, firstCells, secondCells);
					if (found.isPresent()) {
						return found;
					}
				}
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Tests two conjugate pairs for the Skyscraper shape: they must share exactly one crossing line, and the two far
	 * ends must be the cells that survive.
	 */
	private Optional<Deduction> test(CandidateGrid grid, boolean rowForm, int digit, int[] first, int[] second) {
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 2; j++) {
				int baseA = first[i];
				int baseB = second[j];
				int roofA = first[1 - i];
				int roofB = second[1 - j];
				
				// The two base cells must sit on the same crossing line, and the two roofs must not - otherwise
				// the pattern is an X-Wing, which is a different and easier technique.
				if (this.crossOf(grid, rowForm, baseA) != this.crossOf(grid, rowForm, baseB)) {
					continue;
				}
				if (this.crossOf(grid, rowForm, roofA) == this.crossOf(grid, rowForm, roofB)) {
					continue;
				}
				
				Optional<Deduction> found = ConjugateLinks.eliminateSeenByBoth(grid, digit, roofA, roofB, Technique.SKYSCRAPER, baseA, baseB);
				if (found.isPresent()) {
					return found;
				}
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Returns the two cells a digit is confined to on the given line, or null if the line does not hold exactly two.
	 */
	private int[] pairOf(CandidateGrid grid, boolean rowForm, int line, int digit) {
		int[] lineCells = rowForm ? grid.rowCells(line) : grid.columnCells(line);
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
	
	private int crossOf(CandidateGrid grid, boolean rowForm, int cell) {
		return rowForm ? grid.columnOf(cell) : grid.rowOf(cell);
	}
}
