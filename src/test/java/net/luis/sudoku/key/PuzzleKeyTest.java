package net.luis.sudoku.key;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.grid.Variant;
import net.luis.sudoku.version.GenVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link PuzzleKey}.
 */
class PuzzleKeyTest {
	
	private static final int OTHER_GEN_VERSION = GenVersion.CURRENT + 1;
	
	@Test
	void constructor_validArguments_keepsEveryComponent() {
		PuzzleKey key = new PuzzleKey(3, GridSize.TWELVE, Variant.CHAOS, Difficulty.FIVE, -42L);
		assertAll(
			() -> assertEquals(3, key.genVersion()),
			() -> assertSame(GridSize.TWELVE, key.size()),
			() -> assertSame(Variant.CHAOS, key.variant()),
			() -> assertSame(Difficulty.FIVE, key.difficulty()),
			() -> assertEquals(-42L, key.seed())
		);
	}
	
	@Test
	void constructor_genVersionOne_isAccepted() {
		assertDoesNotThrow(() -> new PuzzleKey(1, GridSize.NINE, Variant.CLASSIC, Difficulty.ONE, 0L));
	}
	
	@Test
	void constructor_genVersionZero_throwsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> new PuzzleKey(0, GridSize.NINE, Variant.CLASSIC, Difficulty.ONE, 0L));
	}
	
	@Test
	void constructor_genVersionNegative_throwsIllegalArgumentException() {
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> new PuzzleKey(-1, GridSize.NINE, Variant.CLASSIC, Difficulty.ONE, 0L)),
			() -> assertThrows(IllegalArgumentException.class, () -> new PuzzleKey(Integer.MIN_VALUE, GridSize.NINE, Variant.CLASSIC, Difficulty.ONE, 0L))
		);
	}
	
	@Test
	void constructor_chaosAtFour_throwsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> new PuzzleKey(1, GridSize.FOUR, Variant.CHAOS, Difficulty.ONE, 0L));
	}
	
	@Test
	void constructor_chaosAboveFour_isAccepted() {
		assertAll(
			() -> assertDoesNotThrow(() -> new PuzzleKey(1, GridSize.SIX, Variant.CHAOS, Difficulty.ONE, 0L)),
			() -> assertDoesNotThrow(() -> new PuzzleKey(1, GridSize.NINE, Variant.CHAOS, Difficulty.ONE, 0L)),
			() -> assertDoesNotThrow(() -> new PuzzleKey(1, GridSize.TWELVE, Variant.CHAOS, Difficulty.ONE, 0L)),
			() -> assertDoesNotThrow(() -> new PuzzleKey(1, GridSize.SIXTEEN, Variant.CHAOS, Difficulty.ONE, 0L))
		);
	}
	
	@Test
	void constructor_classicAtEverySize_isAccepted() {
		for (GridSize size : GridSize.values()) {
			assertDoesNotThrow(() -> new PuzzleKey(1, size, Variant.CLASSIC, Difficulty.ONE, 0L), "CLASSIC at " + size);
		}
	}
	
	@Test
	void constructor_everyDifficulty_isAccepted() {
		for (Difficulty difficulty : Difficulty.values()) {
			assertDoesNotThrow(() -> new PuzzleKey(1, GridSize.NINE, Variant.CLASSIC, difficulty, 0L), "Difficulty " + difficulty);
		}
	}
	
	@Test
	void constructor_nullComponents_throwsNullPointerException() {
		assertAll(
			() -> assertThrows(NullPointerException.class, () -> new PuzzleKey(1, null, Variant.CLASSIC, Difficulty.ONE, 0L)),
			() -> assertThrows(NullPointerException.class, () -> new PuzzleKey(1, GridSize.NINE, null, Difficulty.ONE, 0L)),
			() -> assertThrows(NullPointerException.class, () -> new PuzzleKey(1, GridSize.NINE, Variant.CLASSIC, null, 0L)),
			() -> assertThrows(NullPointerException.class, () -> new PuzzleKey(1, null, null, null, 0L))
		);
	}
	
	@Test
	void constructor_nullComponentAndInvalidGenVersion_reportsTheNullFirst() {
		assertThrows(NullPointerException.class, () -> new PuzzleKey(0, null, Variant.CLASSIC, Difficulty.ONE, 0L));
	}
	
	@Test
	void of_validArguments_stampsCurrentGenVersion() {
		PuzzleKey key = PuzzleKey.of(GridSize.SIX, Variant.CHAOS, Difficulty.LISA, 7L);
		assertAll(
			() -> assertEquals(GenVersion.CURRENT, key.genVersion()),
			() -> assertSame(GridSize.SIX, key.size()),
			() -> assertSame(Variant.CHAOS, key.variant()),
			() -> assertSame(Difficulty.LISA, key.difficulty()),
			() -> assertEquals(7L, key.seed())
		);
	}
	
	@Test
	void of_equalsTheCanonicalConstructorWithCurrentGenVersion() {
		assertEquals(new PuzzleKey(GenVersion.CURRENT, GridSize.NINE, Variant.CLASSIC, Difficulty.TWO, 5L), PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.TWO, 5L));
	}
	
	@Test
	void of_chaosAtFour_throwsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> PuzzleKey.of(GridSize.FOUR, Variant.CHAOS, Difficulty.ONE, 0L));
	}
	
	@Test
	void of_nullComponents_throwsNullPointerException() {
		assertAll(
			() -> assertThrows(NullPointerException.class, () -> PuzzleKey.of(null, Variant.CLASSIC, Difficulty.ONE, 0L)),
			() -> assertThrows(NullPointerException.class, () -> PuzzleKey.of(GridSize.NINE, null, Difficulty.ONE, 0L)),
			() -> assertThrows(NullPointerException.class, () -> PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, null, 0L))
		);
	}
	
	@Test
	void isCurrentGenVersion_currentVersion_returnsTrue() {
		assertAll(
			() -> assertTrue(PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.ONE, 0L).isCurrentGenVersion()),
			() -> assertTrue(new PuzzleKey(GenVersion.CURRENT, GridSize.NINE, Variant.CLASSIC, Difficulty.ONE, 0L).isCurrentGenVersion())
		);
	}
	
	@Test
	void isCurrentGenVersion_otherVersion_returnsFalse() {
		assertFalse(new PuzzleKey(OTHER_GEN_VERSION, GridSize.NINE, Variant.CLASSIC, Difficulty.ONE, 0L).isCurrentGenVersion());
	}
	
	@Test
	void equals_sameComponents_areEqualAndShareHashCode() {
		PuzzleKey first = new PuzzleKey(2, GridSize.TWELVE, Variant.CHAOS, Difficulty.FOUR, Long.MIN_VALUE);
		PuzzleKey second = new PuzzleKey(2, GridSize.TWELVE, Variant.CHAOS, Difficulty.FOUR, Long.MIN_VALUE);
		assertAll(
			() -> assertEquals(first, second),
			() -> assertEquals(first.hashCode(), second.hashCode())
		);
	}
	
	@Test
	void equals_eachComponentDifferingIndependently_areNotEqual() {
		PuzzleKey base = new PuzzleKey(2, GridSize.NINE, Variant.CLASSIC, Difficulty.THREE, 11L);
		assertAll(
			() -> assertNotEquals(base, new PuzzleKey(3, GridSize.NINE, Variant.CLASSIC, Difficulty.THREE, 11L)),
			() -> assertNotEquals(base, new PuzzleKey(2, GridSize.SIXTEEN, Variant.CLASSIC, Difficulty.THREE, 11L)),
			() -> assertNotEquals(base, new PuzzleKey(2, GridSize.NINE, Variant.CHAOS, Difficulty.THREE, 11L)),
			() -> assertNotEquals(base, new PuzzleKey(2, GridSize.NINE, Variant.CLASSIC, Difficulty.LISA, 11L)),
			() -> assertNotEquals(base, new PuzzleKey(2, GridSize.NINE, Variant.CLASSIC, Difficulty.THREE, 12L))
		);
	}
	
	@Test
	void equals_nullAndForeignType_areNotEqual() {
		PuzzleKey key = new PuzzleKey(1, GridSize.NINE, Variant.CLASSIC, Difficulty.ONE, 0L);
		assertAll(
			() -> assertNotEquals(null, key),
			() -> assertNotEquals("key", key),
			() -> assertEquals(key, key)
		);
	}
	
	@Test
	void toString_validKey_mentionsEveryComponent() {
		String text = new PuzzleKey(2, GridSize.SIX, Variant.CHAOS, Difficulty.LISA, 99L).toString();
		assertAll(
			() -> assertTrue(text.contains("2"), text),
			() -> assertTrue(text.contains("SIX"), text),
			() -> assertTrue(text.contains("CHAOS"), text),
			() -> assertTrue(text.contains("LISA"), text),
			() -> assertTrue(text.contains("99"), text)
		);
	}
}
