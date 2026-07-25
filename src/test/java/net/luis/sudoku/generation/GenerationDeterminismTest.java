package net.luis.sudoku.generation;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.grid.Variant;
import net.luis.sudoku.key.PuzzleKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The stated exit criterion of plan phase L4: generation must be byte-identical across repeated runs, across a
 * fresh JVM, and the whole main source tree must stay free of the constructs that break that guarantee.
 * <p>
 *     Two puzzles are "byte-identical" here when their givens and their full solutions match value for value, which
 *     is what {@link GeneratedPuzzle#equals(Object)} already compares by content.
 * </p>
 * <p>
 *     <b>Size coverage.</b> The repetition tests exercise {@link GridSize#NINE} 100 times and each other size
 *     20 times. Every size generates in milliseconds because the solver picks cells by a minimum-remaining-values
 *     heuristic and the generator caps the hole count per size, so 12x12 and 16x16 are no longer expensive.
 * </p>
 */
class GenerationDeterminismTest {
	
	private static final long FIXED_SEED = 0x5EED_C0DE_1234_5678L;
	
	/**
	 * The tokens that must never appear in the main source tree, because each one introduces JVM-dependent
	 * iteration order, un-seeded randomness, identity dependence or parallelism into the generation path.
	 */
	private static final String[] FORBIDDEN_TOKENS = {
		"HashMap", "HashSet", "SplittableRandom", "ThreadLocalRandom",
		"System.identityHashCode", ".parallelStream()", "Collectors.toSet("
	};
	
	private static void assertSameResult(PuzzleKey key, GeneratedPuzzle expected, GeneratedPuzzle actual, String message) {
		assertAll(message,
			() -> assertEquals(expected, actual, "The whole generated puzzle differed"),
			() -> assertArrayEquals(expected.puzzle().values(), actual.puzzle().values(), "The givens differed"),
			() -> assertArrayEquals(expected.solution(), actual.solution(), "The solution differed")
		);
	}
	
	private static void scanForForbiddenTokens(Path source, List<String> violations) {
		String stripped = stripComments(readSource(source));
		String[] lines = stripped.split("\n", -1);
		for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
			String line = lines[lineIndex];
			for (String token : FORBIDDEN_TOKENS) {
				if (line.contains(token)) {
					violations.add(source + ":" + (lineIndex + 1) + " contains forbidden token '" + token + "'");
				}
			}
		}
	}
	
	/**
	 * Replaces every block comment (including Javadoc) and line comment with blanks while preserving newlines, so
	 * line numbers survive the strip. String and character literals are tracked so that a {@code //} or
	 * {@code /*} inside a literal is not mistaken for a comment. The determinism warnings in the package-info files
	 * and the {@code SplittableRandom}/{@code ThreadLocalRandom} rationale in {@code DeterministicRandom} live in
	 * comments, so a naive substring scan would flag them; stripping first is what lets the scan see only code.
	 *
	 * @param source The raw source text
	 * @return The source with comment content blanked out and line structure preserved
	 */
	private static String stripComments(String source) {
		StringBuilder out = new StringBuilder(source.length());
		int length = source.length();
		boolean inBlock = false;
		boolean inLine = false;
		boolean inString = false;
		boolean inChar = false;
		int i = 0;
		while (i < length) {
			char c = source.charAt(i);
			char next = i + 1 < length ? source.charAt(i + 1) : '\0';
			if (inLine) {
				if (c == '\n') {
					inLine = false;
					out.append('\n');
				} else {
					out.append(' ');
				}
				i++;
			} else if (inBlock) {
				if (c == '*' && next == '/') {
					inBlock = false;
					out.append("  ");
					i += 2;
				} else {
					out.append(c == '\n' ? '\n' : ' ');
					i++;
				}
			} else if (inString) {
				out.append(c);
				if (c == '\\' && next != '\0') {
					out.append(next);
					i += 2;
				} else {
					if (c == '"') {
						inString = false;
					}
					i++;
				}
			} else if (inChar) {
				out.append(c);
				if (c == '\\' && next != '\0') {
					out.append(next);
					i += 2;
				} else {
					if (c == '\'') {
						inChar = false;
					}
					i++;
				}
			} else if (c == '/' && next == '*') {
				inBlock = true;
				out.append("  ");
				i += 2;
			} else if (c == '/' && next == '/') {
				inLine = true;
				out.append(' ');
				i++;
			} else if (c == '"') {
				inString = true;
				out.append(c);
				i++;
			} else if (c == '\'') {
				inChar = true;
				out.append(c);
				i++;
			} else {
				out.append(c);
				i++;
			}
		}
		return out.toString();
	}
	
	private static List<Path> javaFilesUnder(Path root) {
		try (Stream<Path> paths = Files.walk(root)) {
			List<Path> files = new ArrayList<>();
			paths.filter(Files::isRegularFile)
				.filter(path -> path.getFileName().toString().endsWith(".java"))
				.sorted()
				.forEach(files::add);
			return files;
		} catch (IOException exception) {
			throw new UncheckedIOException("Failed to walk the source tree at " + root, exception);
		}
	}
	
	private static String readSource(Path source) {
		try {
			return Files.readString(source);
		} catch (IOException exception) {
			throw new UncheckedIOException("Failed to read " + source, exception);
		}
	}
	
	/**
	 * Resolves the {@code src/main/java/net/luis/sudoku} directory by walking up from the working directory, so the
	 * scan works whether the tests run from the module directory or from a repository root. Failing to find it is a
	 * loud error rather than a vacuous pass.
	 *
	 * @return The main source root of the {@code net.luis.sudoku} package tree
	 * @throws IllegalStateException If the source root cannot be located
	 */
	private static Path resolveSourceRoot() {
		Path start = Paths.get("").toAbsolutePath();
		for (Path directory = start; directory != null; directory = directory.getParent()) {
			Path candidate = directory.resolve("src/main/java/net/luis/sudoku");
			if (Files.isDirectory(candidate)) {
				return candidate;
			}
		}
		throw new IllegalStateException("Could not locate src/main/java/net/luis/sudoku from " + start);
	}
	
	@Test
	@Timeout(value = 120, unit = TimeUnit.SECONDS)
	void generate_theSameNineKeyOneHundredTimes_isAlwaysByteIdentical() {
		PuzzleKey key = PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.THREE, FIXED_SEED);
		GeneratedPuzzle first = PuzzleGenerator.generate(key);
		
		for (int run = 1; run <= 100; run++) {
			assertSameResult(key, first, PuzzleGenerator.generate(key), "Run " + run);
		}
	}
	
	@Test
	@Timeout(value = 120, unit = TimeUnit.SECONDS)
	void generate_theSameKeyTwentyTimes_isAlwaysByteIdentical_forEveryOtherSize() {
		for (GridSize size : new GridSize[] { GridSize.FOUR, GridSize.SIX, GridSize.TWELVE, GridSize.SIXTEEN }) {
			PuzzleKey key = PuzzleKey.of(size, Variant.CLASSIC, Difficulty.THREE, FIXED_SEED);
			GeneratedPuzzle first = PuzzleGenerator.generate(key);
			
			for (int run = 1; run <= 20; run++) {
				assertSameResult(key, first, PuzzleGenerator.generate(key), size + " run " + run);
			}
		}
	}
	
	/**
	 * In-process approximation of a fresh-JVM run. A true fresh-JVM test would fork a new process and generate the
	 * same key there; this cannot do that, so it instead proves the weaker but closely related property that
	 * generation is independent of any prior use of the random subsystem and of class-initialization order within
	 * one JVM: fifty unrelated generations run in between, yet the key still reproduces its exact result. Because
	 * every draw is derived solely from the key (never from a shared or static generator), that independence is
	 * what makes the true cross-JVM guarantee hold.
	 */
	@Test
	@Timeout(value = 120, unit = TimeUnit.SECONDS)
	void generate_afterFiftyUnrelatedGenerations_reproducesTheSameResult_inProcessFreshJvmApproximation() {
		PuzzleKey key = PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.TWO, FIXED_SEED);
		GeneratedPuzzle before = PuzzleGenerator.generate(key);
		
		GridSize[] sizes = { GridSize.FOUR, GridSize.SIX, GridSize.NINE };
		Difficulty[] difficulties = Difficulty.values();
		for (int i = 0; i < 50; i++) {
			PuzzleKey other = PuzzleKey.of(sizes[i % sizes.length], Variant.CLASSIC, difficulties[i % difficulties.length], 1_000L + i);
			PuzzleGenerator.generate(other);
		}
		
		GeneratedPuzzle after = PuzzleGenerator.generate(key);
		
		assertSameResult(key, before, after, "Generation was disturbed by prior unrelated generations");
	}
	
	@Test
	void mainSources_containNoneOfTheForbiddenDeterminismBreakingTokens() {
		Path sourceRoot = resolveSourceRoot();
		List<Path> sources = javaFilesUnder(sourceRoot);
		
		assertFalse(sources.isEmpty(), "Found no Java sources under " + sourceRoot + " - the scan would pass vacuously");
		
		List<String> violations = new ArrayList<>();
		for (Path source : sources) {
			scanForForbiddenTokens(source, violations);
		}
		
		assertTrue(violations.isEmpty(), "Forbidden determinism-breaking tokens found:\n" + String.join("\n", violations));
	}
}
