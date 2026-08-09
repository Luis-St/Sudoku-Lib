package net.luis.sudoku.solver;

import java.util.List;
import java.util.Optional;

/**
 * The dynamic contradiction chain: {@link Nishio} with the whole pattern repertoire available while the trial runs.
 * <p>
 *     The argument is identical — assume a candidate, push it as far as it goes, discard it if the grid falls apart —
 *     but the propagation may now use locked candidates, subsets, fish and wings rather than singles alone. That
 *     reaches contradictions no plain Nishio finds, at the cost of a trial that is far harder to follow: each step of
 *     the refutation is itself a technique the player has to spot.
 * </p>
 * <p>
 *     The trial deliberately stops below the assumption-based techniques, so no trial ever opens a trial of its own.
 *     This is the last technique in the solver; a puzzle it cannot break is beyond the modelled set entirely.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Nishio
 * @see Technique#DYNAMIC_CONTRADICTION_CHAIN
 */
public final class DynamicContradictionChain implements TechniqueStrategy {
	
	/**
	 * Constructs the dynamic contradiction chain strategy. The strategy is stateless and holds no grid.
	 */
	public DynamicContradictionChain() {}
	
	@Override
	public Technique technique() {
		return Technique.DYNAMIC_CONTRADICTION_CHAIN;
	}
	
	/**
	 * Tries every candidate in turn, reasoning with the full pattern set, and reports the first that cannot be true.
	 *
	 * @param grid The working grid; never mutated
	 * @return The elimination of the first refuted candidate, or empty if none is refuted
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		List<TechniqueStrategy> patterns = Assumptions.PATTERNS;
		return Contradictions.refute(grid, patterns, Assumptions.FULL_STEPS, Technique.DYNAMIC_CONTRADICTION_CHAIN);
	}
}
