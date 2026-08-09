package net.luis.sudoku.solver;

import java.util.Optional;

/**
 * A small accumulator for the candidate eliminations a technique proves, which turns them into a
 * {@link Deduction.Eliminations} only once at least one of them actually changes the grid.
 * <p>
 *     Every strategy has the same obligation: it must never return a no-op deduction, and it must return the same
 *     deduction on every JVM. This builder enforces both. {@link #add(CandidateGrid, int, int)} silently drops a
 *     digit that is not a candidate of the cell anyway, so a caller may offer its whole elimination set without
 *     filtering first, and {@link #build(Technique)} returns an empty optional when nothing survived.
 * </p>
 * <p>
 *     The eliminations are kept in the order they were added, which is the strategy's own deterministic scan order.
 *     A strategy that collects them out of order calls {@link #sortByCell()} to canonicalize.
 * </p>
 *
 * @see Deduction.Eliminations
 */
final class EliminationBuilder {
	
	private int[] cells = new int[16];
	private int[] digits = new int[16];
	private int count;
	
	/**
	 * Records that the given digit is to be removed from the given cell, unless it is not a candidate there.
	 *
	 * @param grid The working grid, read only to check the candidate
	 * @param cell The row-major cell index
	 * @param digit The digit to remove
	 * @return True if the elimination was recorded, false if the digit was not a candidate of the cell
	 */
	boolean add(CandidateGrid grid, int cell, int digit) {
		if (!grid.hasCandidate(cell, digit)) {
			return false;
		}
		
		if (this.count == this.cells.length) {
			this.grow();
		}
		
		this.cells[this.count] = cell;
		this.digits[this.count] = digit;
		this.count++;
		return true;
	}
	
	/**
	 * Records the removal of every digit of the given mask from the given cell.
	 *
	 * @param grid The working grid, read only to check the candidates
	 * @param cell The row-major cell index
	 * @param mask The bitmask of digits to remove, bit {@code d} for digit {@code d}
	 */
	void addAll(CandidateGrid grid, int cell, int mask) {
		int remaining = mask & grid.candidates(cell);
		while (remaining != 0) {
			this.add(grid, cell, Integer.numberOfTrailingZeros(remaining));
			remaining &= remaining - 1;
		}
	}
	
	private void grow() {
		int[] widerCells = new int[this.cells.length * 2];
		int[] widerDigits = new int[this.digits.length * 2];
		System.arraycopy(this.cells, 0, widerCells, 0, this.count);
		System.arraycopy(this.digits, 0, widerDigits, 0, this.count);
		this.cells = widerCells;
		this.digits = widerDigits;
	}
	
	/**
	 * Checks whether nothing has been recorded yet.
	 *
	 * @return True if no elimination was recorded
	 */
	boolean isEmpty() {
		return this.count == 0;
	}
	
	/**
	 * Sorts the recorded eliminations by ascending cell index, then by ascending digit, so the resulting deduction is
	 * canonical no matter which order the strategy visited its pattern in.
	 */
	void sortByCell() {
		for (int i = 1; i < this.count; i++) {
			int cell = this.cells[i];
			int digit = this.digits[i];
			int j = i - 1;
			while (j >= 0 && (this.cells[j] > cell || (this.cells[j] == cell && this.digits[j] > digit))) {
				this.cells[j + 1] = this.cells[j];
				this.digits[j + 1] = this.digits[j];
				j--;
			}
			this.cells[j + 1] = cell;
			this.digits[j + 1] = digit;
		}
	}
	
	/**
	 * Builds the deduction for the given technique.
	 *
	 * @param technique The technique that proved the eliminations
	 * @return The deduction, or an empty optional if nothing was recorded
	 */
	Optional<Deduction> build(Technique technique) {
		if (this.count == 0) {
			return Optional.empty();
		}
		
		int[] resultCells = new int[this.count];
		int[] resultDigits = new int[this.count];
		System.arraycopy(this.cells, 0, resultCells, 0, this.count);
		System.arraycopy(this.digits, 0, resultDigits, 0, this.count);
		return Optional.of(new Deduction.Eliminations(technique, resultCells, resultDigits));
	}
}
