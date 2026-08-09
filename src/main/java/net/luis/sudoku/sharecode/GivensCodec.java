package net.luis.sudoku.sharecode;

import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.grid.Puzzle;

import java.util.Objects;

/**
 * Encodes the givens of a puzzle to and from a compact Base64 string, so a server can ship a finished grid over the
 * wire instead of making every client regenerate it from the key.
 * <p>
 *     Regeneration from a {@link net.luis.sudoku.key.PuzzleKey} is deterministic but not cheap: the generator fills,
 *     digs and rates up to {@code MAX_ATTEMPTS} candidates, which costs up to a second on a desktop JVM at the harder
 *     bands and several times that on a phone. The givens themselves are tiny, so the server pays that cost once and
 *     every client decodes instead. The key still travels alongside, and it still decides the region layout of a
 *     chaos grid, so this codec carries nothing but the digits.
 * </p>
 * <p>
 *     <b>Layout.</b> One header byte holding the edge length, then the cells in index order packed at a fixed width of
 *     {@code bitsFor(size)} bits each, most significant bit first, zero padded to a byte boundary. Digit {@code 0}
 *     means an empty cell. Four bits carry the digits {@code 0..9} of a 9x9 grid, five the {@code 0..16} of a 16x16
 *     one, so a 9x9 puzzle is 42 bytes and a 16x16 one 161 bytes before Base64. The result is rendered in the URL and
 *     filename safe Base64 alphabet (RFC 4648 §5, {@code A-Z a-z 0-9 - _}) <b>without padding</b>, which makes it safe
 *     to drop into JSON, a query string or a path segment unescaped.
 * </p>
 * <p>
 *     Unlike {@link ShareCodeCodec} this format is <b>not</b> pinned to a {@link net.luis.sudoku.version.GenVersion}.
 *     It describes a grid that is already fixed rather than the recipe for building one, so an old string keeps
 *     decoding to the same digits no matter which generator produced them. That is exactly what makes it the durable
 *     way to persist a saved game.
 * </p>
 *
 * @see ShareCodeCodec
 * @see net.luis.sudoku.generation.PuzzleGenerator#fromGivens
 */
public final class GivensCodec {
	
	private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray();
	private static final int[] REVERSE = buildReverse();
	
	private GivensCodec() {}
	
	private static int[] buildReverse() {
		int[] reverse = new int[128];
		java.util.Arrays.fill(reverse, -1);
		
		for (int index = 0; index < ALPHABET.length; index++) {
			reverse[ALPHABET[index]] = index;
		}
		return reverse;
	}
	
	/**
	 * The number of bits one cell occupies at the given size.
	 * <p>
	 *     Wide enough to hold every digit {@code 1..n} plus the {@code 0} that marks an empty cell, so it is the bit
	 *     length of {@code n} itself.
	 * </p>
	 *
	 * @param size The size to measure
	 * @return The bits per cell, 3 at 4x4 and 6x6, 4 at 9x9 and 12x12, 5 at 16x16
	 * @throws NullPointerException If the size is null
	 */
	public static int bitsFor(GridSize size) {
		Objects.requireNonNull(size, "Size must not be null");
		return Integer.SIZE - Integer.numberOfLeadingZeros(size.n());
	}
	
	/**
	 * Encodes the givens of the given puzzle.
	 * <p>
	 *     Only the given cells are carried: a cell the player has since filled in is written as empty, so encoding a
	 *     half played board and encoding the puzzle it started from produce the same string.
	 * </p>
	 *
	 * @param puzzle The puzzle whose givens to encode
	 * @return The Base64 string
	 * @throws NullPointerException If the puzzle is null
	 */
	public static String encode(Puzzle puzzle) {
		Objects.requireNonNull(puzzle, "Puzzle must not be null");
		
		int[] givens = new int[puzzle.size().cellCount()];
		for (int index = 0; index < givens.length; index++) {
			givens[index] = puzzle.cell(index).isGiven() ? puzzle.valueAt(index) : 0;
		}
		return encode(puzzle.size(), givens);
	}
	
