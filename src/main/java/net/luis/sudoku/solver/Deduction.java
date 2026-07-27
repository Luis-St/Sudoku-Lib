package net.luis.sudoku.solver;

import java.util.Arrays;
import java.util.Objects;

/**
 * A single logical step a technique proved on a {@link CandidateGrid}: either a placement or one or more eliminations.
 * <p>
 *     A deduction is a value: it describes what to do without doing it. {@link #applyTo(CandidateGrid)} is what mutates
 *     the grid, and it returns whether the grid actually changed. A correct deduction produced by a strategy always
 *     changes the grid, so the driver treats a {@code false} result as an internal error rather than a normal outcome.
 * </p>
 *
 * @see TechniqueStrategy
 */
public sealed interface Deduction permits Deduction.Placement, Deduction.Eliminations {
	
	/**
	 * Returns the technique that proved this deduction.
	 *
	 * @return The technique
	 */
	Technique technique();
	
	/**
	 * Applies this deduction to the given grid.
	 *
	 * @param grid The grid to mutate
	 * @return True if the grid changed, which a correct deduction always does
	 */
	boolean applyTo(CandidateGrid grid);
	
	/**
	 * A single digit placement: {@code cell} must hold {@code digit}.
	 *
	 * @param technique The technique that proved the placement
	 * @param cell The row-major index of the cell to fill
	 * @param digit The digit to place
	 */
	record Placement(Technique technique, int cell, int digit) implements Deduction {
		
		/**
		 * Constructs a placement.
		 *
		 * @throws NullPointerException If the technique is null
		 */
		public Placement {
			Objects.requireNonNull(technique, "Technique must not be null");
		}
		
		/**
		 * Places the digit into the cell via {@link CandidateGrid#place(int, int)}.
		 *
		 * @param grid The grid to mutate
		 * @return True, as a placement always changes the grid
		 */
		@Override
		public boolean applyTo(CandidateGrid grid) {
			return grid.place(this.cell, this.digit);
		}
	}
	
	/**
	 * One or more candidate eliminations, given as parallel arrays: {@code digits[i]} is removed from {@code cells[i]}.
	 * <p>
	 *     The two arrays have the same, non-zero length. Both are defensively copied on construction and on every
	 *     accessor, so the record stays an immutable value even though its fields are arrays.
	 * </p>
	 *
	 * @param technique The technique that proved the eliminations
	 * @param cells The row-major cell indices to remove a candidate from, parallel to {@code digits}
	 * @param digits The digits to remove, parallel to {@code cells}
	 */
	record Eliminations(Technique technique, int[] cells, int[] digits) implements Deduction {
		
		/**
		 * Constructs an eliminations deduction, defensively copying both arrays.
		 *
		 * @throws NullPointerException If the technique or either array is null
		 * @throws IllegalArgumentException If the arrays differ in length or are empty
		 */
		public Eliminations {
			Objects.requireNonNull(technique, "Technique must not be null");
			Objects.requireNonNull(cells, "Cells must not be null");
			Objects.requireNonNull(digits, "Digits must not be null");
			
			if (cells.length != digits.length) {
				throw new IllegalArgumentException("Cells and digits must have equal length, but were " + cells.length + " and " + digits.length);
			}
			if (cells.length == 0) {
				throw new IllegalArgumentException("An eliminations deduction must remove at least one candidate");
			}
			
			cells = cells.clone();
			digits = digits.clone();
		}
		
		/**
		 * Returns a copy of the cell indices, parallel to {@link #digits()}.
		 *
		 * @return A fresh copy of the cells
		 */
		@Override
		public int[] cells() {
			return this.cells.clone();
		}
		
		/**
		 * Returns a copy of the digits, parallel to {@link #cells()}.
		 *
		 * @return A fresh copy of the digits
		 */
		@Override
		public int[] digits() {
			return this.digits.clone();
		}
		
		/**
		 * Removes every {@code digits[i]} from {@code cells[i]} via {@link CandidateGrid#eliminate(int, int)}.
		 *
		 * @param grid The grid to mutate
		 * @return True if any candidate was actually removed
		 */
		@Override
		public boolean applyTo(CandidateGrid grid) {
			boolean changed = false;
			for (int index = 0; index < this.cells.length; index++) {
				changed |= grid.eliminate(this.cells[index], this.digits[index]);
			}
			return changed;
		}
		
		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			return object instanceof Eliminations other && this.technique == other.technique && Arrays.equals(this.cells, other.cells) && Arrays.equals(this.digits, other.digits);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.technique, Arrays.hashCode(this.cells), Arrays.hashCode(this.digits));
		}
		
		@Override
		public String toString() {
			return "Eliminations[technique=" + this.technique + ", cells=" + Arrays.toString(this.cells) + ", digits=" + Arrays.toString(this.digits) + "]";
		}
	}
}
