package net.luis.sudoku.difficulty;

import net.luis.sudoku.generation.PuzzleGenerator;
import net.luis.sudoku.grid.*;
import net.luis.sudoku.solver.TechniqueReport;
import net.luis.sudoku.solver.TechniqueSolver;

import java.util.Objects;
import java.util.Optional;

/**
 * Rates a puzzle by running the human-technique solver over it and mapping the techniques it required to a
 * {@link Difficulty} band through a {@link DifficultyBands} configuration.
 * <p>
 *     This is spec §4.3's generate-and-rate half: the {@link PuzzleGenerator} digs a
 *     candidate, this rater scores it, and the generator keeps it only if it lands in the requested band. The rating
 *     is a pure, deterministic function of the puzzle, because {@link TechniqueSolver#solve} is deterministic.
 * </p>
 * <p>
 *     The rater returns any of the fifteen bands, {@link Difficulty#LISA} included: level 15 names real branching
 *     techniques, so Lisa is a genuine rating and not merely "the solver gave up". Lisa's runtime modifier set is a
 *     client concern layered on top of the band, and plays no part in rating.
 * </p>
 *
 * @see DifficultyBands
 * @see TechniqueSolver
 */
public record DifficultyRater(DifficultyBands bands) {
	
	/**
	 * Constructs a rater with the {@link DifficultyBands#defaults() default} band configuration.
	 */
	public DifficultyRater() {
		this(DifficultyBands.defaults());
	}
	
	/**
	 * Constructs a rater with the given band configuration.
	 *
	 * @param bands The bands to classify against
	 * @throws NullPointerException If the bands are null
	 */
	public DifficultyRater(DifficultyBands bands) {
		this.bands = Objects.requireNonNull(bands, "Bands must not be null");
	}
	
	/**
	 * Rates the given puzzle by solving it with human techniques and classifying the result.
	 *
	 * @param puzzle The puzzle to rate
	 * @return The difficulty band
	 * @throws NullPointerException If the puzzle is null
	 */
	public Difficulty rate(Puzzle puzzle) {
		Objects.requireNonNull(puzzle, "Puzzle must not be null");
		TechniqueReport report = TechniqueSolver.solve(puzzle);
		return this.bands.classify(puzzle.size(), puzzle.variant(), report);
	}
	
	/**
	 * Rates the given puzzle only as far as the given band, aborting early once it is provably harder.
	 * <p>
	 *     This is the generator's search probe. Rating a candidate against a target band does not need a number, it
	 *     needs a direction, and a capped solve gives that far more cheaply: the strategies above the cap are never
	 *     scanned at all. An empty result means "harder than {@code maxBand}" with no further detail, which is
	 *     exactly what a bisection over the hole budget needs in order to dig less.
	 * </p>
	 *
	 * @param puzzle The puzzle to rate
	 * @param maxBand The hardest band to rate up to
	 * @return The band, or empty if the puzzle is harder than {@code maxBand}
	 * @throws NullPointerException If the puzzle or the band is null
	 */
	public Optional<Difficulty> rateUpTo(Puzzle puzzle, Difficulty maxBand) {
		Objects.requireNonNull(puzzle, "Puzzle must not be null");
		Objects.requireNonNull(maxBand, "Maximum band must not be null");
		
		TechniqueReport report = TechniqueSolver.solve(puzzle, maxBand.index());
		// A stuck report is empty for the same reason a capped one is: the puzzle is harder than what was asked for.
		// At a cap of LISA that means the puzzle is beyond the modelled technique set entirely, which is emphatically
		// not the same thing as a genuine level-15 puzzle, and the generator must not hand it to a player as one.
		// The uncapped rate(Puzzle) still reports LISA for such a grid, so no caller outside the search is affected.
		if (report.exceededCap() || report.stuck()) {
			return Optional.empty();
		}
		return Optional.of(this.bands.classify(puzzle.size(), puzzle.variant(), report));
	}
	
	/**
	 * Classifies an already-computed technique report for a puzzle of the given size and variant, without solving
	 * again.
	 *
	 * @param size The grid size the report belongs to
	 * @param variant The region layout variant the report belongs to
	 * @param report The technique-solver report
	 * @return The difficulty band
	 * @throws NullPointerException If the size, the variant or the report is null
	 */
	public Difficulty rate(GridSize size, Variant variant, TechniqueReport report) {
		Objects.requireNonNull(size, "Size must not be null");
		Objects.requireNonNull(variant, "Variant must not be null");
		Objects.requireNonNull(report, "Report must not be null");
		return this.bands.classify(size, variant, report);
	}
	
	/**
	 * Returns the band configuration this rater uses.
	 *
	 * @return The bands
	 */
	@Override
	public DifficultyBands bands() {
		return this.bands;
	}
}
