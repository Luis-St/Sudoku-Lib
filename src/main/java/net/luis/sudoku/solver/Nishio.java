package net.luis.sudoku.solver;

import java.util.List;
import java.util.Optional;

/**
 * Nishio: assume a candidate, follow it with singles alone, and discard it if it runs into a contradiction.
 * <p>
 *     This is the first technique in the solver that reasons by trial rather than by pattern. Nothing about it is
 *     clever — it simply supposes a digit and pushes the consequences through the grid with the easiest techniques
 *     available. What makes it hard for a player is the bookkeeping: the trial has to be carried far enough to hit a
 *     cell with no candidates left or a unit with nowhere to put a digit, and then unwound.
 * </p>
 * <p>
 *     Only singles are allowed during propagation, which is what keeps Nishio apart from the
 *     {@link DynamicContradictionChain} a level above, where the whole pattern repertoire may be used.
 * </p>
 * <p>
 *     The scan is deterministic — cells ascending, digits ascending — and the first candidate that is refuted is
 *     returned.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Assumptions
 * @see Technique#NISHIO
 */
public final class Nishio implements TechniqueStrategy {
	
	/**
	 * Constructs the Nishio strategy. The strategy is stateless and holds no grid.
	 */
	public Nishio() {}
	
	@Override
	public Technique technique() {
		return Technique.NISHIO;
	}
	
	/**
	 * Tries every candidate in turn and reports the first that cannot be true.
	 *
	 * @param grid The working grid; never mutated
	 * @return The elimination of the first refuted candidate, or empty if none is refuted
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		return Contradictions.refute(grid, this.propagation(), Assumptions.CHAIN_STEPS, this.technique());
	}
	
	/**
	 * Returns the techniques the trial may use, which is what separates this technique from the dynamic form.
	 *
	 * @return The propagation technique set
	 */
	List<TechniqueStrategy> propagation() {
		return Assumptions.SINGLES;
	}
}
