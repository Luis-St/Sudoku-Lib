package net.luis.sudoku.hint;

import net.luis.sudoku.solver.Technique;

import java.util.Objects;

/**
 * The first stage of a hint: the cell the player should look at and the technique that solves it, but not yet the
 * digit.
 * <p>
 *     This mirrors §4.4's first tap — the engine highlights a cell that is solvable from the information already on the
 *     board without revealing the answer. The digit is withheld until the candidate is passed back to
 *     {@link HintEngine#consume(net.luis.sudoku.grid.Puzzle, HintCandidate)} for the second tap.
 * </p>
 *
 * @param cellIndex The row-major index of the cell the hint points at
 * @param technique The technique that makes the cell solvable
 *
 * @see HintEngine#peek(net.luis.sudoku.grid.Puzzle)
 */
public record HintCandidate(int cellIndex, Technique technique) {
	
	/**
	 * Constructs a hint candidate.
	 *
	 * @throws NullPointerException If the technique is null
	 */
	public HintCandidate {
		Objects.requireNonNull(technique, "Technique must not be null");
	}
}
