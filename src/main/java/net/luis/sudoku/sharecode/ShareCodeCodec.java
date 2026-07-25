package net.luis.sudoku.sharecode;

import net.luis.sudoku.key.KeyDerivation;
import net.luis.sudoku.key.PuzzleKey;

import java.util.Objects;

/**
 * Encodes a {@link PuzzleKey} to and from a short Base32 share code, so anyone can regenerate the identical puzzle
 * from the code, fully offline (spec §3.6).
 * <p>
 *     The key is packed into its frozen 15-byte wire form by {@link KeyDerivation#encode(PuzzleKey)} and then
 *     rendered in Base32 (RFC 4648, alphabet {@code A-Z2-7}, uppercase, <b>no padding</b>). Fifteen bytes are exactly
 *     120 bits, which is exactly 24 Base32 characters, so a share code is always 24 characters with no padding at all.
 *     Decoding is the strict inverse: the 24 characters are read back to 15 bytes and {@link KeyDerivation#decode(byte[])}
 *     reconstructs and revalidates the key.
 * </p>
 * <p>
 *     The wire format is load-bearing: it is pinned by golden tests, and any change to it — or to the generator it
 *     addresses — must bump {@link net.luis.sudoku.version.GenVersion}, since an old code must always reproduce the
 *     puzzle it was created for.
 * </p>
 */
public final class ShareCodeCodec {
	
	private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
	private static final int[] REVERSE = buildReverse();
	
	/**
	 * The exact length of every share code, in characters.
	 */
	public static final int CODE_LENGTH = 24;
	
	private ShareCodeCodec() {}
	
	private static int[] buildReverse() {
		int[] reverse = new int[128];
		java.util.Arrays.fill(reverse, -1);
		for (int index = 0; index < ALPHABET.length; index++) {
			reverse[ALPHABET[index]] = index;
		}
		return reverse;
	}
	
	/**
	 * Encodes the given key into its {@value #CODE_LENGTH}-character Base32 share code.
	 *
	 * @param key The key to encode
	 * @return The uppercase, unpadded Base32 share code
	 * @throws NullPointerException If the key is null
	 */
	public static String encode(PuzzleKey key) {
		Objects.requireNonNull(key, "Key must not be null");
		byte[] bytes = KeyDerivation.encode(key);
		StringBuilder code = new StringBuilder(CODE_LENGTH);
		int buffer = 0;
		int bits = 0;
		for (byte value : bytes) {
			buffer = (buffer << 8) | (value & 0xFF);
			bits += 8;
			while (bits >= 5) {
				bits -= 5;
				code.append(ALPHABET[(buffer >>> bits) & 0x1F]);
			}
		}
		if (bits > 0) {
			code.append(ALPHABET[(buffer << (5 - bits)) & 0x1F]);
		}
		return code.toString();
	}
	
	/**
	 * Decodes a share code back into its key.
	 *
	 * @param code The {@value #CODE_LENGTH}-character Base32 share code, case-insensitive
	 * @return The decoded key
	 * @throws NullPointerException If the code is null
	 * @throws IllegalArgumentException If the code has the wrong length, contains a non-Base32 character, or decodes
	 *         to an invalid key
	 */
	public static PuzzleKey decode(String code) {
		Objects.requireNonNull(code, "Code must not be null");
		if (code.length() != CODE_LENGTH) {
			throw new IllegalArgumentException("Share code must be " + CODE_LENGTH + " characters, but was " + code.length());
		}
		byte[] bytes = new byte[KeyDerivation.ENCODED_LENGTH];
		int buffer = 0;
		int bits = 0;
		int byteIndex = 0;
		for (int index = 0; index < code.length(); index++) {
			char symbol = Character.toUpperCase(code.charAt(index));
			int value = symbol < REVERSE.length ? REVERSE[symbol] : -1;
			if (value < 0) {
				throw new IllegalArgumentException("Illegal share-code character '" + code.charAt(index) + "' at position " + index);
			}
			buffer = (buffer << 5) | value;
			bits += 5;
			if (bits >= 8) {
				bits -= 8;
				bytes[byteIndex++] = (byte) ((buffer >>> bits) & 0xFF);
			}
		}
		return KeyDerivation.decode(bytes);
	}
}
