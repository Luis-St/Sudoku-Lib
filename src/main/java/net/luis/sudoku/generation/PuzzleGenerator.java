package net.luis.sudoku.generation;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.difficulty.DifficultyRater;
import net.luis.sudoku.grid.*;
import net.luis.sudoku.key.KeyDerivation;
import net.luis.sudoku.key.PuzzleKey;
import net.luis.sudoku.rng.DeterministicRandom;
import net.luis.sudoku.solver.BacktrackingSolver;

import java.util.Objects;
import java.util.Optional;

/**
 * Orchestrates the generation pipeline: key to seed to partition to filled solution to dug puzzle.
 * <p>
 *     This is spec §3.3. A {@link PuzzleKey} is the only input, and the whole pipeline is deterministic: the same
 *     key yields a byte-identical {@link GeneratedPuzzle} on every JVM, on the server and on Android alike (spec
 *     §3.2). {@link KeyDerivation#randomFor(PuzzleKey)} turns the key into a single seeded
 *     {@link DeterministicRandom}; the partition is the {@link ClassicRegionPartition} box layout for a classic key or
 *     a grown {@link RegionGenerator} jigsaw for a chaos key; {@link SolutionFiller} fills one complete solution and
 *     {@link HoleDigger} digs it into a uniquely solvable puzzle.
 * </p>
 * <p>
 *     The generation runs as a bounded, deterministic attempt loop of at most {@link #MAX_ATTEMPTS} rounds. The
 *     random generator is derived <b>once</b> and every attempt keeps consuming from that same stream, never
 *     resetting it: resetting between attempts would make every attempt fill the identical solution and dig the
 *     identical holes, defeating the loop entirely. A fresh draw sequence per attempt is exactly what lets a later
 *     round differ from an earlier one.
 * </p>
 * <p>
 *     Digging is bounded by a per-size hole budget (see {@code maxHolesFor}) so that generation stays fast and
 *     finite at every size, including 16x16 where an unbounded dig of a near-minimal grid can take minutes. The
 *     budget is a constant of the size, so it never compromises determinism.
 * </p>
 * <p>
 *     <b>Generate and rate (spec §3.3, §4.3).</b> Each attempt is scored by a {@link DifficultyRater}: the candidate
 *     is returned as soon as its rated band equals the key's target band. The target is the key's difficulty clamped
 *     to the size's band ceiling, or the ceiling itself for a {@link Difficulty#LISA} request (Lisa is the hardest
 *     band plus runtime modifiers, not a distinct rating). Difficulty is part of the key and already diffused into the
 *     seed by {@link KeyDerivation}, so the same seed at two difficulties still produces two unrelated puzzles. When no
 *     attempt lands in the target band the generator returns the closest-rated candidate it saw — a deterministic
 *     fallback, since the attempt sequence is itself deterministic.
 * </p>
 *
 * @see SolutionFiller
 * @see HoleDigger
 * @see GeneratedPuzzle
 * @see KeyDerivation
 */
public final class PuzzleGenerator {
	
	private static final DifficultyRater RATER = new DifficultyRater();
	/**
	 * The maximum number of fill-and-dig attempts a single {@link #generate(PuzzleKey)} call makes before returning
	 * the closest-rated candidate it found.
	 * <p>
	 *     Each attempt fills a fresh solution, digs it and rates it; the loop stops early the moment an attempt lands
	 *     in the target band. The bound keeps generation finite even for a band that is rare or unreachable at a given
	 *     size, in which case the closest-rated candidate is returned instead.
	 * </p>
	 * <p>
	 *     Raised from 24 once an attempt stopped paying for a dig per budget step: a whole attempt now digs one walk
	 *     and slices it, so the marginal cost of another attempt is mostly the ratings, and only a <i>failing</i>
	 *     search pays the full bound at all. Measured at 9x9 over 32 seeds a band, doubling it lifted the weakest
	 *     band from 20/32 to 25/32 and the overall hit rate from 83% to 93%, for roughly 40% on the worst case of the
	 *     hardest bands.
	 * </p>
	 */
	public static final int MAX_ATTEMPTS = 48;
	
