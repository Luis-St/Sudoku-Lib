package net.luis.sudoku.solver;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.grid.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link LawOfLeftovers}.
 */
class LawOfLeftoversTest {
	
	/**
	 * A generated NINE CHAOS puzzle (band SIX, seed 7) whose solve applies the technique 523 time(s).
	 * <p>
	 *     Pinned as a key rather than as givens: a jigsaw puzzle is not described by its givens alone,
	 *     since the region layout is half of it, so the generator rebuilds the layout here.
	 * </p>
	 */
	private static Puzzle puzzle() {
		return StrategyFixtures.generated(GridSize.NINE, Variant.CHAOS, Difficulty.SIX, 7L);
	}
	
	@Test
	void technique_returnsTheMatchingConstant() {
		assertEquals(Technique.LAW_OF_LEFTOVERS, new LawOfLeftovers().technique());
	}
	
	@Test
	void find_puzzleThatNeedsIt_appliesItDuringTheSolve() {
		TechniqueReport report = TechniqueSolver.solve(puzzle());
		
		assertAll(
			() -> assertTrue(report.solved(), "The fixture must solve with techniques alone"),
			() -> assertTrue(report.count(Technique.LAW_OF_LEFTOVERS) >= 1,
				"The fixture must still need LAW_OF_LEFTOVERS, but the solve used " + report.usage())
		);
	}
	
	@Test
	void find_firstStateItApplies_agreesWithTheTrueSolution() {
		Puzzle puzzle = puzzle();
		Optional<Deduction> deduction = StrategyFixtures.firstFiring(new LawOfLeftovers(), puzzle);
		
		assertTrue(deduction.isPresent(), "LAW_OF_LEFTOVERS never applied anywhere in the fixture's solve");
		assertEquals(Technique.LAW_OF_LEFTOVERS, deduction.orElseThrow().technique());
		StrategyFixtures.assertAgreesWithSolution(deduction.orElseThrow(), puzzle);
	}
	
	@Test
	void find_emptyGrid_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(StrategyFixtures.empty(GridSize.NINE));
		
		assertTrue(new LawOfLeftovers().find(grid).isEmpty());
	}
	
	@Test
	void find_solvedGrid_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(StrategyFixtures.solved(puzzle()));
		
		assertTrue(new LawOfLeftovers().find(grid).isEmpty());
	}
	
	@Test
	void find_nullGrid_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> new LawOfLeftovers().find(null));
	}
}
