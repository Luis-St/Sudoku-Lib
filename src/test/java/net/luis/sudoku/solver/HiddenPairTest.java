package net.luis.sudoku.solver;

import net.luis.sudoku.grid.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link HiddenPair}.
 */
class HiddenPairTest {
	
	/**
	 * A generated, uniquely solvable 9x9 puzzle in whose fresh candidate grid a hidden pair strips cell 39 of the
	 * digits 1, 5 and 7 and cell 40 of the digit 7, leaving each cell only the pair's two digits.
	 */
	private static final String[] PAIR_PRESENT = {
		"900006030", "020010009", "410000060", "180920000", "300000008", "060800540", "206000000", "000050306", "000004000"
	};
	private static final int[] EXPECTED_CELLS = { 39, 39, 39, 40 };
	private static final int[] EXPECTED_DIGITS = { 1, 5, 7, 7 };
	
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
	void find_unitWithAHiddenPair_returnsTheExpectedEliminations() {
		CandidateGrid grid = new CandidateGrid(puzzle(PAIR_PRESENT));
		
		Optional<Deduction> deduction = new HiddenPair().find(grid);
		
		assertEquals(Optional.of(new Deduction.Eliminations(Technique.HIDDEN_PAIR, EXPECTED_CELLS, EXPECTED_DIGITS)), deduction);
	}
	
	@Test
	void find_eliminations_removeNoDigitOfTheTrueSolution() {
		Puzzle puzzle = puzzle(PAIR_PRESENT);
		int[] solution = BacktrackingSolver.solve(puzzle).orElseThrow();
		Deduction.Eliminations eliminations = (Deduction.Eliminations) new HiddenPair().find(new CandidateGrid(puzzle)).orElseThrow();
		
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
		CandidateGrid grid = new CandidateGrid(puzzle(PAIR_PRESENT));
		Deduction deduction = new HiddenPair().find(grid).orElseThrow();
		
		boolean changed = deduction.applyTo(grid);
		
		assertAll(
			() -> assertTrue(changed),
			() -> assertFalse(grid.hasCandidate(39, 1)),
			() -> assertFalse(grid.hasCandidate(39, 5)),
			() -> assertFalse(grid.hasCandidate(39, 7)),
			() -> assertFalse(grid.hasCandidate(40, 7))
		);
	}
	
	@Test
	void find_emptyGridWithoutAnyHiddenPair_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(emptyNine());
		
		assertTrue(new HiddenPair().find(grid).isEmpty());
	}
	
	@Test
	void technique_always_isHiddenPair() {
		assertEquals(Technique.HIDDEN_PAIR, new HiddenPair().technique());
	}
}
