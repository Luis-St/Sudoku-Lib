package net.luis.sudoku.learn;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.generation.GeneratedPuzzle;
import net.luis.sudoku.generation.PuzzleGenerator;
import net.luis.sudoku.grid.*;
import net.luis.sudoku.key.PuzzleKey;
import net.luis.sudoku.solver.*;

import java.util.*;

/**
 * Finds positions in which one chosen technique is exactly the next thing to do, which is what the learn area needs
 * and what an ordinary puzzle generator cannot promise.
 * <p>
 *     Asking a generator for "a puzzle that needs an X-Wing" and handing the result to a learner does not work: a
 *     puzzle <i>rated</i> at a technique's level still opens with a long stretch of singles, and by the time the
 *     X-Wing is reachable the player has done fifty moves that have nothing to do with what they came to learn. So
 *     this generator does not produce puzzles, it produces <b>positions</b>: it generates a puzzle, walks it forwards
 *     with the solver, and stops at the exact moment the target technique becomes the easiest thing that applies.
 * </p>
 * <p>
 *     What counts as "the easiest thing" is asked twice, in two passes, because the strict answer is the fair one but
 *     is not always reachable:
 * </p>
 * <ol>
 *     <li><b>Strict.</b> The taught technique is the single lowest-ranked technique that applies, so it is the only
 *         move available and the target is the only placement a player can reach. This is what almost every technique
 *         gets, and it is what keeps the app's "the first digit you write must be the target" rule fair.</li>
 *     <li><b>Relaxed.</b> Nothing of a lower {@link Technique#level() level} applies, but a technique of the same
 *         level might. A level is the rating axis and the techniques sharing one are deliberately treated as equally
 *         hard, so the player is still being given no <i>easier</i> way through. Only the techniques that the strict
 *         pass can never find fall back to this: measured over thousands of seeds, a hidden quad is always beaten to
 *         the position by a naked quad or a wing of the same level, and multi-colouring by 3D Medusa, which subsumes
 *         what it does. Neither is rare in real play, they are simply never the <i>first</i> name the solver reaches
 *         for.</li>
 * </ol>
 * <p>
 *     The relaxed pass is only ever entered after the strict one has failed across the whole budget, so a technique
 *     that can be taught strictly always is.
 * </p>
 * <p>
 *     The candidate state at that moment becomes the puzzle's pencil marks, which is the only way the guarantee
 *     survives being written to disk: candidates re-derived from the board alone would restore what the earlier
 *     eliminations removed, and an easier technique would apply again.
 * </p>
 * <p>
 *     Generation is a search with no guaranteed running time. A technique that is common turns up within a few seeds;
 *     a rare one may need many, and some seeds are simply slow to generate. Every entry point therefore takes a
 *     {@link Budget} and returns an empty optional rather than running forever, so a caller on a phone can promise
 *     "a few seconds" and keep that promise by giving up instead.
 * </p>
 *
 * @see LearnPuzzle
 */
public final class LearnPuzzleGenerator {
	
	/**
	 * How many difficulty bands above its own a technique is looked for in.
	 * <p>
	 *     A technique's own band is where it is most likely to be the hardest step, but for the techniques that are
	 *     rarely the hardest step of anything, that band is nearly always solved by something easier before they get
	 *     a chance. Measured over 600 seeds each, the hidden quad, the jellyfish and multi-colouring never once
	 *     surfaced at their own band, and all three do at the bands just above, where the position stays tangled long
	 *     enough for them to become the easiest thing left. Sweeping a few bands costs the common techniques nothing,
	 *     since they are found on the first attempt, at their own band.
	 * </p>
	 */
	private static final int BAND_SPREAD = 5;
	/**
	 * How many seeds a single {@link #generate(Technique, long, Budget)} call may try before giving up, when the
	 * budget does not say otherwise.
	 */
	public static final int DEFAULT_ATTEMPTS = 64;
	/**
	 * How long a single call may run before giving up, when the budget does not say otherwise.
	 */
	public static final long DEFAULT_TIMEOUT_MILLIS = 15_000L;
	
	private LearnPuzzleGenerator() {}
	
