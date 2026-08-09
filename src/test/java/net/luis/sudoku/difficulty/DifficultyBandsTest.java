package net.luis.sudoku.difficulty;

import net.luis.sudoku.generation.PuzzleGenerator;
import net.luis.sudoku.grid.*;
import net.luis.sudoku.key.PuzzleKey;
import net.luis.sudoku.solver.TechniqueReport;
import net.luis.sudoku.solver.TechniqueSolver;
import org.junit.jupiter.api.Test;

import java.util.Set;

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
	 * Produces the report of a genuinely hard 9x9 puzzle, used to check the per-size clamp.
	 * <p>
	 *     This deliberately no longer looks for a <i>stuck</i> report. Since level 15 gained the techniques that
	 *     assume a candidate and play the position out, the solver finishes essentially every puzzle the generator
	 *     can produce, so a scan for a stuck one runs to its bound and fails.
	 * </p>
	 */
	private static TechniqueReport hardNineReport() {
		var key = PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.LISA, 0L);
		return TechniqueSolver.solve(PuzzleGenerator.generate(key).puzzle());
	}
	
	@Test
	void ceiling_isTheHardestSupportedBandOfTheSize() {
		assertAll(
			() -> assertEquals(Difficulty.ONE, BANDS.ceiling(GridSize.FOUR)),
			() -> assertEquals(Difficulty.EIGHT, BANDS.ceiling(GridSize.SIX)),
			() -> assertEquals(Difficulty.LISA, BANDS.ceiling(GridSize.NINE)),
			() -> assertEquals(Difficulty.LISA, BANDS.ceiling(GridSize.TWELVE)),
			() -> assertEquals(Difficulty.LISA, BANDS.ceiling(GridSize.SIXTEEN))
		);
	}
	
	@Test
	void supported_smallSizes_areTheMeasuredSets() {
		assertAll(
			// A 4x4 grid produces band 1 and nothing else.
			() -> assertEquals(Set.of(Difficulty.ONE), BANDS.supported(GridSize.FOUR)),
			// A 6x6 grid has a gap: it steps from band 3 straight to band 7.
			() -> assertEquals(
				Set.of(Difficulty.ONE, Difficulty.TWO, Difficulty.THREE, Difficulty.SEVEN, Difficulty.EIGHT),
				BANDS.supported(GridSize.SIX))
		);
	}
	
	@Test
	void supported_ninePlus_isEveryBand() {
		for (GridSize size : new GridSize[] { GridSize.NINE, GridSize.TWELVE, GridSize.SIXTEEN }) {
			assertEquals(Difficulty.values().length, BANDS.supported(size).size(), size.toString());
		}
	}
	
	@Test
	void supported_returnedSet_isUnmodifiable() {
		assertThrows(UnsupportedOperationException.class, () -> BANDS.supported(GridSize.NINE).add(Difficulty.ONE));
	}
	
	@Test
	void nearestSupported_supportedBand_isReturnedUnchanged() {
		assertAll(
			() -> assertEquals(Difficulty.THREE, BANDS.nearestSupported(GridSize.SIX, Difficulty.THREE)),
			() -> assertEquals(Difficulty.LISA, BANDS.nearestSupported(GridSize.NINE, Difficulty.LISA))
		);
	}
	
	@Test
	void nearestSupported_bandInsideTheSixBySixGap_snapsToTheNearest() {
		assertAll(
			// 4 and 5 are nearer to 3 than to 7; 6 is nearer to 7.
			() -> assertEquals(Difficulty.THREE, BANDS.nearestSupported(GridSize.SIX, Difficulty.FOUR)),
			() -> assertEquals(Difficulty.THREE, BANDS.nearestSupported(GridSize.SIX, Difficulty.FIVE)),
			() -> assertEquals(Difficulty.SEVEN, BANDS.nearestSupported(GridSize.SIX, Difficulty.SIX))
		);
	}
	
	@Test
	void nearestSupported_bandAboveEverySupportedOne_snapsDownToTheCeiling() {
		assertAll(
			() -> assertEquals(Difficulty.ONE, BANDS.nearestSupported(GridSize.FOUR, Difficulty.LISA)),
			() -> assertEquals(Difficulty.EIGHT, BANDS.nearestSupported(GridSize.SIX, Difficulty.LISA))
		);
	}
	
	@Test
	void nearestSupported_nullBand_throws() {
		assertThrows(NullPointerException.class, () -> BANDS.nearestSupported(GridSize.NINE, null));
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
	void classify_hardReport_isLisaAtNine() {
		assertEquals(Difficulty.LISA, BANDS.classify(GridSize.NINE, hardNineReport()));
	}
	
	@Test
	void classify_bandAboveCeiling_clampsToCeiling() {
		// The same report that rates LISA at NINE must clamp to each smaller grid's ceiling.
		TechniqueReport hard = hardNineReport();
		
		assertAll(
			() -> assertEquals(Difficulty.ONE, BANDS.classify(GridSize.FOUR, hard)),
			() -> assertEquals(Difficulty.EIGHT, BANDS.classify(GridSize.SIX, hard))
		);
	}
	
	@Test
	void classify_reportThatStoppedAtALevelCap_throws() {
		var key = PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.LISA, 0L);
		TechniqueReport capped = TechniqueSolver.solve(PuzzleGenerator.generate(key).puzzle(), 2);
		
		assertTrue(capped.exceededCap());
		assertThrows(IllegalArgumentException.class, () -> BANDS.classify(GridSize.NINE, capped));
	}
}
