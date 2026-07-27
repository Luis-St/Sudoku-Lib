package net.luis.sudoku.key;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.grid.Variant;
import net.luis.sudoku.version.GenVersion;

import java.util.Objects;

/**
 * The complete, self-contained description of a puzzle: everything the generator needs and nothing else.
 * <p>
 *     A key is the only input to generation. The same key must yield the same puzzle on every JVM, on the
 *     server and on Android alike, which is why the key is a value type with no mutable state and no
 *     identity-dependent members.
 * </p>
 * <p>
 *     The key is never handed to the random generator as-is. {@link KeyDerivation#deriveState(PuzzleKey)}
 *     folds all five components through SHA-256 first, so the seed only ever reaches the generator diffused
 *     with the rest of the key. That is what makes the same seed at two difficulties produce two entirely
 *     <i>unrelated</i> puzzles instead of two puzzles that share a random stream prefix.
 * </p>
 * <p>
 *     {@link #genVersion()} is part of the key rather than an external build constant, because the generator,
 *     the region builder, the hole-digging order and the difficulty rater all change over time. Saved games
 *     and share codes persist the whole key, so a game resumed after the generator changed reproduces its
 *     original puzzle instead of silently turning into a different one.
 * </p>
 *
 * @param genVersion The generation pipeline version this puzzle was, or is to be, produced with
 * @param size The grid size
 * @param variant The region layout variant
 * @param difficulty The target difficulty band
 * @param seed The user-visible seed
 *
 * @see KeyDerivation
 * @see GenVersion
 */
public record PuzzleKey(int genVersion, GridSize size, Variant variant, Difficulty difficulty, long seed) {
	
	/**
	 * Constructs a puzzle key.
	 *
	 * @throws NullPointerException If the size, the variant or the difficulty is null
	 * @throws IllegalArgumentException If the generator version is less than {@code 1}, or if the variant is
	 *         not supported at the given grid size
	 */
	public PuzzleKey {
		Objects.requireNonNull(size, "Size must not be null");
		Objects.requireNonNull(variant, "Variant must not be null");
		Objects.requireNonNull(difficulty, "Difficulty must not be null");
		if (genVersion < 1) {
			throw new IllegalArgumentException("Generator version " + genVersion + " must be at least 1");
		}
		
		variant.checkSupportedAt(size);
	}
	
	/**
	 * Creates a puzzle key stamped with the generator version of this build.
	 * <p>
	 *     This is the factory every new game goes through. Keys read back from a saved game or a share code
	 *     must use the canonical constructor instead, so that they keep the generator version they were
	 *     created with.
	 * </p>
	 *
	 * @param size The grid size
	 * @param variant The region layout variant
	 * @param difficulty The target difficulty band
	 * @param seed The user-visible seed
	 * @return A new key carrying {@link GenVersion#CURRENT}
	 * @throws NullPointerException If the size, the variant or the difficulty is null
	 * @throws IllegalArgumentException If the variant is not supported at the given grid size
	 */
	public static PuzzleKey of(GridSize size, Variant variant, Difficulty difficulty, long seed) {
		return new PuzzleKey(GenVersion.CURRENT, size, variant, difficulty, seed);
	}
	
	/**
	 * Checks whether this key was stamped with the generator version of this build.
	 * <p>
	 *     A false result does not make the key invalid, it only means that regenerating from it requires the
	 *     historical pipeline. It is the signal a saved game uses to warn that the puzzle predates the current
	 *     generator, and the signal the server uses to refuse a mismatched client at connect time.
	 * </p>
	 *
	 * @return True if {@link #genVersion()} equals {@link GenVersion#CURRENT}, false otherwise
	 */
	public boolean isCurrentGenVersion() {
		return this.genVersion == GenVersion.CURRENT;
	}
}
