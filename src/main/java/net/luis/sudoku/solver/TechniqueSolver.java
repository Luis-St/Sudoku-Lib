package net.luis.sudoku.solver;

import net.luis.sudoku.grid.Puzzle;

import java.util.*;

/**
 * The human-technique solver: it solves a puzzle the way a person would, applying named techniques in escalating
 * order and never guessing.
 * <p>
 *     This is spec §4.2 and the shared foundation of both difficulty rating (phase L6) and hints (phase L8). The
 *     driver repeatedly scans {@link #STRATEGIES} in {@link Technique} order, applies the first deduction the lowest
 *     applicable technique offers, and restarts the scan from the top. Always preferring the lowest technique is what
 *     makes the rating meaningful: a puzzle is only as hard as the hardest technique it genuinely forces. The solver
 *     never calls the {@link BacktrackingSolver} and never guesses, so a puzzle that needs a guess is reported as
 *     {@link TechniqueReport#stuck() stuck} rather than solved.
 * </p>
 * <p>
 *     Both entry points are deterministic and leave the input puzzle untouched, working on a {@link CandidateGrid}
 *     snapshot instead.
 * </p>
 *
 * @see TechniqueReport
 * @see SolveStep
 */
public final class TechniqueSolver {
	
	/**
	 * The strategies in strictly escalating {@link Technique} order, one per technique constant. The driver applies
	 * the first of these that can make progress, so their order is the difficulty order the whole solver depends on.
	 */
	public static final List<TechniqueStrategy> STRATEGIES = List.of(
		new NakedSingle(),
		new HiddenSingle(),
		new NakedPair(),
		new NakedTriple(),
		new HiddenPair(),
		new HiddenTriple(),
		new PointingPair(),
		new BoxLineReduction(),
		new XWing(),
		new Swordfish(),
		new XyWing()
	);
	
	private TechniqueSolver() {}
	
	/**
	 * Solves the given puzzle with human techniques alone and reports the outcome.
	 * <p>
	 *     The puzzle is snapshotted and never mutated. The returned {@link TechniqueReport} records whether the puzzle
	 *     was solved, whether the solver got stuck, the final grid values, and how often each technique fired — the
	 *     inputs the difficulty rater needs.
	 * </p>
	 *
	 * @param puzzle The puzzle to solve
	 * @return The solve report
	 * @throws NullPointerException If the puzzle is null
	 */
	public static TechniqueReport solve(Puzzle puzzle) {
		Objects.requireNonNull(puzzle, "Puzzle must not be null");
		CandidateGrid grid = new CandidateGrid(puzzle);
		EnumMap<Technique, Integer> usage = new EnumMap<>(Technique.class);
		while (!grid.isComplete()) {
			Deduction deduction = nextDeduction(grid);
			if (deduction == null) {
				return new TechniqueReport(false, true, grid.values(), usage);
			}
			deduction.applyTo(grid);
			usage.merge(deduction.technique(), 1, Integer::sum);
		}
		return new TechniqueReport(grid.isSolved(), false, grid.values(), usage);
	}
	
	/**
	 * Returns the next placement the solver would make, for the hint engine.
	 * <p>
	 *     The driver runs exactly as in {@link #solve(Puzzle)} but stops at the first placement, reporting it as a
	 *     {@link SolveStep} whose technique is the hardest one applied since the previous placement — the technique
	 *     that unlocked the cell. Elimination-only techniques applied on the way are folded into that step rather than
	 *     surfaced on their own, because a hint always fills a cell. The input puzzle is not mutated.
	 * </p>
	 *
	 * @param puzzle The puzzle to find the next step for
	 * @return The next placement, or empty if the puzzle is already solved or the solver gets stuck first
	 * @throws NullPointerException If the puzzle is null
	 */
	public static Optional<SolveStep> nextStep(Puzzle puzzle) {
		Objects.requireNonNull(puzzle, "Puzzle must not be null");
		CandidateGrid grid = new CandidateGrid(puzzle);
		Technique hardest = null;
		while (!grid.isComplete()) {
			Deduction deduction = nextDeduction(grid);
			if (deduction == null) {
				return Optional.empty();
			}
			Technique technique = deduction.technique();
			if (hardest == null || technique.rank() > hardest.rank()) {
				hardest = technique;
			}
			if (deduction instanceof Deduction.Placement placement) {
				return Optional.of(new SolveStep(placement.cell(), placement.digit(), hardest));
			}
			deduction.applyTo(grid);
		}
		return Optional.empty();
	}
	
	private static Deduction nextDeduction(CandidateGrid grid) {
		for (TechniqueStrategy strategy : STRATEGIES) {
			Optional<Deduction> deduction = strategy.find(grid);
			if (deduction.isPresent()) {
				return deduction.orElseThrow();
			}
		}
		return null;
	}
}
