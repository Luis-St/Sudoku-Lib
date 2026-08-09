package net.luis.sudoku.difficulty;

import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.solver.*;

import java.util.*;

/**
 * The size-aware configuration that maps a {@link TechniqueReport} to a {@link Difficulty} band.
 * <p>
 *     Difficulty is derived from <b>the work a puzzle forces</b>, not from the number of givens (spec §4.3), and the
 *     rule has two halves. The first is the hardest technique the {@link TechniqueSolver} needed, taken at its
 *     {@link Technique#level() level}, so the fifteen levels and the fifteen bands are one scale. The second is the
 *     {@link TechniqueReport#pathScore() path score}, which measures how <i>much</i> non-routine work the solve took;
 *     a puzzle is promoted to whichever level its score reaches. The band is the harder of the two.
 * </p>
 * <p>
 *     Taking the maximum rather than replacing one signal with the other is what keeps a band meaningful. The score
 *     can only ever promote, so "a puzzle that forces a level-13 technique is at least band 13" holds exactly; the
 *     converse does not, since a band-13 puzzle may instead be a long grind of level-12 work. That is the deliberate
 *     price of every band actually filling: under the hardest-technique signal alone, bands whose techniques are
 *     common but seldom the hardest step stayed near-empty however hard the generator searched.
 * </p>
 * <p>
 *     The result is then held to the bands a size can actually produce (see {@link #supported}). This is a set and
 *     not merely a ceiling, because the reachable bands need not be an unbroken run: a 6x6 grid makes bands 1, 2, 3,
 *     7 and 8 but never 5 or 6. A puzzle the solver cannot finish even with the level-15 branching techniques is
 *     beyond the modelled set and falls in {@link Difficulty#LISA}; the generator excludes such a grid from a Lisa
 *     request rather than passing it off as one.
 * </p>
 * <p>
 *     Every threshold here is deliberately a single, tunable place. The rater will be re-tuned repeatedly, and per
 *     spec §3.5 any change to it must bump {@link net.luis.sudoku.version.GenVersion}, because it changes which puzzle
 *     a key produces.
 * </p>
 *
 * @see DifficultyRater
 */
public final class DifficultyBands {
	
	private static final DifficultyBands DEFAULTS = new DifficultyBands(defaultSupported());
	
	/** The grid side the {@link #SCORE_THRESHOLDS} were calibrated at, which every other size scales against. */
	private static final int REFERENCE_SIDE = 9;
	
	/**
	 * The path score at which a puzzle is promoted to each level, indexed by {@code level - 1}.
	 * <p>
	 *     These are calibration data rather than tuning knobs. They started from the 75th percentile of the measured
	 *     score distribution of the level below each entry, so that a puzzle in the top quarter of its level by sheer
	 *     volume of hard work is promoted one band — that is what fills the bands whose techniques are common but
	 *     seldom the hardest thing on the path. They were then respaced to grow smoothly, because the raw percentiles
	 *     left some bands a window only a few score points wide, which no search can reliably land in, and re-measured
	 *     against {@code ./gradlew bench} at 32 seeds per band, 9x9 classic. Any further change must be measured the
	 *     same way: the distributions themselves shift when the thresholds do, since the generator then selects
	 *     different puzzles.
	 * </p>
	 * <p>
	 *     Level 1 is unreachable by score (every puzzle scores at least 0) and level 4 is reached by any non-routine
	 *     step at all, which is already implied by the hardest technique, so the two low entries only keep the table
	 *     complete. Changing any entry changes which band a puzzle lands in and therefore requires a
	 *     {@link net.luis.sudoku.version.GenVersion} bump.
	 * </p>
	 */
	private static final int[] SCORE_THRESHOLDS = {
		0, Integer.MAX_VALUE, Integer.MAX_VALUE, 1, 50, 85, 150, 215, 310, 360, 580, 1000, 1150, 1800, 3200
	};
	
