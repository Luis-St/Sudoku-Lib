package net.luis.sudoku.hint;

import net.luis.sudoku.solver.Explanation;
import net.luis.sudoku.solver.Technique;

import java.util.Objects;

/**
 * A peeked hint with the argument behind it: the cell to look at, the technique that solves it, and the pattern that
 * technique is made of in this position.
 * <p>
 *     {@link HintCandidate} names the technique and stops there, which is as far as a hint could go while all it had
 *     was a name. A player who is told "a W-Wing solves this" and cannot find the W-Wing has been given the answer to
 *     a question they were not asking. This carries the {@link Explanation} as well, so the cells the argument is
 *     built from can be marked on the board and stepped through, and the player applies the technique instead of
 *     watching a digit appear.
 * </p>
 * <p>
 *     Still no digit: this is the peek, and the peek never reveals the answer. The cell is the one the technique
 *     proves, exactly as in {@link HintCandidate}, so the two stages agree.
 * </p>
 *
 * @param cellIndex The row-major index of the cell the hint points at
 * @param technique The technique that makes the cell solvable
 * @param explanation Why that technique applies here, in the same vocabulary the learn area draws its lessons from
 *
 * @see HintEngine#explain(net.luis.sudoku.grid.Puzzle)
 */
public record ExplainedHint(int cellIndex, Technique technique, Explanation explanation) {
	
	/**
	 * Constructs an explained hint.
	 *
	 * @throws NullPointerException If the technique or the explanation is null
	 */
	public ExplainedHint {
		Objects.requireNonNull(technique, "Technique must not be null");
		Objects.requireNonNull(explanation, "Explanation must not be null");
	}
	
	/**
	 * Returns whether the explanation shows a real pattern rather than merely restating the conclusion.
	 * <p>
	 *     A strategy that has not been taught to explain itself still hints perfectly well; there is simply nothing to
	 *     draw for it beyond the cell it names. A consumer should ask this before offering to walk a player through a
	 *     pattern that turns out to be one cell.
	 * </p>
	 *
	 * @return True if there is a pattern worth showing
	 */
	public boolean hasPattern() {
		return !this.explanation.isConclusionOnly();
	}
	
	/**
	 * Returns this hint without its explanation, which is what the two-stage flow passes back to
	 * {@link HintEngine#consume}.
	 *
	 * @return The plain candidate
	 */
	public HintCandidate candidate() {
		return new HintCandidate(this.cellIndex, this.technique);
	}
}
