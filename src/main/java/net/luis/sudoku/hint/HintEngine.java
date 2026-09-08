package net.luis.sudoku.hint;

import net.luis.sudoku.grid.Puzzle;
import net.luis.sudoku.solver.SolveStep;
import net.luis.sudoku.solver.TechniqueSolver;

import java.util.Objects;
import java.util.Optional;

/**
 * The two-stage hint engine (spec §4.4), built directly on the {@link TechniqueSolver}.
 * <p>
 *     The engine is stateless: {@link #peek(Puzzle)} reports the next cell that is solvable from the information
 *     already on the board without consuming anything, and {@link #consume(Puzzle, HintCandidate)} reveals the digit
 *     for it. Both stages read the puzzle without mutating it, so the caller controls when the digit is actually
 *     placed. The five-hints-per-puzzle cap from §4.4 is a per-session concern and is deliberately <b>not</b> enforced
 *     here — it belongs to the caller, which keeps this library stateless.
 * </p>
 * <p>
 *     Because both stages derive from the same deterministic {@link TechniqueSolver#nextStep(Puzzle)}, peeking and then
 *     consuming on the same board state always agree.
 * </p>
 *
 * @see TechniqueSolver
 */
public final class HintEngine {
	
	private HintEngine() {}
	
	/**
	 * Reports the next hint available on the board without consuming it.
	 *
	 * @param puzzle The current puzzle
	 * @return The cell and technique of the next solvable step, or an empty optional if the puzzle is solved or the
	 *         technique solver cannot make further progress without guessing
	 * @throws NullPointerException If the puzzle is null
	 */
	public static Optional<HintCandidate> peek(Puzzle puzzle) {
		Objects.requireNonNull(puzzle, "Puzzle must not be null");
		return TechniqueSolver.nextStep(puzzle).map(step -> new HintCandidate(step.cellIndex(), step.technique()));
	}
	
	/**
	 * Reports the next hint together with the pattern the player would have to see to make the move themselves.
	 * <p>
	 *     The same hint {@link #peek(Puzzle)} reports - same cell, same technique, same board state - with the
	 *     technique's argument attached. Naming a technique is the smallest useful hint there is, and for anything past
	 *     the singles it is not useful at all: the player who needs to be told that a W-Wing applies is precisely the
	 *     player who cannot find it. Marking the cells the pattern is made of turns the hint into the lesson.
	 * </p>
	 * <p>
	 *     Costlier than {@link #peek(Puzzle)}, since a strategy that records its pattern while it searches does that
	 *     work here. Use {@code peek} where only the cell and the name are wanted.
	 * </p>
	 *
	 * @param puzzle The current puzzle
	 * @return The explained hint, or empty if the puzzle is solved or the solver cannot progress without guessing
	 * @throws NullPointerException If the puzzle is null
	 */
	public static Optional<ExplainedHint> explain(Puzzle puzzle) {
		Objects.requireNonNull(puzzle, "Puzzle must not be null");
		return TechniqueSolver.nextExplainedStep(puzzle)
			.map(explained -> new ExplainedHint(explained.step().cellIndex(), explained.step().technique(), explained.explanation()));
	}
	
	/**
	 * Consumes a hint, revealing the digit to place.
	 * <p>
	 *     The digit is recomputed from the current board rather than stored in the candidate, so the candidate never
	 *     leaks the answer. The puzzle must be in the same state it was {@link #peek(Puzzle) peeked} in — no cell filled
	 *     in between — which is the natural first-tap-then-second-tap flow.
	 * </p>
	 *
	 * @param puzzle The current puzzle, unchanged since the candidate was peeked
	 * @param candidate The candidate returned by {@link #peek(Puzzle)}
	 * @return The cell, digit and technique of the hint
	 * @throws NullPointerException If the puzzle or the candidate is null
	 * @throws IllegalStateException If no hint is available, or the available hint no longer targets the candidate's
	 *         cell because the board changed since it was peeked
	 */
	public static HintResult consume(Puzzle puzzle, HintCandidate candidate) {
		Objects.requireNonNull(puzzle, "Puzzle must not be null");
		Objects.requireNonNull(candidate, "Candidate must not be null");
		
		SolveStep step = TechniqueSolver.nextStep(puzzle).orElseThrow(() -> new IllegalStateException("No hint is available for the current board"));
		if (step.cellIndex() != candidate.cellIndex()) {
			throw new IllegalStateException("The board changed since the hint was peeked; peek again before consuming");
		}
		return new HintResult(step.cellIndex(), step.digit(), step.technique());
	}
}
