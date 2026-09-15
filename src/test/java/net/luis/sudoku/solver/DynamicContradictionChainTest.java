package net.luis.sudoku.solver;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.grid.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link DynamicContradictionChain}.
 */
class DynamicContradictionChainTest {
	
	/**
	 * A generated SIXTEEN CLASSIC puzzle (band LISA, seed 4) whose solve applies the technique 2 time(s).
	 * <p>
	 *     Pinned as a key rather than as givens, so that the fixture is re-derived from the generator that
	 *     is actually shipping rather than frozen at whatever it produced once.
	 * </p>
	 */
	private static Puzzle puzzle() {
		return StrategyFixtures.generated(GridSize.SIXTEEN, Variant.CLASSIC, Difficulty.LISA, 4L);
	}
	
	@Test
	void technique_returnsTheMatchingConstant() {
		assertEquals(Technique.DYNAMIC_CONTRADICTION_CHAIN, new DynamicContradictionChain().technique());
	}
	
	@Test
	void find_puzzleThatNeedsIt_appliesItDuringTheSolve() {
		TechniqueReport report = TechniqueSolver.solve(puzzle());
		
		assertAll(
			() -> assertTrue(report.solved(), "The fixture must solve with techniques alone"),
			() -> assertTrue(report.count(Technique.DYNAMIC_CONTRADICTION_CHAIN) >= 1,
				"The fixture must still need DYNAMIC_CONTRADICTION_CHAIN, but the solve used " + report.usage())
		);
	}
	
	@Test
	void find_firstStateItApplies_agreesWithTheTrueSolution() {
		Puzzle puzzle = puzzle();
		Optional<Deduction> deduction = StrategyFixtures.firstFiring(new DynamicContradictionChain(), puzzle);
		
		assertTrue(deduction.isPresent(), "DYNAMIC_CONTRADICTION_CHAIN never applied anywhere in the fixture's solve");
		assertEquals(Technique.DYNAMIC_CONTRADICTION_CHAIN, deduction.orElseThrow().technique());
		StrategyFixtures.assertAgreesWithSolution(deduction.orElseThrow(), puzzle);
	}
	
	@Test
	void find_emptyGrid_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(StrategyFixtures.empty(GridSize.NINE));
		
		assertTrue(new DynamicContradictionChain().find(grid).isEmpty());
	}
	
	@Test
	void find_solvedGrid_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(StrategyFixtures.solved(puzzle()));
		
		assertTrue(new DynamicContradictionChain().find(grid).isEmpty());
	}
	
	@Test
	void find_nullGrid_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> new DynamicContradictionChain().find(null));
	}
}
