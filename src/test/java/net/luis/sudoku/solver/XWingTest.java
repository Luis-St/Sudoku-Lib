package net.luis.sudoku.solver;

import net.luis.sudoku.grid.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link XWing}.
 */
class XWingTest {
	
	/**
	 * A generated, uniquely solvable 9x9 puzzle in whose fresh candidate grid the digit 3 forms an X-Wing
	 * rectangle, eliminating it from cells 22, 23, 58, 59 and 77. The fixture needs no candidate pruning: the
	 * rectangle is already present in the grid derived straight from the givens.
	 */
	private static final String[] X_WING_PRESENT = {
		"890500700", "701600030", "000700008", "540800000", "003002007", "000005340", "000000002", "037004800", "000010000"
	};
	private static final int[] EXPECTED_CELLS = { 22, 23, 58, 59, 77 };
	private static final int[] EXPECTED_DIGITS = { 3, 3, 3, 3, 3 };
	
	private static int[] parse(String[] rows) {
		int[] values = new int[81];
		for (int row = 0; row < 9; row++) {
			for (int column = 0; column < 9; column++) {
				values[row * 9 + column] = rows[row].charAt(column) - '0';
			}
		}
		return values;
	}
	
	private static Puzzle puzzle(String[] rows) {
		return Puzzle.classicOfGivens(GridSize.NINE, parse(rows));
	}
	
	private static Puzzle emptyNine() {
		return Puzzle.empty(GridSize.NINE, Variant.CLASSIC, ClassicRegionPartition.of(GridSize.NINE));
	}
	
	@Test
	void find_gridWithAnXWing_returnsTheExpectedEliminations() {
		CandidateGrid grid = new CandidateGrid(puzzle(X_WING_PRESENT));
		
		Optional<Deduction> deduction = new XWing().find(grid);
		
		assertEquals(Optional.of(new Deduction.Eliminations(Technique.X_WING, EXPECTED_CELLS, EXPECTED_DIGITS)), deduction);
	}
	
	@Test
	void find_eliminations_removeNoDigitOfTheTrueSolution() {
		Puzzle puzzle = puzzle(X_WING_PRESENT);
		int[] solution = BacktrackingSolver.solve(puzzle).orElseThrow();
		Deduction.Eliminations eliminations = (Deduction.Eliminations) new XWing().find(new CandidateGrid(puzzle)).orElseThrow();
		
		int[] cells = eliminations.cells();
		int[] digits = eliminations.digits();
		assertAll(() -> assertEquals(cells.length, digits.length), () -> {
			for (int index = 0; index < cells.length; index++) {
				assertNotEquals(solution[cells[index]], digits[index], "Eliminated the solution digit at cell " + cells[index]);
			}
		});
	}
	
	@Test
	void find_eliminationsApplied_removeTheCandidates() {
		CandidateGrid grid = new CandidateGrid(puzzle(X_WING_PRESENT));
		Deduction deduction = new XWing().find(grid).orElseThrow();
		
		boolean changed = deduction.applyTo(grid);
		
		assertAll(
			() -> assertTrue(changed),
			() -> assertFalse(grid.hasCandidate(22, 3)),
			() -> assertFalse(grid.hasCandidate(23, 3)),
			() -> assertFalse(grid.hasCandidate(58, 3)),
			() -> assertFalse(grid.hasCandidate(59, 3)),
			() -> assertFalse(grid.hasCandidate(77, 3))
		);
	}
	
	@Test
	void find_eliminationCells_areSortedAscending() {
		CandidateGrid grid = new CandidateGrid(puzzle(X_WING_PRESENT));
		Deduction.Eliminations eliminations = (Deduction.Eliminations) new XWing().find(grid).orElseThrow();
		
		int[] cells = eliminations.cells();
		for (int index = 1; index < cells.length; index++) {
			assertTrue(cells[index - 1] < cells[index], "Cells must be canonical ascending");
		}
	}
	
	@Test
	void find_emptyGridWithoutAnyXWing_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(emptyNine());
		
		assertTrue(new XWing().find(grid).isEmpty());
	}
	
	@Test
	void technique_always_isXWing() {
		assertEquals(Technique.X_WING, new XWing().technique());
	}
}
