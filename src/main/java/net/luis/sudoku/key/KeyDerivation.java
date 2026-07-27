package net.luis.sudoku.key;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.grid.Variant;
import net.luis.sudoku.rng.DeterministicRandom;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Turns a {@link PuzzleKey} into the 64-bit random state the generator is seeded with.
 * <p>
 *     The derivation is {@code fold64(sha256(encode(key)))}. The key is deliberately not passed to the random
 *     generator as a raw seed: folding every component through a cryptographic hash decorrelates the result,
 *     so the same seed at two difficulties, or at two grid sizes, produces two entirely <i>unrelated</i>
 *     puzzles rather than two puzzles drawn from overlapping random streams. Because the generator version is
 *     one of the hashed components, a saved game keeps reproducing its original puzzle after the generator
 *     changes and gets a new version stamp.
 * </p>
 * <p>
 *     {@link #encode(PuzzleKey)} is a wire format. The server, the Android client and the share-code codec all
 *     decode and re-derive from these exact bytes, so the layout below is frozen: changing it silently would
 *     change every puzzle in existence and break every share code. It is fixed-width and big-endian:
 * </p>
 * <table border="1">
 *     <caption>The 15-byte puzzle key encoding</caption>
 *     <tr><th>Offset</th><th>Length</th><th>Content</th></tr>
 *     <tr><td>0</td><td>4</td><td>{@link PuzzleKey#genVersion()} as a big-endian {@code int}</td></tr>
 *     <tr><td>4</td><td>1</td><td>{@link GridSize#n()}, one of {@code 4, 6, 9, 12, 16}</td></tr>
 *     <tr><td>5</td><td>1</td><td>{@link Variant#ordinal()}, {@code CLASSIC = 0}, {@code CHAOS = 1}</td></tr>
 *     <tr><td>6</td><td>1</td><td>{@link Difficulty#index()}, {@code 1..6}</td></tr>
 *     <tr><td>7</td><td>8</td><td>{@link PuzzleKey#seed()} as a big-endian {@code long}</td></tr>
 * </table>
 * <p>
 *     The size and the difficulty are written as their stable semantic values rather than as enum ordinals, so
 *     that reordering or inserting an enum constant cannot change the puzzle a key produces. The fixed widths
 *     are the other half of that guarantee: they make the concatenation unambiguous, so
 *     {@code (genVersion = 1, n = 9)} can never collide with {@code (genVersion = 19)} the way a textual join
 *     would.
 * </p>
 *
 * @see PuzzleKey
 * @see DeterministicRandom
 */
public final class KeyDerivation {
	
	private static final String DIGEST_ALGORITHM = "SHA-256";
	/**
	 * The total width of an encoded puzzle key in bytes.
	 */
	public static final int ENCODED_LENGTH = 15;
	
	private KeyDerivation() {}
	
	/**
	 * Hashes the given bytes with SHA-256.
	 * <p>
	 *     Every conforming Java platform is required to provide {@code SHA-256}, so its absence is a broken
	 *     runtime rather than a condition a caller could handle. The checked {@link NoSuchAlgorithmException}
	 *     is therefore wrapped rather than declared.
	 * </p>
	 *
	 * @param input The bytes to hash
	 * @return The 32-byte digest
	 * @throws NullPointerException If the input is null
	 * @throws IllegalStateException If the runtime does not provide SHA-256
	 */
	public static byte[] sha256(byte[] input) {
		Objects.requireNonNull(input, "Input must not be null");
		
		try {
			return MessageDigest.getInstance(DIGEST_ALGORITHM).digest(input);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("The runtime does not provide the mandatory " + DIGEST_ALGORITHM + " digest", e);
		}
	}
	
	/**
	 * XOR-folds a digest down to 64 bits.
	 * <p>
	 *     The digest is read as a sequence of big-endian eight-byte longs, which are XOR-ed together. A 32-byte
	 *     SHA-256 digest therefore folds four longs into one. The method is kept general rather than tied to
	 *     SHA-256 because the server reuses it for its own daily-seed derivation.
	 * </p>
	 *
	 * @param digest The digest to fold
	 * @return The folded 64-bit value
	 * @throws NullPointerException If the digest is null
	 * @throws IllegalArgumentException If the digest is empty or its length is not a multiple of {@code 8}
	 */
	public static long fold64(byte[] digest) {
		Objects.requireNonNull(digest, "Digest must not be null");
		if (digest.length == 0) {
			throw new IllegalArgumentException("Digest must not be empty");
		}
		if (digest.length % Long.BYTES != 0) {
			throw new IllegalArgumentException("Digest length " + digest.length + " is not a multiple of " + Long.BYTES);
		}
		
		ByteBuffer buffer = ByteBuffer.wrap(digest);
		long folded = 0;
		while (buffer.hasRemaining()) {
			folded ^= buffer.getLong();
		}
		return folded;
	}
	
	/**
	 * Encodes the given key into its frozen 15-byte wire representation.
	 * <p>
	 *     The layout is documented in the class Javadoc and must never drift: it is shared with the server, the
	 *     Android client and the share-code codec. The method is public precisely so it can be golden-tested.
	 * </p>
	 *
	 * @param key The key to encode
	 * @return A new {@value #ENCODED_LENGTH}-byte array
	 * @throws NullPointerException If the key is null
	 */
	public static byte[] encode(PuzzleKey key) {
		Objects.requireNonNull(key, "Key must not be null");
		return ByteBuffer.allocate(ENCODED_LENGTH)
			.putInt(key.genVersion())
			.put((byte) key.size().n())
			.put((byte) key.variant().ordinal())
			.put((byte) key.difficulty().index())
			.putLong(key.seed())
			.array();
	}
	
	/**
	 * Decodes a key from its frozen 15-byte wire representation, the exact inverse of {@link #encode(PuzzleKey)}.
	 * <p>
	 *     Every field is validated as it is read back — an unknown grid edge length, variant ordinal or difficulty
	 *     index is rejected — and the reconstructed key runs through {@link PuzzleKey}'s own constructor checks, so a
	 *     tampered or truncated payload can never yield an invalid key. This is what the share-code codec decodes into.
	 * </p>
	 *
	 * @param encoded The {@value #ENCODED_LENGTH}-byte representation
	 * @return The decoded key
	 * @throws NullPointerException If the array is null
	 * @throws IllegalArgumentException If the length is wrong or any field is out of range
	 */
	public static PuzzleKey decode(byte[] encoded) {
		Objects.requireNonNull(encoded, "Encoded key must not be null");
		if (encoded.length != ENCODED_LENGTH) {
			throw new IllegalArgumentException("Encoded key must be " + ENCODED_LENGTH + " bytes, but was " + encoded.length);
		}
		
		ByteBuffer buffer = ByteBuffer.wrap(encoded);
		int genVersion = buffer.getInt();
		int edgeLength = buffer.get() & 0xFF;
		int variantOrdinal = buffer.get() & 0xFF;
		int difficultyIndex = buffer.get() & 0xFF;
		long seed = buffer.getLong();
		GridSize size = GridSize.ofEdgeLength(edgeLength);
		Variant[] variants = Variant.values();
		if (variantOrdinal >= variants.length) {
			throw new IllegalArgumentException("Unknown variant ordinal " + variantOrdinal);
		}
		return new PuzzleKey(genVersion, size, variants[variantOrdinal], Difficulty.ofIndex(difficultyIndex), seed);
	}
	
	/**
	 * Derives the 64-bit random state the generator is seeded with for the given key.
	 * <p>
	 *     This is {@code fold64(sha256(encode(key)))}. It is a pure function of the key, so two equal keys
	 *     always derive the same state, on any JVM and in any process.
	 * </p>
	 *
	 * @param key The key to derive from
	 * @return The derived random state
	 * @throws NullPointerException If the key is null
	 */
	public static long deriveState(PuzzleKey key) {
		return fold64(sha256(encode(key)));
	}
	
	/**
	 * Creates the deterministic random generator belonging to the given key.
	 *
	 * @param key The key to derive from
	 * @return A generator seeded with {@link #deriveState(PuzzleKey)}
	 * @throws NullPointerException If the key is null
	 */
	public static DeterministicRandom randomFor(PuzzleKey key) {
		return new DeterministicRandom(deriveState(key));
	}
}
