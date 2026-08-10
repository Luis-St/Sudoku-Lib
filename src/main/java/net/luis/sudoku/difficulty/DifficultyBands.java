package net.luis.sudoku.difficulty;

import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.grid.Variant;
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
 *     The result is then held to the bands a size <i>and variant</i> can actually produce (see {@link #supported}).
 *     This is a set and not merely a ceiling, because the reachable bands need not be an unbroken run: a 6x6 grid
 *     makes bands 1, 2, 3, 7 and 8 but never 5 or 6. A puzzle the solver cannot finish even with the level-15
 *     branching techniques is beyond the modelled set and falls in {@link Difficulty#LISA}; the generator excludes
 *     such a grid from a Lisa request rather than passing it off as one.
 * </p>
 * <p>
 *     <b>The variant is part of the key, not a detail.</b> A jigsaw grid has {@link Technique#LAW_OF_LEFTOVERS}
 *     available to it, a technique no classic grid can use, and at 16x16 that one technique is enough to keep the
 *     hard bands out of reach: measured at 32 seeds a band, 16x16 chaos lands its target 32 times out of 32 up to
 *     band 4 and 17 out of 32 at band 8, then falls off a cliff to 9, 7, 6, 7, 4, 3 and 4. Classic at the same size
 *     hits every one of the fifteen. Keyed by size alone, one of those two had to be described wrongly, and the one
 *     that was is the one that quietly hands a player asking for band 13 a puzzle rated 8.
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
	
	private final Map<GridSize, Map<Variant, Set<Difficulty>>> supported;
	
	private DifficultyBands(Map<GridSize, Map<Variant, Set<Difficulty>>> supported) {
		Map<GridSize, Map<Variant, Set<Difficulty>>> copy = new EnumMap<>(GridSize.class);
		supported.forEach((size, byVariant) -> copy.put(size, new EnumMap<>(byVariant)));
		this.supported = copy;
	}
	
	/**
	 * Returns the default band configuration.
	 *
	 * @return The shared default bands
	 */
	public static DifficultyBands defaults() {
		return DEFAULTS;
	}
	
	private static Map<GridSize, Map<Variant, Set<Difficulty>>> defaultSupported() {
		Map<GridSize, Map<Variant, Set<Difficulty>>> supported = new EnumMap<>(GridSize.class);
		// Measured, not assumed. A 4x4 grid is band 1 and nothing else: with four digits and four-cell units, a full
		// house or a naked single is essentially always available, and 16 seeds x 15 requested bands produced a
		// band-1 puzzle every single time. The old ceiling of FIVE was fiction. 4x4 has no chaos layout at all.
		supported.put(GridSize.FOUR, classicOnly(EnumSet.of(Difficulty.ONE)));
		// A 6x6 grid has a gap, which is why a ceiling could never describe it: at 32 seeds per band it produced
		// 1, 2, 3, 7 and 8 reliably but bands 5 and 6 never once, and band 4 only 3 times in 32. Its 2x3 boxes make
		// locked candidates so common that a puzzle needing anything past the singles usually needs a Skyscraper too,
		// so it steps straight from band 3 to band 7. Chaos at 6x6 measured the same set.
		supported.put(GridSize.SIX, bothVariants(EnumSet.of(Difficulty.ONE, Difficulty.TWO, Difficulty.THREE, Difficulty.SEVEN, Difficulty.EIGHT)));
		// 9x9 and 12x12 reach everything in both variants: 32 of 32 at every band for 9x9 chaos, and 20 to 32 of 32
		// for 12x12 chaos, which is the ordinary near-miss rate of the search rather than a wall.
		supported.put(GridSize.NINE, bothVariants(EnumSet.allOf(Difficulty.class)));
		supported.put(GridSize.TWELVE, bothVariants(EnumSet.allOf(Difficulty.class)));
		// 16x16 is the size the variant split exists for. Re-measured 2026-08-10 at 32 seeds a band:
		//
		//   classic  32/32 for bands 1-8, then 29, 21, 22, 32, 27, 29, 32 - every band reachable.
		//   chaos    32/32 to band 4, 29, 31, 23, then 17/32 at band 8 and 9, 7, 6, 7, 4, 3, 4 above it.
		//
		// Band 8 is the last band a chaos request lands more often than it misses, so it is the last band this
		// promises. The cliff is the Law of Leftovers: a jigsaw grid gets a technique classic does not have, it is
		// scored at level 4, and above band 8 it is frequently the hardest step on the path - so the grid solves
		// too easily to rate where it was asked to. Offering 9 to 15 here would not produce hard chaos puzzles; it
		// would produce band-6 puzzles wearing a band-13 label.
		Map<Variant, Set<Difficulty>> sixteen = new EnumMap<>(Variant.class);
		sixteen.put(Variant.CLASSIC, EnumSet.allOf(Difficulty.class));
		sixteen.put(Variant.CHAOS, EnumSet.range(Difficulty.ONE, Difficulty.EIGHT));
		supported.put(GridSize.SIXTEEN, sixteen);
		return supported;
	}
	
	/** A size whose two variants reach the same bands, which is every size the split does not separate. */
	private static Map<Variant, Set<Difficulty>> bothVariants(Set<Difficulty> bands) {
		Map<Variant, Set<Difficulty>> byVariant = new EnumMap<>(Variant.class);
		byVariant.put(Variant.CLASSIC, bands);
		byVariant.put(Variant.CHAOS, bands);
		return byVariant;
	}
	
	/** A size with no jigsaw layout, so asking for its chaos bands is a question with no answer. */
	private static Map<Variant, Set<Difficulty>> classicOnly(Set<Difficulty> bands) {
		Map<Variant, Set<Difficulty>> byVariant = new EnumMap<>(Variant.class);
		byVariant.put(Variant.CLASSIC, bands);
		return byVariant;
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
	 * Returns the hardest band the given size and variant can produce; harder requests clamp to it.
	 *
	 * @param size The grid size
	 * @param variant The region layout variant
	 * @return The band ceiling for that combination
	 */
	public Difficulty ceiling(GridSize size, Variant variant) {
		Difficulty ceiling = null;
		for (Difficulty band : this.supported(size, variant)) {
			if (ceiling == null || band.index() > ceiling.index()) {
				ceiling = band;
			}
		}
		return ceiling;
	}
	
	/**
	 * Returns the bands the given size and variant can actually produce.
	 * <p>
	 *     A ceiling alone cannot describe a grid, because the reachable bands are not always an unbroken run from
	 *     one upwards: a 6x6 grid produces bands 1, 2, 3, 7 and 8 but never 5 or 6, so its ceiling of 8 would
	 *     advertise five bands it cannot deliver. This is the set a client should offer the player, and the set the
	 *     generator snaps a request onto.
	 * </p>
	 *
	 * @param size The grid size
	 * @param variant The region layout variant
	 * @return The supported bands, never empty
	 * @throws NullPointerException If the size or the variant is null
	 * @throws IllegalArgumentException If that variant does not exist at that size, which is chaos at 4x4
	 */
	public Set<Difficulty> supported(GridSize size, Variant variant) {
		Objects.requireNonNull(size, "Grid size must not be null");
		Objects.requireNonNull(variant, "Variant must not be null");
		
		Map<Variant, Set<Difficulty>> byVariant = this.supported.get(size);
		Set<Difficulty> bands = byVariant == null ? null : byVariant.get(variant);
		if (bands == null) {
			throw new IllegalArgumentException("No supported bands for " + variant + " at grid size " + size);
		}
		return Collections.unmodifiableSet(bands);
	}
	
	/**
	 * Returns the supported band closest to the requested one, preferring the easier of two equally close bands.
	 * <p>
	 *     Preferring downwards matters: a player who asks for a band a grid cannot produce is better served by an
	 *     easier puzzle than by one harder than they chose.
	 * </p>
	 *
	 * @param size The grid size
	 * @param variant The region layout variant
	 * @param requested The requested band
	 * @return The nearest supported band, which is {@code requested} itself when it is supported
	 * @throws NullPointerException If the size, the variant or the band is null
	 */
	public Difficulty nearestSupported(GridSize size, Variant variant, Difficulty requested) {
		Objects.requireNonNull(requested, "Requested band must not be null");
		Set<Difficulty> bands = this.supported(size, variant);
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
	 * Classifies a solved-or-stuck technique report into a band, clamped to the ceiling of that size and variant.
	 *
	 * @param size The grid size the report belongs to
	 * @param variant The region layout variant the report belongs to
	 * @param report The technique-solver report for the puzzle
	 * @return The difficulty band, one of {@link Difficulty#ONE} through {@link Difficulty#LISA}
	 */
	public Difficulty classify(GridSize size, Variant variant, TechniqueReport report) {
		if (report.exceededCap()) {
			throw new IllegalArgumentException("A report that stopped at a level cap carries no rating, only the fact that the puzzle is harder than that cap");
		}
		
		Difficulty band = Difficulty.ofIndex(this.effectiveLevel(size, report));
		Difficulty ceiling = this.ceiling(size, variant);
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