	/**
	 * How many hole budgets a single attempt tries on its own solution before giving up on it.
	 * <p>
	 *     The budget search is a bisection over the number of holes, so this many steps narrow the whole range of a
	 *     16x16 grid down to a single value. Spending them on one fixed solution <i>and one fixed dig order</i> is what
	 *     makes the rating a monotone signal to search on: two different digs of the same depth need not rate the same,
	 *     so a search that redrew the order per step would be comparing unrelated puzzles.
	 * </p>
	 */
	public static final int MAX_BUDGET_STEPS = 9;
	
	private PuzzleGenerator() {}
	
	/**
	 * Returns the maximum number of holes the digger may cut for the given size.
	 * <p>
	 *     Digging every removable cell is cheap at 4x4 through 12x12 but explodes at 16x16: proving a near-minimal
	 *     256-cell grid unique branches deeply even with the solver's minimum-remaining-values heuristic, and the
	 *     cost climbs sharply once the grid passes roughly 140 holes. A fixed per-size budget keeps generation fast
	 *     and, crucially, keeps it a <b>bounded, deterministic</b> loop as spec §3.2 requires — the cap is a
	 *     constant of the size, never a wall-clock timeout. The smaller sizes are left effectively uncapped because
	 *     their full dig already completes in milliseconds.
	 * </p>
	 * <p>
	 *     The budget bounds how <i>sparse</i> a puzzle can get, not its difficulty band; phase L6's rater will layer
	 *     its own target on top, always within this ceiling. A 16x16 puzzle therefore keeps at least 116 givens,
	 *     which is well within the playable range for that size.
	 * </p>
	 *
	 * @param size The grid size being generated
	 * @param variant The variant being generated
	 * @return The hole budget for that size and variant
	 */
	private static int maxHolesFor(GridSize size, Variant variant) {
		// Chaos used to be capped separately at half the grid, on the grounds that proving a sparse jigsaw unique is
		// expensive. Measured, that left 41+ givens at 9x9 and every band above 2 came back rated 1 or 2 — the cap,
		// not the rater, decided the difficulty of every chaos puzzle. Both variants now share the per-size cap,
		// which a whole attempt can afford now that it digs one walk rather than one per budget step.
		return switch (size) {
			case SIXTEEN -> 140;
			case TWELVE -> 90;
			default -> Integer.MAX_VALUE;
		};
	}
	
