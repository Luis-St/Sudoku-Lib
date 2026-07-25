package net.luis.sudoku.grid;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link Puzzle}.
 */
class PuzzleTest {
	
	private static final RegionPartition FOUR_CLASSIC = ClassicRegionPartition.of(GridSize.FOUR);
	private static final RegionPartition NINE_CLASSIC = ClassicRegionPartition.of(GridSize.NINE);
	
	/**
	 * A hand-verified solved 4x4 grid, row-major:
	 * <pre>
	 * 1 2 | 3 4
	 * 3 4 | 1 2
	 * ----+----
	 * 2 1 | 4 3
	 * 4 3 | 2 1
	 * </pre>
	 */
	private static final int[] SOLVED_FOUR = {
		1, 2, 3, 4,
		3, 4, 1, 2,
		2, 1, 4, 3,
		4, 3, 2, 1
	};
	
	/**
	 * A full 4x4 grid whose last row is {@code 4 3 1 2}: every row and every box holds 1..4, but column 2
	 * holds {@code 3 1 4 1} and column 3 holds {@code 4 2 3 2}, so the grid is complete yet invalid.
	 */
	private static final int[] COMPLETE_INVALID_FOUR = {
		1, 2, 3, 4,
		3, 4, 1, 2,
		2, 1, 4, 3,
		4, 3, 1, 2
	};
	
	private static Cell[] emptyCells(int count) {
		Cell[] cells = new Cell[count];
		for (int i = 0; i < count; i++) {
			cells[i] = Cell.empty();
		}
		return cells;
	}
	
	private static Puzzle emptyFour() {
		return Puzzle.empty(GridSize.FOUR, Variant.CLASSIC, FOUR_CLASSIC);
	}
	
	private static Puzzle penFilledFour(int[] values) {
		Puzzle puzzle = emptyFour();
		for (int cellIndex = 0; cellIndex < values.length; cellIndex++) {
			if (values[cellIndex] != 0) {
				puzzle.setValue(cellIndex, values[cellIndex]);
			}
		}
		return puzzle;
	}
	
	private static RegionPartition rowPartitionOfFour() {
		List<Region> regions = new ArrayList<>(4);
		regions.add(new Region(0, 1, 2, 3));
		regions.add(new Region(4, 5, 6, 7));
		regions.add(new Region(8, 9, 10, 11));
		regions.add(new Region(12, 13, 14, 15));
		return new RegionPartition(GridSize.FOUR, regions);
	}
	
	@Test
	void constructor_validArguments_createsPuzzle() {
		Cell[] cells = emptyCells(16);
		cells[0] = Cell.given(1);
		Puzzle puzzle = new Puzzle(GridSize.FOUR, Variant.CLASSIC, FOUR_CLASSIC, cells);
		assertAll(
			() -> assertEquals(GridSize.FOUR, puzzle.size()),
			() -> assertEquals(Variant.CLASSIC, puzzle.variant()),
			() -> assertEquals(FOUR_CLASSIC, puzzle.partition()),
			() -> assertTrue(puzzle.cell(0).isGiven()),
			() -> assertEquals(1, puzzle.valueAt(0)),
			() -> assertFalse(puzzle.cell(1).isGiven())
		);
	}
	
	@Test
	void constructor_givenArray_isCopiedButCellsAreAdopted() {
		Cell[] cells = emptyCells(16);
		Puzzle puzzle = new Puzzle(GridSize.FOUR, Variant.CLASSIC, FOUR_CLASSIC, cells);
		Cell adopted = cells[1];
		cells[0] = Cell.given(3);
		adopted.setValue(2);
		assertAll(
			() -> assertFalse(puzzle.cell(0).isGiven()),
			() -> assertEquals(0, puzzle.valueAt(0)),
			() -> assertSame(adopted, puzzle.cell(1)),
			() -> assertEquals(2, puzzle.valueAt(1))
		);
	}
	