	private final Map<GridSize, Set<Difficulty>> supported;
	
	private DifficultyBands(Map<GridSize, Set<Difficulty>> supported) {
		this.supported = new EnumMap<>(supported);
	}
	
	/**
	 * Returns the default band configuration.
	 *
	 * @return The shared default bands
	 */
	public static DifficultyBands defaults() {
		return DEFAULTS;
	}
	
	private static Map<GridSize, Set<Difficulty>> defaultSupported() {
		Map<GridSize, Set<Difficulty>> supported = new EnumMap<>(GridSize.class);
		// Measured, not assumed. A 4x4 grid is band 1 and nothing else: with four digits and four-cell units, a full
		// house or a naked single is essentially always available, and 16 seeds x 15 requested bands produced a
		// band-1 puzzle every single time. The old ceiling of FIVE was fiction.
		supported.put(GridSize.FOUR, EnumSet.of(Difficulty.ONE));
		// A 6x6 grid has a gap, which is why a ceiling could never describe it: at 32 seeds per band it produced
		// 1, 2, 3, 7 and 8 reliably but bands 5 and 6 never once, and band 4 only 3 times in 32. Its 2x3 boxes make
		// locked candidates so common that a puzzle needing anything past the singles usually needs a Skyscraper too,
		// so it steps straight from band 3 to band 7.
		supported.put(GridSize.SIX, EnumSet.of(Difficulty.ONE, Difficulty.TWO, Difficulty.THREE, Difficulty.SEVEN, Difficulty.EIGHT));
		supported.put(GridSize.NINE, EnumSet.allOf(Difficulty.class));
		supported.put(GridSize.TWELVE, EnumSet.allOf(Difficulty.class));
		// 16x16 is the one size still unmeasured; it is assumed to behave like 12x12 until somebody runs the bench.
		supported.put(GridSize.SIXTEEN, EnumSet.allOf(Difficulty.class));
		return supported;
	}
	
	/**
	 * Returns the highest level the given path score alone reaches at the given size, that is the hardest level whose
	 * promotion threshold the score meets.
	 * <p>
	 *     The thresholds are calibrated at 9x9 and scaled by grid side, because the score is a <b>sum over the solve
	 *     path</b> and a smaller grid simply has fewer steps to sum. Held absolute, the 9x9 numbers were unreachable
	 *     on a 6x6 grid: measured, bands 4, 5 and 6 were skipped there entirely while bands 7 and 8 still filled,
	 *     because only the hardest-technique half of the rule was ever doing any work.
	 * </p>
	 * <p>
	 *     Scaling by <i>side</i> rather than by cell count is deliberate and was measured, not assumed. Cell count
	 *     scales 12x12's thresholds by 16/9 and cost it most of band 10; the gentler side ratio lifts the small grids
	 *     without flattening the large ones. A non-zero threshold never scales below {@code 1}, since a threshold
	 *     rounded to zero would promote every puzzle, however trivial, to that level.
	 * </p>
	 */
	private static int scoreLevel(GridSize size, int score) {
		int level = 1;
		for (int index = 0; index < SCORE_THRESHOLDS.length; index++) {
			int threshold = SCORE_THRESHOLDS[index];
			if (threshold == Integer.MAX_VALUE) {
				continue;
			}
			
			int scaled = threshold == 0 ? 0 : Math.max(1, (int) ((long) threshold * size.n() / REFERENCE_SIDE));
			if (score >= scaled) {
				level = index + 1;
			}
		}
		return level;
	}
	
	/**
	 * Returns the hardest band the given size can produce; harder requests clamp to it.
	 *
	 * @param size The grid size
	 * @return The band ceiling for that size
	 */
	public Difficulty ceiling(GridSize size) {
		Difficulty ceiling = null;
		for (Difficulty band : this.supported(size)) {
			if (ceiling == null || band.index() > ceiling.index()) {
				ceiling = band;
			}
		}
		return ceiling;
	}
	
