package net.luis.sudoku.grid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link Cell}.
 */
class CellTest {
	
	@Test
	void constructor_noArguments_createsEmptyNonGivenCell() {
		Cell cell = new Cell();
		assertAll(
			() -> assertFalse(cell.isGiven()),
			() -> assertTrue(cell.isEmpty()),
			() -> assertEquals(0, cell.value()),
			() -> assertEquals(0, cell.pencilMarks()),
			() -> assertEquals(0, cell.pencilMarkCount()),
			() -> assertArrayEquals(new int[0], cell.pencilMarkDigits())
		);
	}
	
	@Test
	void empty_always_createsEmptyNonGivenCell() {
		Cell cell = Cell.empty();
		assertAll(
			() -> assertFalse(cell.isGiven()),
			() -> assertTrue(cell.isEmpty()),
			() -> assertEquals(0, cell.value()),
			() -> assertEquals(0, cell.pencilMarks())
		);
	}
	
	@Test
	void empty_twoInstances_areIndependent() {
		Cell first = Cell.empty();
		Cell second = Cell.empty();
		first.setValue(4);
		assertAll(
			() -> assertEquals(4, first.value()),
			() -> assertEquals(0, second.value())
		);
	}
	
	@Test
	void given_validDigit_marksGivenAndSetsValue() {
		Cell cell = Cell.given(7);
		assertAll(
			() -> assertTrue(cell.isGiven()),
			() -> assertFalse(cell.isEmpty()),
			() -> assertEquals(7, cell.value()),
			() -> assertEquals(0, cell.pencilMarks())
		);
	}
	
	@Test
	void given_boundaryDigits_accepted() {
		assertAll(
			() -> assertEquals(1, Cell.given(1).value()),
			() -> assertEquals(16, Cell.given(16).value()),
			() -> assertTrue(Cell.given(1).isGiven()),
			() -> assertTrue(Cell.given(16).isGiven())
		);
	}
	
	@Test
	void given_digitZero_throws() {
		assertThrows(IllegalArgumentException.class, () -> Cell.given(0));
	}
	
	@Test
	void given_negativeDigit_throws() {
		assertThrows(IllegalArgumentException.class, () -> Cell.given(-1));
	}
	
	@Test
	void given_digitAboveSixteen_throws() {
		assertThrows(IllegalArgumentException.class, () -> Cell.given(17));
	}
	
	@Test
	void setValue_validDigit_setsValueAndClearsEmptyFlag() {
		Cell cell = Cell.empty();
		cell.setValue(9);
		assertAll(
			() -> assertEquals(9, cell.value()),
			() -> assertFalse(cell.isEmpty()),
			() -> assertFalse(cell.isGiven())
		);
	}
	
	@Test
	void setValue_zero_clearsValue() {
		Cell cell = Cell.empty();
		cell.setValue(9);
		cell.setValue(0);
		assertAll(
			() -> assertEquals(0, cell.value()),
			() -> assertTrue(cell.isEmpty())
		);
	}
	
	@Test
	void setValue_boundaryValues_accepted() {
		Cell cell = Cell.empty();
		cell.setValue(16);
		assertEquals(16, cell.value());
		cell.setValue(1);
		assertEquals(1, cell.value());
		cell.setValue(0);
		assertEquals(0, cell.value());
	}
	
	@Test
	void setValue_onGivenCell_throws() {
		Cell cell = Cell.given(3);
		assertAll(
			() -> assertThrows(IllegalStateException.class, () -> cell.setValue(5)),
			() -> assertThrows(IllegalStateException.class, () -> cell.setValue(0)),
			() -> assertEquals(3, cell.value())
		);
	}
	
	@Test
	void setValue_belowRange_throws() {
		Cell cell = Cell.empty();
		assertThrows(IllegalArgumentException.class, () -> cell.setValue(-1));
	}
	
	@Test
	void setValue_aboveRange_throws() {
		Cell cell = Cell.empty();
		assertThrows(IllegalArgumentException.class, () -> cell.setValue(17));
	}
	
	@Test
	void setValue_withPencilMarks_leavesPencilMarksUntouched() {
		Cell cell = Cell.empty();
		cell.addPencilMark(2);
		cell.addPencilMark(8);
		cell.setValue(5);
		assertAll(
			() -> assertEquals(5, cell.value()),
			() -> assertTrue(cell.hasPencilMark(2)),
			() -> assertTrue(cell.hasPencilMark(8))
		);
	}
	