	/**
	 * Generates the puzzle belonging to the given key.
	 * <p>
	 *     The random generator is derived once from the key and drives everything. For a classic key the partition is
	 *     the cached box layout and each attempt fills a fresh solution to dig; for a chaos key the partition and one
	 *     valid solution are grown together (see {@link RegionGenerator}) and every attempt digs that same solution —
	 *     an empty jigsaw grid is deliberately never solved, since it can be fillable yet ruinously slow to solve. Each
	 *     attempt digs its solution into a uniquely solvable set of givens and rates it. The first candidate rated in
	 *     the target band, or the closest-rated candidate if none lands in it, is wrapped in a {@link GeneratedPuzzle}
	 *     together with its solution and returned.
	 * </p>
	 *
	 * @param key The key to generate from
	 * @return The generated puzzle, its known solution and the key it came from
	 * @throws NullPointerException If the key is null
	 * @throws IllegalStateException If not one of the {@link #MAX_ATTEMPTS} attempts produced any fillable candidate,
	 * 		which is unreachable in practice but keeps the bounded loop honest
	 */
	public static GeneratedPuzzle generate(PuzzleKey key) {
		Objects.requireNonNull(key, "Key must not be null");
		int maxHoles = maxHolesFor(key.size(), key.variant());
		Difficulty target = targetBandFor(key);
		DeterministicRandom random = KeyDerivation.randomFor(key);
		
		RegionPartition partition;
		int[] chaosSolution = null;
		if (key.variant() == Variant.CHAOS) {
			RegionGenerator.ChaosLayout layout = RegionGenerator.generateChaosLayout(key.size(), random);
			partition = layout.partition();
			chaosSolution = layout.solution();
		} else {
			partition = ClassicRegionPartition.of(key.size());
		}
		
		int ceiling = Math.min(maxHoles, key.size().cellCount());
		GeneratedPuzzle closestBelow = null;
		int closestBelowDistance = Integer.MAX_VALUE;
		GeneratedPuzzle shallowestAbove = null;
		int shallowestAboveHoles = Integer.MAX_VALUE;
		for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
			int[] solution;
			if (chaosSolution != null) {
				solution = chaosSolution;
			} else {
				Optional<int[]> filled = SolutionFiller.fill(partition, random);
				if (filled.isEmpty()) {
					continue;
				}
				solution = filled.orElseThrow();
			}
			
			// Sparser grids force harder techniques, so the hole count is the dial the target band is found on and
			// the rating is the signal: too easy means dig more, too hard means dig less. The dial is only a
			// monotone signal if every budget describes the *same* dig taken to a different depth, which is why the
			// visit order is drawn once here and the whole walk is dug once: every budget below is a prefix of that
			// one trace, so it costs an array slice rather than a fresh dig and a fresh uniqueness proof per step.
			// A fresh solution each attempt is what gives the search a second chance at a band this solution cannot
			// reach at any depth.
			int[] order = random.shuffledRange(key.size().cellCount());
			int[] trace = HoleDigger.digTrace(partition, solution, order, ceiling);
			
			int fewest = 0;
			int most = trace.length;
			for (int step = 0; step < MAX_BUDGET_STEPS && fewest <= most; step++) {
				int holes = fewest + (most - fewest) / 2;
				int[] givens = HoleDigger.withHoles(solution, trace, holes);
				Puzzle puzzle = Puzzle.ofGivens(key.size(), key.variant(), partition, givens);
				
				// Rating only up to the target is enough to steer the search and never constructs the strategies
				// above it; an empty result means "harder than the target" and nothing more.
				Optional<Difficulty> rated = RATER.rateUpTo(puzzle, target);
				if (rated.isEmpty()) {
					if (holes < shallowestAboveHoles) {
						shallowestAboveHoles = holes;
						shallowestAbove = new GeneratedPuzzle(key, puzzle, solution);
					}
					most = holes - 1;
					continue;
				}
				
				Difficulty band = rated.orElseThrow();
				if (band == target) {
					return new GeneratedPuzzle(key, puzzle, solution);
				}
				
				int distance = target.index() - band.index();
				if (distance < closestBelowDistance) {
					closestBelowDistance = distance;
					closestBelow = new GeneratedPuzzle(key, puzzle, solution);
				}
				fewest = holes + 1;
			}
		}
		
