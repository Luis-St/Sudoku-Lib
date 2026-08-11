package net.luis.sudoku.solver;

/**
 * What part a cell plays in a technique's pattern.
 * <p>
 *     The role is what lets a consumer draw a technique rather than merely draw its answer: an X-Wing whose four
 *     corners are all painted the same colour teaches nothing, while one that distinguishes the two base lines from
 *     the cells the digit is being removed from teaches the argument itself. The roles below are deliberately generic
 *     — several techniques share one shape, and a consumer should be able to render every technique from this one
 *     vocabulary without a per-technique branch.
 * </p>
 *
 * @see PatternCell
 * @see Explanation
 */
public enum CellRole {

	/**
	 * A cell of the pattern with no more specific part to play, such as one cell of a naked subset.
	 */
	PATTERN,
	/**
	 * A cell of the pattern's base set: the lines a fish is defined on, or the cells a chain starts from.
	 */
	BASE,
	/**
	 * A cell of the pattern's cover set: the lines a fish eliminates along.
	 */
	COVER,
	/**
	 * The pivot of a wing: the cell that sees every wing and forces the argument.
	 */
	PIVOT,
	/**
	 * A wing of a wing pattern: a cell the pivot sees, holding one branch of the argument.
	 */
	WING,
	/**
	 * A fin: the extra candidate that spoils an exact pattern but leaves part of its conclusion standing.
	 */
	FIN,
	/**
	 * The floor of a deadly pattern: the cells holding only the two digits that would make the solution ambiguous.
	 */
	FLOOR,
	/**
	 * The roof of a deadly pattern: the cells carrying the extra candidates that save it from being ambiguous.
	 */
	ROOF,
	/**
	 * A chain link the argument assumes to be true.
	 */
	LINK_ON,
	/**
	 * A chain link the argument assumes to be false.
	 */
	LINK_OFF,
	/**
	 * A cell the technique removes a candidate from, or places a digit into: the conclusion.
	 */
	TARGET,
	/**
	 * A cell shown only to explain why the conclusion follows, such as a cell both chain ends happen to see.
	 */
	CONTEXT
}
