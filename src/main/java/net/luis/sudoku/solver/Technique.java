package net.luis.sudoku.solver;

import net.luis.sudoku.difficulty.Difficulty;

/**
 * The human solving techniques the {@link TechniqueSolver} knows, listed in strictly escalating difficulty order.
 * <p>
 *     The declaration order <b>is</b> the difficulty order: {@link #FULL_HOUSE} is the easiest and
 *     {@link #DYNAMIC_CONTRADICTION_CHAIN} the hardest, and {@link #rank()} is simply {@code ordinal() + 1}. The
 *     solver relies on this ordering: it always applies the lowest-ranked technique that makes progress before
 *     considering any harder one, and it restarts the scan from the top after every deduction. That guarantee is what
 *     makes the difficulty rating meaningful — a puzzle is only as hard as the hardest technique it genuinely forces,
 *     never as hard as a technique that merely happened to also apply while an easier one was available.
 * </p>
 * <p>
 *     Each technique additionally carries the {@link #level() difficulty level} it introduces, {@code 1..15}. The
 *     level is the rating axis: a puzzle's band is the level of the hardest technique it forces, so several
 *     techniques of comparable human difficulty deliberately share one level. Levels are grouped as follows, each
 *     line listing the techniques the level <i>newly</i> requires on top of every easier level:
 * </p>
 * <ol>
 *     <li>Full house, last digit, naked single</li>
 *     <li>Hidden single in a region (cross-hatching)</li>
 *     <li>Hidden single in a row or column</li>
 *     <li>Locked candidates: pointing and claiming; the Law of Leftovers</li>
 *     <li>Naked pair</li>
 *     <li>Hidden pair, naked triple</li>
 *     <li>Hidden triple, X-Wing, Skyscraper, 2-String Kite</li>
 *     <li>Swordfish, BUG+1, Crane, XY-Wing, unique rectangle types 1 and 2</li>
 *     <li>XYZ-Wing, W-Wing, finned X-Wing, naked and hidden quads, unique rectangle types 3 and 4</li>
 *     <li>Empty rectangle, finned and sashimi Swordfish, Jellyfish</li>
 *     <li>Simple colouring, WXYZ-Wing, X-Chains</li>
 *     <li>XY-Chains, plain AIC</li>
 *     <li>ALS-XZ, Sue de Coq, 3D Medusa, multi-colouring</li>
 *     <li>Grouped AIC, ALS chains</li>
 *     <li>Nishio, forcing chains and nets, Death Blossom, dynamic contradiction chains — the levels that require
 *         assuming a candidate and playing the position out</li>
 * </ol>
 * <p>
 *     A technique either places a digit or only eliminates candidates, as reported by {@link #placesDigit()}. Most
 *     techniques narrow candidates and let a later single finish the placement; only the singles themselves,
 *     {@link #BUG_PLUS_ONE} and the forcing techniques prove a digit outright.
 * </p>
 *
 * @see TechniqueSolver
 * @see Difficulty
 */
public enum Technique {
	
	// Level 1 - the techniques that need no candidate bookkeeping at all.
	
	/**
	 * A unit with exactly one empty cell left, which must hold the one digit the unit is missing.
	 */
	FULL_HOUSE(1, true),
	/**
	 * A digit already placed in every unit but one, leaving exactly one cell in the grid that can still hold it.
	 */
	LAST_DIGIT(1, true),
	/**
	 * A cell with exactly one remaining candidate, which must therefore hold that digit.
	 */
	NAKED_SINGLE(1, true),
	
	// Level 2 - cross-hatching a region.
	
	/**
	 * A digit that is a candidate of exactly one cell within a region, which must therefore hold that digit.
	 */
	HIDDEN_SINGLE_REGION(2, true),
	
	// Level 3 - the same argument along a line, which is markedly harder to spot by eye.
	
	/**
	 * A digit that is a candidate of exactly one cell within a row or a column, which must therefore hold that digit.
	 */
	HIDDEN_SINGLE_LINE(3, true),
	
	// Level 4 - locked candidates, the first technique that only eliminates.
	
