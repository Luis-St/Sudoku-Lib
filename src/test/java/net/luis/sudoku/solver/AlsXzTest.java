package net.luis.sudoku.solver;

import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.grid.Puzzle;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link AlsXz}.
 */
class AlsXzTest {
	
	/**
	 * A generated NINE CLASSIC puzzle (band THIRTEEN, seed 3) whose solve applies the technique 54 time(s).
	 */
	private static final String GIVENS = "000170900806000400001000000080006000003800050020050030000604007000700500007300012";
	
	private static Puzzle puzzle() {
		return StrategyFixtures.classic(GridSize.NINE, GIVENS);
	}
	
	@Test
	void technique_returnsTheMatchingConstant() {
		assertEquals(Technique.ALS_XZ, new AlsXz().technique());
	}
	
	@Test
	void find_puzzleThatNeedsIt_appliesItDuringTheSolve() {
		TechniqueReport report = TechniqueSolver.solve(puzzle());
		
		assertAll(
			() -> assertTrue(report.solved(), "The fixture must solve with techniques alone"),
			() -> assertTrue(report.count(Technique.ALS_XZ) >= 1,
				"The fixture must still need ALS_XZ, but the solve used " + report.usage())
		);
	}
	
	@Test
	void find_firstStateItApplies_agreesWithTheTrueSolution() {
		Puzzle puzzle = puzzle();
		Optional<Deduction> deduction = StrategyFixtures.firstFiring(new AlsXz(), puzzle);
		
		assertTrue(deduction.isPresent(), "ALS_XZ never applied anywhere in the fixture's solve");
		assertEquals(Technique.ALS_XZ, deduction.orElseThrow().technique());
		StrategyFixtures.assertAgreesWithSolution(deduction.orElseThrow(), puzzle);
	}
	
	@Test
	void find_emptyGrid_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(StrategyFixtures.empty(GridSize.NINE));
		
		assertTrue(new AlsXz().find(grid).isEmpty());
	}
	
	@Test
	void find_solvedGrid_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(StrategyFixtures.solved(puzzle()));
		
		assertTrue(new AlsXz().find(grid).isEmpty());
	}
	
	@Test
	void find_nullGrid_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> new AlsXz().find(null));
	}
}
