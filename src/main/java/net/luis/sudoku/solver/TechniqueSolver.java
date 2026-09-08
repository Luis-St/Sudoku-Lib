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
		new FullHouse(),
		new LastDigit(),
		new NakedSingle(),
		new HiddenSingleRegion(),
		new HiddenSingleLine(),
		new Pointing(),
		new Claiming(),
		new LawOfLeftovers(),
		new NakedPair(),
		new HiddenPair(),
		new NakedTriple(),
		new HiddenTriple(),
		new XWing(),
		new Skyscraper(),
		new TwoStringKite(),
		new Swordfish(),
		new BugPlusOne(),
		new Crane(),
		new XyWing(),
		new UniqueRectangle1(),
		new UniqueRectangle2(),
		new XyzWing(),
		new WWing(),
		new FinnedXWing(),
		new NakedQuad(),
		new HiddenQuad(),
		new UniqueRectangle3(),
		new UniqueRectangle4(),
		new EmptyRectangle(),
		new FinnedSwordfish(),
		new SashimiSwordfish(),
		new Jellyfish(),
		new SimpleColouring(),
		new WxyzWing(),
		new XChain(),
		new XyChain(),
		new Aic(),
		new AlsXz(),
		new SueDeCoq(),
		new Medusa3d(),
		new MultiColouring(),
		new GroupedAic(),
		new AlsChain(),
		new Nishio(),
		new ForcingChain(),
		new ForcingNet(),
		new DeathBlossom(),
		new DynamicContradictionChain()
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
		return solve(puzzle, Technique.MAX_LEVEL);
	}
	
	/**
	 * Solves the given puzzle using only techniques up to {@code maxLevel}, aborting as soon as it would need a
	 * harder one.
	 * <p>
	 *     This is the early-abort form used by the generator's band search. Because the driver always applies the
	 *     lowest applicable technique, "no technique at or below the cap applies" is a proof that the puzzle's rating
	 *     is <b>above</b> the cap, and the report comes back {@link TechniqueReport#exceededCap() exceededCap}. The
	 *     saving is that the harder strategies are never even scanned: rating against a low target never constructs a
	 *     forcing chain. A cap of {@link Technique#MAX_LEVEL} is exactly {@link #solve(Puzzle)}.
	 * </p>
	 * <p>
	 *     A capped report that exceeded its cap carries no usable rating — it says "harder than this" and nothing
	 *     more — so {@link net.luis.sudoku.difficulty.DifficultyBands#classify} rejects it. The cheap "is this puzzle
	 *     trivial" prefilter is just this method at a cap of 3.
	 * </p>
	 *
	 * @param puzzle The puzzle to solve
	 * @param maxLevel The hardest technique level the solver may use, {@code 1..}{@link Technique#MAX_LEVEL}
	 * @return The solve report
	 * @throws NullPointerException If the puzzle is null
	 * @throws IllegalArgumentException If the level is outside {@code 1..}{@link Technique#MAX_LEVEL}
	 */
	public static TechniqueReport solve(Puzzle puzzle, int maxLevel) {
		Objects.requireNonNull(puzzle, "Puzzle must not be null");
		if (maxLevel < 1 || maxLevel > Technique.MAX_LEVEL) {
			throw new IllegalArgumentException("Maximum level " + maxLevel + " is not in 1.." + Technique.MAX_LEVEL);
		}
		
		CandidateGrid grid = new CandidateGrid(puzzle);
		EnumMap<Technique, Integer> usage = new EnumMap<>(Technique.class);
		
		while (!grid.isComplete()) {
			Deduction deduction = nextDeduction(grid, maxLevel);
			if (deduction == null) {
				return new TechniqueReport(false, true, maxLevel < Technique.MAX_LEVEL, grid.values(), usage);
			}
			
			deduction.applyTo(grid);
			usage.merge(deduction.technique(), 1, Integer::sum);
		}
		return new TechniqueReport(grid.isSolved(), false, false, grid.values(), usage);
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
			Deduction deduction = nextDeduction(grid, Technique.MAX_LEVEL);
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
	
	/**
	 * Returns the next placement together with the argument for the technique that unlocks it.
	 * <p>
	 *     The explained twin of {@link #nextStep(Puzzle)}, and it makes the same walk: apply eliminations until a
	 *     placement turns up, and report the hardest technique that was needed on the way. What it adds is the
	 *     {@link Explanation} of <i>that</i> deduction - the pattern a player has to see in order to make the move
	 *     themselves rather than be handed it.
	 * </p>
	 * <p>
	 *     The explanation is of the hardest deduction rather than of the placement, and those are usually different
	 *     deductions. A placement unlocked by an X-Wing is a naked single by the time it is placed; explaining the
	 *     single would show the player the one part of the position they could already see.
	 * </p>
	 * <p>
	 *     Costlier than {@link #nextStep(Puzzle)}, because a strategy that records its pattern while it searches does
	 *     that work here and not there. It is paid once, when a player asks for a hint.
	 * </p>
	 *
	 * @param puzzle The puzzle to find the next step for
	 * @return The next placement with its argument, or empty if the puzzle is already solved or the solver gets stuck
	 * @throws NullPointerException If the puzzle is null
	 */
	public static Optional<ExplainedSolveStep> nextExplainedStep(Puzzle puzzle) {
		Objects.requireNonNull(puzzle, "Puzzle must not be null");
		CandidateGrid grid = new CandidateGrid(puzzle);
		Technique hardest = null;
		Explanation hardestExplanation = null;
		while (!grid.isComplete()) {
			ExplainedDeduction explained = nextExplainedDeduction(grid);
			if (explained == null) {
				return Optional.empty();
			}
			
			Deduction deduction = explained.deduction();
			Technique technique = deduction.technique();
			if (hardest == null || technique.rank() > hardest.rank()) {
				hardest = technique;
				hardestExplanation = explained.explanation();
			}
			if (deduction instanceof Deduction.Placement placement) {
				return Optional.of(new ExplainedSolveStep(new SolveStep(placement.cell(), placement.digit(), hardest), hardestExplanation));
			}
			
			deduction.applyTo(grid);
		}
		return Optional.empty();
	}
	
	/**
	 * Returns the deduction the driver would make next on the given working grid, without applying it.
	 * <p>
	 *     This is {@link #solve(Puzzle)}'s inner step, exposed so a caller can drive the solve itself while watching
	 *     what happens. The learn area needs exactly that: it walks a puzzle forwards looking for the position in
	 *     which one chosen technique is the easiest thing that applies, and it has to see each deduction to recognise
	 *     that moment.
	 * </p>
	 * <p>
	 *     Because the driver always returns the lowest-ranked technique that makes progress, a returned deduction of
	 *     technique {@code T} is a proof that <b>no</b> technique easier than {@code T} applies to this grid.
	 * </p>
	 *
	 * @param grid The working grid, which is not mutated
	 * @return The next deduction, or an empty optional if no technique applies
	 * @throws NullPointerException If the grid is null
	 */
	public static Optional<Deduction> nextDeduction(CandidateGrid grid) {
		Objects.requireNonNull(grid, "Grid must not be null");
		
		return Optional.ofNullable(nextDeduction(grid, Technique.MAX_LEVEL));
	}
	
	/**
	 * Returns the deduction the driver would make next using only techniques up to {@code maxLevel}, without applying
	 * it.
	 * <p>
	 *     An empty result is the useful half: it proves that <b>nothing</b> at or below the given level applies to
	 *     this grid. The learn area asks exactly that question, to establish that a position really does need the
	 *     technique it is about to teach and cannot be solved past with anything easier.
	 * </p>
	 *
	 * @param grid The working grid, which is not mutated
	 * @param maxLevel The hardest technique level to consider, {@code 1..}{@link Technique#MAX_LEVEL}
	 * @return The next deduction, or an empty optional if nothing at or below that level applies
	 * @throws NullPointerException If the grid is null
	 * @throws IllegalArgumentException If the level is outside {@code 1..}{@link Technique#MAX_LEVEL}
	 */
	public static Optional<Deduction> nextDeductionUpTo(CandidateGrid grid, int maxLevel) {
		Objects.requireNonNull(grid, "Grid must not be null");
		if (maxLevel < 1 || maxLevel > Technique.MAX_LEVEL) {
			throw new IllegalArgumentException("Maximum level " + maxLevel + " is not in 1.." + Technique.MAX_LEVEL);
		}
		
		return Optional.ofNullable(nextDeduction(grid, maxLevel));
	}
	
	/**
	 * The explained twin of {@link #nextDeduction(CandidateGrid, int)}, uncapped.
	 * <p>
	 *     Scans in the same escalating order and returns the same deduction, which the
	 *     {@link TechniqueStrategy#findExplained(CandidateGrid)} contract guarantees: the teaching path and the
	 *     solving path have to agree, or a hint would show a pattern the solver never used.
	 * </p>
	 */
	private static ExplainedDeduction nextExplainedDeduction(CandidateGrid grid) {
		for (TechniqueStrategy strategy : STRATEGIES) {
			Optional<ExplainedDeduction> explained = strategy.findExplained(grid);
			if (explained.isPresent()) {
				return explained.orElseThrow();
			}
		}
		return null;
	}
	
	private static Deduction nextDeduction(CandidateGrid grid, int maxLevel) {
		for (TechniqueStrategy strategy : STRATEGIES) {
			// STRATEGIES is in escalating level order, so the first strategy above the cap ends the scan: everything
			// after it is at least as hard. This is where the early abort actually saves its time.
			if (strategy.technique().level() > maxLevel) {
				return null;
			}
			
			Optional<Deduction> deduction = strategy.find(grid);
			if (deduction.isPresent()) {
				return deduction.orElseThrow();
			}
		}
		return null;
	}
}