	/**
	 * A digit whose candidates within a region all lie on one line, which removes it from the rest of that line.
	 */
	POINTING(4, false),
	/**
	 * A digit whose candidates within a line all lie in one region, which removes it from the rest of that region.
	 */
	CLAIMING(4, false),
	/**
	 * An equal number of lines and regions, whose non-shared cells must hold the same digits, which removes a digit
	 * from one leftover once it is impossible in the other.
	 */
	LAW_OF_LEFTOVERS(4, false),
	
	// Level 5.
	
	/**
	 * Two cells of a unit sharing the identical two-candidate set, which removes those two digits from the rest of the unit.
	 */
	NAKED_PAIR(5, false),
	
	// Level 6.
	
	/**
	 * Two digits confined to the same two cells of a unit, which removes every other candidate from those two cells.
	 */
	HIDDEN_PAIR(6, false),
	/**
	 * Three cells of a unit confined to a common three-candidate set, which removes those three digits from the rest of the unit.
	 */
	NAKED_TRIPLE(6, false),
	
	// Level 7 - the first patterns that span more than one unit.
	
	/**
	 * Three digits confined to the same three cells of a unit, which removes every other candidate from those three cells.
	 */
	HIDDEN_TRIPLE(7, false),
	/**
	 * A digit confined to the same two columns across two rows (or mirror), which removes it from those columns elsewhere.
	 */
	X_WING(7, false),
	/**
	 * Two conjugate pairs of one digit in two lines whose ends share a region, which removes the digit from the cells both far ends see.
	 */
	SKYSCRAPER(7, false),
	/**
	 * A conjugate pair in a row and one in a column meeting in a common region, which removes the digit from the cell the two far ends see.
	 */
	TWO_STRING_KITE(7, false),
	
	// Level 8.
	
	/**
	 * The three-row/three-column generalization of the X-Wing.
	 */
	SWORDFISH(8, false),
	/**
	 * A grid one cell away from the invalid bi-value universal grave, whose extra candidate must be the digit that cell holds.
	 */
	BUG_PLUS_ONE(8, true),
	/**
	 * A conjugate pair whose two ends each see one end of a third line's conjugate pair, which removes the digit from the remaining cell.
	 */
	CRANE(8, false),
	/**
	 * A bi-value pivot with two bi-value wings, which removes the shared third digit from the cells both wings see.
	 */
	XY_WING(8, false),
	/**
	 * A deadly rectangle whose fourth cell holds the two shared digits plus extras, which removes the two shared digits from it.
	 */
	UNIQUE_RECTANGLE_1(8, false),
	/**
	 * A deadly rectangle with one extra candidate repeated in two cells, which removes that digit from every cell both of them see.
	 */
	UNIQUE_RECTANGLE_2(8, false),
	
	// Level 9.
	
	/**
	 * A three-candidate pivot with two bi-value wings, which removes the shared digit from the cells the pivot and both wings see.
	 */
	XYZ_WING(9, false),
	/**
	 * Two cells with the identical two-candidate set linked by a conjugate pair of one of them, which removes the other digit from the cells both see.
	 */
	W_WING(9, false),
	/**
	 * An X-Wing whose cover set holds one extra candidate, which still eliminates in the cells the fin also sees.
	 */
	FINNED_X_WING(9, false),
	/**
	 * Four cells of a unit confined to a common four-candidate set, which removes those four digits from the rest of the unit.
	 */
	NAKED_QUAD(9, false),
	/**
	 * Four digits confined to the same four cells of a unit, which removes every other candidate from those four cells.
	 */
	HIDDEN_QUAD(9, false),
	/**
	 * A deadly rectangle whose extra candidates form a naked subset with a further cell of the unit, which removes that subset elsewhere in the unit.
	 */
	UNIQUE_RECTANGLE_3(9, false),
	/**
	 * A deadly rectangle whose floor digit is locked in one of its lines, which removes the other shared digit from the roof cells.
	 */
	UNIQUE_RECTANGLE_4(9, false),
	
	// Level 10 - the larger and imperfect fish.
	