	@Test
	void clear_withValueAndPencilMarks_clearsBoth() {
		Cell cell = Cell.empty();
		cell.setValue(4);
		cell.addPencilMark(1);
		cell.addPencilMark(9);
		cell.clear();
		assertAll(
			() -> assertEquals(0, cell.value()),
			() -> assertTrue(cell.isEmpty()),
			() -> assertEquals(0, cell.pencilMarks()),
			() -> assertEquals(0, cell.pencilMarkCount()),
			() -> assertArrayEquals(new int[0], cell.pencilMarkDigits())
		);
	}
	
	@Test
	void clear_onAlreadyEmptyCell_staysEmpty() {
		Cell cell = Cell.empty();
		cell.clear();
		assertAll(
			() -> assertTrue(cell.isEmpty()),
			() -> assertEquals(0, cell.pencilMarks())
		);
	}
	
	@Test
	void clear_onGivenCell_throws() {
		Cell cell = Cell.given(6);
		assertAll(
			() -> assertThrows(IllegalStateException.class, cell::clear),
			() -> assertEquals(6, cell.value()),
			() -> assertTrue(cell.isGiven())
		);
	}
	
	@Test
	void addPencilMark_newDigit_returnsTrueAndSetsMark() {
		Cell cell = Cell.empty();
		assertAll(
			() -> assertTrue(cell.addPencilMark(3)),
			() -> assertTrue(cell.hasPencilMark(3)),
			() -> assertEquals(1, cell.pencilMarkCount())
		);
	}
	
	@Test
	void addPencilMark_alreadyPresentDigit_returnsFalse() {
		Cell cell = Cell.empty();
		cell.addPencilMark(3);
		assertAll(
			() -> assertFalse(cell.addPencilMark(3)),
			() -> assertTrue(cell.hasPencilMark(3)),
			() -> assertEquals(1, cell.pencilMarkCount())
		);
	}
	
	@Test
	void addPencilMark_boundaryDigits_accepted() {
		Cell cell = Cell.empty();
		assertAll(
			() -> assertTrue(cell.addPencilMark(1)),
			() -> assertTrue(cell.addPencilMark(16)),
			() -> assertArrayEquals(new int[] { 1, 16 }, cell.pencilMarkDigits())
		);
	}
	
	@Test
	void removePencilMark_presentDigit_returnsTrueAndClearsMark() {
		Cell cell = Cell.empty();
		cell.addPencilMark(5);
		assertAll(
			() -> assertTrue(cell.removePencilMark(5)),
			() -> assertFalse(cell.hasPencilMark(5)),
			() -> assertEquals(0, cell.pencilMarkCount())
		);
	}
	
	@Test
	void removePencilMark_absentDigit_returnsFalse() {
		Cell cell = Cell.empty();
		assertAll(
			() -> assertFalse(cell.removePencilMark(5)),
			() -> assertFalse(cell.hasPencilMark(5))
		);
	}
	
	@Test
	void removePencilMark_repeatedRemoval_returnsFalseSecondTime() {
		Cell cell = Cell.empty();
		cell.addPencilMark(5);
		assertTrue(cell.removePencilMark(5));
		assertFalse(cell.removePencilMark(5));
	}
	
	@Test
	void togglePencilMark_absentDigit_returnsTrueAndSetsMark() {
		Cell cell = Cell.empty();
		assertAll(
			() -> assertTrue(cell.togglePencilMark(4)),
			() -> assertTrue(cell.hasPencilMark(4))
		);
	}
	
	@Test
	void togglePencilMark_presentDigit_returnsFalseAndClearsMark() {
		Cell cell = Cell.empty();
		cell.addPencilMark(4);
		assertAll(
			() -> assertFalse(cell.togglePencilMark(4)),
			() -> assertFalse(cell.hasPencilMark(4))
		);
	}
	
	@Test
	void togglePencilMark_twice_returnsToOriginalState() {
		Cell cell = Cell.empty();
		assertTrue(cell.togglePencilMark(11));
		assertFalse(cell.togglePencilMark(11));
		assertEquals(0, cell.pencilMarks());
	}
	
	@Test
	void hasPencilMark_presentAndAbsentDigits_reportsCorrectly() {
		Cell cell = Cell.empty();
		cell.addPencilMark(2);
		assertAll(
			() -> assertTrue(cell.hasPencilMark(2)),
			() -> assertFalse(cell.hasPencilMark(3))
		);
	}
	
	@Test
	void pencilMarkCount_variousMarkCounts_returnsNumberOfMarks() {
		Cell cell = Cell.empty();
		assertEquals(0, cell.pencilMarkCount());
		cell.addPencilMark(1);
		assertEquals(1, cell.pencilMarkCount());
		cell.addPencilMark(9);
		cell.addPencilMark(16);
		assertEquals(3, cell.pencilMarkCount());
		cell.removePencilMark(9);
		assertEquals(2, cell.pencilMarkCount());
	}
	
