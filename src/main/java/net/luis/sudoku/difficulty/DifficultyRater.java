package net.luis.sudoku.difficulty;

import net.luis.sudoku.generation.PuzzleGenerator;
import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.grid.Puzzle;
import net.luis.sudoku.solver.TechniqueReport;
import net.luis.sudoku.solver.TechniqueSolver;

import java.util.Objects;

/**
 * Rates a puzzle by running the human-technique solver over it and mapping the techniques it required to a numbered
 * {@link Difficulty} band through a {@link DifficultyBands} configuration.
 * <p>
 *     This is spec §4.3's generate-and-rate half: the {@link PuzzleGenerator} digs a
 *     candidate, this rater scores it, and the generator keeps it only if it lands in the requested band. The rating
 *     is a pure, deterministic function of the puzzle, because {@link TechniqueSolver#solve} is deterministic.
 * </p>
 * <p>
 *     The rater always returns a numbered band ({@link Difficulty#ONE} through {@link Difficulty#FIVE}), never
 *     {@link Difficulty#LISA}: Lisa is the hardest band plus a runtime modifier set, not a distinct rating.
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
	 * @return The numbered difficulty band
	 * @throws NullPointerException If the puzzle is null
	 */
	public Difficulty rate(Puzzle puzzle) {
		Objects.requireNonNull(puzzle, "Puzzle must not be null");
		TechniqueReport report = TechniqueSolver.solve(puzzle);
		return this.bands.classify(puzzle.size(), report);
	}
	
	/**
	 * Classifies an already-computed technique report for a puzzle of the given size, without solving again.
	 *
	 * @param size The grid size the report belongs to
	 * @param report The technique-solver report
	 * @return The numbered difficulty band
	 * @throws NullPointerException If the size or report is null
	 */
	public Difficulty rate(GridSize size, TechniqueReport report) {
		Objects.requireNonNull(size, "Size must not be null");
		Objects.requireNonNull(report, "Report must not be null");
		return this.bands.classify(size, report);
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
