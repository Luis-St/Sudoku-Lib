package net.luis.sudoku.sharecode;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.grid.Variant;
import net.luis.sudoku.key.PuzzleKey;
import net.luis.sudoku.version.GenVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShareCodeCodecTest {
	
	/** Independent RFC 4648 Base32 (no padding) used only to craft the malformed-payload fixture. */
	private static String base32(byte[] bytes) {
		char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
		StringBuilder result = new StringBuilder();
		int buffer = 0;
		int bits = 0;
		for (byte value : bytes) {
			buffer = (buffer << 8) | (value & 0xFF);
			bits += 8;
			while (bits >= 5) {
				bits -= 5;
				result.append(alphabet[(buffer >>> bits) & 0x1F]);
			}
		}
		if (bits > 0) {
			result.append(alphabet[(buffer << (5 - bits)) & 0x1F]);
		}
		return result.toString();
	}
	
	@Test
	void encode_isTwentyFourUppercaseBase32Characters() {
		String code = ShareCodeCodec.encode(PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.THREE, 12345L));
		
		assertEquals(ShareCodeCodec.CODE_LENGTH, code.length());
		assertTrue(code.chars().allMatch(c -> "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".indexOf(c) >= 0),
			"Code has a non-Base32 character: " + code);
	}
	
	@Test
	void encode_pinnedGoldenStrings() {
		// Computed independently (Python base64.b32encode of the 15-byte wire form), not by running the codec.
		assertAll(
			() -> assertEquals("AAAAAAIJAABQAAAAAAAAAAAA",
				ShareCodeCodec.encode(new PuzzleKey(1, GridSize.NINE, Variant.CLASSIC, Difficulty.THREE, 0L))),
			() -> assertEquals("AAAAAAQQAEH7777777777777",
				ShareCodeCodec.encode(new PuzzleKey(2, GridSize.SIXTEEN, Variant.CHAOS, Difficulty.LISA, -1L)))
		);
	}
	
	@Test
	void decode_ofEncode_roundTripsEveryFieldCombination() {
		long[] seeds = { 0L, 1L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, 0x0123_4567_89AB_CDEFL };
		for (GridSize size : GridSize.values()) {
			for (Variant variant : Variant.values()) {
				if (!variant.isSupportedAt(size)) {
					continue;
				}
				for (Difficulty difficulty : Difficulty.values()) {
					for (long seed : seeds) {
						PuzzleKey key = PuzzleKey.of(size, variant, difficulty, seed);
						
						assertEquals(key, ShareCodeCodec.decode(ShareCodeCodec.encode(key)),
							"Round trip failed for " + key);
					}
				}
			}
		}
	}
	
	@Test
	void decode_ofEncode_roundTripsExtremeGenVersion() {
		PuzzleKey key = new PuzzleKey(Integer.MAX_VALUE, GridSize.NINE, Variant.CLASSIC, Difficulty.ONE, Long.MAX_VALUE);
		
		assertEquals(key, ShareCodeCodec.decode(ShareCodeCodec.encode(key)));
	}
	
	@Test
	void decode_isCaseInsensitive() {
		PuzzleKey key = PuzzleKey.of(GridSize.SIX, Variant.CHAOS, Difficulty.TWO, 99L);
		String code = ShareCodeCodec.encode(key);
		
		assertEquals(key, ShareCodeCodec.decode(code.toLowerCase()));
	}
	
	@Test
	void encode_currentGenVersionKey_decodesToTheSameKey() {
		PuzzleKey key = PuzzleKey.of(GridSize.TWELVE, Variant.CLASSIC, Difficulty.FIVE, 7L);
		
		PuzzleKey decoded = ShareCodeCodec.decode(ShareCodeCodec.encode(key));
		
		assertEquals(GenVersion.CURRENT, decoded.genVersion());
		assertTrue(decoded.isCurrentGenVersion());
	}
	
	@Test
	void decode_wrongLength_throws() {
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> ShareCodeCodec.decode("TOOSHORT")),
			() -> assertThrows(IllegalArgumentException.class, () -> ShareCodeCodec.decode("AAAAAAIJAABQAAAAAAAAAAAAA"))
		);
	}
	
	@Test
	void decode_illegalCharacter_throws() {
		// '1', '8', '0' and '9' are not in the RFC 4648 Base32 alphabet.
		assertThrows(IllegalArgumentException.class, () -> ShareCodeCodec.decode("1AAAAAIJAABQAAAAAAAAAAAA"));
	}
	
	@Test
	void decode_payloadWithUnknownSize_throws() {
		// A valid-length code whose size byte is not a supported edge length must be rejected by key validation.
		// Byte layout: 4 genVersion, 1 size(n), 1 variant, 1 difficulty, 8 seed. Craft n = 7 (unsupported).
		byte[] bytes = { 0, 0, 0, 1, 7, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 };
		String code = base32(bytes);
		
		assertThrows(IllegalArgumentException.class, () -> ShareCodeCodec.decode(code));
	}
	
	@Test
	void encode_nullKey_throws() {
		assertThrows(NullPointerException.class, () -> ShareCodeCodec.encode(null));
	}
	
	@Test
	void decode_nullCode_throws() {
		assertThrows(NullPointerException.class, () -> ShareCodeCodec.decode(null));
	}
}
