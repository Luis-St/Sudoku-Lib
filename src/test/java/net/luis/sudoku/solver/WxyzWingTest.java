package net.luis.sudoku.solver;

import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.grid.Puzzle;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link WxyzWing}.
 */
class WxyzWingTest {
	
	/**
	 * A generated NINE CLASSIC puzzle (band ELEVEN, seed 0) whose solve applies the technique 166 time(s).
	 */
	private static final String GIVENS = "090160300006000000080030002804710050927800400003900000600508970000390506000001803";
	
	private static Puzzle puzzle() {
		return StrategyFixtures.classic(GridSize.NINE, GIVENS);
	}
	
	@Test
	void technique_returnsTheMatchingConstant() {
		assertEquals(Technique.WXYZ_WING, new WxyzWing().technique());
	}
	
	@Test
	void find_puzzleThatNeedsIt_appliesItDuringTheSolve() {
		TechniqueReport report = TechniqueSolver.solve(puzzle());
		
		assertAll(
			() -> assertTrue(report.solved(), "The fixture must solve with techniques alone"),
			() -> assertTrue(report.count(Technique.WXYZ_WING) >= 1,
				"The fixture must still need WXYZ_WING, but the solve used " + report.usage())
		);
	}
	
	@Test
	void find_firstStateItApplies_agreesWithTheTrueSolution() {
		Puzzle puzzle = puzzle();
		Optional<Deduction> deduction = StrategyFixtures.firstFiring(new WxyzWing(), puzzle);
		
		assertTrue(deduction.isPresent(), "WXYZ_WING never applied anywhere in the fixture's solve");
		assertEquals(Technique.WXYZ_WING, deduction.orElseThrow().technique());
		StrategyFixtures.assertAgreesWithSolution(deduction.orElseThrow(), puzzle);
	}
	
	@Test
	void find_emptyGrid_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(StrategyFixtures.empty(GridSize.NINE));
		
		assertTrue(new WxyzWing().find(grid).isEmpty());
	}
	
	@Test
	void find_solvedGrid_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(StrategyFixtures.solved(puzzle()));
		
		assertTrue(new WxyzWing().find(grid).isEmpty());
	}
	
	@Test
	void find_nullGrid_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> new WxyzWing().find(null));
	}
}
