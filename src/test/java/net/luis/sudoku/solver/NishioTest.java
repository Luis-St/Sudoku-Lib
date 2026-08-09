package net.luis.sudoku.solver;

import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.grid.Puzzle;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link Nishio}.
 */
class NishioTest {
	
	/**
	 * A generated NINE CLASSIC puzzle (band LISA, seed 0) whose solve applies the technique 187 time(s).
	 */
	private static final String GIVENS = "100000590000680000300005004507100000000000301002000070000050000050004600600970003";
	
	private static Puzzle puzzle() {
		return StrategyFixtures.classic(GridSize.NINE, GIVENS);
	}
	
	@Test
	void technique_returnsTheMatchingConstant() {
		assertEquals(Technique.NISHIO, new Nishio().technique());
	}
	
	@Test
	void find_puzzleThatNeedsIt_appliesItDuringTheSolve() {
		TechniqueReport report = TechniqueSolver.solve(puzzle());
		
		assertAll(
			() -> assertTrue(report.solved(), "The fixture must solve with techniques alone"),
			() -> assertTrue(report.count(Technique.NISHIO) >= 1,
				"The fixture must still need NISHIO, but the solve used " + report.usage())
		);
	}
	
	@Test
	void find_firstStateItApplies_agreesWithTheTrueSolution() {
		Puzzle puzzle = puzzle();
		Optional<Deduction> deduction = StrategyFixtures.firstFiring(new Nishio(), puzzle);
		
		assertTrue(deduction.isPresent(), "NISHIO never applied anywhere in the fixture's solve");
		assertEquals(Technique.NISHIO, deduction.orElseThrow().technique());
		StrategyFixtures.assertAgreesWithSolution(deduction.orElseThrow(), puzzle);
	}
	
	@Test
	void find_emptyGrid_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(StrategyFixtures.empty(GridSize.NINE));
		
		assertTrue(new Nishio().find(grid).isEmpty());
	}
	
	@Test
	void find_solvedGrid_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(StrategyFixtures.solved(puzzle()));
		
		assertTrue(new Nishio().find(grid).isEmpty());
	}
	
	@Test
	void find_nullGrid_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> new Nishio().find(null));
	}
}
