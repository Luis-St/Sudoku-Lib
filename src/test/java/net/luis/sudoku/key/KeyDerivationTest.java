package net.luis.sudoku.key;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.grid.Variant;
import net.luis.sudoku.rng.DeterministicRandom;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link KeyDerivation}.
 * <p>
 *     The golden expectations in this class are computed independently of the production code: the encoded
 *     bytes are written out by hand, the digest is taken with a longhand {@link MessageDigest} call and the
 *     XOR fold is done with explicit shifts. Nothing here is a copy of what {@link KeyDerivation} returned.
 * </p>
 */
class KeyDerivationTest {
	
	private static final long[] SWEEP_SEEDS = { 0L, 1L, -1L, Long.MIN_VALUE, Long.MAX_VALUE };
	
	/**
	 * The SHA-256 digest of the empty input, from FIPS 180-4.
	 */
	private static final String EMPTY_DIGEST_HEX = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
	
	/**
	 * The hand-written encoding of {@code (genVersion = 1, NINE, CLASSIC, THREE, seed = 0)}.
	 */
	private static final byte[] GOLDEN_ENCODING_ONE = {
		0x00, 0x00, 0x00, 0x01,
		0x09,
		0x00,
		0x03,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
	};
	
	/**
	 * The hand-written encoding of {@code (genVersion = 2, SIXTEEN, CHAOS, LISA, seed = -1)}.
	 */
	private static final byte[] GOLDEN_ENCODING_TWO = {
		0x00, 0x00, 0x00, 0x02,
		0x10,
		0x01,
		0x06,
		(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
	};
	
	private static final PuzzleKey GOLDEN_KEY_ONE = new PuzzleKey(1, GridSize.NINE, Variant.CLASSIC, Difficulty.THREE, 0L);
	private static final PuzzleKey GOLDEN_KEY_TWO = new PuzzleKey(2, GridSize.SIXTEEN, Variant.CHAOS, Difficulty.LISA, -1L);
	
	private static byte[] independentSha256(byte[] input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			digest.update(input);
			return digest.digest();
		} catch (NoSuchAlgorithmException e) {
			throw new AssertionError(e);
		}
	}
	
	private static long independentFold64(byte[] digest) {
		long folded = 0;
		for (int offset = 0; offset < digest.length; offset += 8) {
			long chunk = 0;
			for (int i = 0; i < 8; i++) {
				chunk = (chunk << 8) | (digest[offset + i] & 0xFFL);
			}
			folded ^= chunk;
		}
		return folded;
	}
	