	/**
	 * A digit locked into one line of a region, with a conjugate pair on the crossing line, which removes it from the cell both constrain.
	 */
	EMPTY_RECTANGLE(10, false),
	/**
	 * A Swordfish whose cover set holds one extra candidate, which still eliminates in the cells the fin also sees.
	 */
	FINNED_SWORDFISH(10, false),
	/**
	 * A finned Swordfish missing one of its corner candidates, which eliminates on the same argument as the finned form.
	 */
	SASHIMI_SWORDFISH(10, false),
	/**
	 * The four-row/four-column generalization of the X-Wing and Swordfish.
	 */
	JELLYFISH(10, false),
	
	// Level 11 - the first genuine chains.
	
	/**
	 * A two-colouring of one digit's conjugate-pair graph, eliminating where a colour repeats in a unit or where both colours are seen.
	 */
	SIMPLE_COLOURING(11, false),
	/**
	 * The four-cell generalization of the XYZ-Wing over a set of four candidates.
	 */
	WXYZ_WING(11, false),
	/**
	 * An alternating chain of strong and weak links in a single digit, which removes that digit from the cells both ends see.
	 */
	X_CHAIN(11, false),
	
	// Level 12.
	
	/**
	 * A chain of bi-value cells linked by shared digits, which removes the end digit from the cells both ends see.
	 */
	XY_CHAIN(12, false),
	/**
	 * A plain alternating inference chain over candidates of any digit, which eliminates on the cells its two ends both see.
	 */
	AIC(12, false),
	
	// Level 13 - almost-locked sets and multi-colour arguments.
	
	/**
	 * Two almost-locked sets sharing a restricted common digit, which removes any second common digit from the cells both sets see.
	 */
	ALS_XZ(13, false),
	/**
	 * A region-line intersection whose candidates split into two disjoint locked sets, which removes the used digits from both the region and the line.
	 */
	SUE_DE_COQ(13, false),
	/**
	 * A two-colouring of the strong-link graph over every digit at once, eliminating on the same rules as simple colouring.
	 */
	MEDUSA_3D(13, false),
	/**
	 * Two separate colour clusters of one digit whose colours see each other, which eliminates outside both clusters.
	 */
	MULTI_COLOURING(13, false),
	
	// Level 14 - the hardest techniques that still argue purely forwards, without assuming anything.
	
	/**
	 * An alternating inference chain whose links may be whole groups of candidates in a unit rather than single cells.
	 */
	GROUPED_AIC(14, false),
	/**
	 * A chain of almost-locked sets joined by restricted common digits, which eliminates on the digits its two ends share.
	 */
	ALS_CHAIN(14, false),
	
	// Level 15 - the techniques that assume a candidate and play the position out, and the end of the modelled set.
	//
	// NISHIO lives here rather than at 14 because it is not a weaker relative of a forcing chain, it is the same
	// machinery under a tighter bound: assume, propagate, discard on contradiction. Leaving it at 14 made level 15
	// unreachable, because in any position hard enough to defeat levels 1..13 some candidate almost always dies under
	// propagation, so Nishio fired first and the forcing techniques were never reached. Measured at 32 seeds before
	// the move: 0/32 puzzles rated 15, and NISHIO was the hardest technique in 15 of them.
	//
	// The level therefore means "a trial was required", which is also how the field now separates the hardest
	// puzzles (trial-and-error depth rather than a named technique).
	
	/**
	 * A single candidate whose assumption propagates by singles into a contradiction, which removes that candidate.
	 */
	NISHIO(15, false),
	/**
	 * Every candidate of one cell or unit leading by chains to the same conclusion, which proves that conclusion.
	 */
	FORCING_CHAIN(15, true),
	/**
	 * A forcing chain whose branches may themselves branch, proving a conclusion no single chain reaches.
	 */
	FORCING_NET(15, true),
	/**
	 * A stem cell each of whose candidates is the restricted common digit of an almost-locked set, which eliminates what every petal excludes.
	 */
	DEATH_BLOSSOM(15, false),
	/**
	 * A candidate whose assumption propagates through the full technique set into a contradiction, which removes that candidate.
	 */
	DYNAMIC_CONTRADICTION_CHAIN(15, false);
	
