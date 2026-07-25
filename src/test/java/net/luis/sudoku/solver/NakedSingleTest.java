package net.luis.sudoku.solver;

import net.luis.sudoku.grid.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link NakedSingle}.
 */
class NakedSingleTest {
	
	/**
	 * A generated, uniquely solvable 9x9 puzzle whose fresh candidate grid already collapses cell 11 to the single
	 * candidate 2.
	 */
	private static final String[] SINGLE_PRESENT = {
		"050000700", "070005046", "018070002", "009004001", "000009060", "000710030", "003090085", "000000000", "501000300"
	};
	
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
	void find_gridWithACollapsedCell_returnsThatPlacement() {
		CandidateGrid grid = new CandidateGrid(puzzle(SINGLE_PRESENT));
		
		Optional<Deduction> deduction = new NakedSingle().find(grid);
		
		assertEquals(Optional.of(new Deduction.Placement(Technique.NAKED_SINGLE, 11, 2)), deduction);
	}
	
	@Test
	void find_placement_agreesWithTheUniqueSolution() {
		Puzzle puzzle = puzzle(SINGLE_PRESENT);
		int[] solution = BacktrackingSolver.solve(puzzle).orElseThrow();
		Deduction.Placement placement = (Deduction.Placement) new NakedSingle().find(new CandidateGrid(puzzle)).orElseThrow();
		
		assertEquals(solution[placement.cell()], placement.digit(), "The naked single must match the true solution");
	}
	
	@Test
	void find_placementApplied_fillsTheCell() {
		CandidateGrid grid = new CandidateGrid(puzzle(SINGLE_PRESENT));
		Deduction deduction = new NakedSingle().find(grid).orElseThrow();
		
		boolean changed = deduction.applyTo(grid);
		
		assertAll(
			() -> assertTrue(changed),
			() -> assertEquals(2, grid.value(11))
		);
	}
	
	@Test
	void find_gridWithoutAnySingle_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(emptyNine());
		
		assertTrue(new NakedSingle().find(grid).isEmpty());
	}
	
	@Test
	void technique_always_isNakedSingle() {
		assertEquals(Technique.NAKED_SINGLE, new NakedSingle().technique());
	}
}
