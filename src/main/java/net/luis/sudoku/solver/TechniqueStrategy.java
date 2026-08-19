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
	
	/**
	 * Finds the same deduction {@link #find(CandidateGrid)} would, together with an {@link Explanation} of why it
	 * follows.
	 * <p>
	 *     This must return the deduction {@code find} returns, on every grid: the teaching path and the solving path
	 *     have to agree, or a lesson would show a pattern the solver never used. The explanation is the only addition.
	 * </p>
	 * <p>
	 *     The default builds {@link Explanation#conclusionOnly(Deduction)}, which restates the deduction and claims
	 *     nothing about the pattern behind it. A strategy that has been taught to explain itself overrides this; one
	 *     that has not is still usable everywhere, it simply cannot be taught, which
	 *     {@link Explanation#isConclusionOnly()} reports.
	 * </p>
	 *
	 * @param grid The current working grid, which must not be mutated
	 * @return The first deduction and its explanation, or an empty optional if this technique cannot make progress
	 */
	default Optional<ExplainedDeduction> findExplained(CandidateGrid grid) {
		return this.find(grid).map(deduction -> new ExplainedDeduction(deduction, Explanation.conclusionOnly(deduction)));
	}
}
