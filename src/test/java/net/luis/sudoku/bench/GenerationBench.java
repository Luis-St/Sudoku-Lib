package net.luis.sudoku.bench;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.difficulty.DifficultyRater;
import net.luis.sudoku.generation.GeneratedPuzzle;
import net.luis.sudoku.generation.PuzzleGenerator;
import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.grid.Variant;
import net.luis.sudoku.key.PuzzleKey;
import net.luis.sudoku.solver.*;

import java.util.*;

/**
 * Measures what the generator actually produces: how often a requested band is hit, how long it takes, and which
 * technique ends up being the hardest one.
 * <p>
 *     This is a measurement tool, not a test, so it lives outside the JUnit suite and is run on demand:
 *     {@code ./gradlew bench -Pseeds=32 -Psize=NINE -Pvariant=CLASSIC}. It exists because every remaining decision
 *     about the difficulty model — where the band boundaries go, what the path-score weights are, whether level 15
 *     is reachable at all — has to be made on measured numbers, and the 8-seed figures the rework started from are
 *     too few to carry that weight.
 * </p>
 * <p>
 *     Two columns matter beyond the hit rate. <b>Rated</b> is what the player would actually get, which is not the
 *     request whenever the generator falls back to its closest candidate. <b>Hardest technique</b> is the histogram
 *     the max-metric ratings come out of, and it is the direct evidence for whether a band is empty because the
 *     generator cannot reach it or because the metric can never assign it.
 * </p>
 */
public final class GenerationBench {
	
	private static final DifficultyRater RATER = new DifficultyRater();
	
	/** Path scores of every generated puzzle, keyed by the level its hardest technique gave it. */
	private static final Map<Integer, List<Integer>> SCORES_BY_LEVEL = new TreeMap<>();
	
	private GenerationBench() {}
	
	public static void main(String[] args) {
		int seeds = intProperty("seeds", 16);
		GridSize size = GridSize.valueOf(property("size", "NINE"));
		Variant variant = Variant.valueOf(property("variant", "CLASSIC"));
		
		System.out.printf("Generation bench: %d seeds, %s, %s, genVersion %d%n", seeds, size, variant, PuzzleKey.of(size, variant, Difficulty.ONE, 0L).genVersion());
		System.out.println();
		System.out.println("band | hit | avg ms | worst ms |  rated bands (count)  | hardest techniques");
		System.out.println("-----+-----+--------+----------+-----------------------+-------------------");
		
		for (Difficulty target : Difficulty.values()) {
			if (!variant.isSupportedAt(size)) {
				continue;
			}
			report(target, measure(target, size, variant, seeds));
		}
		
		System.out.println();
		System.out.println("Path score by hardest-technique level (the calibration input for the hybrid banding rule)");
		System.out.println("level |   n | min | p25 | med | p75 |  max");
		System.out.println("------+-----+-----+-----+-----+-----+-----");
		SCORES_BY_LEVEL.forEach((level, scores) -> {
			List<Integer> sorted = scores.stream().sorted().toList();
			System.out.printf("%5d | %3d | %3d | %3d | %3d | %3d | %4d%n", level, sorted.size(),
				sorted.get(0), percentile(sorted, 25), percentile(sorted, 50), percentile(sorted, 75), sorted.get(sorted.size() - 1));
		});
	}
	
	private static int percentile(List<Integer> sorted, int percent) {
		return sorted.get(Math.min(sorted.size() - 1, sorted.size() * percent / 100));
	}
	
	private static BandResult measure(Difficulty requested, GridSize size, Variant variant, int seeds) {
		// A size need not support every band, and a request outside its set is snapped to the nearest one it does
		// support. Scoring against the raw request would then report a deliberate, correct substitution as a miss.
		Difficulty target = RATER.bands().nearestSupported(size, requested);
		BandResult result = new BandResult();
		result.substituted = target != requested;
		for (long seed = 0; seed < seeds; seed++) {
			PuzzleKey key = PuzzleKey.of(size, variant, requested, seed);
			
			long start = System.nanoTime();
			GeneratedPuzzle generated = PuzzleGenerator.generate(key);
			result.record(System.nanoTime() - start);
			
			// Uncapped, because the bench wants the real rating of what was returned, not the search's verdict.
			TechniqueReport report = TechniqueSolver.solve(generated.puzzle());
			Difficulty rated = RATER.rate(size, report);
			result.rated.merge(rated, 1, Integer::sum);
			if (rated == target) {
				result.hits++;
			}
			report.hardestTechnique().ifPresent(technique -> result.techniques.merge(technique, 1, Integer::sum));
			if (report.stuck()) {
				result.stuck++;
			}
			int hardestLevel = report.hardestTechnique().map(Technique::level).orElse(1);
			SCORES_BY_LEVEL.computeIfAbsent(hardestLevel, level -> new ArrayList<>()).add(report.pathScore());
		}
		return result;
	}
	
	private static void report(Difficulty target, BandResult result) {
		System.out.printf("%4d | %d/%d | %6.0f | %8.0f | %-21s | %s%n",
			target.index(), result.hits, result.samples, result.averageMillis(), result.worstMillis(),
			histogram(result.rated, band -> String.valueOf(band.index())),
			histogram(result.techniques, Enum::name) + (result.stuck > 0 ? " [stuck x" + result.stuck + "]" : "") + (result.substituted ? " [unsupported, snapped]" : ""));
	}
	
	private static <K> String histogram(Map<K, Integer> counts, java.util.function.Function<K, String> naming) {
		StringJoiner joiner = new StringJoiner(", ");
		counts.entrySet().stream()
			.sorted(Map.Entry.<K, Integer>comparingByValue().reversed())
			.forEach(entry -> joiner.add(naming.apply(entry.getKey()) + " x" + entry.getValue()));
		return joiner.toString();
	}
	
	private static String property(String name, String fallback) {
		String value = System.getProperty(name);
		return value == null || value.isBlank() ? fallback : value;
	}
	
	private static int intProperty(String name, int fallback) {
		return Integer.parseInt(property(name, String.valueOf(fallback)));
	}
	
	private static final class BandResult {
		
		private final Map<Difficulty, Integer> rated = new EnumMap<>(Difficulty.class);
		private final Map<Technique, Integer> techniques = new EnumMap<>(Technique.class);
		private boolean substituted;
		private int samples;
		private int hits;
		private int stuck;
		private long totalNanos;
		private long worstNanos;
		
		private void record(long nanos) {
			this.samples++;
			this.totalNanos += nanos;
			this.worstNanos = Math.max(this.worstNanos, nanos);
		}
		
		private double averageMillis() {
			return this.samples == 0 ? 0 : this.totalNanos / (double) this.samples / 1_000_000;
		}
		
		private double worstMillis() {
			return this.worstNanos / 1_000_000.0;
		}
	}
}
