package net.luis.sudoku.sharecode;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.generation.GeneratedPuzzle;
import net.luis.sudoku.generation.PuzzleGenerator;
import net.luis.sudoku.grid.*;
import net.luis.sudoku.key.PuzzleKey;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link GivensCodec}.
 */
class GivensCodecTest {
	
	private static final String URL_SAFE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
	
	/** Independent RFC 4648 §5 Base64 (no padding) used only to craft the malformed-payload fixtures. */
	private static String base64(byte[] bytes) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
	
	/** Every cell filled, digits cycling through {@code 1..n}. */
	private static int[] fullGrid(GridSize size) {
		int[] givens = new int[size.cellCount()];
		for (int index = 0; index < givens.length; index++) {
			givens[index] = index % size.n() + 1;
		}
		return givens;
	}
	
	/** No cell filled. */
	private static int[] emptyGrid(GridSize size) {
		return new int[size.cellCount()];
	}
	
	/** A mix of holes and digits that touches both bounds, {@code 0} and {@code n}, at every size. */
	private static int[] partialGrid(GridSize size) {
		int[] givens = new int[size.cellCount()];
		for (int index = 0; index < givens.length; index++) {
			givens[index] = (index * 7 + 3) % (size.n() + 1);
		}
		return givens;
	}
	
	@Test
	void bitsFor_everySize_isTheBitLengthOfTheEdgeLength() {
		assertAll(
			() -> assertEquals(3, GivensCodec.bitsFor(GridSize.FOUR)),
			() -> assertEquals(3, GivensCodec.bitsFor(GridSize.SIX)),
			() -> assertEquals(4, GivensCodec.bitsFor(GridSize.NINE)),
			() -> assertEquals(4, GivensCodec.bitsFor(GridSize.TWELVE)),
			() -> assertEquals(5, GivensCodec.bitsFor(GridSize.SIXTEEN))
		);
	}
	
	@Test
	void bitsFor_everySize_holdsEveryDigitAndTheEmptyMarker() {
		for (GridSize size : GridSize.values()) {
			assertTrue(size.n() < 1 << GivensCodec.bitsFor(size), "Digits do not fit for " + size);
		}
	}
	