	/**
	 * The highest {@link #level()} any technique carries, and therefore the number of difficulty bands.
	 */
	public static final int MAX_LEVEL = 15;
	
	/**
	 * The hardest level whose techniques are considered routine and score nothing.
	 * <p>
	 *     Levels 1 to 3 are the singles: every puzzle of every band is mostly made of them, so counting them would
	 *     measure a puzzle's <i>length</i> rather than its difficulty, and would score a long easy grid above a short
	 *     brutal one. The path score deliberately measures only the non-routine work.
	 * </p>
	 */
	public static final int ROUTINE_LEVEL = 3;
	
	private final int level;
	private final boolean placesDigit;
	
	Technique(int level, boolean placesDigit) {
		this.level = level;
		this.placesDigit = placesDigit;
	}
	
	/**
	 * Returns the hardest technique of the given difficulty level.
	 * <p>
	 *     Because the declaration order is the difficulty order, this is the last constant carrying the level, which
	 *     is exactly the rank boundary the level occupies.
	 * </p>
	 *
	 * @param level The difficulty level to look up
	 * @return The hardest technique of that level
	 * @throws IllegalArgumentException If no technique carries the given level
	 */
	public static Technique hardestOfLevel(int level) {
		Technique hardest = null;
		for (Technique technique : values()) {
			if (technique.level == level) {
				hardest = technique;
			}
		}
		if (hardest == null) {
			throw new IllegalArgumentException("No technique with level " + level);
		}
		return hardest;
	}
	
	/**
	 * Returns what one application of this technique contributes to a puzzle's path score.
	 * <p>
	 *     The path score is the second half of the rating: a band is the harder of the hardest technique's level and
	 *     the level implied by the summed score, so that a puzzle demanding a great deal of level-9 work is not
	 *     rated identically to one demanding a single level-9 step. A pure hardest-technique rating cannot tell those
	 *     apart, which is why bands whose techniques are common but seldom the hardest stay near-empty under it.
	 * </p>
	 * <p>
	 *     The weight is the square of the level above {@link #ROUTINE_LEVEL}, and zero at or below it. Squaring keeps
	 *     a handful of genuinely hard steps worth more than a long tail of merely awkward ones, which is the ordering
	 *     the score exists to capture.
	 * </p>
	 *
	 * @return The score of one application, {@code 0} for the routine levels
	 */
	public int score() {
		return this.level <= ROUTINE_LEVEL ? 0 : this.level * this.level;
	}
	
	/**
	 * Returns the difficulty rank of this technique, which is its {@link #ordinal()} plus one.
	 * <p>
	 *     A higher rank is always a harder technique. The rank orders techniques <i>within</i> a level as well as
	 *     across levels; use {@link #level()} for the rating itself, which deliberately treats a level's techniques as
	 *     equally hard.
	 * </p>
	 *
	 * @return The rank, {@code 1} for {@link #FULL_HOUSE} up to the number of techniques
	 */
	public int rank() {
		return this.ordinal() + 1;
	}
	
	/**
	 * Returns the difficulty level this technique belongs to.
	 * <p>
	 *     This is the rating axis: a puzzle that forces this technique and nothing harder is rated at this level. It
	 *     maps straight onto {@link Difficulty#ofIndex(int)}.
	 * </p>
	 *
	 * @return The level, {@code 1..15}
	 */
	public int level() {
		return this.level;
	}
	
	/**
	 * Returns the difficulty band a puzzle whose hardest forced technique is this one is rated at.
	 *
	 * @return The difficulty band matching {@link #level()}
	 */
	public Difficulty difficulty() {
		return Difficulty.ofIndex(this.level);
	}
	
	/**
	 * Checks whether this technique places a digit directly rather than only eliminating candidates.
	 *
	 * @return True for the singles, {@link #BUG_PLUS_ONE} and the forcing techniques, false for every other technique
	 */
	public boolean placesDigit() {
		return this.placesDigit;
	}
}
