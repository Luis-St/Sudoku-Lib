package net.luis.sudoku.solver;

import net.luis.sudoku.grid.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link CandidateGrid}.
 */
class CandidateGridTest {
	
	/**
	 * A hand-built 4x4 grid whose candidate sets are simple enough to verify by hand. The classic 2x2 boxes make
	 * region 0 the cells {0,1,4,5} and region 3 the cells {10,11,14,15}. The givens are cell 0 = 1, cell 1 = 2,
	 * cell 4 = 3 and cell 15 = 4.
	 */
	private static final int[] PARTIAL_FOUR = { 1, 2, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4 };
	/**
	 * A fully and correctly solved 4x4 grid, used for the completeness and validity checks.
	 */
	private static final int[] SOLVED_FOUR = { 1, 2, 3, 4, 3, 4, 1, 2, 2, 1, 4, 3, 4, 3, 2, 1 };
	
	private static CandidateGrid partialFour() {
		return new CandidateGrid(Puzzle.classicOfGivens(GridSize.FOUR, PARTIAL_FOUR));
	}
	
	private static CandidateGrid emptyFour() {
		return new CandidateGrid(Puzzle.empty(GridSize.FOUR, Variant.CLASSIC, ClassicRegionPartition.of(GridSize.FOUR)));
	}
	
	private static int mask(int... digits) {
		int mask = 0;
		for (int digit : digits) {
			mask |= 1 << digit;
		}
		return mask;
	}
	
	@Test
	void constructor_handBuiltGrid_derivesEveryCandidateSet() {
		CandidateGrid grid = partialFour();
		
		assertAll(
			() -> assertEquals(0, grid.candidates(0), "A filled cell must carry an empty candidate mask"),
			() -> assertEquals(mask(3, 4), grid.candidates(2), "Cell 2 keeps 3 and 4 after removing the row's 1 and 2"),
			() -> assertEquals(mask(3), grid.candidates(3), "Cell 3 keeps only 3 after row 1,2 and column 4"),
			() -> assertEquals(mask(4), grid.candidates(5), "Cell 5 keeps only 4 after region 1,2,3"),
			() -> assertEquals(mask(1, 2, 3), grid.candidates(10), "Cell 10 loses only the region's 4")
		);
	}
	
	@Test
	void constructor_handBuiltGrid_reportsCountsAndValues() {
		CandidateGrid grid = partialFour();
		
		assertAll(
			() -> assertEquals(4, grid.n()),
			() -> assertEquals(16, grid.cellCount()),
			() -> assertEquals(1, grid.value(0)),
			() -> assertEquals(0, grid.value(2)),
			() -> assertFalse(grid.isEmpty(0)),
			() -> assertTrue(grid.isEmpty(2)),
			() -> assertEquals(0, grid.candidateCount(0)),
			() -> assertEquals(3, grid.candidateCount(10)),
			() -> assertTrue(grid.hasCandidate(10, 2)),
			() -> assertFalse(grid.hasCandidate(10, 4))
		);
	}
	
	@Test
	void place_emptyCell_removesDigitFromEveryPeerAndClearsTheCell() {
		CandidateGrid grid = emptyFour();
		
		boolean changed = grid.place(0, 1);
		
		assertAll(
			() -> assertTrue(changed, "A placement always changes the grid"),
			() -> assertEquals(1, grid.value(0)),
			() -> assertEquals(0, grid.candidates(0), "The placed cell is cleared of candidates"),
			() -> assertFalse(grid.hasCandidate(1, 1), "A row peer loses the placed digit"),
			() -> assertFalse(grid.hasCandidate(4, 1), "A column peer loses the placed digit"),
			() -> assertFalse(grid.hasCandidate(5, 1), "A region peer loses the placed digit"),
			() -> assertTrue(grid.hasCandidate(10, 1), "A non-peer keeps the digit")
		);
	}
	
	@Test
	void place_filledCell_throwsIllegalStateException() {
		CandidateGrid grid = partialFour();
		
		assertThrows(IllegalStateException.class, () -> grid.place(0, 2));
	}
	
	@Test
	void place_nonCandidateDigit_throwsIllegalArgumentException() {
		CandidateGrid grid = partialFour();
		
		assertThrows(IllegalArgumentException.class, () -> grid.place(3, 1));
	}
	
	@Test
	void eliminate_presentThenAbsentCandidate_returnsTrueThenFalse() {
		CandidateGrid grid = emptyFour();
		
		assertAll(
			() -> assertTrue(grid.eliminate(0, 3), "Removing a present candidate reports a change"),
			() -> assertFalse(grid.eliminate(0, 3), "Removing it again reports no change"),
			() -> assertFalse(grid.hasCandidate(0, 3))
		);
	}
	