	@Test
	void pencilMarkDigits_noMarks_returnsEmptyArray() {
		Cell cell = Cell.empty();
		int[] digits = cell.pencilMarkDigits();
		assertAll(
			() -> assertNotNull(digits),
			() -> assertEquals(0, digits.length)
		);
	}
	
	@Test
	void pencilMarkDigits_marksAddedOutOfOrder_returnsAscendingDigits() {
		Cell cell = Cell.empty();
		cell.addPencilMark(9);
		cell.addPencilMark(1);
		cell.addPencilMark(16);
		cell.addPencilMark(4);
		assertArrayEquals(new int[] { 1, 4, 9, 16 }, cell.pencilMarkDigits());
	}
	
	@Test
	void clearPencilMarks_withMarks_removesAll() {
		Cell cell = Cell.empty();
		cell.setValue(3);
		cell.addPencilMark(1);
		cell.addPencilMark(2);
		cell.clearPencilMarks();
		assertAll(
			() -> assertEquals(0, cell.pencilMarks()),
			() -> assertEquals(0, cell.pencilMarkCount()),
			() -> assertEquals(3, cell.value())
		);
	}
	
	@Test
	void setPencilMarks_validMask_setsExactlyThoseDigits() {
		Cell cell = Cell.empty();
		int mask = 1 << 1 | 1 << 5 | 1 << 16;
		cell.setPencilMarks(mask);
		assertAll(
			() -> assertEquals(mask, cell.pencilMarks()),
			() -> assertEquals(3, cell.pencilMarkCount()),
			() -> assertArrayEquals(new int[] { 1, 5, 16 }, cell.pencilMarkDigits())
		);
	}
	
	@Test
	void setPencilMarks_zeroMask_clearsAllMarks() {
		Cell cell = Cell.empty();
		cell.addPencilMark(7);
		cell.setPencilMarks(0);
		assertAll(
			() -> assertEquals(0, cell.pencilMarks()),
			() -> assertFalse(cell.hasPencilMark(7))
		);
	}
	
	@Test
	void setPencilMarks_bitZeroSet_throws() {
		Cell cell = Cell.empty();
		assertThrows(IllegalArgumentException.class, () -> cell.setPencilMarks(1));
	}
	
