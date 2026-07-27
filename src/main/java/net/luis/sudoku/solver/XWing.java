package net.luis.sudoku.solver;

import java.util.Arrays;
import java.util.Optional;

/**
 * The X-Wing fish technique.
 * <p>
 *     In its row form, a digit whose candidates in two rows are confined to the very same two columns — exactly
 *     two candidates in each of those rows — forms a rectangle. Whichever way the digit is assigned in the two
 *     rows, both of those columns end up carrying the digit inside the rectangle, so it can be eliminated from
 *     those two columns in every other row. The column form is the exact mirror: two columns confined to the
 *     same two rows eliminate the digit from those rows elsewhere.
 * </p>
 * <p>
 *     The scan is fully deterministic. The row form is tested first, over every digit in ascending value order
 *     and every pair of rows in ascending index order, and only then the column form. The first elimination that
 *     actually removes a candidate is returned; a rectangle that would remove nothing is skipped so that
 *     {@link #find} never produces a no-op {@link Deduction.Eliminations}.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Technique#X_WING
 */
public final class XWing implements TechniqueStrategy {
	
	/**
	 * Constructs the X-Wing strategy. The strategy is stateless and holds no grid.
	 */
	public XWing() {}
	
	@Override
	public Technique technique() {
		return Technique.X_WING;
	}
	
	/**
	 * Scans for an X-Wing rectangle, row form before column form, and returns the first elimination it enables.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first X-Wing elimination, or empty if no rectangle makes progress
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		Optional<Deduction> rows = this.scan(grid, true);
		if (rows.isPresent()) {
			return rows;
		}
		return this.scan(grid, false);
	}
	
	/**
	 * Scans one orientation of the technique. When {@code rowForm} is true the base sets are rows and the cover
	 * sets are columns; otherwise the roles are swapped.
	 *
	 * @param grid The working grid
	 * @param rowForm True to scan the row form, false to scan the column form
	 * @return The first elimination found in this orientation, or empty
	 */
	private Optional<Deduction> scan(CandidateGrid grid, boolean rowForm) {
		int n = grid.n();
		int[] cells = new int[grid.cellCount()];
		int[] digits = new int[grid.cellCount()];
		for (int digit = 1; digit <= n; digit++) {
			for (int base1 = 0; base1 < n; base1++) {
				int mask1 = this.lineMask(grid, rowForm, base1, digit);
				if (Integer.bitCount(mask1) != 2) {
					continue;
				}
				
				for (int base2 = base1 + 1; base2 < n; base2++) {
					int mask2 = this.lineMask(grid, rowForm, base2, digit);
					if (mask2 != mask1) {
						continue;
					}
					
					int count = 0;
					int coverA = Integer.numberOfTrailingZeros(mask1);
					int coverB = Integer.numberOfTrailingZeros(mask1 & (mask1 - 1));
					for (int cover : new int[] { coverA, coverB }) {
						int[] coverCells = rowForm ? grid.columnCells(cover) : grid.rowCells(cover);
						for (int cell : coverCells) {
							int base = rowForm ? grid.rowOf(cell) : grid.columnOf(cell);
							if (base != base1 && base != base2 && grid.hasCandidate(cell, digit)) {
								cells[count] = cell;
								digits[count] = digit;
								count++;
							}
						}
					}
					
					if (count > 0) {
						int[] resultCells = Arrays.copyOf(cells, count);
						int[] resultDigits = Arrays.copyOf(digits, count);
						this.sortByCell(resultCells, resultDigits);
						return Optional.of(new Deduction.Eliminations(Technique.X_WING, resultCells, resultDigits));
					}
				}
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Builds the bitmask of cover-line indices in which the digit is a candidate along the given base line. For
	 * the row form the returned bits are columns; for the column form they are rows.
	 *
	 * @param grid The working grid
	 * @param rowForm True if the base line is a row, false if it is a column
	 * @param base The base line index
	 * @param digit The digit to test
	 * @return The bitmask of cover positions, with bit {@code p} set for cover line {@code p}
	 */
	private int lineMask(CandidateGrid grid, boolean rowForm, int base, int digit) {
		int mask = 0;
		int[] lineCells = rowForm ? grid.rowCells(base) : grid.columnCells(base);
		for (int cell : lineCells) {
			if (grid.hasCandidate(cell, digit)) {
				int cover = rowForm ? grid.columnOf(cell) : grid.rowOf(cell);
				mask |= 1 << cover;
			}
		}
		return mask;
	}
	
	/**
	 * Sorts the parallel elimination arrays by ascending cell index so the deduction is canonical regardless of
	 * the order the cover lines were visited in.
	 *
	 * @param cells The cell indices, reordered in place
	 * @param digits The parallel digits, reordered in lock-step
	 */
	private void sortByCell(int[] cells, int[] digits) {
		for (int i = 1; i < cells.length; i++) {
			int cell = cells[i];
			int digit = digits[i];
			int j = i - 1;
			while (j >= 0 && cells[j] > cell) {
				cells[j + 1] = cells[j];
				digits[j + 1] = digits[j];
				j--;
			}
			cells[j + 1] = cell;
			digits[j + 1] = digit;
		}
	}
}
