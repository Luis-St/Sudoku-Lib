package net.luis.sudoku.solver;

import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.grid.Puzzle;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link SashimiSwordfish}.
 */
class SashimiSwordfishTest {
	
	/**
	 * A generated NINE CLASSIC puzzle (band FOURTEEN, seed 2) whose solve applies the technique 10 time(s).
	 */
	private static final String GIVENS = "410008300003009008000020090100000000004930080200060105000000960060500000001000800";
	
	private static Puzzle puzzle() {
		return StrategyFixtures.classic(GridSize.NINE, GIVENS);
	}
	
	@Test
	void technique_returnsTheMatchingConstant() {
		assertEquals(Technique.SASHIMI_SWORDFISH, new SashimiSwordfish().technique());
	}
	
	@Test
	void find_puzzleThatNeedsIt_appliesItDuringTheSolve() {
		TechniqueReport report = TechniqueSolver.solve(puzzle());
		
		assertAll(
			() -> assertTrue(report.solved(), "The fixture must solve with techniques alone"),
			() -> assertTrue(report.count(Technique.SASHIMI_SWORDFISH) >= 1,
				"The fixture must still need SASHIMI_SWORDFISH, but the solve used " + report.usage())
		);
	}
	
	@Test
	void find_firstStateItApplies_agreesWithTheTrueSolution() {
		Puzzle puzzle = puzzle();
		Optional<Deduction> deduction = StrategyFixtures.firstFiring(new SashimiSwordfish(), puzzle);
		
		assertTrue(deduction.isPresent(), "SASHIMI_SWORDFISH never applied anywhere in the fixture's solve");
		assertEquals(Technique.SASHIMI_SWORDFISH, deduction.orElseThrow().technique());
		StrategyFixtures.assertAgreesWithSolution(deduction.orElseThrow(), puzzle);
	}
	
	@Test
	void find_emptyGrid_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(StrategyFixtures.empty(GridSize.NINE));
		
		assertTrue(new SashimiSwordfish().find(grid).isEmpty());
	}
	
	@Test
	void find_solvedGrid_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(StrategyFixtures.solved(puzzle()));
		
		assertTrue(new SashimiSwordfish().find(grid).isEmpty());
	}
	
	@Test
	void find_nullGrid_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> new SashimiSwordfish().find(null));
	}
}
