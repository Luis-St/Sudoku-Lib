package net.luis.sudoku.difficulty;

import net.luis.sudoku.grid.*;
import net.luis.sudoku.solver.TechniqueReport;
import net.luis.sudoku.solver.TechniqueSolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DifficultyBandsTest {
	
	private static final DifficultyBands BANDS = DifficultyBands.defaults();
	// A complete, valid 4x4 grid: no technique fires, so the report's hardest technique is empty.
	private static final int[] SOLVED_FOUR = {
		1, 2, 3, 4,
		3, 4, 1, 2,
		2, 1, 4, 3,
		4, 3, 2, 1
	};
	
	private static TechniqueReport reportOf(int[] givens, GridSize size) {
		return TechniqueSolver.solve(Puzzle.classicOfGivens(size, givens));
	}
	
	/**
	 * Produces a technique report that is genuinely stuck by scanning generated hard 9x9 puzzles until the technique
	 * solver fails to finish one. Such puzzles are common at the hardest difficulty, so the scan is bounded and fast.
	 */
	private static TechniqueReport stuckNineReport() {
		for (long seed = 0; seed < 200; seed++) {
			var key = net.luis.sudoku.key.PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.FIVE, seed);
			var puzzle = net.luis.sudoku.generation.PuzzleGenerator.generate(key).puzzle();
			TechniqueReport report = TechniqueSolver.solve(puzzle);
			if (report.stuck()) {
				return report;
			}
		}
		return fail("Could not find a stuck 9x9 puzzle within 200 seeds");
	}
	
	@Test
	void ceiling_isTheDocumentedPerSizeCap() {
		assertAll(
			() -> assertEquals(Difficulty.TWO, BANDS.ceiling(GridSize.FOUR)),
			() -> assertEquals(Difficulty.THREE, BANDS.ceiling(GridSize.SIX)),
			() -> assertEquals(Difficulty.FIVE, BANDS.ceiling(GridSize.NINE)),
			() -> assertEquals(Difficulty.FIVE, BANDS.ceiling(GridSize.TWELVE)),
			() -> assertEquals(Difficulty.FIVE, BANDS.ceiling(GridSize.SIXTEEN))
		);
	}
	
	@Test
	void ceiling_isNeverLisa() {
		for (GridSize size : GridSize.values()) {
			assertNotEquals(Difficulty.LISA, BANDS.ceiling(size));
		}
	}
	
	@Test
	void classify_noTechniqueRequired_isBandOne() {
		TechniqueReport report = reportOf(SOLVED_FOUR, GridSize.FOUR);
		
		assertEquals(Difficulty.ONE, BANDS.classify(GridSize.FOUR, report));
	}
	
	@Test
	void classify_singlesOnlyPuzzle_isBandOne() {
		// A 4x4 with two holes that resolve to naked/hidden singles only.
		int[] givens = {
			1, 2, 3, 4,
			3, 4, 1, 2,
			2, 1, 4, 3,
			4, 3, 0, 0
		};
		TechniqueReport report = reportOf(givens, GridSize.FOUR);
		
		assertTrue(report.solved());
		assertEquals(Difficulty.ONE, BANDS.classify(GridSize.FOUR, report));
	}
	
	@Test
	void classify_stuckReport_isBandFiveAtNine() {
		TechniqueReport stuck = stuckNineReport();
		
		assertEquals(Difficulty.FIVE, BANDS.classify(GridSize.NINE, stuck));
	}
	
	@Test
	void classify_bandAboveCeiling_clampsToCeiling() {
		// The same stuck report that rates FIVE at NINE must clamp to the FOUR grid's ceiling of TWO.
		TechniqueReport stuck = stuckNineReport();
		
		assertEquals(Difficulty.TWO, BANDS.classify(GridSize.FOUR, stuck));
		assertEquals(Difficulty.THREE, BANDS.classify(GridSize.SIX, stuck));
	}
}
