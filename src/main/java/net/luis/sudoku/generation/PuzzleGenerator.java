package net.luis.sudoku.generation;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.difficulty.DifficultyBands;
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
	private static final DifficultyBands BANDS = DifficultyBands.defaults();
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
	 * <p>
	 *     Raised again from 48 to 192 for issue 2.3.0/3, and this is the change that stops a player being handed a
	 *     puzzle harder than the tier they picked. At 48 the bound was where the misses came from rather than a
	 *     property of the bands: measured over the daily key sequence, tier 9 landed on 54 days of 60 and missed
	 *     <i>high</i> on the other six (four at band 10, two at band 11), never once low, because an over-shooting
	 *     candidate wins {@link #chooseFallback}'s comparison whenever it is the nearer one. At 192 every one of those
	 *     days becomes an exact hit, and a full bench at 9x9 classic goes from 340 of 360 to <b>360 of 360</b>, every
	 *     band 24 of 24 - band 15 alone was 16 of 24. The bands were reachable all along.
	 * </p>
	 * <p>
	 *     The average costs almost nothing, because only a search that is <i>failing</i> ever reaches past the old
	 *     bound: 27 to 30 ms at band 9, 139 to 163 ms at band 15. The worst case is what grows, roughly doubling, and
	 *     it is paid by the server's pool warm-up rather than by a player waiting for a board.
	 * </p>
	 * <p>
	 *     It is the bound at <b>every</b> size, 16x16 included, and that was measured rather than assumed: cutting
	 *     16x16 back to the old 48 to save its far more expensive attempts dropped it from 60 of 60 to 51 of 60 and
	 *     brought the over-shoots straight back - a band-9 request rated 11, a band-8 rated 10 - for about a third
	 *     off the time. Accuracy is what the raise is for, so 16x16 pays for it; what it does not pay for is the
	 *     offer rules, which are the part that costs there (see {@link #offerBudgetFor(GridSize, Variant)}).
	 * </p>
	 */
	public static final int MAX_ATTEMPTS = 192;
	
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
	
	/**
	 * How many further attempts the offer rules are worth once a puzzle in the target band has been found.
	 * <p>
	 *     The rules of {@link DifficultyBands#assessOffer} - the work floor and ceiling and the bottleneck - are a
	 *     <b>preference, not a requirement</b>, and this is what keeps them one. It was introduced for the ceiling
	 *     alone: held for the whole of {@link #MAX_ATTEMPTS} it turned a search that used to finish in two or three attempts into one that ran the
	 *     bound out. 9x9 chaos is where that showed: a jigsaw layout is grown once and every attempt re-digs
	 *     <i>that</i> solution, so the range of puzzles one seed can reach is narrow, and a seed whose layout has no
	 *     light band-15 puzzle in it will not find one however long it looks. Measured, the average generation of a
	 *     band-15 chaos puzzle went from 229 ms to 1511 ms, and that cost is paid on a phone by a player waiting for
	 *     an offline board.
	 * </p>
	 * <p>
	 *     So the rules get a small budget, counted from the first candidate that lands in the band rather than from
	 *     the start of the search: the attempts spent <i>finding</i> the band are not attempts spent on its shape.
	 *     Inside the budget only a puzzle that meets every rule is accepted; at the end of it the candidate nearest to
	 *     meeting them is handed over. The band is never given up on - the attempts past this budget are exactly the
	 *     ones that made every band land - only the shape is.
	 * </p>
	 */
	private static final int OFFER_BUDGET = 16;
	
	/**
	 * The {@link #OFFER_BUDGET} of a jigsaw grid and of any 12x12 grid, where an attempt is far dearer.
	 * <p>
	 *     Every chaos attempt re-digs the one grown solution and re-proves uniqueness on a jigsaw layout, so each
	 *     further attempt costs a large share of a whole classic generation. Measured at 12 seeds a band, bands 5 to
	 *     12, a budget of 8 cost 500 to 1700 ms per board against 3's 140 to 1600 ms and returned the same puzzles
	 *     within noise. Chaos openings are short by nature, so the rules have less to fix there in the first place.
	 *     A 12x12 attempt is dearer for the plain reason that it has 144 cells, and the rules are calibrated at 9x9
	 *     and only side-scaled there, so they are met less often and a full budget is run out more often.
	 * </p>
	 */
	private static final int SMALL_OFFER_BUDGET = 3;
	
	/**
	 * How many attempts the offer rules are worth at the given size and variant.
	 * <p>
	 *     The rules are a comfort preference, so what it may spend is a fraction of a second of extra searching -
	 *     and at 16x16 one further attempt is measured in seconds, not in milliseconds, because it fills a solution
	 *     whose cost is heavy-tailed with no ceiling at all ({@link SolutionFiller} bounds it into restarts rather
	 *     than into a hang). It buys none there, and the first candidate in the band is taken exactly as it always
	 *     was. The ceilings are calibrated at 9x9 and merely side-scaled above it in any case, so 16x16 is also
	 *     where they are least entitled to spend anything.
	 * </p>
	 *
	 * @param size The grid size
	 * @param variant The region layout variant
	 * @return The offer budget, {@code 0} where the rules are not worth an attempt at all
	 */
	private static int offerBudgetFor(GridSize size, Variant variant) {
		if (size.n() == 16) {
			return 0;
		}
		return variant == Variant.CHAOS || size.n() == 12 ? SMALL_OFFER_BUDGET : OFFER_BUDGET;
	}
	
	/**
	 * How many bands of slack the easier side of a missed target is given when the two are compared.
	 * <p>
	 *     A miss is a choice between a puzzle below the requested band and one above it, and the two are not equally
	 *     good answers: being handed something harder than was asked for is the failure a player notices, while
	 *     something easier is merely a quiet one. One band of bias is enough to settle every tie and every near-tie
	 *     in favour of the easier puzzle without ever reaching for a wildly easier one to avoid a marginally harder.
	 * </p>
	 */
	private static final int OVERSHOOT_PENALTY = 1;
	
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
		int offerBudget = offerBudgetFor(key.size(), key.variant());
		GeneratedPuzzle closestBelow = null;
		int closestBelowDistance = Integer.MAX_VALUE;
		// The best candidate in the target band that breaks one of the offer rules. Kept because it is still the band
		// that was asked for, which every off-band candidate is not; the one nearest to meeting the rules wins.
		GeneratedPuzzle bestInBand = null;
		int bestInBandPenalty = Integer.MAX_VALUE;
		// The attempt the band was first reached on, which is where OFFER_BUDGET starts counting.
		int bandFoundOn = -1;
		// Held as its parts rather than as a GeneratedPuzzle, because the one thing a GeneratedPuzzle must carry is
		// the band it rated and that is exactly what this candidate does not know yet: rateUpTo stopped at the target
		// and reported only "harder than that". chooseFallback re-rates it if it ends up being the one returned.
		Puzzle shallowestAbove = null;
		int[] shallowestAboveSolution = null;
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
				Optional<DifficultyRater.Rating> rated = RATER.rateUpTo(puzzle, target);
				if (rated.isEmpty()) {
					if (holes < shallowestAboveHoles) {
						shallowestAboveHoles = holes;
						shallowestAbove = puzzle;
						shallowestAboveSolution = solution;
					}
					most = holes - 1;
					continue;
				}
				
				Difficulty band = rated.orElseThrow().band();
				if (band == target) {
					// Issue 2.3.0/3: the right band is no longer the whole test. A band names the hardest technique
					// a puzzle forces and says nothing about how much of that work there is or where on the path it
					// sits: one band spans a several-fold range of work, and a puzzle can be singles for half the
					// board around a single hard step. A candidate that breaks an offer rule steers the search and is
					// kept, since it is the tier that was asked for, which no off-band candidate is.
					DifficultyBands.OfferAssessment offer = BANDS.assessOffer(key.size(), key.variant(), band, holes, rated.orElseThrow().report());
					if (offer.acceptable()) {
						return new GeneratedPuzzle(key, puzzle, solution, band);
					}
					if (offer.penalty() < bestInBandPenalty) {
						bestInBandPenalty = offer.penalty();
						bestInBand = new GeneratedPuzzle(key, puzzle, solution, band);
					}
					if (bandFoundOn < 0) {
						bandFoundOn = attempt;
					}
					// Past its budget the rules stop being worth more attempts, and the nearest candidate seen so far -
					// which is this one or an earlier one - is the answer (see OFFER_BUDGET).
					if (attempt - bandFoundOn >= offerBudget) {
						return bestInBand;
					}
					// Too much work means a sparser dig went too far. Everything else - too little work, a long
					// singles opening, too few hard steps - is a puzzle that gives in too easily, so dig deeper.
					if (offer.tooHeavy()) {
						most = holes - 1;
					} else {
						fewest = holes + 1;
					}
					continue;
				}
				
				int distance = target.index() - band.index();
				if (distance < closestBelowDistance) {
					closestBelowDistance = distance;
					closestBelow = new GeneratedPuzzle(key, puzzle, solution, band);
				}
				fewest = holes + 1;
			}
		}
		
		GeneratedPuzzle fallback = bestInBand != null
			? bestInBand
			: chooseFallback(key, closestBelow, closestBelowDistance, shallowestAbove, shallowestAboveSolution, target);
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
	 * <p>
	 *     The probe is also where the over-shooting candidate finally learns its own band, which is why it is carried
	 *     here as a bare puzzle rather than as a {@link GeneratedPuzzle}. When even the probe cannot name it, the
	 *     puzzle is rated once without a cap: that is the expensive path this method exists to avoid, but it is only
	 *     reached when the over-shoot both wins the comparison and lies more than two bands above the target, and a
	 *     puzzle handed out under a band nobody measured is worse than the rating it costs.
	 * </p>
	 * <p>
	 *     The two sides are not weighed evenly, and must not be (issue 2.3.0/3): the over-shoot carries
	 *     {@link #OVERSHOOT_PENALTY}, so an equally distant - or one band nearer - puzzle above the target loses to
	 *     the one below it. That is the rule {@link net.luis.sudoku.difficulty.DifficultyBands#nearestSupported}
	 *     already states for the same question one level up, that a player asking for a band they cannot have is
	 *     better served by an easier puzzle than a harder one, applied here where it was not.
	 * </p>
	 */
	private static GeneratedPuzzle chooseFallback(
		PuzzleKey key, GeneratedPuzzle closestBelow, int closestBelowDistance, Puzzle shallowestAbove, int[] shallowestAboveSolution, Difficulty target
	) {
		if (shallowestAbove == null) {
			return closestBelow;
		}
		
		int probe = Math.min(target.index() + 2, Difficulty.LISA.index());
		Optional<DifficultyRater.Rating> probed = RATER.rateUpTo(shallowestAbove, Difficulty.ofIndex(probe));
		int aboveDistance = probed.map(rating -> rating.band().index() - target.index()).orElse(3) + OVERSHOOT_PENALTY;
		if (aboveDistance >= closestBelowDistance) {
			return closestBelow;
		}
		return new GeneratedPuzzle(key, shallowestAbove, shallowestAboveSolution, probed.map(DifficultyRater.Rating::band).orElseGet(() -> RATER.rate(shallowestAbove)));
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
	 *     The rated band is <b>not</b> re-derived, which is why the sender has to state it. Re-rating here would cost
	 *     far more than the decode it is attached to and could only disagree with a puzzle that is already in the
	 *     player's hands. The overload without it falls back to the key's requested band, which is right only where
	 *     the two cannot differ.
	 * </p>
	 *
	 * @param key The key the givens belong to, which supplies the size, the variant and the chaos layout
	 * @param givens One entry per cell in index order, {@code 0} for an empty cell
	 * @param rated The band the sender rated this grid at, which is what {@link GeneratedPuzzle#rated()} reports
	 * @return The rebuilt puzzle together with its derived solution
	 * @throws NullPointerException If the key, the givens or the rated band are null
	 * @throws IllegalArgumentException If the givens do not match the key's size, contain an illegal digit, or do not
	 * 		describe a uniquely solvable puzzle
	 */
	public static GeneratedPuzzle fromGivens(PuzzleKey key, int[] givens, Difficulty rated) {
		Objects.requireNonNull(key, "Key must not be null");
		Objects.requireNonNull(givens, "Givens must not be null");
		Objects.requireNonNull(rated, "Rated band must not be null");
		if (givens.length != key.size().cellCount()) {
			throw new IllegalArgumentException("Givens must hold " + key.size().cellCount() + " cells, but held " + givens.length);
		}
		
		Puzzle puzzle = Puzzle.ofGivens(key.size(), key.variant(), partitionFor(key), givens);
		if (BacktrackingSolver.countSolutions(puzzle, 2) != 1) {
			throw new IllegalArgumentException("Givens for " + key + " do not describe a uniquely solvable puzzle");
		}
		
		int[] solution = BacktrackingSolver.solve(puzzle).orElseThrow(() -> new IllegalArgumentException("Givens for " + key + " are not solvable"));
		return new GeneratedPuzzle(key, puzzle, solution, rated);
	}
	
	/**
	 * Rebuilds a puzzle from givens whose rated band was never recorded, taking the key's requested band as the
	 * rating.
	 * <p>
	 *     That assumption is safe only where the sender could not have produced anything else: a saved game the same
	 *     build generated, or a share code, both of which name a band the generator hit. Where the sender <i>does</i>
	 *     know what its search settled on — a pooled row, a match snapshot — pass it, because a generator that missed
	 *     its target hands back a grid the key's band does not describe.
	 * </p>
	 *
	 * @param key The key the givens belong to
	 * @param givens One entry per cell in index order, {@code 0} for an empty cell
	 * @return The rebuilt puzzle, rated at the key's requested band
	 */
	public static GeneratedPuzzle fromGivens(PuzzleKey key, int[] givens) {
		Objects.requireNonNull(key, "Key must not be null");
		return fromGivens(key, givens, key.difficulty());
	}
	
	/**
	 * Returns the band the generator should aim for given a key's requested difficulty.
	 * <p>
	 *     The request is snapped to what the key's size <i>and variant</i> can reach, because neither a small grid nor
	 *     a 16x16 jigsaw can produce a genuinely hard band (spec §4.3). {@link Difficulty#LISA} needs no special case:
	 *     it is a rating of its own now — the puzzle must genuinely force a level-15 technique — and snaps like any
	 *     other band on a grid that cannot reach it.
	 * </p>
	 *
	 * @param key The key being generated for
	 * @return The target band
	 */
	private static Difficulty targetBandFor(PuzzleKey key) {
		return RATER.bands().nearestSupported(key.size(), key.variant(), key.difficulty());
	}
}