	/**
	 * Returns the bands the given size can actually produce.
	 * <p>
	 *     A ceiling alone cannot describe a grid size, because the reachable bands are not always an unbroken run
	 *     from one upwards: a 6x6 grid produces bands 1, 2, 3, 7 and 8 but never 5 or 6, so its ceiling of 8 would
	 *     advertise five bands it cannot deliver. This is the set a client should offer the player, and the set the
	 *     generator snaps a request onto.
	 * </p>
	 *
	 * @param size The grid size
	 * @return The supported bands, never empty
	 * @throws NullPointerException If the size is null, or if no set is configured for it
	 */
	public Set<Difficulty> supported(GridSize size) {
		return Collections.unmodifiableSet(Objects.requireNonNull(this.supported.get(size), "No supported bands for grid size " + size));
	}
	
	/**
	 * Returns the supported band closest to the requested one, preferring the easier of two equally close bands.
	 * <p>
	 *     Preferring downwards matters: a player who asks for a band a size cannot produce is better served by an
	 *     easier puzzle than by one harder than they chose.
	 * </p>
	 *
	 * @param size The grid size
	 * @param requested The requested band
	 * @return The nearest band the size supports, which is {@code requested} itself when it is supported
	 * @throws NullPointerException If the size or the band is null
	 */
	public Difficulty nearestSupported(GridSize size, Difficulty requested) {
		Objects.requireNonNull(requested, "Requested band must not be null");
		Set<Difficulty> bands = this.supported(size);
		if (bands.contains(requested)) {
			return requested;
		}
		
		Difficulty nearest = null;
		int nearestDistance = Integer.MAX_VALUE;
		for (Difficulty band : bands) {
			int distance = Math.abs(band.index() - requested.index());
			// Strictly closer wins; on a tie the lower band is already in hand, because Difficulty iterates ascending.
			if (distance < nearestDistance) {
				nearestDistance = distance;
				nearest = band;
			}
		}
		return nearest;
	}
	
	/**
	 * Classifies a solved-or-stuck technique report into a band, clamped to the size's ceiling.
	 *
	 * @param size The grid size the report belongs to
	 * @param report The technique-solver report for the puzzle
	 * @return The difficulty band, one of {@link Difficulty#ONE} through {@link Difficulty#LISA}
	 */
	public Difficulty classify(GridSize size, TechniqueReport report) {
		if (report.exceededCap()) {
			throw new IllegalArgumentException("A report that stopped at a level cap carries no rating, only the fact that the puzzle is harder than that cap");
		}
		
		Difficulty band = Difficulty.ofIndex(this.effectiveLevel(size, report));
		Difficulty ceiling = this.ceiling(size);
		return band.index() > ceiling.index() ? ceiling : band;
	}
	
	/**
	 * Returns the rating signal of a report: the harder of the hardest technique's level and the level its path score
	 * implies, the maximum level when the solver got stuck (the puzzle is beyond even the branching techniques), or
	 * {@code 1} when no technique fired at all, which only happens for an already-solved grid.
	 * <p>
	 *     Taking the <b>maximum</b> of the two signals rather than replacing one with the other is what keeps the
	 *     bands meaningful: the score can only ever promote a puzzle, never demote it, so "a puzzle that forces a
	 *     level-13 technique is at least band 13" still holds exactly. What it no longer promises is the converse —
	 *     a band-13 puzzle may instead be a long grind of level-12 work, which is the deliberate price of having
	 *     every band actually fill.
	 * </p>
	 */
	private int effectiveLevel(GridSize size, TechniqueReport report) {
		if (report.stuck()) {
			return Technique.MAX_LEVEL;
		}
		
		int hardest = report.hardestTechnique().map(Technique::level).orElse(1);
		return Math.max(hardest, scoreLevel(size, report.pathScore()));
	}
}
