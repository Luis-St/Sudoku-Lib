package net.luis.sudoku.solver;

import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.grid.Puzzle;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link HiddenQuad}.
 */
class HiddenQuadTest {
	
	/**
	 * A generated TWELVE CLASSIC puzzle (band THIRTEEN, seed 7) whose solve applies the technique 1 time(s).
	 */
	private static final String GIVENS = "0000c080030a0a704000b09c003ca9020000030060900c00b0400020005670c000080940c060800007051ba00000040804053006c0000008000006000004b67a000060b100c00500";
	
	private static Puzzle puzzle() {
		return StrategyFixtures.classic(GridSize.TWELVE, GIVENS);
	}
	
	@Test
	void technique_returnsTheMatchingConstant() {
		assertEquals(Technique.HIDDEN_QUAD, new HiddenQuad().technique());
	}
	
	@Test
	void find_puzzleThatNeedsIt_appliesItDuringTheSolve() {
		TechniqueReport report = TechniqueSolver.solve(puzzle());
		
		assertAll(
			() -> assertTrue(report.solved(), "The fixture must solve with techniques alone"),
			() -> assertTrue(report.count(Technique.HIDDEN_QUAD) >= 1,
				"The fixture must still need HIDDEN_QUAD, but the solve used " + report.usage())
		);
	}
	
	@Test
	void find_firstStateItApplies_agreesWithTheTrueSolution() {
		Puzzle puzzle = puzzle();
		Optional<Deduction> deduction = StrategyFixtures.firstFiring(new HiddenQuad(), puzzle);
		
		assertTrue(deduction.isPresent(), "HIDDEN_QUAD never applied anywhere in the fixture's solve");
		assertEquals(Technique.HIDDEN_QUAD, deduction.orElseThrow().technique());
		StrategyFixtures.assertAgreesWithSolution(deduction.orElseThrow(), puzzle);
	}
	
	@Test
	void find_emptyGrid_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(StrategyFixtures.empty(GridSize.TWELVE));
		
		assertTrue(new HiddenQuad().find(grid).isEmpty());
	}
	
	@Test
	void find_solvedGrid_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(StrategyFixtures.solved(puzzle()));
		
		assertTrue(new HiddenQuad().find(grid).isEmpty());
	}
	
	@Test
	void find_nullGrid_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> new HiddenQuad().find(null));
	}
}
