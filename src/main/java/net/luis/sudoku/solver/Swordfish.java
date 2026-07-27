package net.luis.sudoku.solver;

import java.util.Arrays;
import java.util.Optional;

/**
 * The Swordfish fish technique, the three-line generalization of the {@link XWing X-Wing}.
 * <p>
 *     In its row form, three rows whose candidates for a digit all fall within the same three columns — each row
 *     carrying two or three of them — force the digit into those three columns one per row. The digit can then be
 *     eliminated from those three columns in every other row. The column form is the exact mirror over three
 *     columns confined to three rows.
 * </p>
 * <p>
 *     Each participating base line is required to hold two or three candidates: a line with a single candidate is
 *     a hidden single and is handled by a lower-ranked technique. The scan is fully deterministic. The row form
 *     is tested first, over every digit in ascending value order and every triple of rows in ascending index
 *     order, and only then the column form. The first elimination that actually removes a candidate is returned;
 *     a fish that would remove nothing is skipped so that {@link #find} never produces a no-op
 *     {@link Deduction.Eliminations}.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Technique#SWORDFISH
 */
public final class Swordfish implements TechniqueStrategy {
	
	/**
	 * Constructs the Swordfish strategy. The strategy is stateless and holds no grid.
	 */
	public Swordfish() {}
	
	@Override
	public Technique technique() {
		return Technique.SWORDFISH;
	}
	
	/**
	 * Scans for a Swordfish, row form before column form, and returns the first elimination it enables.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first Swordfish elimination, or empty if no fish makes progress
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
			int[] lineMasks = new int[n];
			for (int base = 0; base < n; base++) {
				lineMasks[base] = this.lineMask(grid, rowForm, base, digit);
			}
			
			for (int base1 = 0; base1 < n; base1++) {
				if (!this.eligible(lineMasks[base1])) {
					continue;
				}
				
				for (int base2 = base1 + 1; base2 < n; base2++) {
					if (!this.eligible(lineMasks[base2])) {
						continue;
					}
					
					for (int base3 = base2 + 1; base3 < n; base3++) {
						if (!this.eligible(lineMasks[base3])) {
							continue;
						}
						
						int union = lineMasks[base1] | lineMasks[base2] | lineMasks[base3];
						if (Integer.bitCount(union) != 3) {
							continue;
						}
						
						int count = 0;
						int covers = union;
						while (covers != 0) {
							int cover = Integer.numberOfTrailingZeros(covers);
							covers &= covers - 1;
							int[] coverCells = rowForm ? grid.columnCells(cover) : grid.rowCells(cover);
							for (int cell : coverCells) {
								int base = rowForm ? grid.rowOf(cell) : grid.columnOf(cell);
								
								if (base != base1 && base != base2 && base != base3 && grid.hasCandidate(cell, digit)) {
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
							return Optional.of(new Deduction.Eliminations(Technique.SWORDFISH, resultCells, resultDigits));
						}
					}
				}
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Checks whether a base line may take part in a Swordfish: it must hold two or three candidates of the digit.
	 *
	 * @param mask The cover bitmask of the base line
	 * @return True if the line holds two or three candidates, false otherwise
	 */
	private boolean eligible(int mask) {
		int count = Integer.bitCount(mask);
		return count == 2 || count == 3;
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
