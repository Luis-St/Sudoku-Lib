package net.luis.sudoku.grid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link Region}.
 */
class RegionTest {
	
	@Test
	void region_withSortedCells_keepsOrder() {
		Region region = new Region(0, 1, 4, 5);
		assertArrayEquals(new int[] { 0, 1, 4, 5 }, region.cells());
	}
	
	@Test
	void region_withUnsortedCells_sortsAscending() {
		Region region = new Region(5, 0, 4, 1);
		assertArrayEquals(new int[] { 0, 1, 4, 5 }, region.cells());
	}
	
	@Test
	void region_withReverseSortedCells_sortsAscending() {
		Region region = new Region(80, 42, 17, 3, 0);
		assertArrayEquals(new int[] { 0, 3, 17, 42, 80 }, region.cells());
	}
	
	@Test
	void region_withSingleCell_isAccepted() {
		Region region = new Region(7);
		assertAll(
			() -> assertEquals(1, region.size()),
			() -> assertEquals(7, region.cell(0)),
			() -> assertArrayEquals(new int[] { 7 }, region.cells())
		);
	}
	
	@Test
	void region_withZeroCell_isAccepted() {
		Region region = new Region(0);
		assertAll(
			() -> assertEquals(1, region.size()),
			() -> assertEquals(0, region.cell(0)),
			() -> assertTrue(region.contains(0))
		);
	}
	