	@Test
	void setPencilMarks_bitAboveSixteenSet_throws() {
		Cell cell = Cell.empty();
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> cell.setPencilMarks(1 << 17)),
			() -> assertThrows(IllegalArgumentException.class, () -> cell.setPencilMarks(1 << 20))
		);
	}
	
	@Test
	void pencilMarkMethods_digitZero_throw() {
		Cell cell = Cell.empty();
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> cell.hasPencilMark(0)),
			() -> assertThrows(IllegalArgumentException.class, () -> cell.addPencilMark(0)),
			() -> assertThrows(IllegalArgumentException.class, () -> cell.removePencilMark(0)),
			() -> assertThrows(IllegalArgumentException.class, () -> cell.togglePencilMark(0))
		);
	}
	
	@Test
	void pencilMarkMethods_digitSeventeen_throw() {
		Cell cell = Cell.empty();
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> cell.hasPencilMark(17)),
			() -> assertThrows(IllegalArgumentException.class, () -> cell.addPencilMark(17)),
			() -> assertThrows(IllegalArgumentException.class, () -> cell.removePencilMark(17)),
			() -> assertThrows(IllegalArgumentException.class, () -> cell.togglePencilMark(17))
		);
	}
	
	@Test
	void pencilMarkMethods_negativeDigit_throw() {
		Cell cell = Cell.empty();
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> cell.hasPencilMark(-1)),
			() -> assertThrows(IllegalArgumentException.class, () -> cell.addPencilMark(-1)),
			() -> assertThrows(IllegalArgumentException.class, () -> cell.removePencilMark(-1)),
			() -> assertThrows(IllegalArgumentException.class, () -> cell.togglePencilMark(-1))
		);
	}
	
	@Test
	void addPencilMark_onGivenCell_throws() {
		Cell cell = Cell.given(2);
		assertAll(
			() -> assertThrows(IllegalStateException.class, () -> cell.addPencilMark(5)),
			() -> assertEquals(0, cell.pencilMarks())
		);
	}
	
	@Test
	void togglePencilMark_onGivenCell_throws() {
		Cell cell = Cell.given(2);
		assertAll(
			() -> assertThrows(IllegalStateException.class, () -> cell.togglePencilMark(5)),
			() -> assertEquals(0, cell.pencilMarks())
		);
	}
	
	@Test
	void setPencilMarks_onGivenCell_throws() {
		Cell cell = Cell.given(2);
		assertAll(
			() -> assertThrows(IllegalStateException.class, () -> cell.setPencilMarks(1 << 3)),
			() -> assertEquals(0, cell.pencilMarks())
		);
	}
	
	@Test
	void removePencilMark_onGivenCell_isSilentNoOp() {
		Cell cell = Cell.given(2);
		assertAll(
			() -> assertFalse(cell.removePencilMark(5)),
			() -> assertEquals(0, cell.pencilMarks()),
			() -> assertTrue(cell.isGiven()),
			() -> assertEquals(2, cell.value())
		);
	}
	
	@Test
	void clearPencilMarks_onGivenCell_isSilentNoOp() {
		Cell cell = Cell.given(2);
		assertDoesNotThrow(cell::clearPencilMarks);
		assertAll(
			() -> assertEquals(0, cell.pencilMarks()),
			() -> assertTrue(cell.isGiven()),
			() -> assertEquals(2, cell.value())
		);
	}
	
	@Test
	void copy_ofPlainCell_isEqualToOriginal() {
		Cell cell = Cell.empty();
		cell.setValue(6);
		cell.addPencilMark(1);
		cell.addPencilMark(12);
		Cell copy = cell.copy();
		assertAll(
			() -> assertNotSame(cell, copy),
			() -> assertEquals(cell, copy),
			() -> assertEquals(cell.hashCode(), copy.hashCode()),
			() -> assertEquals(6, copy.value()),
			() -> assertArrayEquals(new int[] { 1, 12 }, copy.pencilMarkDigits())
		);
	}
	
	@Test
	void copy_mutatingCopy_leavesOriginalUnchanged() {
		Cell cell = Cell.empty();
		cell.setValue(6);
		cell.addPencilMark(1);
		Cell copy = cell.copy();
		copy.setValue(9);
		copy.addPencilMark(4);
		copy.removePencilMark(1);
		assertAll(
			() -> assertEquals(6, cell.value()),
			() -> assertTrue(cell.hasPencilMark(1)),
			() -> assertFalse(cell.hasPencilMark(4))
		);
	}
	
	@Test
	void copy_mutatingOriginal_leavesCopyUnchanged() {
		Cell cell = Cell.empty();
		cell.setValue(6);
		cell.addPencilMark(1);
		Cell copy = cell.copy();
		cell.setValue(9);
		cell.addPencilMark(4);
		cell.removePencilMark(1);
		assertAll(
			() -> assertEquals(6, copy.value()),
			() -> assertTrue(copy.hasPencilMark(1)),
			() -> assertFalse(copy.hasPencilMark(4))
		);
	}
	
	@Test
	void copy_ofGivenCell_preservesGivenFlag() {
		Cell cell = Cell.given(8);
		Cell copy = cell.copy();
		assertAll(
			() -> assertTrue(copy.isGiven()),
			() -> assertEquals(8, copy.value()),
			() -> assertEquals(cell, copy),
			() -> assertThrows(IllegalStateException.class, () -> copy.setValue(1))
		);
	}
	
	@Test
	void restoreFrom_nonGivenSource_copiesValueAndPencilMarks() {
		Cell source = Cell.empty();
		source.setValue(5);
		source.addPencilMark(2);
		source.addPencilMark(7);
		Cell target = Cell.empty();
		target.setValue(1);
		target.addPencilMark(13);
		target.restoreFrom(source);
		assertAll(
			() -> assertFalse(target.isGiven()),
			() -> assertEquals(5, target.value()),
			() -> assertArrayEquals(new int[] { 2, 7 }, target.pencilMarkDigits()),
			() -> assertEquals(source, target)
		);
	}
	
	@Test
	void restoreFrom_givenSourceOntoNonGiven_copiesGivenFlag() {
		Cell source = Cell.given(7);
		Cell target = Cell.empty();
		target.setValue(2);
		target.addPencilMark(5);
		target.restoreFrom(source);
		assertAll(
			() -> assertTrue(target.isGiven()),
			() -> assertEquals(7, target.value()),
			() -> assertEquals(0, target.pencilMarks()),
			() -> assertEquals(source, target),
			() -> assertThrows(IllegalStateException.class, () -> target.setValue(3))
		);
	}
	
	@Test
	void restoreFrom_nonGivenSourceOntoGiven_clearsGivenFlag() {
		Cell source = Cell.empty();
		source.setValue(2);
		source.addPencilMark(5);
		Cell target = Cell.given(7);
		target.restoreFrom(source);
		assertAll(
			() -> assertFalse(target.isGiven()),
			() -> assertEquals(2, target.value()),
			() -> assertTrue(target.hasPencilMark(5)),
			() -> assertEquals(source, target),
			() -> assertDoesNotThrow(() -> target.setValue(3))
		);
	}
	
	@Test
	void restoreFrom_source_isNotAliased() {
		Cell source = Cell.empty();
		source.setValue(5);
		source.addPencilMark(2);
		Cell target = Cell.empty();
		target.restoreFrom(source);
		target.setValue(9);
		target.addPencilMark(11);
		assertAll(
			() -> assertEquals(5, source.value()),
			() -> assertFalse(source.hasPencilMark(11))
		);
	}
	
	@Test
	void restoreFrom_afterArbitraryMutation_restoresFullSnapshot() {
		Cell cell = Cell.empty();
		cell.setValue(4);
		cell.addPencilMark(1);
		cell.addPencilMark(6);
		cell.addPencilMark(16);
		Cell snapshot = cell.copy();
		
		cell.setValue(0);
		cell.clearPencilMarks();
		cell.addPencilMark(9);
		cell.setValue(12);
		cell.clear();
		
		cell.restoreFrom(snapshot);
		assertAll(
			() -> assertEquals(snapshot, cell),
			() -> assertEquals(snapshot.hashCode(), cell.hashCode()),
			() -> assertEquals(4, cell.value()),
			() -> assertFalse(cell.isGiven()),
			() -> assertArrayEquals(new int[] { 1, 6, 16 }, cell.pencilMarkDigits())
		);
	}
	
	@Test
	void restoreFrom_givenSnapshotAfterFlagFlip_restoresGivenState() {
		Cell cell = Cell.given(3);
		Cell snapshot = cell.copy();
		
		Cell replacement = Cell.empty();
		replacement.setValue(8);
		replacement.addPencilMark(2);
		cell.restoreFrom(replacement);
		assertFalse(cell.isGiven());
		
		cell.restoreFrom(snapshot);
		assertAll(
			() -> assertEquals(snapshot, cell),
			() -> assertTrue(cell.isGiven()),
			() -> assertEquals(3, cell.value()),
			() -> assertEquals(0, cell.pencilMarks())
		);
	}
	
	@Test
	void equals_sameThreeLayers_areEqual() {
		Cell first = Cell.empty();
		first.setValue(5);
		first.addPencilMark(2);
		first.addPencilMark(9);
		Cell second = Cell.empty();
		second.setValue(5);
		second.addPencilMark(9);
		second.addPencilMark(2);
		assertAll(
			() -> assertEquals(first, second),
			() -> assertEquals(second, first),
			() -> assertEquals(first.hashCode(), second.hashCode())
		);
	}
	
	@Test
	void equals_sameInstance_isEqual() {
		Cell cell = Cell.given(4);
		assertEquals(cell, cell);
	}
	
	@Test
	void equals_givenCellsWithSameValue_areEqual() {
		assertAll(
			() -> assertEquals(Cell.given(4), Cell.given(4)),
			() -> assertEquals(Cell.given(4).hashCode(), Cell.given(4).hashCode())
		);
	}
	
	@Test
	void equals_differingGivenFlag_areNotEqual() {
		Cell given = Cell.given(4);
		Cell pen = Cell.empty();
		pen.setValue(4);
		assertAll(
			() -> assertNotEquals(given, pen),
			() -> assertNotEquals(pen, given)
		);
	}
	
	@Test
	void equals_differingValue_areNotEqual() {
		Cell first = Cell.empty();
		first.setValue(3);
		Cell second = Cell.empty();
		second.setValue(4);
		assertNotEquals(first, second);
	}
	
	@Test
	void equals_differingPencilMarks_areNotEqual() {
		Cell first = Cell.empty();
		first.addPencilMark(3);
		Cell second = Cell.empty();
		second.addPencilMark(4);
		assertAll(
			() -> assertNotEquals(first, second),
			() -> assertNotEquals(Cell.empty(), first)
		);
	}
	
	@Test
	void equals_nullOrOtherType_isFalse() {
		Cell cell = Cell.empty();
		assertAll(
			() -> assertNotEquals(null, cell),
			() -> assertNotEquals("cell", cell)
		);
	}
	
	@Test
	void toString_anyCell_isNotNull() {
		assertAll(
			() -> assertNotNull(Cell.empty().toString()),
			() -> assertNotNull(Cell.given(5).toString())
		);
	}
}
