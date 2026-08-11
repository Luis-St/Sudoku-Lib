package net.luis.sudoku.solver;

import java.util.Objects;

/**
 * One cell of a technique's pattern, with the part it plays and the candidates that matter about it.
 * <p>
 *     The digit mask is the same encoding {@link CandidateGrid#candidates(int)} uses, bit {@code d} standing for
 *     digit {@code d}. It is deliberately not the cell's full candidate set: it is the subset the argument is about,
 *     so a consumer can dim everything else. A mask of {@code 0} means the whole cell matters rather than any
 *     particular digit in it.
 * </p>
 *
 * @param cell The row-major cell index
 * @param role The part this cell plays in the pattern
 * @param digits The bitmask of the candidates that matter here, or {@code 0} for the whole cell
 *
 * @see Explanation
 */
public record PatternCell(int cell, CellRole role, int digits) {

	/**
	 * Constructs a pattern cell.
	 *
	 * @throws NullPointerException If the role is null
	 * @throws IllegalArgumentException If the cell index or the digit mask is negative
	 */
	public PatternCell {
		Objects.requireNonNull(role, "Cell role must not be null");

		if (cell < 0) {
			throw new IllegalArgumentException("Cell index must not be negative, but was " + cell);
		}
		if (digits < 0) {
			throw new IllegalArgumentException("Digit mask must not be negative, but was " + digits);
		}
	}

	/**
	 * Creates a pattern cell that is about the whole cell rather than any particular candidate.
	 *
	 * @param cell The row-major cell index
	 * @param role The part this cell plays
	 * @return The pattern cell
	 */
	public static PatternCell of(int cell, CellRole role) {
		return new PatternCell(cell, role, 0);
	}

	/**
	 * Creates a pattern cell that is about a single candidate.
	 *
	 * @param cell The row-major cell index
	 * @param role The part this cell plays
	 * @param digit The digit that matters here
	 * @return The pattern cell
	 * @throws IllegalArgumentException If the digit is not positive
	 */
	public static PatternCell of(int cell, CellRole role, int digit) {
		if (digit <= 0) {
			throw new IllegalArgumentException("Digit must be positive, but was " + digit);
		}
		return new PatternCell(cell, role, 1 << digit);
	}

	/**
	 * Returns the digits of {@link #digits()} in ascending order.
	 *
	 * @return The digits, empty if the mask is {@code 0}
	 */
	public int[] digitList() {
		int[] result = new int[Integer.bitCount(this.digits)];
		int index = 0;
		int remaining = this.digits;
		while (remaining != 0) {
			result[index++] = Integer.numberOfTrailingZeros(remaining);
			remaining &= remaining - 1;
		}
		return result;
	}
}
