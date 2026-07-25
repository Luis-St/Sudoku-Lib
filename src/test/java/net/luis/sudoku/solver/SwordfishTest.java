package net.luis.sudoku.solver;

import net.luis.sudoku.grid.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link Swordfish}.
 */
class SwordfishTest {
	
	/**
	 * A generated, uniquely solvable 9x9 puzzle in whose fresh candidate grid the digit 8 forms a Swordfish over
	 * three lines, eliminating it from cells 37, 41, 46 and 50. The fixture needs no candidate pruning: the fish
	 * is already present in the grid derived straight from the givens.
	 */
	private static final String[] SWORDFISH_PRESENT = {
		"000300500", "008000090", "020410008", "400290170", "100000000", "000000300", "000004006", "870600050", "000082007"
	};
	private static final int[] EXPECTED_CELLS = { 37, 41, 46, 50 };
	private static final int[] EXPECTED_DIGITS = { 8, 8, 8, 8 };
	
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
	void find_gridWithASwordfish_returnsTheExpectedEliminations() {
		CandidateGrid grid = new CandidateGrid(puzzle(SWORDFISH_PRESENT));
		
		Optional<Deduction> deduction = new Swordfish().find(grid);
		
		assertEquals(Optional.of(new Deduction.Eliminations(Technique.SWORDFISH, EXPECTED_CELLS, EXPECTED_DIGITS)), deduction);
	}
	
	@Test
	void find_eliminations_removeNoDigitOfTheTrueSolution() {
		Puzzle puzzle = puzzle(SWORDFISH_PRESENT);
		int[] solution = BacktrackingSolver.solve(puzzle).orElseThrow();
		Deduction.Eliminations eliminations = (Deduction.Eliminations) new Swordfish().find(new CandidateGrid(puzzle)).orElseThrow();
		
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
		CandidateGrid grid = new CandidateGrid(puzzle(SWORDFISH_PRESENT));
		Deduction deduction = new Swordfish().find(grid).orElseThrow();
		
		boolean changed = deduction.applyTo(grid);
		
		assertAll(
			() -> assertTrue(changed),
			() -> assertFalse(grid.hasCandidate(37, 8)),
			() -> assertFalse(grid.hasCandidate(41, 8)),
			() -> assertFalse(grid.hasCandidate(46, 8)),
			() -> assertFalse(grid.hasCandidate(50, 8))
		);
	}
	
	@Test
	void find_eliminationCells_areSortedAscending() {
		CandidateGrid grid = new CandidateGrid(puzzle(SWORDFISH_PRESENT));
		Deduction.Eliminations eliminations = (Deduction.Eliminations) new Swordfish().find(grid).orElseThrow();
		
		int[] cells = eliminations.cells();
		for (int index = 1; index < cells.length; index++) {
			assertTrue(cells[index - 1] < cells[index], "Cells must be canonical ascending");
		}
	}
	
	@Test
	void find_emptyGridWithoutAnySwordfish_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(emptyNine());
		
		assertTrue(new Swordfish().find(grid).isEmpty());
	}
	
	@Test
	void technique_always_isSwordfish() {
		assertEquals(Technique.SWORDFISH, new Swordfish().technique());
	}
}
