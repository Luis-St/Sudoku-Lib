package net.luis.sudoku.solver;

import java.util.Objects;

/**
 * A single digit placement made by the {@link TechniqueSolver}, together with the technique that unlocked it.
 * <p>
 *     A step always represents a placement: {@code cellIndex} receives {@code digit}. The {@code technique} is the
 *     hardest technique that had to be applied to reach that placement since the previous placement — a naked single
 *     found immediately reports {@link Technique#NAKED_SINGLE}, while one that only became a single after an
 *     {@link Technique#X_WING} elimination reports {@code X_WING}. This is exactly what the L8 hint engine needs to
 *     explain a move.
 * </p>
 *
 * @param cellIndex The row-major index of the placed cell
 * @param digit The digit placed into the cell
 * @param technique The hardest technique applied to unlock this placement
 *
 * @see TechniqueSolver#nextStep(net.luis.sudoku.grid.Puzzle)
 */
public record SolveStep(int cellIndex, int digit, Technique technique) {
	
	/**
	 * Constructs a solve step.
	 * <p>
	 *     No range validation is performed on the cell or the digit: the solver is the only producer of steps and
	 *     only ever builds valid ones.
	 * </p>
	 *
	 * @throws NullPointerException If the technique is null
	 */
	public SolveStep {
		Objects.requireNonNull(technique, "Technique must not be null");
	}
}
