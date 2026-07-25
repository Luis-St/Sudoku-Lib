package net.luis.sudoku.solver;

import net.luis.sudoku.grid.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link PointingPair}.
 */
class PointingPairTest {
	
	/**
	 * A generated, uniquely solvable 9x9 puzzle in whose fresh candidate grid the digit 3 is confined within its
	 * region to column 0, so a pointing pair removes it from cells 27 and 36 elsewhere on that column.
	 */
	private static final String[] POINTING_PRESENT = {
		"050000700", "070005046", "018070002", "009004001", "000009060", "000710030", "003090085", "000000000", "501000300"
	};
	private static final int[] EXPECTED_CELLS = { 27, 36 };
	private static final int[] EXPECTED_DIGITS = { 3, 3 };
	
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
	void find_regionConfinedToALine_returnsTheExpectedEliminations() {
		CandidateGrid grid = new CandidateGrid(puzzle(POINTING_PRESENT));
		
		Optional<Deduction> deduction = new PointingPair().find(grid);
		
		assertEquals(Optional.of(new Deduction.Eliminations(Technique.POINTING_PAIR, EXPECTED_CELLS, EXPECTED_DIGITS)), deduction);
	}
	
	@Test
	void find_eliminations_removeNoDigitOfTheTrueSolution() {
		Puzzle puzzle = puzzle(POINTING_PRESENT);
		int[] solution = BacktrackingSolver.solve(puzzle).orElseThrow();
		Deduction.Eliminations eliminations = (Deduction.Eliminations) new PointingPair().find(new CandidateGrid(puzzle)).orElseThrow();
		
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
		CandidateGrid grid = new CandidateGrid(puzzle(POINTING_PRESENT));
		Deduction deduction = new PointingPair().find(grid).orElseThrow();
		
		boolean changed = deduction.applyTo(grid);
		
		assertAll(
			() -> assertTrue(changed),
			() -> assertFalse(grid.hasCandidate(27, 3)),
			() -> assertFalse(grid.hasCandidate(36, 3))
		);
	}
	
	@Test
	void find_emptyGridWithoutAnyPointingPair_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(emptyNine());
		
		assertTrue(new PointingPair().find(grid).isEmpty());
	}
	
	@Test
	void technique_always_isPointingPair() {
		assertEquals(Technique.POINTING_PAIR, new PointingPair().technique());
	}
}
