package net.luis.sudoku.solver;

import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.grid.Puzzle;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link UniqueRectangle4}.
 */
class UniqueRectangle4Test {
	
	/**
	 * A generated NINE CLASSIC puzzle (band NINE, seed 9) whose solve applies the technique 37 time(s).
	 */
	private static final String GIVENS = "007030000020840003009500401043000000700000008082000005060907500004300617000100900";
	
	private static Puzzle puzzle() {
		return StrategyFixtures.classic(GridSize.NINE, GIVENS);
	}
	
	@Test
	void technique_returnsTheMatchingConstant() {
		assertEquals(Technique.UNIQUE_RECTANGLE_4, new UniqueRectangle4().technique());
	}
	
	@Test
	void find_puzzleThatNeedsIt_appliesItDuringTheSolve() {
		TechniqueReport report = TechniqueSolver.solve(puzzle());
		
		assertAll(
			() -> assertTrue(report.solved(), "The fixture must solve with techniques alone"),
			() -> assertTrue(report.count(Technique.UNIQUE_RECTANGLE_4) >= 1,
				"The fixture must still need UNIQUE_RECTANGLE_4, but the solve used " + report.usage())
		);
	}
	
	@Test
	void find_firstStateItApplies_agreesWithTheTrueSolution() {
		Puzzle puzzle = puzzle();
		Optional<Deduction> deduction = StrategyFixtures.firstFiring(new UniqueRectangle4(), puzzle);
		
		assertTrue(deduction.isPresent(), "UNIQUE_RECTANGLE_4 never applied anywhere in the fixture's solve");
		assertEquals(Technique.UNIQUE_RECTANGLE_4, deduction.orElseThrow().technique());
		StrategyFixtures.assertAgreesWithSolution(deduction.orElseThrow(), puzzle);
	}
	
	@Test
	void find_emptyGrid_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(StrategyFixtures.empty(GridSize.NINE));
		
		assertTrue(new UniqueRectangle4().find(grid).isEmpty());
	}
	
	@Test
	void find_solvedGrid_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(StrategyFixtures.solved(puzzle()));
		
		assertTrue(new UniqueRectangle4().find(grid).isEmpty());
	}
	
	@Test
	void find_nullGrid_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> new UniqueRectangle4().find(null));
	}
}