	/**
	 * Encodes the given digits.
	 *
	 * @param size The size the digits describe
	 * @param givens One entry per cell in index order, {@code 0} for an empty cell
	 * @return The Base64 string
	 * @throws NullPointerException If the size or the givens are null
	 * @throws IllegalArgumentException If the array length does not match the size, or an entry is outside {@code 0..n}
	 */
	public static String encode(GridSize size, int[] givens) {
		Objects.requireNonNull(size, "Size must not be null");
		Objects.requireNonNull(givens, "Givens must not be null");
		if (givens.length != size.cellCount()) {
			throw new IllegalArgumentException("Givens must hold " + size.cellCount() + " cells, but held " + givens.length);
		}
		
		int bits = bitsFor(size);
		byte[] bytes = new byte[1 + (givens.length * bits + 7) / 8];
		bytes[0] = (byte) size.n();
		
		int bitIndex = 8;
		for (int index = 0; index < givens.length; index++) {
			int digit = givens[index];
			if (digit < 0 || digit > size.n()) {
				throw new IllegalArgumentException("Digit at cell " + index + " must be between 0 and " + size.n() + ", but was " + digit);
			}
			
			for (int bit = bits - 1; bit >= 0; bit--) {
				if ((digit >>> bit & 1) != 0) {
					bytes[bitIndex >>> 3] |= (byte) (0x80 >>> (bitIndex & 7));
				}
				bitIndex++;
			}
		}
		return base64(bytes);
	}
	
	/**
	 * Decodes a string back into the digits it carries.
	 * <p>
	 *     The size travels in the string, so the caller does not have to know it up front; a caller that does should
	 *     still check the returned length against the size it expected, because a payload for the wrong size decodes
	 *     perfectly well on its own terms.
	 * </p>
	 *
	 * @param encoded The string produced by one of the {@code encode} methods
	 * @return One entry per cell in index order, {@code 0} for an empty cell
	 * @throws NullPointerException If the string is null
	 * @throws IllegalArgumentException If the string is empty, contains a character outside the alphabet, carries an
	 *         unknown edge length, has the wrong length for that edge length, or holds a digit above {@code n}
	 */
	public static int[] decode(String encoded) {
		Objects.requireNonNull(encoded, "Encoded givens must not be null");
		
		byte[] bytes = decodeBase64(encoded);
		if (bytes.length == 0) {
			throw new IllegalArgumentException("Encoded givens must not be empty");
		}
		
		GridSize size = GridSize.ofEdgeLength(bytes[0] & 0xFF);
		int bits = bitsFor(size);
		int expected = 1 + (size.cellCount() * bits + 7) / 8;
		if (bytes.length != expected) {
			throw new IllegalArgumentException("Encoded givens for a " + size.n() + "x" + size.n() + " grid must be " + expected + " bytes, but were " + bytes.length);
		}
		
		int[] givens = new int[size.cellCount()];
		int bitIndex = 8;
		for (int index = 0; index < givens.length; index++) {
			int digit = 0;
			for (int bit = 0; bit < bits; bit++) {
				digit = (digit << 1) | (bytes[bitIndex >>> 3] >>> (7 - (bitIndex & 7)) & 1);
				bitIndex++;
			}
			
			if (digit > size.n()) {
				throw new IllegalArgumentException("Digit at cell " + index + " must be between 0 and " + size.n() + ", but was " + digit);
			}
			givens[index] = digit;
		}
		return givens;
	}
	
	private static String base64(byte[] bytes) {
		StringBuilder encoded = new StringBuilder((bytes.length * 4 + 2) / 3);
		int buffer = 0;
		int bits = 0;
		for (byte value : bytes) {
			buffer = (buffer << 8) | (value & 0xFF);
			bits += 8;
			
			while (bits >= 6) {
				bits -= 6;
				encoded.append(ALPHABET[(buffer >>> bits) & 0x3F]);
			}
		}
		
		if (bits > 0) {
			encoded.append(ALPHABET[(buffer << (6 - bits)) & 0x3F]);
		}
		return encoded.toString();
	}
	
	private static byte[] decodeBase64(String encoded) {
		byte[] bytes = new byte[encoded.length() * 6 / 8];
		int buffer = 0;
		int bits = 0;
		int byteIndex = 0;
		for (int index = 0; index < encoded.length(); index++) {
			char symbol = encoded.charAt(index);
			int value = symbol < REVERSE.length ? REVERSE[symbol] : -1;
			if (value < 0) {
				throw new IllegalArgumentException("Illegal givens character '" + symbol + "' at position " + index);
			}
			
			buffer = (buffer << 6) | value;
			bits += 6;
			if (bits >= 8) {
				bits -= 8;
				bytes[byteIndex++] = (byte) ((buffer >>> bits) & 0xFF);
			}
		}
		return bytes;
	}
}
