package net.luis.sudoku.difficulty;

/**
 * The difficulty bands a rated puzzle can fall into: five numbered tiers plus {@link #LISA}.
 * <p>
 *     A band is derived from which solving techniques the puzzle requires and how often, never from the number
 *     of givens, which is a weak and misleading proxy. The scale is size-aware: every grid size defines its own
 *     band thresholds rather than sharing a global scale.
 * </p>
 * <p>
 *     Difficulty is deliberately an enum rather than an integer, so that {@code LISA} is a member of the same
 *     type as the numbered tiers instead of a magic sixth number.
 * </p>
 * <p>
 *     Only the <i>band</i> is modelled here, because only the band feeds the generator and belongs in the puzzle
 *     key. Lisa is additionally a fixed set of gameplay modifiers, but that modifier set is a client-side runtime
 *     rule set and is intentionally not part of this library: future modifiers must be addable without touching
 *     generation.
 * </p>
 *
 * @see #LISA
 */
public enum Difficulty {
	
	/**
	 * The easiest band, solvable with the most basic techniques alone.
	 */
	ONE(1),
	/**
	 * The second band.
	 */
	TWO(2),
	/**
	 * The third band.
	 */
	THREE(3),
	/**
	 * The fourth band.
	 */
	FOUR(4),
	/**
	 * The hardest of the five numbered bands.
	 */
	FIVE(5),
	/**
	 * The special tier: the hardest band the given grid size can produce, plus a fixed set of gameplay modifiers.
	 * <p>
	 *     Lisa is available at every grid size, exactly like the numbered tiers. At 4x4 and 6x6 the rating simply
	 *     cannot reach a genuinely hard band, so Lisa there is the hardest band that size can produce rather than
	 *     an absolute difficulty.
	 * </p>
	 * <p>
	 *     Lisa is offered in normal single-player games and in the daily puzzle only. It is rejected in every
	 *     multiplayer mode, which is what {@link #isAllowedInMultiplayer()} reports.
	 * </p>
	 * <p>
	 *     The accompanying modifier set mainly removes assistance rather than raising punishment, is fixed rather
	 *     than individually toggleable, and lives in the client runtime, not in this library. Currency is still
	 *     awarded linearly at index {@code 6}, with no Lisa multiplier.
	 * </p>
	 */
	LISA(6);
	
	private final int index;
	
	Difficulty(int index) {
		this.index = index;
	}
	
	/**
	 * Returns the difficulty band matching the given index.
	 *
	 * @param index The difficulty index to look up
	 * @return The matching difficulty band
	 * @throws IllegalArgumentException If the index is not in {@code 1..6}
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
	 *     encode into a puzzle key. Rewards are linear in this index for every band, Lisa included.
	 * </p>
	 *
	 * @return The index, {@code 1..6}
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
