package net.luis.sudoku.solver;

/**
 * The human solving techniques the {@link TechniqueSolver} knows, listed in strictly escalating difficulty order.
 * <p>
 *     The declaration order <b>is</b> the difficulty order: {@link #NAKED_SINGLE} is the easiest and {@link #XY_WING}
 *     the hardest, and {@link #rank()} is simply {@code ordinal() + 1}. The solver relies on this ordering: it always
 *     applies the lowest-ranked technique that makes progress before considering any harder one, and it restarts the
 *     scan from the top after every deduction. That guarantee is what makes the difficulty rating meaningful — a
 *     puzzle is only as hard as the hardest technique it genuinely forces, never as hard as a technique that merely
 *     happened to also apply while an easier one was available.
 * </p>
 * <p>
 *     A technique either places a digit or only eliminates candidates. Only {@link #NAKED_SINGLE} and
 *     {@link #HIDDEN_SINGLE} place a digit directly, as reported by {@link #placesDigit()}; every other technique
 *     narrows candidates and lets a later single finish the placement.
 * </p>
 *
 * @see TechniqueSolver
 */
public enum Technique {
	
	/**
	 * A cell with exactly one remaining candidate, which must therefore hold that digit.
	 */
	NAKED_SINGLE,
	/**
	 * A digit that is a candidate of exactly one cell within a unit, which must therefore hold that digit.
	 */
	HIDDEN_SINGLE,
	/**
	 * Two cells of a unit sharing the identical two-candidate set, which removes those two digits from the rest of the unit.
	 */
	NAKED_PAIR,
	/**
	 * Three cells of a unit confined to a common three-candidate set, which removes those three digits from the rest of the unit.
	 */
	NAKED_TRIPLE,
	/**
	 * Two digits confined to the same two cells of a unit, which removes every other candidate from those two cells.
	 */
	HIDDEN_PAIR,
	/**
	 * Three digits confined to the same three cells of a unit, which removes every other candidate from those three cells.
	 */
	HIDDEN_TRIPLE,
	/**
	 * A digit whose candidates within a region all lie on one line, which removes it from the rest of that line.
	 */
	POINTING_PAIR,
	/**
	 * A digit whose candidates within a line all lie in one region, which removes it from the rest of that region.
	 */
	BOX_LINE_REDUCTION,
	/**
	 * A digit confined to the same two columns across two rows (or mirror), which removes it from those columns elsewhere.
	 */
	X_WING,
	/**
	 * The three-row/three-column generalization of the X-Wing.
	 */
	SWORDFISH,
	/**
	 * A bi-value pivot with two bi-value wings, which removes the shared third digit from the cells both wings see.
	 */
	XY_WING;
	
	/**
	 * Returns the difficulty rank of this technique, which is its {@link #ordinal()} plus one.
	 * <p>
	 *     Ranks run from {@code 1} for {@link #NAKED_SINGLE} to {@code 11} for {@link #XY_WING}, so a higher rank is
	 *     always a harder technique.
	 * </p>
	 *
	 * @return The rank, in {@code 1..11}
	 */
	public int rank() {
		return this.ordinal() + 1;
	}
	
	/**
	 * Checks whether this technique places a digit directly rather than only eliminating candidates.
	 *
	 * @return True for {@link #NAKED_SINGLE} and {@link #HIDDEN_SINGLE}, false for every other technique
	 */
	public boolean placesDigit() {
		return this == NAKED_SINGLE || this == HIDDEN_SINGLE;
	}
}
