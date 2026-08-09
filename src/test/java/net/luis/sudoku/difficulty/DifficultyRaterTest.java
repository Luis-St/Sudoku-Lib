package net.luis.sudoku.difficulty;

import net.luis.sudoku.generation.GeneratedPuzzle;
import net.luis.sudoku.generation.PuzzleGenerator;
import net.luis.sudoku.grid.*;
import net.luis.sudoku.key.PuzzleKey;
import net.luis.sudoku.solver.TechniqueReport;
import net.luis.sudoku.solver.TechniqueSolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DifficultyRaterTest {
	
	private static final DifficultyRater RATER = new DifficultyRater();
	
	@Test
	void rate_singlesOnlyPuzzle_isBandOne() {
		int[] givens = {
			1, 2, 3, 4,
			3, 4, 1, 2,
			2, 1, 4, 3,
			4, 3, 0, 0
		};
		Puzzle puzzle = Puzzle.classicOfGivens(GridSize.FOUR, givens);
		
		assertEquals(Difficulty.ONE, RATER.rate(puzzle));
	}
	
	@Test
	void rate_lisaRequest_doesReturnLisa() {
		// Lisa is a rating of its own now: level 15 names the techniques that assume a candidate and play the
		// position out, so a Lisa request that lands is genuinely rated Lisa rather than clamped below it.
		GeneratedPuzzle generated = PuzzleGenerator.generate(PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.LISA, 0L));
		
		assertEquals(Difficulty.LISA, RATER.rate(generated.puzzle()));
	}
	
	@Test
	void rate_isDeterministic() {
		Puzzle puzzle = PuzzleGenerator.generate(PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.THREE, 42L)).puzzle();
		
		assertEquals(RATER.rate(puzzle), RATER.rate(puzzle));
	}
	
	@Test
	void rate_withPrecomputedReport_matchesSolveThenClassify() {
		Puzzle puzzle = PuzzleGenerator.generate(PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.TWO, 3L)).puzzle();
		TechniqueReport report = TechniqueSolver.solve(puzzle);
		
		assertEquals(RATER.rate(puzzle), RATER.rate(GridSize.NINE, report));
	}
	
	@Test
	void rate_nullPuzzle_throws() {
		assertThrows(NullPointerException.class, () -> RATER.rate(null));
	}
	
	@Test
	void constructor_nullBands_throws() {
		assertThrows(NullPointerException.class, () -> new DifficultyRater(null));
	}
	
	@Test
	void bands_returnsTheConfiguredBands() {
		DifficultyBands bands = DifficultyBands.defaults();
		
		assertSame(bands, new DifficultyRater(bands).bands());
	}
}
