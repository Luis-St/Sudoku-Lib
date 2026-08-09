package net.luis.sudoku.difficulty;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link Difficulty}.
 */
class DifficultyTest {
	
	@Test
	void values_allConstants_areTheFourteenTiersPlusLisa() {
		assertArrayEquals(new Difficulty[] {
			Difficulty.ONE, Difficulty.TWO, Difficulty.THREE, Difficulty.FOUR, Difficulty.FIVE, Difficulty.SIX,
			Difficulty.SEVEN, Difficulty.EIGHT, Difficulty.NINE, Difficulty.TEN, Difficulty.ELEVEN, Difficulty.TWELVE,
			Difficulty.THIRTEEN, Difficulty.FOURTEEN, Difficulty.LISA
		}, Difficulty.values());
	}
	
	@Test
	void values_declarationOrder_isAscendingByIndex() {
		Difficulty[] values = Difficulty.values();
		for (int i = 1; i < values.length; i++) {
			assertTrue(values[i - 1].index() < values[i].index(), values[i - 1] + " before " + values[i]);
		}
	}
	
	@Test
	void valueOf_knownAndUnknownNames_behavesAsExpected() {
		assertAll(
			() -> assertSame(Difficulty.ONE, Difficulty.valueOf("ONE")),
			() -> assertSame(Difficulty.LISA, Difficulty.valueOf("LISA")),
			() -> assertThrows(IllegalArgumentException.class, () -> Difficulty.valueOf("SIXTEEN"))
		);
	}
	
	@Test
	void index_everyConstant_returnsItsDeclaredIndex() {
		assertAll(
			() -> assertEquals(1, Difficulty.ONE.index()),
			() -> assertEquals(5, Difficulty.FIVE.index()),
			() -> assertEquals(10, Difficulty.TEN.index()),
			() -> assertEquals(14, Difficulty.FOURTEEN.index()),
			() -> assertEquals(15, Difficulty.LISA.index())
		);
	}
	
	@Test
	void index_everyConstant_isInOneToFifteen() {
		for (Difficulty difficulty : Difficulty.values()) {
			assertTrue(difficulty.index() >= 1 && difficulty.index() <= 15, difficulty + " index " + difficulty.index());
		}
	}
	
	@Test
	void ofIndex_everyValidIndex_returnsTheMatchingConstant() {
		assertAll(
			() -> assertSame(Difficulty.ONE, Difficulty.ofIndex(1)),
			() -> assertSame(Difficulty.FIVE, Difficulty.ofIndex(5)),
			() -> assertSame(Difficulty.TEN, Difficulty.ofIndex(10)),
			() -> assertSame(Difficulty.FOURTEEN, Difficulty.ofIndex(14)),
			() -> assertSame(Difficulty.LISA, Difficulty.ofIndex(15))
		);
	}
	
	@Test
	void ofIndex_zeroOrSixteen_throws() {
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> Difficulty.ofIndex(0)),
			() -> assertThrows(IllegalArgumentException.class, () -> Difficulty.ofIndex(16))
		);
	}
	
	@Test
	void ofIndex_negativeIndices_throws() {
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> Difficulty.ofIndex(-1)),
			() -> assertThrows(IllegalArgumentException.class, () -> Difficulty.ofIndex(-6)),
			() -> assertThrows(IllegalArgumentException.class, () -> Difficulty.ofIndex(Integer.MIN_VALUE))
		);
	}
	
	@Test
	void ofIndex_largeIndex_throws() {
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> Difficulty.ofIndex(100)),
			() -> assertThrows(IllegalArgumentException.class, () -> Difficulty.ofIndex(Integer.MAX_VALUE))
		);
	}
	
	@Test
	void ofIndex_everyConstantsOwnIndex_roundTrips() {
		for (Difficulty difficulty : Difficulty.values()) {
			assertSame(difficulty, Difficulty.ofIndex(difficulty.index()), difficulty.toString());
		}
	}
	
	@Test
	void isLisa_lisa_returnsTrue() {
		assertTrue(Difficulty.LISA.isLisa());
	}
	
	@Test
	void isLisa_numberedTiers_returnsFalse() {
		for (Difficulty difficulty : Difficulty.values()) {
			if (difficulty != Difficulty.LISA) {
				assertFalse(difficulty.isLisa(), difficulty.toString());
			}
		}
	}
	
	@Test
	void isAllowedInMultiplayer_lisa_returnsFalse() {
		assertFalse(Difficulty.LISA.isAllowedInMultiplayer());
	}
	
	@Test
	void isAllowedInMultiplayer_numberedTiers_returnsTrue() {
		for (Difficulty difficulty : Difficulty.values()) {
			if (difficulty != Difficulty.LISA) {
				assertTrue(difficulty.isAllowedInMultiplayer(), difficulty.toString());
			}
		}
	}
	
	@Test
	void isAllowedInMultiplayer_everyConstant_isTheInverseOfIsLisa() {
		for (Difficulty difficulty : Difficulty.values()) {
			assertEquals(!difficulty.isLisa(), difficulty.isAllowedInMultiplayer(), difficulty.toString());
		}
	}
}