	@Test
	void constructor_partitionSizeMismatch_throws() {
		Cell[] cells = emptyCells(16);
		assertThrows(IllegalArgumentException.class, () -> new Puzzle(GridSize.FOUR, Variant.CLASSIC, NINE_CLASSIC, cells));
	}
	
	@Test
	void constructor_chaosAtFour_throws() {
		Cell[] cells = emptyCells(16);
		assertThrows(IllegalArgumentException.class, () -> new Puzzle(GridSize.FOUR, Variant.CHAOS, FOUR_CLASSIC, cells));
	}
	
	@Test
	void constructor_wrongCellCount_throws() {
		Cell[] tooFew = emptyCells(15);
		Cell[] tooMany = emptyCells(17);
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> new Puzzle(GridSize.FOUR, Variant.CLASSIC, FOUR_CLASSIC, tooFew)),
			() -> assertThrows(IllegalArgumentException.class, () -> new Puzzle(GridSize.FOUR, Variant.CLASSIC, FOUR_CLASSIC, tooMany))
		);
	}
	
	@Test
	void constructor_nullCell_throws() {
		Cell[] cells = emptyCells(16);
		cells[7] = null;
		assertThrows(NullPointerException.class, () -> new Puzzle(GridSize.FOUR, Variant.CLASSIC, FOUR_CLASSIC, cells));
	}
	
	@Test
	void empty_validArguments_createsAllEmptyNonGivenCells() {
		Puzzle puzzle = emptyFour();
		assertAll(
			() -> assertEquals(GridSize.FOUR, puzzle.size()),
			() -> assertEquals(Variant.CLASSIC, puzzle.variant()),
			() -> assertEquals(FOUR_CLASSIC, puzzle.partition()),
			() -> assertArrayEquals(new int[16], puzzle.values()),
			() -> assertFalse(puzzle.isComplete())
		);
		for (int cellIndex = 0; cellIndex < 16; cellIndex++) {
			Cell cell = puzzle.cell(cellIndex);
			assertTrue(cell.isEmpty());
			assertFalse(cell.isGiven());
		}
	}
	
	@Test
	void empty_distinctCellInstancesPerPosition() {
		Puzzle puzzle = emptyFour();
		puzzle.setValue(0, 1);
		assertAll(
			() -> assertEquals(1, puzzle.valueAt(0)),
			() -> assertEquals(0, puzzle.valueAt(1)),
			() -> assertNotSame(puzzle.cell(0), puzzle.cell(1))
		);
	}
	
	@Test
	void empty_chaosAtFour_throws() {
		assertThrows(IllegalArgumentException.class, () -> Puzzle.empty(GridSize.FOUR, Variant.CHAOS, FOUR_CLASSIC));
	}
	
	@Test
	void empty_partitionSizeMismatch_throws() {
		assertThrows(IllegalArgumentException.class, () -> Puzzle.empty(GridSize.FOUR, Variant.CLASSIC, NINE_CLASSIC));
	}
	
	@Test
	void ofGivens_validGivens_marksNonZeroCellsAsGiven() {
		int[] givens = {
			1, 0, 0, 0,
			0, 2, 0, 0,
			0, 0, 3, 0,
			0, 0, 0, 4
		};
		Puzzle puzzle = Puzzle.ofGivens(GridSize.FOUR, Variant.CLASSIC, FOUR_CLASSIC, givens);
		assertAll(
			() -> assertTrue(puzzle.cell(0).isGiven()),
			() -> assertEquals(1, puzzle.valueAt(0)),
			() -> assertTrue(puzzle.cell(5).isGiven()),
			() -> assertEquals(2, puzzle.valueAt(5)),
			() -> assertTrue(puzzle.cell(10).isGiven()),
			() -> assertTrue(puzzle.cell(15).isGiven()),
			() -> assertFalse(puzzle.cell(1).isGiven()),
			() -> assertTrue(puzzle.cell(1).isEmpty()),
			() -> assertArrayEquals(givens, puzzle.values())
		);
	}
	
	@Test
	void ofGivens_allZero_createsEmptyPuzzle() {
		Puzzle puzzle = Puzzle.ofGivens(GridSize.FOUR, Variant.CLASSIC, FOUR_CLASSIC, new int[16]);
		assertAll(
			() -> assertArrayEquals(new int[16], puzzle.values()),
			() -> assertFalse(puzzle.cell(0).isGiven()),
			() -> assertEquals(emptyFour(), puzzle)
		);
	}
	
	@Test
	void ofGivens_wrongLength_throws() {
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> Puzzle.ofGivens(GridSize.FOUR, Variant.CLASSIC, FOUR_CLASSIC, new int[15])),
			() -> assertThrows(IllegalArgumentException.class, () -> Puzzle.ofGivens(GridSize.FOUR, Variant.CLASSIC, FOUR_CLASSIC, new int[17]))
		);
	}
	
	@Test
	void ofGivens_valueAboveGridSize_throws() {
		int[] givens = new int[16];
		givens[4] = 5;
		assertThrows(IllegalArgumentException.class, () -> Puzzle.ofGivens(GridSize.FOUR, Variant.CLASSIC, FOUR_CLASSIC, givens));
	}
	
	@Test
	void ofGivens_negativeValue_throws() {
		int[] givens = new int[16];
		givens[4] = -1;
		assertThrows(IllegalArgumentException.class, () -> Puzzle.ofGivens(GridSize.FOUR, Variant.CLASSIC, FOUR_CLASSIC, givens));
	}
	
	@Test
	void ofGivens_chaosAtFour_throws() {
		assertThrows(IllegalArgumentException.class, () -> Puzzle.ofGivens(GridSize.FOUR, Variant.CHAOS, FOUR_CLASSIC, new int[16]));
	}
	
	@Test
	void ofGivens_partitionSizeMismatch_throws() {
		assertThrows(IllegalArgumentException.class, () -> Puzzle.ofGivens(GridSize.FOUR, Variant.CLASSIC, NINE_CLASSIC, new int[16]));
	}
	
	@Test
	void classicOfGivens_validGivens_usesClassicVariantAndPartition() {
		Puzzle puzzle = Puzzle.classicOfGivens(GridSize.FOUR, SOLVED_FOUR);
		assertAll(
			() -> assertEquals(GridSize.FOUR, puzzle.size()),
			() -> assertEquals(Variant.CLASSIC, puzzle.variant()),
			() -> assertEquals(FOUR_CLASSIC, puzzle.partition()),
			() -> assertArrayEquals(SOLVED_FOUR, puzzle.values()),
			() -> assertTrue(puzzle.cell(0).isGiven())
		);
	}
	
	@Test
	void classicOfGivens_wrongLength_throws() {
		assertThrows(IllegalArgumentException.class, () -> Puzzle.classicOfGivens(GridSize.FOUR, new int[9]));
	}
	
	@Test
	void classicOfGivens_valueAboveGridSize_throws() {
		int[] givens = new int[16];
		givens[0] = 9;
		assertThrows(IllegalArgumentException.class, () -> Puzzle.classicOfGivens(GridSize.FOUR, givens));
	}
	
	@Test
	void rowCells_everyRowOfFour_returnsAscendingIndices() {
		Puzzle puzzle = emptyFour();
		assertAll(
			() -> assertArrayEquals(new int[] { 0, 1, 2, 3 }, puzzle.rowCells(0)),
			() -> assertArrayEquals(new int[] { 4, 5, 6, 7 }, puzzle.rowCells(1)),
			() -> assertArrayEquals(new int[] { 8, 9, 10, 11 }, puzzle.rowCells(2)),
			() -> assertArrayEquals(new int[] { 12, 13, 14, 15 }, puzzle.rowCells(3))
		);
	}
	
	@Test
	void columnCells_everyColumnOfFour_returnsAscendingIndices() {
		Puzzle puzzle = emptyFour();
		assertAll(
			() -> assertArrayEquals(new int[] { 0, 4, 8, 12 }, puzzle.columnCells(0)),
			() -> assertArrayEquals(new int[] { 1, 5, 9, 13 }, puzzle.columnCells(1)),
			() -> assertArrayEquals(new int[] { 2, 6, 10, 14 }, puzzle.columnCells(2)),
			() -> assertArrayEquals(new int[] { 3, 7, 11, 15 }, puzzle.columnCells(3))
		);
	}
	
	@Test
	void regionCells_everyRegionOfFour_returnsAscendingIndices() {
		Puzzle puzzle = emptyFour();
		assertAll(
			() -> assertArrayEquals(new int[] { 0, 1, 4, 5 }, puzzle.regionCells(0)),
			() -> assertArrayEquals(new int[] { 2, 3, 6, 7 }, puzzle.regionCells(1)),
			() -> assertArrayEquals(new int[] { 8, 9, 12, 13 }, puzzle.regionCells(2)),
			() -> assertArrayEquals(new int[] { 10, 11, 14, 15 }, puzzle.regionCells(3))
		);
	}
	
	@Test
	void regionCells_customRowPartition_followsThePartition() {
		Puzzle puzzle = Puzzle.empty(GridSize.FOUR, Variant.CLASSIC, rowPartitionOfFour());
		assertAll(
			() -> assertArrayEquals(new int[] { 0, 1, 2, 3 }, puzzle.regionCells(0)),
			() -> assertArrayEquals(new int[] { 4, 5, 6, 7 }, puzzle.regionCells(1)),
			() -> assertArrayEquals(new int[] { 8, 9, 10, 11 }, puzzle.regionCells(2)),
			() -> assertArrayEquals(new int[] { 12, 13, 14, 15 }, puzzle.regionCells(3))
		);
	}
	
	@Test
	void cell_rowAndColumn_agreesWithCellIndexForEveryCell() {
		Puzzle puzzle = Puzzle.classicOfGivens(GridSize.FOUR, SOLVED_FOUR);
		for (int row = 0; row < 4; row++) {
			for (int column = 0; column < 4; column++) {
				int cellIndex = row * 4 + column;
				assertSame(puzzle.cell(cellIndex), puzzle.cell(row, column));
			}
		}
	}
	
	@Test
	void valueAt_bothForms_agreeForEveryCell() {
		Puzzle puzzle = Puzzle.classicOfGivens(GridSize.FOUR, SOLVED_FOUR);
		for (int row = 0; row < 4; row++) {
			for (int column = 0; column < 4; column++) {
				int cellIndex = row * 4 + column;
				assertEquals(SOLVED_FOUR[cellIndex], puzzle.valueAt(cellIndex));
				assertEquals(SOLVED_FOUR[cellIndex], puzzle.valueAt(row, column));
			}
		}
	}
	
	@Test
	void valueAt_emptyCell_returnsZero() {
		Puzzle puzzle = emptyFour();
		assertAll(
			() -> assertEquals(0, puzzle.valueAt(0)),
			() -> assertEquals(0, puzzle.valueAt(3, 3))
		);
	}
	
	@Test
	void setValue_validDigit_setsCellValue() {
		Puzzle puzzle = emptyFour();
		puzzle.setValue(6, 3);
		assertAll(
			() -> assertEquals(3, puzzle.valueAt(6)),
			() -> assertEquals(3, puzzle.valueAt(1, 2)),
			() -> assertFalse(puzzle.cell(6).isGiven())
		);
	}
	
	@Test
	void setValue_zero_clearsCellValue() {
		Puzzle puzzle = emptyFour();
		puzzle.setValue(6, 3);
		puzzle.setValue(6, 0);
		assertAll(
			() -> assertEquals(0, puzzle.valueAt(6)),
			() -> assertTrue(puzzle.cell(6).isEmpty())
		);
	}
	
	@Test
	void setValue_onGivenCell_throws() {
		int[] givens = new int[16];
		givens[2] = 3;
		Puzzle puzzle = Puzzle.classicOfGivens(GridSize.FOUR, givens);
		assertAll(
			() -> assertThrows(IllegalStateException.class, () -> puzzle.setValue(2, 1)),
			() -> assertThrows(IllegalStateException.class, () -> puzzle.setValue(2, 0)),
			() -> assertEquals(3, puzzle.valueAt(2))
		);
	}
	
	@Test
	void setValue_digitAboveGridSize_throws() {
		Puzzle puzzle = emptyFour();
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> puzzle.setValue(0, 5)),
			() -> assertThrows(IllegalArgumentException.class, () -> puzzle.setValue(0, 17))
		);
	}
	
	@Test
	void setValue_negativeDigit_throws() {
		Puzzle puzzle = emptyFour();
		assertThrows(IllegalArgumentException.class, () -> puzzle.setValue(0, -1));
	}
	
	@Test
	void setValue_invalidCellIndex_throws() {
		Puzzle puzzle = emptyFour();
		assertAll(
			() -> assertThrows(IndexOutOfBoundsException.class, () -> puzzle.setValue(-1, 1)),
			() -> assertThrows(IndexOutOfBoundsException.class, () -> puzzle.setValue(16, 1))
		);
	}
	
	@Test
	void values_filledPuzzle_returnsRowMajorSnapshot() {
		Puzzle puzzle = penFilledFour(SOLVED_FOUR);
		assertArrayEquals(SOLVED_FOUR, puzzle.values());
	}
	
	@Test
	void values_mutatingResult_doesNotAffectPuzzle() {
		Puzzle puzzle = penFilledFour(SOLVED_FOUR);
		int[] values = puzzle.values();
		values[0] = 4;
		values[15] = 0;
		assertAll(
			() -> assertEquals(1, puzzle.valueAt(0)),
			() -> assertEquals(1, puzzle.valueAt(15)),
			() -> assertArrayEquals(SOLVED_FOUR, puzzle.values())
		);
	}
	
	@Test
	void values_calledTwice_returnsDistinctArrays() {
		Puzzle puzzle = emptyFour();
		assertNotSame(puzzle.values(), puzzle.values());
	}
	
	@Test
	void isComplete_emptyGrid_false() {
		assertFalse(emptyFour().isComplete());
	}
	
	@Test
	void isComplete_partiallyFilledGrid_false() {
		Puzzle puzzle = emptyFour();
		puzzle.setValue(0, 1);
		puzzle.setValue(1, 2);
		assertFalse(puzzle.isComplete());
	}
	
	@Test
	void isComplete_oneCellMissing_false() {
		int[] values = SOLVED_FOUR.clone();
		values[15] = 0;
		assertFalse(penFilledFour(values).isComplete());
	}
	
	@Test
	void isComplete_fullGrid_true() {
		assertAll(
			() -> assertTrue(penFilledFour(SOLVED_FOUR).isComplete()),
			() -> assertTrue(penFilledFour(COMPLETE_INVALID_FOUR).isComplete())
		);
	}
	
	@Test
	void hasConflictAt_emptyCell_false() {
		Puzzle puzzle = emptyFour();
		puzzle.setValue(0, 1);
		puzzle.setValue(3, 1);
		assertAll(
			() -> assertFalse(puzzle.hasConflictAt(1)),
			() -> assertFalse(puzzle.hasConflictAt(15))
		);
	}
	
	@Test
	void hasConflictAt_uniqueValue_false() {
		Puzzle puzzle = emptyFour();
		puzzle.setValue(0, 1);
		puzzle.setValue(6, 1);
		assertAll(
			() -> assertFalse(puzzle.hasConflictAt(0)),
			() -> assertFalse(puzzle.hasConflictAt(6))
		);
	}
	
	@Test
	void hasConflictAt_rowDuplicate_true() {
		Puzzle puzzle = emptyFour();
		puzzle.setValue(0, 1);
		puzzle.setValue(3, 1);
		assertAll(
			() -> assertTrue(puzzle.hasConflictAt(0)),
			() -> assertTrue(puzzle.hasConflictAt(3))
		);
	}
	
	@Test
	void hasConflictAt_columnDuplicate_true() {
		Puzzle puzzle = emptyFour();
		puzzle.setValue(0, 1);
		puzzle.setValue(12, 1);
		assertAll(
			() -> assertTrue(puzzle.hasConflictAt(0)),
			() -> assertTrue(puzzle.hasConflictAt(12))
		);
	}
	
	@Test
	void hasConflictAt_regionDuplicate_true() {
		Puzzle puzzle = emptyFour();
		puzzle.setValue(0, 1);
		puzzle.setValue(5, 1);
		assertAll(
			() -> assertTrue(puzzle.hasConflictAt(0)),
			() -> assertTrue(puzzle.hasConflictAt(5))
		);
	}
	
	@Test
	void conflictingCells_rowDuplicate_returnsBothIndicesAscending() {
		Puzzle puzzle = emptyFour();
		puzzle.setValue(3, 1);
		puzzle.setValue(0, 1);
		assertArrayEquals(new int[] { 0, 3 }, puzzle.conflictingCells());
	}
	
	@Test
	void conflictingCells_columnDuplicate_returnsBothIndicesAscending() {
		Puzzle puzzle = emptyFour();
		puzzle.setValue(12, 2);
		puzzle.setValue(0, 2);
		assertArrayEquals(new int[] { 0, 12 }, puzzle.conflictingCells());
	}
	
	@Test
	void conflictingCells_regionDuplicate_returnsBothIndicesAscending() {
		Puzzle puzzle = emptyFour();
		puzzle.setValue(5, 3);
		puzzle.setValue(0, 3);
		assertArrayEquals(new int[] { 0, 5 }, puzzle.conflictingCells());
	}
	
	@Test
	void conflictingCells_twoGivenCells_returnsBothIndicesAscending() {
		int[] givens = new int[16];
		givens[0] = 1;
		givens[12] = 1;
		Puzzle puzzle = Puzzle.classicOfGivens(GridSize.FOUR, givens);
		assertAll(
			() -> assertTrue(puzzle.cell(0).isGiven()),
			() -> assertTrue(puzzle.cell(12).isGiven()),
			() -> assertTrue(puzzle.hasConflictAt(0)),
			() -> assertTrue(puzzle.hasConflictAt(12)),
			() -> assertArrayEquals(new int[] { 0, 12 }, puzzle.conflictingCells()),
			() -> assertFalse(puzzle.isValid())
		);
	}
	
	@Test
	void conflictingCells_givenAndPenValue_returnsBothIndicesAscending() {
		int[] givens = new int[16];
		givens[0] = 1;
		Puzzle puzzle = Puzzle.classicOfGivens(GridSize.FOUR, givens);
		puzzle.setValue(3, 1);
		assertAll(
			() -> assertTrue(puzzle.cell(0).isGiven()),
			() -> assertFalse(puzzle.cell(3).isGiven()),
			() -> assertArrayEquals(new int[] { 0, 3 }, puzzle.conflictingCells()),
			() -> assertFalse(puzzle.isValid())
		);
	}
	
	@Test
	void conflictingCells_cleanGrid_returnsEmptyArray() {
		Puzzle puzzle = emptyFour();
		puzzle.setValue(0, 1);
		puzzle.setValue(5, 2);
		puzzle.setValue(10, 3);
		puzzle.setValue(15, 4);
		int[] conflicts = puzzle.conflictingCells();
		assertAll(
			() -> assertNotNull(conflicts),
			() -> assertEquals(0, conflicts.length)
		);
	}
	
	@Test
	void conflictingCells_emptyGrid_returnsEmptyArray() {
		assertArrayEquals(new int[0], emptyFour().conflictingCells());
	}
	
	@Test
	void conflictingCells_solvedGrid_returnsEmptyArray() {
		assertArrayEquals(new int[0], penFilledFour(SOLVED_FOUR).conflictingCells());
	}
	
	@Test
	void conflictingCells_severalConflicts_returnsAllIndicesAscending() {
		Puzzle puzzle = emptyFour();
		puzzle.setValue(0, 1);
		puzzle.setValue(3, 1);
		puzzle.setValue(9, 2);
		puzzle.setValue(13, 2);
		assertArrayEquals(new int[] { 0, 3, 9, 13 }, puzzle.conflictingCells());
	}
	
	@Test
	void isValid_emptyGrid_true() {
		assertTrue(emptyFour().isValid());
	}
	
	@Test
	void isValid_conflictFreePartialGrid_true() {
		Puzzle puzzle = emptyFour();
		puzzle.setValue(0, 1);
		puzzle.setValue(5, 2);
		assertTrue(puzzle.isValid());
	}
	
	@Test
	void isValid_conflictingGrid_false() {
		Puzzle puzzle = emptyFour();
		puzzle.setValue(0, 1);
		puzzle.setValue(1, 1);
		assertFalse(puzzle.isValid());
	}
	
	@Test
	void isValid_completeButInvalidGrid_false() {
		assertFalse(penFilledFour(COMPLETE_INVALID_FOUR).isValid());
	}
	
	@Test
	void isValid_solvedGrid_true() {
		assertTrue(penFilledFour(SOLVED_FOUR).isValid());
	}
	
	@Test
	void isSolved_emptyGrid_false() {
		Puzzle puzzle = emptyFour();
		assertAll(
			() -> assertTrue(puzzle.isValid()),
			() -> assertFalse(puzzle.isComplete()),
			() -> assertFalse(puzzle.isSolved())
		);
	}
	
	@Test
	void isSolved_partiallyFilledGrid_false() {
		int[] values = SOLVED_FOUR.clone();
		values[15] = 0;
		Puzzle puzzle = penFilledFour(values);
		assertAll(
			() -> assertTrue(puzzle.isValid()),
			() -> assertFalse(puzzle.isComplete()),
			() -> assertFalse(puzzle.isSolved())
		);
	}
	
	@Test
	void isSolved_completeButInvalidGrid_false() {
		Puzzle puzzle = penFilledFour(COMPLETE_INVALID_FOUR);
		assertAll(
			() -> assertTrue(puzzle.isComplete()),
			() -> assertFalse(puzzle.isValid()),
			() -> assertFalse(puzzle.isSolved())
		);
	}
	
	@Test
	void isSolved_completeAndValidGrid_true() {
		Puzzle puzzle = penFilledFour(SOLVED_FOUR);
		assertAll(
			() -> assertTrue(puzzle.isComplete()),
			() -> assertTrue(puzzle.isValid()),
			() -> assertTrue(puzzle.isSolved())
		);
	}
	
	@Test
	void isSolved_solvedGridFromGivens_true() {
		assertTrue(Puzzle.classicOfGivens(GridSize.FOUR, SOLVED_FOUR).isSolved());
	}
	
	@Test
	void copy_ofPuzzle_isEqualButNotSame() {
		int[] givens = new int[16];
		givens[0] = 1;
		Puzzle puzzle = Puzzle.classicOfGivens(GridSize.FOUR, givens);
		puzzle.setValue(5, 2);
		puzzle.cell(6).addPencilMark(3);
		Puzzle copy = puzzle.copy();
		assertAll(
			() -> assertNotSame(puzzle, copy),
			() -> assertEquals(puzzle, copy),
			() -> assertEquals(puzzle.hashCode(), copy.hashCode()),
			() -> assertArrayEquals(puzzle.values(), copy.values()),
			() -> assertTrue(copy.cell(0).isGiven()),
			() -> assertTrue(copy.cell(6).hasPencilMark(3))
		);
	}
	
	@Test
	void copy_mutatingCopy_doesNotAffectOriginal() {
		Puzzle puzzle = emptyFour();
		puzzle.setValue(0, 1);
		Puzzle copy = puzzle.copy();
		copy.setValue(0, 4);
		copy.setValue(7, 2);
		copy.cell(9).addPencilMark(3);
		assertAll(
			() -> assertNotSame(puzzle.cell(0), copy.cell(0)),
			() -> assertEquals(1, puzzle.valueAt(0)),
			() -> assertEquals(0, puzzle.valueAt(7)),
			() -> assertFalse(puzzle.cell(9).hasPencilMark(3))
		);
	}
	
	@Test
	void copy_mutatingOriginal_doesNotAffectCopy() {
		Puzzle puzzle = emptyFour();
		puzzle.setValue(0, 1);
		Puzzle copy = puzzle.copy();
		puzzle.setValue(0, 4);
		puzzle.setValue(7, 2);
		puzzle.cell(9).addPencilMark(3);
		assertAll(
			() -> assertEquals(1, copy.valueAt(0)),
			() -> assertEquals(0, copy.valueAt(7)),
			() -> assertFalse(copy.cell(9).hasPencilMark(3))
		);
	}
	
	@Test
	void equals_sameState_areEqual() {
		Puzzle first = emptyFour();
		Puzzle second = emptyFour();
		first.setValue(0, 1);
		second.setValue(0, 1);
		assertAll(
			() -> assertEquals(first, second),
			() -> assertEquals(second, first),
			() -> assertEquals(first.hashCode(), second.hashCode())
		);
	}
	
	@Test
	void equals_sameInstance_isEqual() {
		Puzzle puzzle = emptyFour();
		assertEquals(puzzle, puzzle);
	}
	
	@Test
	void equals_differentSize_areNotEqual() {
		Puzzle four = emptyFour();
		Puzzle nine = Puzzle.empty(GridSize.NINE, Variant.CLASSIC, NINE_CLASSIC);
		assertNotEquals(four, nine);
	}
	
	@Test
	void equals_differentVariant_areNotEqual() {
		Puzzle classic = Puzzle.empty(GridSize.NINE, Variant.CLASSIC, NINE_CLASSIC);
		Puzzle chaos = Puzzle.empty(GridSize.NINE, Variant.CHAOS, NINE_CLASSIC);
		assertNotEquals(classic, chaos);
	}
	
	@Test
	void equals_differentPartition_areNotEqual() {
		Puzzle boxes = emptyFour();
		Puzzle rows = Puzzle.empty(GridSize.FOUR, Variant.CLASSIC, rowPartitionOfFour());
		assertNotEquals(boxes, rows);
	}
	
	@Test
	void equals_differentCellValues_areNotEqual() {
		Puzzle first = emptyFour();
		Puzzle second = emptyFour();
		first.setValue(0, 1);
		assertNotEquals(first, second);
	}
	
	@Test
	void equals_differentGivenFlags_areNotEqual() {
		int[] givens = new int[16];
		givens[0] = 1;
		Puzzle given = Puzzle.classicOfGivens(GridSize.FOUR, givens);
		Puzzle pen = emptyFour();
		pen.setValue(0, 1);
		assertNotEquals(given, pen);
	}
	
	@Test
	void equals_differentPencilMarks_areNotEqual() {
		Puzzle first = emptyFour();
		Puzzle second = emptyFour();
		first.cell(0).addPencilMark(2);
		assertNotEquals(first, second);
	}
	
	@Test
	void equals_nullOrOtherType_isFalse() {
		Puzzle puzzle = emptyFour();
		assertAll(
			() -> assertNotEquals(null, puzzle),
			() -> assertNotEquals("puzzle", puzzle)
		);
	}
	
	@Test
	void toString_anyPuzzle_isNotNull() {
		assertNotNull(emptyFour().toString());
	}
}
