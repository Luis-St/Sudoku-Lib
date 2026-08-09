package net.luis.sudoku.difficulty;

import net.luis.sudoku.solver.Technique;

/**
 * The difficulty bands a rated puzzle can fall into: fourteen numbered tiers plus {@link #LISA}.
 * <p>
 *     A band is derived from which solving techniques the puzzle requires, never from the number of givens, which is
 *     a weak and misleading proxy. Every band corresponds to exactly one {@link Technique#level() technique level}:
 *     a puzzle sits in the band of the hardest technique it genuinely forces. The scale is size-aware: every grid
 *     size defines its own band ceiling rather than sharing a global scale.
 * </p>
 * <p>
 *     Difficulty is deliberately an enum rather than an integer, so that {@code LISA} is a member of the same type as
 *     the numbered tiers instead of a magic fifteenth number.
 * </p>
 * <p>
 *     Only the <i>band</i> is modelled here, because only the band feeds the generator and belongs in the puzzle key.
 *     Lisa is additionally a fixed set of gameplay modifiers, but that modifier set is a client-side runtime rule set
 *     and is intentionally not part of this library: future modifiers must be addable without touching generation.
 * </p>
 *
 * @see #LISA
 * @see Technique#level()
 */
public enum Difficulty {
	
	/**
	 * The easiest band: full houses, last digits and naked singles alone.
	 */
	ONE(1),
	/**
	 * Hidden singles within a region, found by cross-hatching.
	 */
	TWO(2),
	/**
	 * Hidden singles along a row or a column.
	 */
	THREE(3),
	/**
	 * Locked candidates, pointing and claiming.
	 */
	FOUR(4),
	/**
	 * Naked pairs.
	 */
	FIVE(5),
	/**
	 * Hidden pairs and naked triples.
	 */
	SIX(6),
	/**
	 * Hidden triples and the first cross-unit patterns: X-Wing, Skyscraper, 2-String Kite.
	 */
	SEVEN(7),
	/**
	 * Swordfish, BUG+1, Crane, XY-Wing and the first two unique-rectangle types.
	 */
	EIGHT(8),
	/**
	 * XYZ-Wing, W-Wing, finned X-Wing, quads and the remaining unique-rectangle types.
	 */
	NINE(9),
	/**
	 * Empty rectangles, the finned and sashimi Swordfish, and the Jellyfish.
	 */
	TEN(10),
	/**
	 * The first genuine chains: simple colouring, WXYZ-Wing, X-Chains.
	 */
	ELEVEN(11),
	/**
	 * XY-Chains and plain alternating inference chains.
	 */
	TWELVE(12),
	/**
	 * Almost-locked-set logic and multi-colour arguments: ALS-XZ, Sue de Coq, 3D Medusa, multi-colouring.
	 */
	THIRTEEN(13),
	/**
	 * The hardest of the fourteen numbered bands: grouped AIC, ALS chains and Nishio.
	 */
	FOURTEEN(14),
	/**
	 * The special tier: the branching techniques, plus a fixed set of gameplay modifiers.
	 * <p>
	 *     A Lisa puzzle genuinely forces a level-15 technique — a forcing chain or net, a Death Blossom or a dynamic
	 *     contradiction chain — or is beyond the modelled technique set entirely. Lisa is available at every grid
	 *     size, exactly like the numbered tiers. At the small sizes the rating simply cannot reach a genuinely hard
	 *     band, so Lisa there is the hardest band that size can produce rather than an absolute difficulty.
	 * </p>
	 * <p>
	 *     Lisa is offered in normal single-player games and in the daily puzzle only. It is rejected in every
	 *     multiplayer mode, which is what {@link #isAllowedInMultiplayer()} reports.
	 * </p>
	 * <p>
	 *     The accompanying modifier set mainly removes assistance rather than raising punishment, is fixed rather
	 *     than individually toggleable, and lives in the client runtime, not in this library. Currency is still
	 *     awarded linearly at index {@code 15}, with no Lisa multiplier.
	 * </p>
	 */
	LISA(15);
	
	private final int index;
	
	Difficulty(int index) {
		this.index = index;
	}
	
	/**
	 * Returns the difficulty band matching the given index.
	 *
	 * @param index The difficulty index to look up
	 * @return The matching difficulty band
	 * @throws IllegalArgumentException If the index is not in {@code 1..15}
	 */
	public static Difficulty ofIndex(int index) {
		for (Difficulty difficulty : values()) {
			if (difficulty.index == index) {
				return difficulty;
			}
		}
		throw new IllegalArgumentException("No difficulty with index " + index);
	}
	
	/**
	 * Returns the currency index of this band, which is the value the reward calculation is scaled by.
	 * <p>
	 *     The index is a stable semantic value rather than the enum ordinal, so it is safe to persist and to
	 *     encode into a puzzle key. It equals the {@link Technique#level() technique level} the band corresponds to.
	 *     Rewards are linear in this index for every band, Lisa included.
	 * </p>
	 *
	 * @return The index, {@code 1..15}
	 */
	public int index() {
		return this.index;
	}
	
	/**
	 * Checks whether this band is the special Lisa tier.
	 *
	 * @return True if this is {@link #LISA}, false otherwise
	 */
	public boolean isLisa() {
		return this == LISA;
	}
	
	/**
	 * Checks whether this band may be played in a multiplayer mode.
	 * <p>
	 *     Every numbered tier is allowed. {@link #LISA} is the only band that is not, because its modifier set
	 *     changes how the game plays and therefore cannot be compared fairly between players.
	 * </p>
	 *
	 * @return True for every band except {@link #LISA}, false for {@link #LISA}
	 */
	public boolean isAllowedInMultiplayer() {
		return !this.isLisa();
	}
}