	@Test
	void region_withNoCells_throws() {
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> new Region()),
			() -> assertThrows(IllegalArgumentException.class, () -> new Region(new int[0]))
		);
	}
	
	@Test
	void region_withNullArray_throws() {
		assertThrows(NullPointerException.class, () -> new Region((int[]) null));
	}
	
	@Test
	void region_withNegativeCell_throws() {
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> new Region(-1)),
			() -> assertThrows(IllegalArgumentException.class, () -> new Region(0, 1, -1)),
			() -> assertThrows(IllegalArgumentException.class, () -> new Region(-5, 3, 7)),
			() -> assertThrows(IllegalArgumentException.class, () -> new Region(Integer.MIN_VALUE, 0))
		);
	}
	
	@Test
	void region_withDuplicateCell_throws() {
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> new Region(1, 1)),
			() -> assertThrows(IllegalArgumentException.class, () -> new Region(0, 1, 2, 1)),
			() -> assertThrows(IllegalArgumentException.class, () -> new Region(5, 0, 5)),
			() -> assertThrows(IllegalArgumentException.class, () -> new Region(9, 9, 9))
		);
	}
	
	@Test
	void region_withDistinctNonNegativeCells_doesNotThrow() {
		assertAll(
			() -> assertDoesNotThrow(() -> new Region(0)),
			() -> assertDoesNotThrow(() -> new Region(0, 1, 2, 3)),
			() -> assertDoesNotThrow(() -> new Region(3, 2, 1, 0)),
			() -> assertDoesNotThrow(() -> new Region(Integer.MAX_VALUE, 0))
		);
	}
	
	@Test
	void region_mutatingTheInputArrayAfterwards_doesNotAffectTheRegion() {
		int[] input = { 3, 1, 2 };
		Region region = new Region(input);
		input[0] = 99;
		input[1] = 98;
		input[2] = 97;
		assertAll(
			() -> assertArrayEquals(new int[] { 1, 2, 3 }, region.cells()),
			() -> assertEquals(1, region.cell(0)),
			() -> assertFalse(region.contains(99))
		);
	}
	
	@Test
	void region_construction_doesNotSortTheInputArray() {
		int[] input = { 3, 1, 2 };
		new Region(input);
		assertArrayEquals(new int[] { 3, 1, 2 }, input);
	}
	
	@Test
	void size_variousRegions_returnsCellCount() {
		assertAll(
			() -> assertEquals(1, new Region(4).size()),
			() -> assertEquals(3, new Region(3, 1, 2).size()),
			() -> assertEquals(9, new Region(0, 1, 2, 9, 10, 11, 18, 19, 20).size())
		);
	}
	
	@Test
	void cell_everyPosition_returnsAscendingIndices() {
		Region region = new Region(20, 0, 11, 2, 9, 18, 1, 19, 10);
		int[] expected = { 0, 1, 2, 9, 10, 11, 18, 19, 20 };
		for (int position = 0; position < expected.length; position++) {
			assertEquals(expected[position], region.cell(position), "position " + position);
		}
	}
	
	@Test
	void cell_negativePosition_throws() {
		Region region = new Region(0, 1, 4, 5);
		assertThrows(IndexOutOfBoundsException.class, () -> region.cell(-1));
	}
	
	@Test
	void cell_positionAtOrAboveSize_throws() {
		Region region = new Region(0, 1, 4, 5);
		assertAll(
			() -> assertThrows(IndexOutOfBoundsException.class, () -> region.cell(4)),
			() -> assertThrows(IndexOutOfBoundsException.class, () -> region.cell(100))
		);
	}
	
	@Test
	void cells_repeatedCalls_returnIndependentCopies() {
		Region region = new Region(0, 1, 4, 5);
		int[] first = region.cells();
		int[] second = region.cells();
		assertAll(
			() -> assertNotSame(first, second),
			() -> assertArrayEquals(first, second)
		);
	}
	
	@Test
	void cells_mutatingTheReturnedArray_doesNotAffectTheRegion() {
		Region region = new Region(0, 1, 4, 5);
		int[] returned = region.cells();
		returned[0] = 42;
		returned[3] = -7;
		assertAll(
			() -> assertArrayEquals(new int[] { 0, 1, 4, 5 }, region.cells()),
			() -> assertEquals(0, region.cell(0)),
			() -> assertEquals(5, region.cell(3)),
			() -> assertTrue(region.contains(0)),
			() -> assertFalse(region.contains(42))
		);
	}
	
	@Test
	void contains_containedCell_returnsTrue() {
		Region region = new Region(5, 0, 4, 1);
		assertAll(
			() -> assertTrue(region.contains(0)),
			() -> assertTrue(region.contains(1)),
			() -> assertTrue(region.contains(4)),
			() -> assertTrue(region.contains(5))
		);
	}
	
	@Test
	void contains_missingCell_returnsFalse() {
		Region region = new Region(5, 0, 4, 1);
		assertAll(
			() -> assertFalse(region.contains(-1)),
			() -> assertFalse(region.contains(2)),
			() -> assertFalse(region.contains(3)),
			() -> assertFalse(region.contains(6)),
			() -> assertFalse(region.contains(Integer.MAX_VALUE)),
			() -> assertFalse(region.contains(Integer.MIN_VALUE))
		);
	}
	
	@Test
	void contains_everyCellOfARange_matchesMembership() {
		Region region = new Region(0, 1, 2, 9, 10, 11, 18, 19, 20);
		for (int cellIndex = 0; cellIndex < 30; cellIndex++) {
			int row = cellIndex / 9;
			int column = cellIndex % 9;
			boolean expected = row < 3 && column < 3;
			assertEquals(expected, region.contains(cellIndex), "cell " + cellIndex);
		}
	}
	
	@Test
	void equals_sameInstance_returnsTrue() {
		Region region = new Region(0, 1, 4, 5);
		assertEquals(region, region);
	}
	
	@Test
	void equals_sameCellsSameOrder_returnsTrue() {
		assertEquals(new Region(0, 1, 4, 5), new Region(0, 1, 4, 5));
	}
	
	@Test
	void equals_sameCellsDifferentOrder_returnsTrue() {
		assertAll(
			() -> assertEquals(new Region(3, 1, 2), new Region(1, 2, 3)),
			() -> assertEquals(new Region(1, 2, 3), new Region(3, 1, 2)),
			() -> assertEquals(new Region(5, 4, 1, 0), new Region(0, 1, 4, 5))
		);
	}
	
	@Test
	void equals_differentCells_returnsFalse() {
		assertAll(
			() -> assertNotEquals(new Region(1, 2, 3), new Region(1, 2, 4)),
			() -> assertNotEquals(new Region(1, 2, 3), new Region(1, 2)),
			() -> assertNotEquals(new Region(1, 2), new Region(1, 2, 3))
		);
	}
	
	@Test
	void equals_nullOrOtherType_returnsFalse() {
		Region region = new Region(1, 2, 3);
		assertAll(
			() -> assertNotEquals(null, region),
			() -> assertNotEquals("Region[1, 2, 3]", region),
			() -> assertNotEquals(region, new Object())
		);
	}
	
	@Test
	void hashCode_equalRegions_areEqual() {
		assertAll(
			() -> assertEquals(new Region(1, 2, 3).hashCode(), new Region(3, 1, 2).hashCode()),
			() -> assertEquals(new Region(0, 1, 4, 5).hashCode(), new Region(5, 4, 1, 0).hashCode())
		);
	}
	
	@Test
	void hashCode_sameInstance_isStable() {
		Region region = new Region(3, 1, 2);
		assertEquals(region.hashCode(), region.hashCode());
	}
	
	@Test
	void hashCode_differentRegions_differ() {
		assertNotEquals(new Region(1, 2, 3).hashCode(), new Region(1, 2, 4).hashCode());
	}
	
	@Test
	void toString_anyRegion_listsCellsAscending() {
		assertAll(
			() -> assertEquals("Region[1, 2, 3]", new Region(3, 1, 2).toString()),
			() -> assertEquals("Region[7]", new Region(7).toString())
		);
	}
}
