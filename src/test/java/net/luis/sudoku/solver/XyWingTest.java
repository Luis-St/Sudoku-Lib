package net.luis.sudoku.solver;

import net.luis.sudoku.grid.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link XyWing}.
 */
class XyWingTest {
	
	/**
	 * A generated, uniquely solvable 9x9 puzzle in whose fresh candidate grid a bi-value pivot and its two
	 * bi-value wings form an XY-Wing that removes the shared digit 5 from cell 29.
	 */
	private static final String[] XY_WING_PRESENT = {
		"408063002", "000200050", "000000106", "000000000", "021007005", "607050000", "903080040", "000400020", "002079000"
	};
	private static final int[] EXPECTED_CELLS = { 29 };
	private static final int[] EXPECTED_DIGITS = { 5 };
	
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
	void find_gridWithAnXyWing_returnsTheExpectedEliminations() {
		CandidateGrid grid = new CandidateGrid(puzzle(XY_WING_PRESENT));
		
		Optional<Deduction> deduction = new XyWing().find(grid);
		
		assertEquals(Optional.of(new Deduction.Eliminations(Technique.XY_WING, EXPECTED_CELLS, EXPECTED_DIGITS)), deduction);
	}
	
	@Test
	void find_eliminations_removeNoDigitOfTheTrueSolution() {
		Puzzle puzzle = puzzle(XY_WING_PRESENT);
		int[] solution = BacktrackingSolver.solve(puzzle).orElseThrow();
		Deduction.Eliminations eliminations = (Deduction.Eliminations) new XyWing().find(new CandidateGrid(puzzle)).orElseThrow();
		
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
		CandidateGrid grid = new CandidateGrid(puzzle(XY_WING_PRESENT));
		Deduction deduction = new XyWing().find(grid).orElseThrow();
		
		boolean changed = deduction.applyTo(grid);
		
		assertAll(
			() -> assertTrue(changed),
			() -> assertFalse(grid.hasCandidate(29, 5))
		);
	}
	
	@Test
	void find_emptyGridWithoutAnyXyWing_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(emptyNine());
		
		assertTrue(new XyWing().find(grid).isEmpty());
	}
	
	@Test
	void technique_always_isXyWing() {
		assertEquals(Technique.XY_WING, new XyWing().technique());
	}
}
