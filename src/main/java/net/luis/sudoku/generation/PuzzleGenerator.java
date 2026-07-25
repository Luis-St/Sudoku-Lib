package net.luis.sudoku.generation;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.difficulty.DifficultyRater;
import net.luis.sudoku.grid.*;
import net.luis.sudoku.key.KeyDerivation;
import net.luis.sudoku.key.PuzzleKey;
import net.luis.sudoku.rng.DeterministicRandom;

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
	 */
	public static final int MAX_ATTEMPTS = 16;
	
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
		if (variant == Variant.CHAOS) {
			// Proving a sparse jigsaw grid uniquely solvable is far more expensive than a classic one, so chaos digs
			// to about half the grid — still a challenging puzzle given the irregular regions, but bounded and fast.
			return size.cellCount() / 2;
		}
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
		GeneratedPuzzle closest = null;
		int closestDistance = Integer.MAX_VALUE;
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
			int[] givens = HoleDigger.dig(partition, solution, random, maxHoles);
			Puzzle puzzle = Puzzle.ofGivens(key.size(), key.variant(), partition, givens);
			Difficulty rated = RATER.rate(puzzle);
			if (rated == target) {
				return new GeneratedPuzzle(key, puzzle, solution);
			}
			int distance = Math.abs(rated.index() - target.index());
			if (distance < closestDistance) {
				closestDistance = distance;
				closest = new GeneratedPuzzle(key, puzzle, solution);
			}
		}
		if (closest == null) {
			throw new IllegalStateException("Failed to generate any puzzle for " + key + " within " + MAX_ATTEMPTS + " attempts");
		}
		return closest;
	}
	
	/**
	 * Returns the numbered band the generator should aim for given a key's requested difficulty.
	 * <p>
	 *     A {@link Difficulty#LISA} request targets the size's hardest band, since Lisa is that band plus a runtime
	 *     modifier set rather than a distinct rating (spec §4.3). A numbered request is clamped to the size's ceiling,
	 *     because a small grid cannot reach a genuinely hard band. The rater only ever returns numbered bands, so the
	 *     target is always a numbered band too.
	 * </p>
	 *
	 * @param key The key being generated for
	 * @return The numbered target band
	 */
	private static Difficulty targetBandFor(PuzzleKey key) {
		Difficulty ceiling = RATER.bands().ceiling(key.size());
		if (key.difficulty().isLisa() || key.difficulty().index() > ceiling.index()) {
			return ceiling;
		}
		return key.difficulty();
	}
}
