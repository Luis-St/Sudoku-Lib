package net.luis.sudoku.hint;

import net.luis.sudoku.grid.Puzzle;
import net.luis.sudoku.solver.Technique;

import java.util.Objects;

/**
 * The second stage of a hint: the cell, the digit to place there and the technique that justified it.
 * <p>
 *     This mirrors §4.4's second tap — the hint is consumed and the correct digit is revealed for the cell.
 * </p>
 *
 * @param cellIndex The row-major index of the cell to fill
 * @param digit The digit to place into the cell
 * @param technique The technique that justified the placement
 *
 * @see HintEngine#consume(Puzzle, HintCandidate)
 */
public record HintResult(int cellIndex, int digit, Technique technique) {
	
	/**
	 * Constructs a hint result.
	 *
	 * @throws NullPointerException If the technique is null
	 */
	public HintResult {
		Objects.requireNonNull(technique, "Technique must not be null");
	}
}
