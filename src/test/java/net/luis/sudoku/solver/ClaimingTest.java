package net.luis.sudoku.solver;

import net.luis.sudoku.grid.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link Claiming}.
 */
class ClaimingTest {
	
	/**
	 * A generated, uniquely solvable 9x9 puzzle in whose fresh candidate grid the digit 2 is confined within
	 * column 1 to region 0, so a box-line reduction removes it from cells 9 and 11 in the rest of that region.
	 * This fixture was chosen so that its dual, the {@link Pointing}, makes no progress on the same grid,
	 * which pins the elimination on box-line reduction alone.
	 */
	private static final String[] BOX_PRESENT = {
		"005020100", "010007300", "300105046", "003008200", "050070060", "000000000", "009000020", "000680007", "070003408"
	};
	private static final int[] EXPECTED_CELLS = { 9, 11 };
	private static final int[] EXPECTED_DIGITS = { 2, 2 };
	
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
	void find_lineConfinedToARegion_returnsTheExpectedEliminations() {
		CandidateGrid grid = new CandidateGrid(puzzle(BOX_PRESENT));
		
		Optional<Deduction> deduction = new Claiming().find(grid);
		
		assertEquals(Optional.of(new Deduction.Eliminations(Technique.CLAIMING, EXPECTED_CELLS, EXPECTED_DIGITS)), deduction);
	}
	
	@Test
	void find_thisFixture_isNotAlsoAPointingPair() {
		CandidateGrid grid = new CandidateGrid(puzzle(BOX_PRESENT));
		
		assertTrue(new Pointing().find(grid).isEmpty(), "The fixture must isolate box-line reduction from its dual");
	}
	
	@Test
	void find_eliminations_removeNoDigitOfTheTrueSolution() {
		Puzzle puzzle = puzzle(BOX_PRESENT);
		int[] solution = BacktrackingSolver.solve(puzzle).orElseThrow();
		Deduction.Eliminations eliminations = (Deduction.Eliminations) new Claiming().find(new CandidateGrid(puzzle)).orElseThrow();
		
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
		CandidateGrid grid = new CandidateGrid(puzzle(BOX_PRESENT));
		Deduction deduction = new Claiming().find(grid).orElseThrow();
		
		boolean changed = deduction.applyTo(grid);
		
		assertAll(
			() -> assertTrue(changed),
			() -> assertFalse(grid.hasCandidate(9, 2)),
			() -> assertFalse(grid.hasCandidate(11, 2))
		);
	}
	
	@Test
	void find_emptyGridWithoutAnyBoxLineReduction_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(emptyNine());
		
		assertTrue(new Claiming().find(grid).isEmpty());
	}
	
	@Test
	void technique_always_isBoxLineReduction() {
		assertEquals(Technique.CLAIMING, new Claiming().technique());
	}
}
