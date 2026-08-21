package net.luis.sudoku.solver;

import java.util.List;
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
	
	/**
	 * Finds <b>every</b> placement this technique proves in the given grid, each explained.
	 * <p>
	 *     {@link #find(CandidateGrid)} answers "what is the next move", which is one move by design: a solver applies
	 *     it and scans again. A <i>lesson</i> asks the other question, "which cells does this technique solve here",
	 *     and the two differ whenever a technique applies in several places at once, which the easy ones do
	 *     constantly. A position with three full houses has three right answers, and a player who fills the third has
	 *     used the technique exactly as well as one who filled the first.
	 * </p>
	 * <p>
	 *     Every returned placement holds in the grid <b>as given</b>. Nothing is applied between finds, so no
	 *     placement here depends on another having been made first, and the results may be offered in any order and
	 *     judged independently. The scan order of {@link #find} is kept, and its result is always the first element.
	 * </p>
	 * <p>
	 *     The default returns whatever {@code findExplained} found, if that was a placement at all. A technique that
	 *     only eliminates therefore returns nothing, which is correct: it places nothing. A placement technique that
	 *     can apply in several places overrides this, and the ones that can only ever apply in one need not.
	 * </p>
	 *
	 * @param grid The working grid, which must not be mutated
	 * @return The placements, in scan order, without repeats; empty if this technique places nothing here
	 */
	default List<ExplainedDeduction> findAllPlacements(CandidateGrid grid) {
		return this.findExplained(grid)
			.filter(explained -> explained.deduction() instanceof Deduction.Placement)
			.map(List::of)
			.orElseGet(List::of);
	}
}
