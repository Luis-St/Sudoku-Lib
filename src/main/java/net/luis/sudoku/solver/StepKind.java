package net.luis.sudoku.solver;

/**
 * What one step of an {@link Explanation} is saying.
 * <p>
 *     A step kind is a narration beat, not a sentence: the library never produces user-facing text, because the text
 *     has to be translated and the library has no locale. A consumer maps each kind onto one localized sentence and
 *     fills in the cells, digits and units the step carries. Keeping the beats to this short list is what makes that
 *     mapping finite instead of one sentence per technique.
 * </p>
 *
 * @see ExplanationStep
 */
public enum StepKind {

	/**
	 * "Look at this digit." Opens an argument that is about one digit throughout, such as any fish.
	 */
	FOCUS_DIGIT,
	/**
	 * "Look at these units." Names the rows, columns or regions the pattern is defined on.
	 */
	FOCUS_UNIT,
	/**
	 * "These cells form the pattern." Shows the pattern cells in their roles.
	 */
	PATTERN,
	/**
	 * "These cells are linked." Shows one inference of a chain or wing argument.
	 */
	LINK,
	/**
	 * "Whichever way it falls, this follows." The reasoning beat between the pattern and its conclusion.
	 */
	IMPLICATION,
	/**
	 * "So this candidate can go." The eliminating conclusion.
	 */
	ELIMINATION,
	/**
	 * "So this cell must hold this digit." The placing conclusion.
	 */
	PLACEMENT
}