	@Test
	void candidateDigits_emptyCell_returnsAscendingDigits() {
		CandidateGrid grid = partialFour();
		
		assertArrayEquals(new int[] { 1, 2, 3 }, grid.candidateDigits(10));
	}
	
	@Test
	void candidateDigits_filledCell_returnsEmptyArray() {
		CandidateGrid grid = partialFour();
		
		assertArrayEquals(new int[0], grid.candidateDigits(0));
	}
	
	@Test
	void peers_cell_holdsEveryOtherCellOfItsRowColumnAndRegion() {
		CandidateGrid grid = partialFour();
		
		assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 8, 12 }, grid.peers(0));
	}
	
	@Test
	void peers_nineByNineCell_holdsTwentyPeers() {
		CandidateGrid grid = new CandidateGrid(Puzzle.empty(GridSize.NINE, Variant.CLASSIC, ClassicRegionPartition.of(GridSize.NINE)));
		
		assertEquals(20, grid.peers(0).length);
	}
	
	@Test
	void peersOverload_sharedUnitBothDirections_returnsTrue() {
		CandidateGrid grid = partialFour();
		
		assertAll(
			() -> assertTrue(grid.peers(0, 1), "Cells sharing a row and region are peers"),
			() -> assertTrue(grid.peers(1, 0), "The relation is symmetric"),
			() -> assertTrue(grid.peers(0, 5), "Cells sharing a region are peers"),
			() -> assertTrue(grid.peers(2, 10), "Cells sharing a column are peers")
		);
	}
	
	@Test
	void peersOverload_unrelatedCellsOrSameCell_returnsFalse() {
		CandidateGrid grid = partialFour();
		
		assertAll(
			() -> assertFalse(grid.peers(0, 15), "Cells sharing no unit are not peers"),
			() -> assertFalse(grid.peers(0, 0), "A cell is not its own peer")
		);
	}
	
	@Test
	void unitAccessors_fourByFourGrid_haveTheExpectedSizesAndContents() {
		CandidateGrid grid = partialFour();
		
		assertAll(
			() -> assertEquals(4, grid.rows().size()),
			() -> assertEquals(4, grid.columns().size()),
			() -> assertEquals(4, grid.regions().size()),
			() -> assertEquals(12, grid.allUnits().size()),
			() -> assertArrayEquals(new int[] { 0, 1, 2, 3 }, grid.rows().get(0)),
			() -> assertArrayEquals(new int[] { 0, 4, 8, 12 }, grid.columns().get(0)),
			() -> assertArrayEquals(new int[] { 0, 1, 4, 5 }, grid.regions().get(0)),
			() -> assertArrayEquals(new int[] { 10, 11, 14, 15 }, grid.regionCells(3))
		);
	}
	
	@Test
	void allUnits_fourByFourGrid_listsRowsThenColumnsThenRegions() {
		CandidateGrid grid = partialFour();
		
		List<int[]> all = grid.allUnits();
		
		assertAll(
			() -> assertArrayEquals(grid.rows().get(0), all.get(0), "The first units are the rows"),
			() -> assertArrayEquals(grid.columns().get(0), all.get(4), "The columns follow the rows"),
			() -> assertArrayEquals(grid.regions().get(0), all.get(8), "The regions follow the columns")
		);
	}
	
	@Test
	void cellLookups_fourByFourGrid_reportRowColumnAndRegion() {
		CandidateGrid grid = partialFour();
		
		assertAll(
			() -> assertEquals(2, grid.rowOf(10)),
			() -> assertEquals(2, grid.columnOf(10)),
			() -> assertEquals(3, grid.regionOf(10))
		);
	}
	
	@Test
	void isComplete_solvedGrid_returnsTrue() {
		CandidateGrid grid = new CandidateGrid(Puzzle.classicOfGivens(GridSize.FOUR, SOLVED_FOUR));
		
		assertAll(
			() -> assertTrue(grid.isComplete()),
			() -> assertTrue(grid.isSolved())
		);
	}
	
	@Test
	void isComplete_partialGrid_returnsFalse() {
		CandidateGrid grid = partialFour();
		
		assertAll(
			() -> assertFalse(grid.isComplete()),
			() -> assertFalse(grid.isSolved())
		);
	}
	
	@Test
	void values_returnedArray_isADefensiveCopy() {
		CandidateGrid grid = partialFour();
		
		int[] first = grid.values();
		first[0] = 9;
		int[] second = grid.values();
		
		assertAll(
			() -> assertNotSame(first, second, "Every call returns a fresh array"),
			() -> assertEquals(1, grid.value(0), "Mutating the returned array must not touch the grid"),
			() -> assertEquals(1, second[0])
		);
	}
}
