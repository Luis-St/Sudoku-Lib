package net.luis.sudoku.grid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link GridSize}.
 */
class GridSizeTest {
	
	@Test
	void values_allConstants_areTheFiveSupportedSizes() {
		assertArrayEquals(new GridSize[] { GridSize.FOUR, GridSize.SIX, GridSize.NINE, GridSize.TWELVE, GridSize.SIXTEEN }, GridSize.values());
	}
	
	@Test
	void four_fields_are4x2x2() {
		assertAll(
			() -> assertEquals(4, GridSize.FOUR.n()),
			() -> assertEquals(16, GridSize.FOUR.cellCount()),
			() -> assertEquals(2, GridSize.FOUR.boxWidth()),
			() -> assertEquals(2, GridSize.FOUR.boxHeight())
		);
	}
	
	@Test
	void six_fields_are6x3x2() {
		assertAll(
			() -> assertEquals(6, GridSize.SIX.n()),
			() -> assertEquals(36, GridSize.SIX.cellCount()),
			() -> assertEquals(3, GridSize.SIX.boxWidth()),
			() -> assertEquals(2, GridSize.SIX.boxHeight())
		);
	}
	
	@Test
	void nine_fields_are9x3x3() {
		assertAll(
			() -> assertEquals(9, GridSize.NINE.n()),
			() -> assertEquals(81, GridSize.NINE.cellCount()),
			() -> assertEquals(3, GridSize.NINE.boxWidth()),
			() -> assertEquals(3, GridSize.NINE.boxHeight())
		);
	}
	
	@Test
	void twelve_fields_are12x4x3() {
		assertAll(
			() -> assertEquals(12, GridSize.TWELVE.n()),
			() -> assertEquals(144, GridSize.TWELVE.cellCount()),
			() -> assertEquals(4, GridSize.TWELVE.boxWidth()),
			() -> assertEquals(3, GridSize.TWELVE.boxHeight())
		);
	}
	
	@Test
	void sixteen_fields_are16x4x4() {
		assertAll(
			() -> assertEquals(16, GridSize.SIXTEEN.n()),
			() -> assertEquals(256, GridSize.SIXTEEN.cellCount()),
			() -> assertEquals(4, GridSize.SIXTEEN.boxWidth()),
			() -> assertEquals(4, GridSize.SIXTEEN.boxHeight())
		);
	}
	
	@Test
	void boxDimensions_everySize_multiplyToN() {
		for (GridSize size : GridSize.values()) {
			assertEquals(size.n(), size.boxWidth() * size.boxHeight(), "box dimensions of " + size);
		}
	}
	
	@Test
	void boxDimensions_everySize_divideNEvenly() {
		for (GridSize size : GridSize.values()) {
			assertAll(
				() -> assertEquals(0, size.n() % size.boxWidth(), "box width of " + size),
				() -> assertEquals(0, size.n() % size.boxHeight(), "box height of " + size)
			);
		}
	}
	
	@Test
	void cellCount_everySize_isNSquared() {
		for (GridSize size : GridSize.values()) {
			assertEquals(size.n() * size.n(), size.cellCount(), "cell count of " + size);
		}
	}
	
	@Test
	void ofEdgeLength_supportedLengths_returnsMatchingSize() {
		assertAll(
			() -> assertSame(GridSize.FOUR, GridSize.ofEdgeLength(4)),
			() -> assertSame(GridSize.SIX, GridSize.ofEdgeLength(6)),
			() -> assertSame(GridSize.NINE, GridSize.ofEdgeLength(9)),
			() -> assertSame(GridSize.TWELVE, GridSize.ofEdgeLength(12)),
			() -> assertSame(GridSize.SIXTEEN, GridSize.ofEdgeLength(16))
		);
	}
	
	@Test
	void ofEdgeLength_everySizeOwnN_roundTrips() {
		for (GridSize size : GridSize.values()) {
			assertSame(size, GridSize.ofEdgeLength(size.n()), "round trip of " + size);
		}
	}
	
