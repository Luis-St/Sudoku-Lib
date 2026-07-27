package net.luis.sudoku.grid;

import java.util.Arrays;
import java.util.Objects;

/**
 * A single mutable cell of a puzzle, carrying the three layers a player can see: the given clue, the pen value
 * and the pencil marks.
 * <p>
 *     A cell is either a given or it is not, and that decision is made once at construction time. A given can
 *     never be edited through {@link #setValue(int)}, {@link #clear()} or any of the adding pencil-mark methods;
 *     only {@link #restoreFrom(Cell)} may change the flag, because the undo stack stores whole previous cell
 *     states rather than diffs.
 * </p>
 * <p>
 *     Pencil marks are stored as an {@code int} bitmask in which bit {@code d} is set for digit {@code d}, so
 *     bit 0 is never used and the highest usable bit is 16. The bitmask keeps the marks allocation-free and
 *     gives every iteration over them an ascending, hash-independent order.
 * </p>
 * <p>
 *     A cell does not know the grid it belongs to, so it validates digits against the absolute maximum of
 *     {@code 1..16} only. Narrowing that to {@code 1..n} is {@link Puzzle}'s responsibility.
 * </p>
 *
 * @see Puzzle
 */
public final class Cell {
	
	private static final int MAX_DIGIT = 16;
	private static final int VALID_MARKS = ((1 << (MAX_DIGIT + 1)) - 1) & ~1;
	
	private boolean given;
	private int value;
	private int pencilMarks;
	
	/**
	 * Constructs an empty cell that is not a given and carries no pencil marks.
	 */
	public Cell() {}
	
	private Cell(boolean given, int value, int pencilMarks) {
		this.given = given;
		this.value = value;
		this.pencilMarks = pencilMarks;
	}
	
	/**
	 * Creates an empty cell that is not a given.
	 *
	 * @return A new empty cell
	 */
	public static Cell empty() {
		return new Cell();
	}
	
	/**
	 * Creates a given cell holding the passed clue digit.
	 *
	 * @param digit The clue digit
	 * @return A new given cell
	 * @throws IllegalArgumentException If the digit is not in {@code 1..16}
	 */
	public static Cell given(int digit) {
		checkDigit(digit);
		return new Cell(true, digit, 0);
	}
	
	private static void checkDigit(int digit) {
		if (digit < 1 || digit > MAX_DIGIT) {
			throw new IllegalArgumentException("Digit " + digit + " is not in 1.." + MAX_DIGIT);
		}
	}
	
	/**
	 * Checks whether this cell is a given clue of the puzzle.
	 *
	 * @return True if this cell is a given, false otherwise
	 */
	public boolean isGiven() {
		return this.given;
	}
	
	/**
	 * Checks whether this cell currently holds no digit.
	 *
	 * @return True if {@link #value()} is {@code 0}, false otherwise
	 */
	public boolean isEmpty() {
		return this.value == 0;
	}
	
	/**
	 * Returns the digit this cell currently holds.
	 *
	 * @return The digit, or {@code 0} if the cell is empty
	 */
	public int value() {
		return this.value;
	}
	
	/**
	 * Sets the digit of this cell.
	 *
	 * @param value The digit to place, or {@code 0} to clear the cell
	 * @throws IllegalStateException If this cell is a given
	 * @throws IllegalArgumentException If the value is not in {@code 0..16}
	 */
	public void setValue(int value) {
		this.checkNotGiven("set the value of");
		if (value != 0) {
			checkDigit(value);
		}
		this.value = value;
	}
	
	/**
	 * Removes the digit and every pencil mark of this cell.
	 *
	 * @throws IllegalStateException If this cell is a given
	 */
	public void clear() {
		this.checkNotGiven("clear");
		this.value = 0;
		this.pencilMarks = 0;
	}
	
	/**
	 * Returns the raw pencil-mark bitmask, in which bit {@code d} is set for digit {@code d}.
	 *
	 * @return The pencil-mark bitmask
	 */
	public int pencilMarks() {
		return this.pencilMarks;
	}
	
	/**
	 * Replaces every pencil mark of this cell with the given bitmask.
	 *
	 * @param mask The bitmask to apply, with bit {@code d} set for digit {@code d}
	 * @throws IllegalStateException If this cell is a given
	 * @throws IllegalArgumentException If bit 0 or any bit above bit 16 is set
	 */
	public void setPencilMarks(int mask) {
		this.checkNotGiven("set pencil marks on");
		if ((mask & ~VALID_MARKS) != 0) {
			throw new IllegalArgumentException("Pencil mark mask " + Integer.toBinaryString(mask) + " contains bits outside 1.." + MAX_DIGIT);
		}
		this.pencilMarks = mask;
	}
	
