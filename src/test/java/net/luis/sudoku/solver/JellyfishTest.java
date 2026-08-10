package net.luis.sudoku.solver;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.grid.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link Jellyfish}.
 */
class JellyfishTest {
	
	/**
	 * A generated TWELVE CLASSIC puzzle (band LISA, seed 28) whose solve applies the technique 1 time(s).
	 * <p>
	 *     Pinned as a key rather than as givens, so that the fixture is re-derived from the generator that
	 *     is actually shipping rather than frozen at whatever it produced once.
	 * </p>
	 */
	private static Puzzle puzzle() {
		return StrategyFixtures.generated(GridSize.TWELVE, Variant.CLASSIC, Difficulty.LISA, 28L);
	}
	
	@Test
	void technique_returnsTheMatchingConstant() {
		assertEquals(Technique.JELLYFISH, new Jellyfish().technique());
	}
	
	@Test
	void find_puzzleThatNeedsIt_appliesItDuringTheSolve() {
		TechniqueReport report = TechniqueSolver.solve(puzzle());
		
		assertAll(
			() -> assertTrue(report.solved(), "The fixture must solve with techniques alone"),
			() -> assertTrue(report.count(Technique.JELLYFISH) >= 1,
				"The fixture must still need JELLYFISH, but the solve used " + report.usage())
		);
	}
	
	@Test
	void find_firstStateItApplies_agreesWithTheTrueSolution() {
		Puzzle puzzle = puzzle();
		Optional<Deduction> deduction = StrategyFixtures.firstFiring(new Jellyfish(), puzzle);
		
		assertTrue(deduction.isPresent(), "JELLYFISH never applied anywhere in the fixture's solve");
		assertEquals(Technique.JELLYFISH, deduction.orElseThrow().technique());
		StrategyFixtures.assertAgreesWithSolution(deduction.orElseThrow(), puzzle);
	}
	
	@Test
	void find_emptyGrid_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(StrategyFixtures.empty(GridSize.TWELVE));
		
		assertTrue(new Jellyfish().find(grid).isEmpty());
	}
	
	@Test
	void find_solvedGrid_returnsEmpty() {
		CandidateGrid grid = new CandidateGrid(StrategyFixtures.solved(puzzle()));
		
		assertTrue(new Jellyfish().find(grid).isEmpty());
	}
	
	@Test
	void find_nullGrid_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> new Jellyfish().find(null));
	}
}
