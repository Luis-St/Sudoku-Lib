package net.luis.sudoku.solver;

import net.luis.sudoku.grid.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link NakedTriple}.
 */
class NakedTripleTest {
	
	/**
	 * A generated, uniquely solvable 9x9 puzzle in whose fresh candidate grid a naked triple removes the digit 9
	 * from cell 8 and the digits 1 and 9 from cell 15.
	 */
	private static final String[] TRIPLE_PRESENT = {
		"050000700", "070005046", "018070002", "009004001", "000009060", "000710030", "003090085", "000000000", "501000300"
	};
	private static final int[] EXPECTED_CELLS = { 8, 15, 15 };
	private static final int[] EXPECTED_DIGITS = { 9, 1, 9 };
	
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
	void find_unitWithANakedTriple_returnsTheExpectedEliminations() {
		CandidateGrid grid = new CandidateGrid(puzzle(TRIPLE_PRESENT));
		
		Optional<Deduction> deduction = new NakedTriple().find(grid);
		
		assertEquals(Optional.of(new Deduction.Eliminations(Technique.NAKED_TRIPLE, EXPECTED_CELLS, EXPECTED_DIGITS)), deduction);
	}
	
	@Test
	void find_eliminations_removeNoDigitOfTheTrueSolution() {
		Puzzle puzzle = puzzle(TRIPLE_PRESENT);
		int[] solution = BacktrackingSolver.solve(puzzle).orElseThrow();
		Deduction.Eliminations eliminations = (Deduction.Eliminations) new NakedTriple().find(new CandidateGrid(puzzle)).orElseThrow();
		
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
		Deduction deduction = new NakedTriple().find(grid).orElseThrow();
		
		boolean changed = deduction.applyTo(grid);
		
		assertAll(
			() -> assertTrue(changed),
			() -> assertFalse(grid.hasCandidate(8, 9)),
			() -> assertFalse(grid.hasCandidate(15, 1)),
			() -> assertFalse(grid.hasCandidate(15, 9))
		);
	}
	
	@Test
	void find_emptyGridWithoutAnyTriple_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(emptyNine());
		
		assertTrue(new NakedTriple().find(grid).isEmpty());
	}
	
	@Test
	void technique_always_isNakedTriple() {
		assertEquals(Technique.NAKED_TRIPLE, new NakedTriple().technique());
	}
}
