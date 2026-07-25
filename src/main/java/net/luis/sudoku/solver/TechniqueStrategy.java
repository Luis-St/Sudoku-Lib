package net.luis.sudoku.solver;

import java.util.Optional;

/**
 * One human solving technique, able to find the next deduction it can make on a {@link CandidateGrid}.
 * <p>
 *     Every strategy is deterministic and read-only during a search: {@link #find(CandidateGrid)} scans the grid in a
 *     fixed ascending order — by cell, then unit, then digit as the technique dictates — and returns the <b>first</b>
 *     deduction it can make, or an empty optional if the pattern is absent. It must never mutate the grid; only the
 *     returned {@link Deduction} mutates it, and only once the driver chooses to apply it.
 * </p>
 * <p>
 *     A strategy only ever returns a deduction that actually changes the grid. An {@link Deduction.Eliminations} that
 *     would remove nothing is never returned, so the driver can trust that applying any returned deduction makes
 *     progress.
 * </p>
 *
 * @see Deduction
 * @see TechniqueSolver
 */
public interface TechniqueStrategy {
	
	/**
	 * Returns the technique this strategy implements.
	 *
	 * @return The technique
	 */
	Technique technique();
	
	/**
	 * Finds the first deduction this technique can make on the given grid, in ascending scan order.
	 *
	 * @param grid The current working grid, which must not be mutated
	 * @return The first deduction, or an empty optional if this technique cannot make progress
	 */
	Optional<Deduction> find(CandidateGrid grid);
}
