package net.luis.sudoku.solver;

import java.util.Objects;

/**
 * A {@link SolveStep} together with the argument for the technique that unlocked it.
 * <p>
 *     A step says which cell can be filled and which technique proves it. That is enough to <i>name</i> a hint and
 *     not nearly enough to <i>apply</i> one: a player told that a W-Wing solves the board is no closer to seeing the
 *     W-Wing. The explanation is the pattern itself, in the same {@link Explanation} vocabulary the learn area's
 *     lessons are drawn from, so a hint can show the cells the argument is made of on the board in front of the
 *     player rather than describe them.
 * </p>
 * <p>
 *     The explanation belongs to the <b>hardest</b> deduction on the way to the placement, which is the same
 *     deduction {@link SolveStep#technique()} names. That is the step worth showing: the eliminations before it are
 *     routine by construction, since the driver always plays the easiest technique that makes progress.
 * </p>
 * <p>
 *     A strategy that has not been taught to explain itself yields an explanation that merely restates its
 *     conclusion, which {@link Explanation#isConclusionOnly()} reports. A consumer that has nothing to draw for such
 *     a step should say so rather than draw an empty pattern.
 * </p>
 *
 * @param step The placement and the technique that unlocked it
 * @param explanation Why that technique applies here
 *
 * @see TechniqueSolver#nextExplainedStep(net.luis.sudoku.grid.Puzzle)
 */
public record ExplainedSolveStep(SolveStep step, Explanation explanation) {
	
	/**
	 * Constructs an explained step.
	 *
	 * @throws NullPointerException If the step or the explanation is null
	 */
	public ExplainedSolveStep {
		Objects.requireNonNull(step, "Step must not be null");
		Objects.requireNonNull(explanation, "Explanation must not be null");
	}
}