	/**
	 * Finds one position in which the given technique is the next thing to do.
	 *
	 * @param technique The technique to teach
	 * @param firstSeed The seed to start the search at; consecutive seeds are tried from there
	 * @param budget How much work to spend before giving up
	 * @return The exercise, or an empty optional if the budget ran out first
	 * @throws NullPointerException If the technique or the budget is null
	 * @throws IllegalArgumentException If the technique is not one the learn area teaches
	 */
	public static Optional<LearnPuzzle> generate(Technique technique, long firstSeed, Budget budget) {
		Objects.requireNonNull(technique, "Technique must not be null");
		Objects.requireNonNull(budget, "Budget must not be null");
		if (!LearnTechniques.isTaught(technique)) {
			throw new IllegalArgumentException("The learn area does not teach " + technique);
		}
		
		long deadline = System.currentTimeMillis() + budget.timeoutMillis();
		for (boolean strict : new boolean[] { true, false }) {
			for (int attempt = 0; attempt < budget.attempts(); attempt++) {
				if (System.currentTimeMillis() >= deadline) {
					return Optional.empty();
				}
				
				Optional<LearnPuzzle> found = fromSeed(technique, firstSeed + attempt, bandFor(technique, attempt), strict);
				if (found.isPresent()) {
					return found;
				}
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Finds several positions for the given technique, each showing the pattern in a visibly different place.
	 * <p>
	 *     Distinctness is by {@link LearnPuzzle#layoutKey()}, so the caller gets five different pictures of one idea
	 *     rather than five grids that happen to differ in cells nobody looks at. If the budget runs out first, the
	 *     positions found so far are returned rather than nothing: four good examples beat none.
	 * </p>
	 *
	 * @param technique The technique to teach
	 * @param count How many positions are wanted
	 * @param firstSeed The seed to start the search at
	 * @param budget How much work to spend before giving up
	 * @return The positions found, at most {@code count} of them, each with a different layout
	 * @throws NullPointerException If the technique or the budget is null
	 * @throws IllegalArgumentException If the technique is not taught, or the count is not positive
	 */
	public static List<LearnPuzzle> generateSet(Technique technique, int count, long firstSeed, Budget budget) {
		Objects.requireNonNull(technique, "Technique must not be null");
		Objects.requireNonNull(budget, "Budget must not be null");
		if (!LearnTechniques.isTaught(technique)) {
			throw new IllegalArgumentException("The learn area does not teach " + technique);
		}
		if (count < 1) {
			throw new IllegalArgumentException("Count must be positive, but was " + count);
		}
		
		long deadline = System.currentTimeMillis() + budget.timeoutMillis();
		List<LearnPuzzle> found = new ArrayList<>(count);
		// A sorted set rather than a hashed one: the generator is held to the same determinism rules as the rest of
		// the core, and a sorted set has an iteration order that does not depend on the JVM.
		Set<String> layouts = new TreeSet<>();
		for (boolean strict : new boolean[] { true, false }) {
			for (int attempt = 0; attempt < budget.attempts() && found.size() < count; attempt++) {
				if (System.currentTimeMillis() >= deadline) {
					return List.copyOf(found);
				}
				
				Optional<LearnPuzzle> puzzle = fromSeed(technique, firstSeed + attempt, bandFor(technique, attempt), strict);
				if (puzzle.isPresent() && layouts.add(puzzle.get().layoutKey())) {
					found.add(puzzle.get());
				}
			}
			if (found.size() >= count) {
				break;
			}
		}
		return List.copyOf(found);
	}
	
	/**
	 * Returns the difficulty band to generate at for the given attempt.
	 * <p>
	 *     Attempt zero is always the technique's own band, so a common technique is found immediately and at the
	 *     difficulty it belongs to. Later attempts fan upwards through {@link #BAND_SPREAD} bands, capped at the
	 *     hardest band that exists.
	 * </p>
	 *
	 * @param technique The technique being looked for
	 * @param attempt The zero-based attempt number
	 * @return The band to generate at
	 */
	private static Difficulty bandFor(Technique technique, int attempt) {
		int band = technique.level() + attempt % BAND_SPREAD;
		return Difficulty.ofIndex(Math.min(band, Technique.MAX_LEVEL));
	}
	
	/**
	 * Walks one generated puzzle forwards, looking for the moment the target technique becomes the easiest thing that
	 * applies.
	 *
	 * @param technique The technique to teach
	 * @param seed The seed of the puzzle to walk
	 * @param difficulty The band to generate the puzzle at
	 * @param strict True to demand that the technique is the only move available, false to allow a sibling of its own
	 *        level to apply as well
	 * @return The exercise, or empty if this puzzle never reaches such a moment
	 */
	private static Optional<LearnPuzzle> fromSeed(Technique technique, long seed, Difficulty difficulty, boolean strict) {
		GeneratedPuzzle generated = PuzzleGenerator.generate(PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, difficulty, seed));
		int[] solution = generated.solution();
		
		CandidateGrid grid = new CandidateGrid(generated.puzzle());
		while (!grid.isComplete()) {
			Optional<Deduction> next = TechniqueSolver.nextDeduction(grid);
			if (next.isEmpty()) {
				return Optional.empty();
			}
			
			Deduction deduction = next.get();
			if (accepts(grid, deduction, technique, strict)) {
				Optional<LearnPuzzle> puzzle = capture(technique, grid, solution);
				if (puzzle.isPresent()) {
					return puzzle;
				}
				// This position leads nowhere usable, but a later one in the same solve still might, so the walk
				// carries on rather than throwing the whole seed away.
			}
			
			deduction.applyTo(grid);
		}
		return Optional.empty();
	}
	
	/**
	 * Turns the current position into an exercise, if the technique really does lead to a placement from here.
	 * <p>
	 *     The target is the first placement reached from this position, and it counts only if the target technique is
	 *     the hardest one used to get there. If the solver needs something harder in between, the placement would be
	 *     unreachable for a player who has only been taught this technique, and the position is rejected.
	 * </p>
	 *
	 * @param technique The technique to teach
	 * @param grid The position, in which nothing easier than the technique applies
	 * @param solution The solution of the underlying puzzle
	 * @return The exercise, or empty if the position does not lead to a placement this technique earns
	 */
	private static Optional<LearnPuzzle> capture(Technique technique, CandidateGrid grid, int[] solution) {
		Optional<ExplainedDeduction> explained = strategyFor(technique).findExplained(grid);
		if (explained.isEmpty()) {
			return Optional.empty();
		}
		
		// The walk starts by applying the technique itself, so the target is by construction a placement the player
		// reaches *through* it rather than one they could have reached around it.
		CandidateGrid walk = grid.copy();
		Deduction first = explained.get().deduction();
		if (first instanceof Deduction.Placement) {
			return build(technique, grid, solution, (Deduction.Placement) first, explained.get());
		}
		first.applyTo(walk);
		
		while (!walk.isComplete()) {
			// Capped at the technique's own level: if reaching the placement needs something harder, the exercise
			// would be unfinishable for a player who has only been taught this.
			Optional<Deduction> next = TechniqueSolver.nextDeductionUpTo(walk, technique.level());
			if (next.isEmpty()) {
				return Optional.empty();
			}
			
			Deduction deduction = next.get();
			if (deduction instanceof Deduction.Placement) {
				return build(technique, grid, solution, (Deduction.Placement) deduction, explained.get());
			}
			
			deduction.applyTo(walk);
		}
		return Optional.empty();
	}
	
	/**
	 * Checks whether this position is one the technique can be taught in.
	 *
	 * @param grid The position
	 * @param deduction The lowest-ranked deduction available in it
	 * @param technique The technique being taught
	 * @param strict True to demand the technique is the only move available
	 * @return True if the position is acceptable
	 */
	private static boolean accepts(CandidateGrid grid, Deduction deduction, Technique technique, boolean strict) {
		if (strict) {
			// The driver returns the lowest-ranked technique that applies, so this alone proves the taught technique
			// is the only move: nothing easier exists, and no sibling of its own level was reached first.
			return deduction.technique() == technique;
		}
		if (deduction.technique().level() < technique.level()) {
			return false;
		}
		// Nothing easier applies exactly when the driver, capped one level below the target, finds nothing. A level 1
		// technique has nothing below it, so there is nothing to rule out.
		return technique.level() <= 1 || TechniqueSolver.nextDeductionUpTo(grid, technique.level() - 1).isEmpty();
	}
	
	/**
	 * Builds the exercise from the position and the placement the technique leads to.
	 *
	 * @param technique The technique to teach
	 * @param grid The position as the player will meet it
	 * @param solution The solution of the underlying puzzle
	 * @param placement The placement the technique leads to
	 * @param explained The technique's deduction and explanation in this position
	 * @return The exercise
	 */
	private static Optional<LearnPuzzle> build(Technique technique, CandidateGrid grid, int[] solution, Deduction.Placement placement, ExplainedDeduction explained) {
		int[] pencil = new int[LearnPuzzle.CELL_COUNT];
		for (int cell = 0; cell < LearnPuzzle.CELL_COUNT; cell++) {
			pencil[cell] = grid.isEmpty(cell) ? grid.candidates(cell) : 0;
		}
		return Optional.of(new LearnPuzzle(technique, grid.values(), solution, pencil, placement.cell(), placement.digit(), explained.explanation()));
	}
	
	/**
	 * Returns the strategy implementing the given technique.
	 *
	 * @param technique The technique
	 * @return Its strategy
	 * @throws IllegalStateException If no strategy implements it
	 */
	private static TechniqueStrategy strategyFor(Technique technique) {
		for (TechniqueStrategy strategy : TechniqueSolver.STRATEGIES) {
			if (strategy.technique() == technique) {
				return strategy;
			}
		}
		throw new IllegalStateException("No strategy implements " + technique);
	}
	
	/**
	 * Rebuilds a position as a {@link Puzzle}, for a caller that wants to hand it to the ordinary solver.
	 * <p>
	 *     Every filled cell of the position becomes a given. That is deliberate: the digits already on the board were
	 *     placed by the solver, not by the player, so in the exercise they are facts rather than moves, and nothing
	 *     should let them be erased.
	 * </p>
	 *
	 * @param puzzle The exercise
	 * @return The position as a puzzle
	 * @throws NullPointerException If the exercise is null
	 */
	public static Puzzle asPuzzle(LearnPuzzle puzzle) {
		Objects.requireNonNull(puzzle, "Puzzle must not be null");
		
		return Puzzle.classicOfGivens(GridSize.NINE, puzzle.board());
	}
	
	/**
	 * How much work a generation call may do before giving up.
	 * <p>
	 *     Both limits are checked, and whichever is reached first ends the search. The attempt count keeps a fast
	 *     machine from spinning on a technique that never turns up; the deadline keeps a slow one from freezing an
	 *     interface that has promised the player a few seconds.
	 * </p>
	 *
	 * @param attempts The greatest number of seeds to try, at least one
	 * @param timeoutMillis The greatest time to spend, in milliseconds, at least one
	 */
	public record Budget(int attempts, long timeoutMillis) {
		
		/**
		 * Constructs a budget.
		 *
		 * @throws IllegalArgumentException If either limit is not positive
		 */
		public Budget {
			if (attempts < 1) {
				throw new IllegalArgumentException("Attempts must be positive, but was " + attempts);
			}
			if (timeoutMillis < 1) {
				throw new IllegalArgumentException("Timeout must be positive, but was " + timeoutMillis);
			}
		}
		
		/**
		 * Returns the budget a caller gets when it does not ask for a particular one.
		 *
		 * @return The default budget
		 */
		public static Budget standard() {
			return new Budget(DEFAULT_ATTEMPTS, DEFAULT_TIMEOUT_MILLIS);
		}
		
		/**
		 * Returns a budget suited to generating offline, where taking a long time costs nobody anything.
		 * <p>
		 *     An hour per call, which sounds absurd until it is measured: five <i>distinct</i> examples of simple
		 *     colouring took 456 seconds to find, of the X-Chain 358, and of the jellyfish 765, all of them past the
		 *     five minutes this used to allow. A run that gives up short leaves a technique with a carousel it cannot
		 *     fill, which is worse in every way than an export that takes an afternoon and is then committed forever.
		 * </p>
		 *
		 * @return A generous budget
		 */
		public static Budget offline() {
			return new Budget(100_000, 3_600_000L);
		}
	}
}
