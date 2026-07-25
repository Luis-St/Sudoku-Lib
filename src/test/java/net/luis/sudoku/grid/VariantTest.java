package net.luis.sudoku.grid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link Variant}.
 */
class VariantTest {
	
	@Test
	void values_allConstants_areClassicAndChaos() {
		assertArrayEquals(new Variant[] { Variant.CLASSIC, Variant.CHAOS }, Variant.values());
	}
	
	@Test
	void valueOf_knownAndUnknownNames_behavesAsExpected() {
		assertAll(
			() -> assertSame(Variant.CLASSIC, Variant.valueOf("CLASSIC")),
			() -> assertSame(Variant.CHAOS, Variant.valueOf("CHAOS")),
			() -> assertThrows(IllegalArgumentException.class, () -> Variant.valueOf("JIGSAW"))
		);
	}
	
	@Test
	void isSupportedAt_classicAtEverySize_returnsTrue() {
		for (GridSize size : GridSize.values()) {
			assertTrue(Variant.CLASSIC.isSupportedAt(size), "CLASSIC at " + size);
		}
	}
	
	@Test
	void isSupportedAt_chaosAtFour_returnsFalse() {
		assertFalse(Variant.CHAOS.isSupportedAt(GridSize.FOUR));
	}
	
	@Test
	void isSupportedAt_chaosAboveFour_returnsTrue() {
		assertAll(
			() -> assertTrue(Variant.CHAOS.isSupportedAt(GridSize.SIX)),
			() -> assertTrue(Variant.CHAOS.isSupportedAt(GridSize.NINE)),
			() -> assertTrue(Variant.CHAOS.isSupportedAt(GridSize.TWELVE)),
			() -> assertTrue(Variant.CHAOS.isSupportedAt(GridSize.SIXTEEN))
		);
	}
	
	@Test
	void isSupportedAt_everyCombination_matchesTheRule() {
		for (Variant variant : Variant.values()) {
			for (GridSize size : GridSize.values()) {
				boolean expected = variant != Variant.CHAOS || size != GridSize.FOUR;
				assertEquals(expected, variant.isSupportedAt(size), variant + " at " + size);
			}
		}
	}
	
	@Test
	void checkSupportedAt_classicAtEverySize_doesNotThrow() {
		for (GridSize size : GridSize.values()) {
			assertDoesNotThrow(() -> Variant.CLASSIC.checkSupportedAt(size), "CLASSIC at " + size);
		}
	}
	
	@Test
	void checkSupportedAt_chaosAtFour_throws() {
		assertThrows(IllegalArgumentException.class, () -> Variant.CHAOS.checkSupportedAt(GridSize.FOUR));
	}
	
	@Test
	void checkSupportedAt_chaosAboveFour_doesNotThrow() {
		assertAll(
			() -> assertDoesNotThrow(() -> Variant.CHAOS.checkSupportedAt(GridSize.SIX)),
			() -> assertDoesNotThrow(() -> Variant.CHAOS.checkSupportedAt(GridSize.NINE)),
			() -> assertDoesNotThrow(() -> Variant.CHAOS.checkSupportedAt(GridSize.TWELVE)),
			() -> assertDoesNotThrow(() -> Variant.CHAOS.checkSupportedAt(GridSize.SIXTEEN))
		);
	}
	
	@Test
	void checkSupportedAt_everyCombination_agreesWithIsSupportedAt() {
		for (Variant variant : Variant.values()) {
			for (GridSize size : GridSize.values()) {
				if (variant.isSupportedAt(size)) {
					assertDoesNotThrow(() -> variant.checkSupportedAt(size), variant + " at " + size);
				} else {
					assertThrows(IllegalArgumentException.class, () -> variant.checkSupportedAt(size), variant + " at " + size);
				}
			}
		}
	}
}