	/**
	 * Checks whether the given digit is currently pencilled into this cell.
	 *
	 * @param digit The digit to look for
	 * @return True if the mark is present, false otherwise
	 * @throws IllegalArgumentException If the digit is not in {@code 1..16}
	 */
	public boolean hasPencilMark(int digit) {
		checkDigit(digit);
		return (this.pencilMarks & (1 << digit)) != 0;
	}
	
	/**
	 * Adds a pencil mark for the given digit.
	 *
	 * @param digit The digit to pencil in
	 * @return True if the mark was added, false if it was already present
	 * @throws IllegalStateException If this cell is a given
	 * @throws IllegalArgumentException If the digit is not in {@code 1..16}
	 */
	public boolean addPencilMark(int digit) {
		this.checkNotGiven("add a pencil mark to");
		checkDigit(digit);
		
		int bit = 1 << digit;
		if ((this.pencilMarks & bit) != 0) {
			return false;
		}
		
		this.pencilMarks |= bit;
		return true;
	}
	
	/**
	 * Removes the pencil mark of the given digit. This is a no-op on a given cell, as a given never carries marks.
	 *
	 * @param digit The digit to erase
	 * @return True if the mark was present and got removed, false otherwise
	 * @throws IllegalArgumentException If the digit is not in {@code 1..16}
	 */
	public boolean removePencilMark(int digit) {
		checkDigit(digit);
		
		int bit = 1 << digit;
		if ((this.pencilMarks & bit) == 0) {
			return false;
		}
		
		this.pencilMarks &= ~bit;
		return true;
	}
	
	/**
	 * Adds the pencil mark of the given digit if it is missing, and removes it otherwise.
	 *
	 * @param digit The digit to toggle
	 * @return True if the mark is present after the call, false if it was erased
	 * @throws IllegalStateException If this cell is a given
	 * @throws IllegalArgumentException If the digit is not in {@code 1..16}
	 */
	public boolean togglePencilMark(int digit) {
		this.checkNotGiven("toggle a pencil mark on");
		checkDigit(digit);
		
		this.pencilMarks ^= 1 << digit;
		return (this.pencilMarks & (1 << digit)) != 0;
	}
	
	/**
	 * Removes every pencil mark of this cell. This is a no-op on a given cell, as a given never carries marks.
	 */
	public void clearPencilMarks() {
		this.pencilMarks = 0;
	}
	
	/**
	 * Returns how many digits are currently pencilled into this cell.
	 *
	 * @return The number of pencil marks
	 */
	public int pencilMarkCount() {
		return Integer.bitCount(this.pencilMarks);
	}
	
	/**
	 * Returns the pencilled digits in ascending order.
	 *
	 * @return A new array of the marked digits, empty if there are none
	 */
	public int[] pencilMarkDigits() {
		int[] digits = new int[Integer.bitCount(this.pencilMarks)];
		
		int count = 0;
		for (int digit = 1; digit <= MAX_DIGIT; digit++) {
			if ((this.pencilMarks & (1 << digit)) != 0) {
				digits[count++] = digit;
			}
		}
		return digits;
	}
	
	/**
	 * Creates an independent copy of this cell, including its value, its pencil marks and its given flag.
	 *
	 * @return The copied cell
	 */
	public Cell copy() {
		return new Cell(this.given, this.value, this.pencilMarks);
	}
	
	/**
	 * Overwrites this cell's value, pencil marks and given flag with those of the passed cell.
	 * <p>
	 *     This is the one method that may turn a given into a non-given and back, because the undo stack stores
	 *     whole previous cell states instead of diffs.
	 * </p>
	 *
	 * @param other The cell to copy the state from
	 */
	public void restoreFrom(Cell other) {
		this.given = other.given;
		this.value = other.value;
		this.pencilMarks = other.pencilMarks;
	}
	
	private void checkNotGiven(String action) {
		if (this.given) {
			throw new IllegalStateException("Cannot " + action + " a given cell holding " + this.value);
		}
	}
	
	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		return object instanceof Cell cell && this.given == cell.given && this.value == cell.value && this.pencilMarks == cell.pencilMarks;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.given, this.value, this.pencilMarks);
	}
	
	@Override
	public String toString() {
		return "Cell[value=" + this.value + ", given=" + this.given + ", pencilMarks=" + Arrays.toString(this.pencilMarkDigits()) + "]";
	}
}