	private static String toHex(byte[] bytes) {
		StringBuilder builder = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			builder.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
		}
		return builder.toString();
	}
	
	private static List<PuzzleKey> sweepKeys() {
		List<PuzzleKey> keys = new ArrayList<>();
		for (int genVersion = 1; genVersion <= 3; genVersion++) {
			for (GridSize size : GridSize.values()) {
				for (Variant variant : Variant.values()) {
					if (!variant.isSupportedAt(size)) {
						continue;
					}
					for (Difficulty difficulty : Difficulty.values()) {
						for (long seed : SWEEP_SEEDS) {
							keys.add(new PuzzleKey(genVersion, size, variant, difficulty, seed));
						}
					}
				}
			}
		}
		return keys;
	}
	
	@Test
	void sha256_emptyInput_matchesTheFipsVector() {
		assertEquals(EMPTY_DIGEST_HEX, toHex(KeyDerivation.sha256(new byte[0])));
	}
	
	@Test
	void sha256_arbitraryInput_matchesAnIndependentDigest() {
		byte[] input = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
		assertAll(
			() -> assertEquals(32, KeyDerivation.sha256(input).length),
			() -> assertArrayEquals(independentSha256(input), KeyDerivation.sha256(input))
		);
	}
	
	@Test
	void sha256_nullInput_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> KeyDerivation.sha256(null));
	}
	
	@Test
	void fold64_twoHandComputedLongs_returnsTheirXor() {
		byte[] digest = {
			0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
			0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17
		};
		assertEquals(0x111311171113111FL, KeyDerivation.fold64(digest));
	}
	
	@Test
	void fold64_singleLong_returnsThatLong() {
		byte[] digest = { (byte) 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01 };
		assertEquals(Long.MIN_VALUE + 1L, KeyDerivation.fold64(digest));
	}
	
	@Test
	void fold64_allZeroDigest_returnsZero() {
		assertAll(
			() -> assertEquals(0L, KeyDerivation.fold64(new byte[8])),
			() -> assertEquals(0L, KeyDerivation.fold64(new byte[32]))
		);
	}
	
	@Test
	void fold64_fourHandComputedLongs_returnsTheirXor() {
		byte[] digest = new byte[32];
		digest[7] = 0x01;
		digest[15] = 0x02;
		digest[23] = 0x04;
		digest[31] = 0x08;
		assertEquals(0x0FL, KeyDerivation.fold64(digest));
	}
	
	@Test
	void fold64_realDigest_matchesAnIndependentFold() {
		byte[] digest = independentSha256("sudoku".getBytes(StandardCharsets.UTF_8));
		assertAll(
			() -> assertEquals(32, digest.length),
			() -> assertEquals(independentFold64(digest), KeyDerivation.fold64(digest))
		);
	}
	
	@Test
	void fold64_emptyDigest_throwsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> KeyDerivation.fold64(new byte[0]));
	}
	
	@Test
	void fold64_lengthNotAMultipleOfEight_throwsIllegalArgumentException() {
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> KeyDerivation.fold64(new byte[1])),
			() -> assertThrows(IllegalArgumentException.class, () -> KeyDerivation.fold64(new byte[7])),
			() -> assertThrows(IllegalArgumentException.class, () -> KeyDerivation.fold64(new byte[15])),
			() -> assertThrows(IllegalArgumentException.class, () -> KeyDerivation.fold64(new byte[31]))
		);
	}
	
	@Test
	void fold64_nullDigest_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> KeyDerivation.fold64(null));
	}
	
	@Test
	void encode_goldenKeys_matchTheHandWrittenByteLayout() {
		assertAll(
			() -> assertArrayEquals(GOLDEN_ENCODING_ONE, KeyDerivation.encode(GOLDEN_KEY_ONE)),
			() -> assertArrayEquals(GOLDEN_ENCODING_TWO, KeyDerivation.encode(GOLDEN_KEY_TWO))
		);
	}
	
	@Test
	void encode_anyKey_isFifteenBytesLong() {
		assertAll(
			() -> assertEquals(15, KeyDerivation.ENCODED_LENGTH),
			() -> assertEquals(15, KeyDerivation.encode(GOLDEN_KEY_ONE).length),
			() -> assertEquals(15, KeyDerivation.encode(GOLDEN_KEY_TWO).length)
		);
	}
	
	@Test
	void encode_everySize_writesTheEdgeLengthNotTheOrdinal() {
		assertAll(
			() -> assertEquals(4, KeyDerivation.encode(new PuzzleKey(1, GridSize.FOUR, Variant.CLASSIC, Difficulty.ONE, 0L))[4]),
			() -> assertEquals(6, KeyDerivation.encode(new PuzzleKey(1, GridSize.SIX, Variant.CLASSIC, Difficulty.ONE, 0L))[4]),
			() -> assertEquals(9, KeyDerivation.encode(new PuzzleKey(1, GridSize.NINE, Variant.CLASSIC, Difficulty.ONE, 0L))[4]),
			() -> assertEquals(12, KeyDerivation.encode(new PuzzleKey(1, GridSize.TWELVE, Variant.CLASSIC, Difficulty.ONE, 0L))[4]),
			() -> assertEquals(16, KeyDerivation.encode(new PuzzleKey(1, GridSize.SIXTEEN, Variant.CLASSIC, Difficulty.ONE, 0L))[4])
		);
	}
	
	@Test
	void encode_everyDifficulty_writesTheIndexNotTheOrdinal() {
		for (Difficulty difficulty : Difficulty.values()) {
			assertEquals(difficulty.index(), KeyDerivation.encode(new PuzzleKey(1, GridSize.NINE, Variant.CLASSIC, difficulty, 0L))[6], "Difficulty " + difficulty);
		}
	}
	
	@Test
	void encode_bothVariants_writeTheOrdinal() {
		assertAll(
			() -> assertEquals(0, KeyDerivation.encode(new PuzzleKey(1, GridSize.NINE, Variant.CLASSIC, Difficulty.ONE, 0L))[5]),
			() -> assertEquals(1, KeyDerivation.encode(new PuzzleKey(1, GridSize.NINE, Variant.CHAOS, Difficulty.ONE, 0L))[5])
		);
	}
	
	@Test
	void encode_maximumComponents_writeBigEndianWords() {
		byte[] encoded = KeyDerivation.encode(new PuzzleKey(Integer.MAX_VALUE, GridSize.NINE, Variant.CLASSIC, Difficulty.ONE, Long.MAX_VALUE));
		assertEquals("7fffffff0900017fffffffffffffff", toHex(encoded));
	}
	
	@Test
	void encode_returnsAFreshArrayEachCall() {
		byte[] first = KeyDerivation.encode(GOLDEN_KEY_ONE);
		byte[] second = KeyDerivation.encode(GOLDEN_KEY_ONE);
		first[0] = 0x7F;
		assertAll(
			() -> assertNotSame(first, second),
			() -> assertArrayEquals(GOLDEN_ENCODING_ONE, second)
		);
	}
	
	@Test
	void encode_nullKey_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> KeyDerivation.encode(null));
	}
	
	@Test
	void deriveState_goldenKeys_matchAnIndependentlyComputedLong() {
		assertAll(
			() -> assertEquals(independentFold64(independentSha256(GOLDEN_ENCODING_ONE)), KeyDerivation.deriveState(GOLDEN_KEY_ONE)),
			() -> assertEquals(independentFold64(independentSha256(GOLDEN_ENCODING_TWO)), KeyDerivation.deriveState(GOLDEN_KEY_TWO))
		);
	}
	
	@Test
	void deriveState_goldenKeys_matchThePinnedRegressionValues() {
		assertAll(
			() -> assertEquals(255335976165330420L, KeyDerivation.deriveState(GOLDEN_KEY_ONE)),
			() -> assertEquals(1674858060028078552L, KeyDerivation.deriveState(GOLDEN_KEY_TWO))
		);
	}
	
	@Test
	void deriveState_sameKeyTwice_returnsTheSameValue() {
		PuzzleKey key = new PuzzleKey(2, GridSize.TWELVE, Variant.CHAOS, Difficulty.FIVE, 1234567890L);
		assertEquals(KeyDerivation.deriveState(key), KeyDerivation.deriveState(key));
	}
	
	@Test
	void deriveState_separatelyConstructedEqualKeys_returnTheSameValue() {
		PuzzleKey first = new PuzzleKey(2, GridSize.TWELVE, Variant.CHAOS, Difficulty.FIVE, 1234567890L);
		PuzzleKey second = new PuzzleKey(2, GridSize.TWELVE, Variant.CHAOS, Difficulty.FIVE, 1234567890L);
		assertAll(
			() -> assertEquals(first, second),
			() -> assertEquals(KeyDerivation.deriveState(first), KeyDerivation.deriveState(second))
		);
	}
	
	@Test
	void deriveState_eachComponentChangedIndependently_changesTheState() {
		PuzzleKey base = new PuzzleKey(2, GridSize.NINE, Variant.CLASSIC, Difficulty.THREE, 42L);
		long state = KeyDerivation.deriveState(base);
		assertAll(
			() -> assertNotEquals(state, KeyDerivation.deriveState(new PuzzleKey(3, GridSize.NINE, Variant.CLASSIC, Difficulty.THREE, 42L))),
			() -> assertNotEquals(state, KeyDerivation.deriveState(new PuzzleKey(2, GridSize.SIXTEEN, Variant.CLASSIC, Difficulty.THREE, 42L))),
			() -> assertNotEquals(state, KeyDerivation.deriveState(new PuzzleKey(2, GridSize.NINE, Variant.CHAOS, Difficulty.THREE, 42L))),
			() -> assertNotEquals(state, KeyDerivation.deriveState(new PuzzleKey(2, GridSize.NINE, Variant.CLASSIC, Difficulty.LISA, 42L))),
			() -> assertNotEquals(state, KeyDerivation.deriveState(new PuzzleKey(2, GridSize.NINE, Variant.CLASSIC, Difficulty.THREE, 43L)))
		);
	}
	
	@Test
	void deriveState_exhaustiveSweepOfEveryValidKey_producesNoCollision() {
		List<PuzzleKey> keys = sweepKeys();
		Map<Long, PuzzleKey> seen = new TreeMap<>();
		for (PuzzleKey key : keys) {
			long state = KeyDerivation.deriveState(key);
			PuzzleKey previous = seen.put(state, key);
			if (previous != null) {
				fail("Derived state " + state + " collides between " + previous + " and " + key);
			}
		}
		assertAll(
			() -> assertEquals(3 * 9 * 6 * SWEEP_SEEDS.length, keys.size()),
			() -> assertEquals(keys.size(), seen.size())
		);
	}
	
	@Test
	void deriveState_nullKey_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> KeyDerivation.deriveState(null));
	}
	
	@Test
	void randomFor_validKey_isSeededWithTheDerivedState() {
		PuzzleKey key = new PuzzleKey(1, GridSize.NINE, Variant.CLASSIC, Difficulty.THREE, 0L);
		DeterministicRandom random = KeyDerivation.randomFor(key);
		assertAll(
			() -> assertEquals(KeyDerivation.deriveState(key), random.seed()),
			() -> assertEquals(255335976165330420L, random.seed())
		);
	}
	
	@Test
	void randomFor_equalKeys_produceTheSameSequence() {
		PuzzleKey first = new PuzzleKey(1, GridSize.SIXTEEN, Variant.CHAOS, Difficulty.LISA, -7L);
		PuzzleKey second = new PuzzleKey(1, GridSize.SIXTEEN, Variant.CHAOS, Difficulty.LISA, -7L);
		DeterministicRandom left = KeyDerivation.randomFor(first);
		DeterministicRandom right = KeyDerivation.randomFor(second);
		long leftFirstDraw = left.nextLong();
		long rightFirstDraw = right.nextLong();
		int leftSecondDraw = left.nextInt(16);
		int rightSecondDraw = right.nextInt(16);
		assertAll(
			() -> assertEquals(left.seed(), right.seed()),
			() -> assertEquals(leftFirstDraw, rightFirstDraw),
			() -> assertEquals(leftSecondDraw, rightSecondDraw)
		);
	}
	
	@Test
	void randomFor_nullKey_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> KeyDerivation.randomFor(null));
	}
}