	@Test
	void bitsFor_nullSize_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> GivensCodec.bitsFor(null));
	}
	
	@Test
	void decode_ofEncode_roundTripsAFullGridAtEverySize() {
		for (GridSize size : GridSize.values()) {
			int[] givens = fullGrid(size);
			
			assertArrayEquals(givens, GivensCodec.decode(GivensCodec.encode(size, givens)), "Round trip failed for " + size);
		}
	}
	
	@Test
	void decode_ofEncode_roundTripsAnEmptyGridAtEverySize() {
		for (GridSize size : GridSize.values()) {
			int[] givens = emptyGrid(size);
			
			assertArrayEquals(givens, GivensCodec.decode(GivensCodec.encode(size, givens)), "Round trip failed for " + size);
		}
	}
	
	@Test
	void decode_ofEncode_roundTripsAPartialGridAtEverySize() {
		for (GridSize size : GridSize.values()) {
			int[] givens = partialGrid(size);
			
			assertArrayEquals(givens, GivensCodec.decode(GivensCodec.encode(size, givens)), "Round trip failed for " + size);
		}
	}
	
	@Test
	void decode_ofEncode_roundTripsAGeneratedPuzzleAtEverySize() {
		for (GridSize size : GridSize.values()) {
			Puzzle puzzle = PuzzleGenerator.generate(PuzzleKey.of(size, Variant.CLASSIC, Difficulty.TWO, 4711L)).puzzle();
			
			assertArrayEquals(puzzle.values(), GivensCodec.decode(GivensCodec.encode(puzzle)), "Round trip failed for " + size);
		}
	}
	
	@Test
	void encode_emptyFourByFourGrid_isTheGoldenString() {
		// Computed independently (Python urlsafe_b64encode of the 7-byte wire form 04 00 00 00 00 00 00), not by
		// running the codec.
		assertEquals("BAAAAAAAAA", GivensCodec.encode(GridSize.FOUR, emptyGrid(GridSize.FOUR)));
	}
	
	@Test
	void encode_playerValueOnAnEmptyCell_isIgnored() {
		int[] givens = partialGrid(GridSize.NINE);
		int emptyCell = -1;
		for (int index = 0; index < givens.length; index++) {
			if (givens[index] == 0) {
				emptyCell = index;
				break;
			}
		}
		assertNotEquals(-1, emptyCell, "The fixture has no empty cell to play into");
		
		Puzzle puzzle = Puzzle.ofGivens(GridSize.NINE, Variant.CLASSIC, ClassicRegionPartition.of(GridSize.NINE), givens);
		puzzle.setValue(emptyCell, 5);
		
		assertEquals(GivensCodec.encode(GridSize.NINE, givens), GivensCodec.encode(puzzle),
			"A value the player filled in leaked into the encoding");
	}
	
	@Test
	void encode_isUrlSafeBase64WithoutPadding() {
		for (GridSize size : GridSize.values()) {
			String encoded = GivensCodec.encode(size, partialGrid(size));
			
			assertTrue(encoded.chars().allMatch(c -> URL_SAFE_ALPHABET.indexOf(c) >= 0),
				"Encoding has a non URL-safe Base64 character for " + size + ": " + encoded);
			assertFalse(encoded.contains("="), "Encoding is padded for " + size + ": " + encoded);
		}
	}
	
	@Test
	void decode_nineByNinePayload_yieldsExactlyEightyOneEntries() {
		String encoded = GivensCodec.encode(GridSize.NINE, partialGrid(GridSize.NINE));
		
		assertEquals(81, GivensCodec.decode(encoded).length);
	}
	
	@Test
	void encode_nullPuzzle_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> GivensCodec.encode(null));
	}
	
	@Test
	void encode_nullSize_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> GivensCodec.encode(null, new int[81]));
	}
	
	@Test
	void encode_nullGivens_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> GivensCodec.encode(GridSize.NINE, null));
	}
	
	@Test
	void encode_wrongArrayLength_throwsIllegalArgumentException() {
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> GivensCodec.encode(GridSize.NINE, new int[80])),
			() -> assertThrows(IllegalArgumentException.class, () -> GivensCodec.encode(GridSize.NINE, new int[82])),
			() -> assertThrows(IllegalArgumentException.class, () -> GivensCodec.encode(GridSize.NINE, new int[0]))
		);
	}
	
	@Test
	void encode_negativeDigit_throwsIllegalArgumentException() {
		int[] givens = new int[81];
		givens[42] = -1;
		
		assertThrows(IllegalArgumentException.class, () -> GivensCodec.encode(GridSize.NINE, givens));
	}
	
	@Test
	void encode_digitAboveTheEdgeLength_throwsIllegalArgumentException() {
		int[] givens = new int[81];
		givens[0] = 10;
		
		assertThrows(IllegalArgumentException.class, () -> GivensCodec.encode(GridSize.NINE, givens));
	}
	
	@Test
	void encode_digitExactlyAtTheEdgeLength_isAccepted() {
		int[] givens = new int[81];
		givens[0] = 9;
		
		assertEquals(9, GivensCodec.decode(GivensCodec.encode(GridSize.NINE, givens))[0]);
	}
	
	@Test
	void decode_nullString_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> GivensCodec.decode(null));
	}
	
	@Test
	void decode_emptyString_throwsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> GivensCodec.decode(""));
	}
	
	@Test
	void decode_illegalCharacter_throwsIllegalArgumentException() {
		// '+' and '/' belong to the standard alphabet but not to the URL-safe one, '?' to neither, and the last case
		// carries a character outside ASCII entirely, which the reverse table cannot even index.
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> GivensCodec.decode("+AAAAAAAAA")),
			() -> assertThrows(IllegalArgumentException.class, () -> GivensCodec.decode("BAAAAAAAA/")),
			() -> assertThrows(IllegalArgumentException.class, () -> GivensCodec.decode("BAAAA?AAAA")),
			() -> assertThrows(IllegalArgumentException.class, () -> GivensCodec.decode("BAAAA" + (char) 233 + "AAAA"))
		);
	}
	
	@Test
	void decode_unknownEdgeLengthInTheHeader_throwsIllegalArgumentException() {
		// 7 and 0 are not supported edge lengths, so the header byte alone must be rejected.
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> GivensCodec.decode(base64(new byte[] { 7 }))),
			() -> assertThrows(IllegalArgumentException.class, () -> GivensCodec.decode(base64(new byte[] { 0 })))
		);
	}
	
	@Test
	void decode_truncatedPayload_throwsIllegalArgumentException() {
		String encoded = GivensCodec.encode(GridSize.NINE, partialGrid(GridSize.NINE));
		
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> GivensCodec.decode(encoded.substring(0, encoded.length() - 2))),
			() -> assertThrows(IllegalArgumentException.class, () -> GivensCodec.decode(encoded.substring(0, 2))),
			() -> assertThrows(IllegalArgumentException.class, () -> GivensCodec.decode(encoded + "AA"))
		);
	}
	
	@Test
	void decode_digitAboveTheEdgeLength_throwsIllegalArgumentException() {
		// A well-formed 9x9 payload whose very first cell carries the four-bit value 10.
		byte[] bytes = new byte[42];
		bytes[0] = 9;
		bytes[1] = (byte) 0xA0;
		
		assertThrows(IllegalArgumentException.class, () -> GivensCodec.decode(base64(bytes)));
	}
	
	@Test
	void decode_ofEncode_isStableAcrossRepeatedCalls() {
		int[] givens = partialGrid(GridSize.SIXTEEN);
		
		assertEquals(GivensCodec.encode(GridSize.SIXTEEN, givens), GivensCodec.encode(GridSize.SIXTEEN, givens));
	}
	
	@Test
	void encode_thenDecode_thenFromGivens_reproducesAGeneratedClassicPuzzle() {
		PuzzleKey key = PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.THREE, 2024L);
		GeneratedPuzzle generated = PuzzleGenerator.generate(key);
		
		GeneratedPuzzle rebuilt = PuzzleGenerator.fromGivens(key, GivensCodec.decode(GivensCodec.encode(generated.puzzle())));
		
		assertAll(
			() -> assertEquals(generated.puzzle(), rebuilt.puzzle(), "The rebuilt puzzle differs"),
			() -> assertArrayEquals(generated.solution(), rebuilt.solution(), "The derived solution differs")
		);
	}
	
	@Test
	void encode_thenDecode_thenFromGivens_reproducesAGeneratedChaosPuzzle() {
		PuzzleKey key = PuzzleKey.of(GridSize.NINE, Variant.CHAOS, Difficulty.THREE, 2024L);
		GeneratedPuzzle generated = PuzzleGenerator.generate(key);
		
		GeneratedPuzzle rebuilt = PuzzleGenerator.fromGivens(key, GivensCodec.decode(GivensCodec.encode(generated.puzzle())));
		
		assertAll(
			() -> assertEquals(generated.puzzle(), rebuilt.puzzle(), "The rebuilt puzzle differs"),
			() -> assertEquals(generated.puzzle().partition(), rebuilt.puzzle().partition(), "The rebuilt layout differs"),
			() -> assertArrayEquals(generated.solution(), rebuilt.solution(), "The derived solution differs")
		);
	}
}
