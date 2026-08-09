package net.luis.sudoku.solver;

import net.luis.sudoku.grid.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link HiddenTriple}.
 */
class HiddenTripleTest {
	
	/**
	 * A generated, uniquely solvable 9x9 puzzle in whose fresh candidate grid a hidden triple removes the digit 5
	 * from cells 67 and 68 and the digit 3 from cell 75.
	 */
	private static final String[] TRIPLE_PRESENT = {
		"060548002", "000030050", "000000000", "401000500", "000900004", "000070800", "028001069", "003400000", "500087000"
	};
	private static final int[] EXPECTED_CELLS = { 67, 68, 75 };
	private static final int[] EXPECTED_DIGITS = { 5, 5, 3 };
	
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
	void find_unitWithAHiddenTriple_returnsTheExpectedEliminations() {
		CandidateGrid grid = new CandidateGrid(puzzle(TRIPLE_PRESENT));
		
		Optional<Deduction> deduction = new HiddenTriple().find(grid);
		
		assertEquals(Optional.of(new Deduction.Eliminations(Technique.HIDDEN_TRIPLE, EXPECTED_CELLS, EXPECTED_DIGITS)), deduction);
	}
	
	@Test
	void find_eliminations_removeNoDigitOfTheTrueSolution() {
		Puzzle puzzle = puzzle(TRIPLE_PRESENT);
		int[] solution = BacktrackingSolver.solve(puzzle).orElseThrow();
		Deduction.Eliminations eliminations = (Deduction.Eliminations) new HiddenTriple().find(new CandidateGrid(puzzle)).orElseThrow();
		
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
		CandidateGrid grid = new CandidateGrid(puzzle(TRIPLE_PRESENT));
		Deduction deduction = new HiddenTriple().find(grid).orElseThrow();
		
		boolean changed = deduction.applyTo(grid);
		
		assertAll(
			() -> assertTrue(changed),
			() -> assertFalse(grid.hasCandidate(67, 5)),
			() -> assertFalse(grid.hasCandidate(68, 5)),
			() -> assertFalse(grid.hasCandidate(75, 3))
		);
	}
	
	@Test
	void find_emptyGridWithoutAnyHiddenTriple_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(emptyNine());
		
		assertTrue(new HiddenTriple().find(grid).isEmpty());
	}
	
	@Test
	void technique_always_isHiddenTriple() {
		assertEquals(Technique.HIDDEN_TRIPLE, new HiddenTriple().technique());
	}
}
