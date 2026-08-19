package net.luis.sudoku.solver;

import java.util.Objects;

/**
 * A {@link Deduction} together with the {@link Explanation} of why it follows.
 * <p>
 *     The two are kept as separate values rather than merged into one, because the solver only ever needs the
 *     deduction and building an explanation costs work that a rating run would pay for millions of times over. The
 *     pairing exists for the teaching path, which asks for it explicitly through
 *     {@link TechniqueStrategy#findExplained(CandidateGrid)}.
 * </p>
 *
 * @param deduction What the technique proved
 * @param explanation Why it follows
 *
 * @see Explanation
 */
public record ExplainedDeduction(Deduction deduction, Explanation explanation) {
	
	/**
	 * Constructs an explained deduction.
	 *
	 * @throws NullPointerException If either component is null
	 * @throws IllegalArgumentException If the two disagree about which technique they belong to
	 */
	public ExplainedDeduction {
		Objects.requireNonNull(deduction, "Deduction must not be null");
		Objects.requireNonNull(explanation, "Explanation must not be null");
		
		if (deduction.technique() != explanation.technique()) {
			throw new IllegalArgumentException("Deduction and explanation disagree: " + deduction.technique() + " against " + explanation.technique());
		}
	}
	
	/**
	 * Returns the technique both components belong to.
	 *
	 * @return The technique
	 */
	public Technique technique() {
		return this.deduction.technique();
	}
}
