package net.luis.sudoku.difficulty;

import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.solver.Technique;
import net.luis.sudoku.solver.TechniqueReport;

import java.util.*;

/**
 * The size-aware configuration that maps a {@link TechniqueReport} to a numbered {@link Difficulty} band.
 * <p>
 *     Difficulty is derived from <b>which techniques a puzzle forces</b>, not from the number of givens (spec §4.3).
 *     The dominant signal is the hardest technique the {@link net.luis.sudoku.solver.TechniqueSolver} needed; a puzzle
 *     it cannot finish without guessing is beyond the modelled technique set and falls in the hardest band. The rank
 *     boundaries below group the eleven techniques into five bands:
 * </p>
 * <ul>
 *     <li>{@link Difficulty#ONE} — nothing harder than a hidden single.</li>
 *     <li>{@link Difficulty#TWO} — up to a naked/hidden pair or triple.</li>
 *     <li>{@link Difficulty#THREE} — up to a pointing pair or box/line reduction.</li>
 *     <li>{@link Difficulty#FOUR} — an X-Wing, Swordfish or XY-Wing was required.</li>
 *     <li>{@link Difficulty#FIVE} — the technique set could not finish it; a guess is needed.</li>
 * </ul>
 * <p>
 *     The mapping is then <b>clamped</b> to the hardest band a size can actually produce: a 4x4 or 6x6 grid simply
 *     cannot demand a genuinely hard band (spec §4.3), so a request for a higher band there collapses to the size's
 *     ceiling. {@link Difficulty#LISA} is not a band of its own — it is the hardest band plus a runtime modifier set —
 *     so the rater only ever returns numbered bands, and the generator targets the size ceiling for a LISA request.
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
	
	private static final DifficultyBands DEFAULTS = new DifficultyBands(defaultCeilings());
	
	private final Map<GridSize, Difficulty> ceilings;
	
	private DifficultyBands(Map<GridSize, Difficulty> ceilings) {
		this.ceilings = new EnumMap<>(ceilings);
	}
	
	/**
	 * Returns the default band configuration.
	 *
	 * @return The shared default bands
	 */
	public static DifficultyBands defaults() {
		return DEFAULTS;
	}
	
	private static Map<GridSize, Difficulty> defaultCeilings() {
		Map<GridSize, Difficulty> ceilings = new EnumMap<>(GridSize.class);
		ceilings.put(GridSize.FOUR, Difficulty.TWO);
		ceilings.put(GridSize.SIX, Difficulty.THREE);
		ceilings.put(GridSize.NINE, Difficulty.FIVE);
		ceilings.put(GridSize.TWELVE, Difficulty.FIVE);
		ceilings.put(GridSize.SIXTEEN, Difficulty.FIVE);
		return ceilings;
	}
	
	/**
	 * Returns the hardest numbered band the given size can produce; harder requests clamp to it.
	 *
	 * @param size The grid size
	 * @return The band ceiling for that size, never {@link Difficulty#LISA}
	 */
	public Difficulty ceiling(GridSize size) {
		return Objects.requireNonNull(this.ceilings.get(size), "No band ceiling for grid size " + size);
	}
	
	/**
	 * Classifies a solved-or-stuck technique report into a numbered band, clamped to the size's ceiling.
	 *
	 * @param size The grid size the report belongs to
	 * @param report The technique-solver report for the puzzle
	 * @return The numbered difficulty band, one of {@link Difficulty#ONE} through {@link Difficulty#FIVE}
	 */
	public Difficulty classify(GridSize size, TechniqueReport report) {
		int rank = this.effectiveRank(report);
		Difficulty band = this.bandForRank(rank);
		Difficulty ceiling = this.ceiling(size);
		return band.index() > ceiling.index() ? ceiling : band;
	}
	
	/**
	 * Returns the rating signal of a report: the hardest technique's {@link Technique#rank()}, or a value above every
	 * technique when the solver got stuck (a guess was needed), or {@code 1} when no technique fired at all.
	 */
	private int effectiveRank(TechniqueReport report) {
		if (report.stuck()) {
			return Technique.values().length + 1;
		}
		return report.hardestTechnique().map(Technique::rank).orElse(1);
	}
	
	private Difficulty bandForRank(int rank) {
		if (rank <= Technique.HIDDEN_SINGLE.rank()) {
			return Difficulty.ONE;
		}
		if (rank <= Technique.HIDDEN_TRIPLE.rank()) {
			return Difficulty.TWO;
		}
		if (rank <= Technique.BOX_LINE_REDUCTION.rank()) {
			return Difficulty.THREE;
		}
		if (rank <= Technique.XY_WING.rank()) {
			return Difficulty.FOUR;
		}
		return Difficulty.FIVE;
	}
}