	@Test
	void ofEdgeLength_unsupportedLength_throws() {
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> GridSize.ofEdgeLength(0)),
			() -> assertThrows(IllegalArgumentException.class, () -> GridSize.ofEdgeLength(1)),
			() -> assertThrows(IllegalArgumentException.class, () -> GridSize.ofEdgeLength(5)),
			() -> assertThrows(IllegalArgumentException.class, () -> GridSize.ofEdgeLength(8)),
			() -> assertThrows(IllegalArgumentException.class, () -> GridSize.ofEdgeLength(25)),
			() -> assertThrows(IllegalArgumentException.class, () -> GridSize.ofEdgeLength(-4)),
			() -> assertThrows(IllegalArgumentException.class, () -> GridSize.ofEdgeLength(Integer.MAX_VALUE))
		);
	}
	
	@Test
	void valueOf_unknownName_throws() {
		assertAll(
			() -> assertSame(GridSize.NINE, GridSize.valueOf("NINE")),
			() -> assertThrows(IllegalArgumentException.class, () -> GridSize.valueOf("TEN"))
		);
	}
	
	@Test
	void indexOf_knownCoordinates_isRowMajor() {
		assertAll(
			() -> assertEquals(0, GridSize.FOUR.indexOf(0, 0)),
			() -> assertEquals(3, GridSize.FOUR.indexOf(0, 3)),
			() -> assertEquals(4, GridSize.FOUR.indexOf(1, 0)),
			() -> assertEquals(15, GridSize.FOUR.indexOf(3, 3)),
			() -> assertEquals(0, GridSize.NINE.indexOf(0, 0)),
			() -> assertEquals(10, GridSize.NINE.indexOf(1, 1)),
			() -> assertEquals(80, GridSize.NINE.indexOf(8, 8)),
			() -> assertEquals(255, GridSize.SIXTEEN.indexOf(15, 15))
		);
	}
	
	@Test
	void indexOfRowOfColumnOf_everyCellOfFour_roundTrips() {
		this.assertRoundTrip(GridSize.FOUR);
	}
	
	@Test
	void indexOfRowOfColumnOf_everyCellOfNine_roundTrips() {
		this.assertRoundTrip(GridSize.NINE);
	}
	
	@Test
	void indexOfRowOfColumnOf_everyCellOfEverySize_roundTrips() {
		for (GridSize size : GridSize.values()) {
			this.assertRoundTrip(size);
		}
	}
	
	@Test
	void indexOf_negativeRow_throws() {
		assertAll(
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.FOUR.indexOf(-1, 0)),
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.NINE.indexOf(-1, 0)),
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.NINE.indexOf(Integer.MIN_VALUE, 0))
		);
	}
	
	@Test
	void indexOf_rowAtOrAboveN_throws() {
		assertAll(
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.FOUR.indexOf(4, 0)),
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.NINE.indexOf(9, 0)),
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.NINE.indexOf(Integer.MAX_VALUE, 0))
		);
	}
	
	@Test
	void indexOf_negativeColumn_throws() {
		assertAll(
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.FOUR.indexOf(0, -1)),
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.NINE.indexOf(0, -1))
		);
	}
	
	@Test
	void indexOf_columnAtOrAboveN_throws() {
		assertAll(
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.FOUR.indexOf(0, 4)),
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.NINE.indexOf(0, 9))
		);
	}
	
	@Test
	void indexOf_boundaryCoordinates_doesNotThrow() {
		assertAll(
			() -> assertDoesNotThrow(() -> GridSize.FOUR.indexOf(0, 0)),
			() -> assertDoesNotThrow(() -> GridSize.FOUR.indexOf(3, 3))
		);
	}
	
	@Test
	void rowOf_knownIndices_returnsRow() {
		assertAll(
			() -> assertEquals(0, GridSize.FOUR.rowOf(0)),
			() -> assertEquals(0, GridSize.FOUR.rowOf(3)),
			() -> assertEquals(1, GridSize.FOUR.rowOf(4)),
			() -> assertEquals(3, GridSize.FOUR.rowOf(15)),
			() -> assertEquals(8, GridSize.NINE.rowOf(80))
		);
	}
	
	@Test
	void rowOf_negativeIndex_throws() {
		assertAll(
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.FOUR.rowOf(-1)),
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.NINE.rowOf(-1))
		);
	}
	
	@Test
	void rowOf_indexAtOrAboveCellCount_throws() {
		assertAll(
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.FOUR.rowOf(16)),
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.NINE.rowOf(81)),
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.SIXTEEN.rowOf(256))
		);
	}
	
	@Test
	void columnOf_knownIndices_returnsColumn() {
		assertAll(
			() -> assertEquals(0, GridSize.FOUR.columnOf(0)),
			() -> assertEquals(3, GridSize.FOUR.columnOf(3)),
			() -> assertEquals(0, GridSize.FOUR.columnOf(4)),
			() -> assertEquals(3, GridSize.FOUR.columnOf(15)),
			() -> assertEquals(8, GridSize.NINE.columnOf(80))
		);
	}
	
	@Test
	void columnOf_negativeIndex_throws() {
		assertAll(
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.FOUR.columnOf(-1)),
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.NINE.columnOf(-1))
		);
	}
	
	@Test
	void columnOf_indexAtOrAboveCellCount_throws() {
		assertAll(
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.FOUR.columnOf(16)),
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.NINE.columnOf(81)),
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.SIXTEEN.columnOf(256))
		);
	}
	
	@Test
	void isValidDigit_inRange_returnsTrue() {
		for (GridSize size : GridSize.values()) {
			for (int digit = 1; digit <= size.n(); digit++) {
				assertTrue(size.isValidDigit(digit), "digit " + digit + " on " + size);
			}
		}
	}
	
	@Test
	void isValidDigit_belowOne_returnsFalse() {
		assertAll(
			() -> assertFalse(GridSize.FOUR.isValidDigit(0)),
			() -> assertFalse(GridSize.FOUR.isValidDigit(-1)),
			() -> assertFalse(GridSize.NINE.isValidDigit(0)),
			() -> assertFalse(GridSize.NINE.isValidDigit(Integer.MIN_VALUE))
		);
	}
	
	@Test
	void isValidDigit_aboveN_returnsFalse() {
		assertAll(
			() -> assertFalse(GridSize.FOUR.isValidDigit(5)),
			() -> assertFalse(GridSize.NINE.isValidDigit(10)),
			() -> assertFalse(GridSize.SIXTEEN.isValidDigit(17)),
			() -> assertFalse(GridSize.NINE.isValidDigit(Integer.MAX_VALUE))
		);
	}
	
	@Test
	void isValidDigit_boundaries_matchOneToN() {
		for (GridSize size : GridSize.values()) {
			assertAll(
				() -> assertFalse(size.isValidDigit(0), "0 on " + size),
				() -> assertTrue(size.isValidDigit(1), "1 on " + size),
				() -> assertTrue(size.isValidDigit(size.n()), "n on " + size),
				() -> assertFalse(size.isValidDigit(size.n() + 1), "n+1 on " + size)
			);
		}
	}
	
	@Test
	void checkCellIndex_inRange_doesNotThrow() {
		for (GridSize size : GridSize.values()) {
			assertAll(
				() -> assertDoesNotThrow(() -> size.checkCellIndex(0), "0 on " + size),
				() -> assertDoesNotThrow(() -> size.checkCellIndex(size.cellCount() - 1), "last cell on " + size)
			);
		}
	}
	
	@Test
	void checkCellIndex_negative_throws() {
		assertAll(
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.FOUR.checkCellIndex(-1)),
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.NINE.checkCellIndex(-1)),
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.NINE.checkCellIndex(Integer.MIN_VALUE))
		);
	}
	
	@Test
	void checkCellIndex_atOrAboveCellCount_throws() {
		assertAll(
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.FOUR.checkCellIndex(16)),
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.NINE.checkCellIndex(81)),
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.SIXTEEN.checkCellIndex(256)),
			() -> assertThrows(IndexOutOfBoundsException.class, () -> GridSize.NINE.checkCellIndex(Integer.MAX_VALUE))
		);
	}
	
	@Test
	void checkDigit_inRange_doesNotThrow() {
		for (GridSize size : GridSize.values()) {
			assertAll(
				() -> assertDoesNotThrow(() -> size.checkDigit(1), "1 on " + size),
				() -> assertDoesNotThrow(() -> size.checkDigit(size.n()), "n on " + size)
			);
		}
	}
	
	@Test
	void checkDigit_outOfRange_throws() {
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> GridSize.FOUR.checkDigit(0)),
			() -> assertThrows(IllegalArgumentException.class, () -> GridSize.FOUR.checkDigit(-1)),
			() -> assertThrows(IllegalArgumentException.class, () -> GridSize.FOUR.checkDigit(5)),
			() -> assertThrows(IllegalArgumentException.class, () -> GridSize.NINE.checkDigit(10)),
			() -> assertThrows(IllegalArgumentException.class, () -> GridSize.SIXTEEN.checkDigit(17))
		);
	}
	
	private void assertRoundTrip(GridSize size) {
		int n = size.n();
		for (int cellIndex = 0; cellIndex < size.cellCount(); cellIndex++) {
			int row = size.rowOf(cellIndex);
			int column = size.columnOf(cellIndex);
			int index = cellIndex;
			assertAll(
				() -> assertTrue(row >= 0 && row < n, "row of cell " + index + " on " + size),
				() -> assertTrue(column >= 0 && column < n, "column of cell " + index + " on " + size),
				() -> assertEquals(index / n, row, "row of cell " + index + " on " + size),
				() -> assertEquals(index % n, column, "column of cell " + index + " on " + size),
				() -> assertEquals(index, size.indexOf(row, column), "index of cell " + index + " on " + size)
			);
		}
		for (int row = 0; row < n; row++) {
			for (int column = 0; column < n; column++) {
				int cellIndex = size.indexOf(row, column);
				int expectedRow = row;
				int expectedColumn = column;
				assertAll(
					() -> assertEquals(expectedRow, size.rowOf(cellIndex), "row of (" + expectedRow + "," + expectedColumn + ") on " + size),
					() -> assertEquals(expectedColumn, size.columnOf(cellIndex), "column of (" + expectedRow + "," + expectedColumn + ") on " + size)
				);
			}
		}
	}
}