		GeneratedPuzzle fallback = chooseFallback(closestBelow, closestBelowDistance, shallowestAbove, target);
		if (fallback == null) {
			throw new IllegalStateException("Failed to generate any puzzle for " + key + " within " + MAX_ATTEMPTS + " attempts");
		}
		return fallback;
	}
	
	/**
	 * Picks the candidate to return when no attempt landed in the target band.
	 * <p>
	 *     The two sides of the search are not symmetric: a candidate rated below the target has a known distance,
	 *     while one that overshot only proved "harder than the target", since the capped rating stops there. So the
	 *     over-shooting candidate is re-rated once, itself capped two bands above the target, which is enough to tell
	 *     a near miss from a wild one without paying for a full rating of a hard puzzle. Anything still above that
	 *     cap is treated as three bands out, which loses to any nearer candidate below.
	 * </p>
	 */
	private static GeneratedPuzzle chooseFallback(GeneratedPuzzle closestBelow, int closestBelowDistance, GeneratedPuzzle shallowestAbove, Difficulty target) {
		if (shallowestAbove == null) {
			return closestBelow;
		}
		
		int probe = Math.min(target.index() + 2, Difficulty.LISA.index());
		int aboveDistance = RATER.rateUpTo(shallowestAbove.puzzle(), Difficulty.ofIndex(probe))
			.map(band -> band.index() - target.index())
			.orElse(3);
		return aboveDistance < closestBelowDistance ? shallowestAbove : closestBelow;
	}
	
	/**
	 * Returns the region layout a key's puzzle is built on, without generating the puzzle.
	 * <p>
	 *     A classic key always maps to the cached box layout of its size. A chaos key grows its jigsaw from the key's
	 *     own random stream, and because that grow happens <i>first</i> in {@link #generate(PuzzleKey)}, before any
	 *     filling, digging or rating, reproducing it here costs a small fraction of a full generation. That is what
	 *     lets a client that was handed a finished set of givens rebuild the board they belong to.
	 * </p>
	 *
	 * @param key The key to build the layout for
	 * @return The region layout
	 * @throws NullPointerException If the key is null
	 */
	public static RegionPartition partitionFor(PuzzleKey key) {
		Objects.requireNonNull(key, "Key must not be null");
		if (key.variant() != Variant.CHAOS) {
			return ClassicRegionPartition.of(key.size());
		}
		return RegionGenerator.generateChaosLayout(key.size(), KeyDerivation.randomFor(key)).partition();
	}
	
	/**
	 * Rebuilds a puzzle from givens that were generated elsewhere, deriving the layout from the key and the solution
	 * from the givens.
	 * <p>
	 *     This is the receiving half of shipping a grid over the wire. The digits carry no proof of anything, so they
	 *     are not taken on trust: the puzzle is solved and required to have exactly one solution, which is the same
	 *     property {@link HoleDigger} guarantees for a puzzle this side generated itself. Solving is a plain
	 *     backtracking search over a mostly filled grid and costs milliseconds, so nothing is gained by sending the
	 *     answer alongside — and a second field that can disagree with the first is a bug waiting to happen.
	 * </p>
	 * <p>
	 *     The rated band is <b>not</b> re-derived. The key states which band was asked for and the sender rated what
	 *     it actually produced; re-rating here would cost more than the decode it is attached to and could only
	 *     disagree with a puzzle that is already in the player's hands.
	 * </p>
	 *
	 * @param key The key the givens belong to, which supplies the size, the variant and the chaos layout
	 * @param givens One entry per cell in index order, {@code 0} for an empty cell
	 * @return The rebuilt puzzle together with its derived solution
	 * @throws NullPointerException If the key or the givens are null
	 * @throws IllegalArgumentException If the givens do not match the key's size, contain an illegal digit, or do not
	 * 		describe a uniquely solvable puzzle
	 */
	public static GeneratedPuzzle fromGivens(PuzzleKey key, int[] givens) {
		Objects.requireNonNull(key, "Key must not be null");
		Objects.requireNonNull(givens, "Givens must not be null");
		if (givens.length != key.size().cellCount()) {
			throw new IllegalArgumentException("Givens must hold " + key.size().cellCount() + " cells, but held " + givens.length);
		}
		
		Puzzle puzzle = Puzzle.ofGivens(key.size(), key.variant(), partitionFor(key), givens);
		if (BacktrackingSolver.countSolutions(puzzle, 2) != 1) {
			throw new IllegalArgumentException("Givens for " + key + " do not describe a uniquely solvable puzzle");
		}
		
		int[] solution = BacktrackingSolver.solve(puzzle).orElseThrow(() -> new IllegalArgumentException("Givens for " + key + " are not solvable"));
		return new GeneratedPuzzle(key, puzzle, solution);
	}
	
	/**
	 * Returns the band the generator should aim for given a key's requested difficulty.
	 * <p>
	 *     The request is clamped to the size's ceiling, because a small grid cannot reach a genuinely hard band
	 *     (spec §4.3). {@link Difficulty#LISA} needs no special case: it is a rating of its own now — the puzzle must
	 *     genuinely force a level-15 technique — and clamps like any other band on a size that cannot reach it.
	 * </p>
	 *
	 * @param key The key being generated for
	 * @return The target band
	 */
	private static Difficulty targetBandFor(PuzzleKey key) {
		return RATER.bands().nearestSupported(key.size(), key.difficulty());
	}
}
