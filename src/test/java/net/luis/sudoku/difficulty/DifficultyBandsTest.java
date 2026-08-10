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
		// The seed is searched for rather than pinned: the generator misses Lisa on some seeds by design, and a
		// pinned one also breaks on every GenVersion bump, because the key's version feeds the random stream.
		for (long seed = 0; seed < 16; seed++) {
			var key = PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.LISA, seed);
			var generated = PuzzleGenerator.generate(key);
			if (generated.rated() == Difficulty.LISA) {
				return TechniqueSolver.solve(generated.puzzle());
			}
		}
		throw new AssertionError("No seed in the first 16 produced a Lisa 9x9 puzzle");
	}
	
	@Test
	void ceiling_isTheHardestSupportedBandOfTheSize() {
		assertAll(
			() -> assertEquals(Difficulty.ONE, BANDS.ceiling(GridSize.FOUR, Variant.CLASSIC)),
			() -> assertEquals(Difficulty.EIGHT, BANDS.ceiling(GridSize.SIX, Variant.CLASSIC)),
			() -> assertEquals(Difficulty.LISA, BANDS.ceiling(GridSize.NINE, Variant.CLASSIC)),
			() -> assertEquals(Difficulty.LISA, BANDS.ceiling(GridSize.TWELVE, Variant.CLASSIC)),
			() -> assertEquals(Difficulty.LISA, BANDS.ceiling(GridSize.SIXTEEN, Variant.CLASSIC))
		);
	}
	
	@Test
	void supported_smallSizes_areTheMeasuredSets() {
		assertAll(
			// A 4x4 grid produces band 1 and nothing else.
			() -> assertEquals(Set.of(Difficulty.ONE), BANDS.supported(GridSize.FOUR, Variant.CLASSIC)),
			// A 6x6 grid has a gap: it steps from band 3 straight to band 7.
			() -> assertEquals(
				Set.of(Difficulty.ONE, Difficulty.TWO, Difficulty.THREE, Difficulty.SEVEN, Difficulty.EIGHT),
				BANDS.supported(GridSize.SIX, Variant.CLASSIC))
		);
	}
	
	@Test
	void supported_ninePlus_isEveryBand() {
		for (GridSize size : new GridSize[] { GridSize.NINE, GridSize.TWELVE, GridSize.SIXTEEN }) {
			assertEquals(Difficulty.values().length, BANDS.supported(size, Variant.CLASSIC).size(), size.toString());
		}
	}
	
	@Test
	void supported_sixteenByChaos_stopsAtBandEight() {
		// The measurement the variant split exists for: at 32 seeds a band, 16x16 chaos lands its target 17 times
		// out of 32 at band 8 and only 9, 7, 6, 7, 4, 3 and 4 times above it. Band 8 is the last band it reaches
		// more often than it misses, so it is the last one promised.
		assertAll(
			() -> assertEquals(Difficulty.EIGHT, BANDS.ceiling(GridSize.SIXTEEN, Variant.CHAOS)),
			() -> assertEquals(8, BANDS.supported(GridSize.SIXTEEN, Variant.CHAOS).size()),
			() -> assertFalse(BANDS.supported(GridSize.SIXTEEN, Variant.CHAOS).contains(Difficulty.LISA)),
			// Classic at the same size reaches all fifteen, which is the whole point of keying by variant.
			() -> assertEquals(Difficulty.LISA, BANDS.ceiling(GridSize.SIXTEEN, Variant.CLASSIC))
		);
	}
	
	@Test
	void supported_everyOtherSize_isTheSameSetInBothVariants() {
		// Only 16x16 splits. Measured: 9x9 chaos hit 32 of 32 at every band and 12x12 chaos 20 to 32 of 32, which
		// is the search's ordinary near-miss rate rather than a wall.
		for (GridSize size : new GridSize[] { GridSize.SIX, GridSize.NINE, GridSize.TWELVE }) {
			assertEquals(BANDS.supported(size, Variant.CLASSIC), BANDS.supported(size, Variant.CHAOS), size.toString());
		}
	}
	
	@Test
	void supported_chaosAtFourByFour_throwsBecauseThereIsNoSuchGrid() {
		assertThrows(IllegalArgumentException.class, () -> BANDS.supported(GridSize.FOUR, Variant.CHAOS));
	}
	
	@Test
	void supported_nullSizeOrVariant_throws() {
		assertAll(
			() -> assertThrows(NullPointerException.class, () -> BANDS.supported(null, Variant.CLASSIC)),
			() -> assertThrows(NullPointerException.class, () -> BANDS.supported(GridSize.NINE, null))
		);
	}
	
	@Test
	void nearestSupported_hardBandOnSixteenByChaos_snapsToBandEight() {
		// The defect this closes: a player asking for band 13 on a 16x16 jigsaw used to be handed a puzzle that
		// rated around 8 while still being labelled 13.
		assertAll(
			() -> assertEquals(Difficulty.EIGHT, BANDS.nearestSupported(GridSize.SIXTEEN, Variant.CHAOS, Difficulty.THIRTEEN)),
			() -> assertEquals(Difficulty.EIGHT, BANDS.nearestSupported(GridSize.SIXTEEN, Variant.CHAOS, Difficulty.LISA)),
			// ...and classic at the same size is untouched.
			() -> assertEquals(Difficulty.THIRTEEN, BANDS.nearestSupported(GridSize.SIXTEEN, Variant.CLASSIC, Difficulty.THIRTEEN))
		);
	}
	
	@Test
	void classify_aboveTheChaosCeiling_clampsPerVariant() {
		TechniqueReport hard = hardNineReport();
		
		assertAll(
			() -> assertEquals(Difficulty.EIGHT, BANDS.classify(GridSize.SIXTEEN, Variant.CHAOS, hard)),
			() -> assertEquals(Difficulty.LISA, BANDS.classify(GridSize.SIXTEEN, Variant.CLASSIC, hard))
		);
	}
	
	@Test
	void supported_returnedSet_isUnmodifiable() {
		assertThrows(UnsupportedOperationException.class, () -> BANDS.supported(GridSize.NINE, Variant.CLASSIC).add(Difficulty.ONE));
	}
	
	@Test
	void nearestSupported_supportedBand_isReturnedUnchanged() {
		assertAll(
			() -> assertEquals(Difficulty.THREE, BANDS.nearestSupported(GridSize.SIX, Variant.CLASSIC, Difficulty.THREE)),
			() -> assertEquals(Difficulty.LISA, BANDS.nearestSupported(GridSize.NINE, Variant.CLASSIC, Difficulty.LISA))
		);
	}
	
	@Test
	void nearestSupported_bandInsideTheSixBySixGap_snapsToTheNearest() {
		assertAll(
			// 4 and 5 are nearer to 3 than to 7; 6 is nearer to 7.
			() -> assertEquals(Difficulty.THREE, BANDS.nearestSupported(GridSize.SIX, Variant.CLASSIC, Difficulty.FOUR)),
			() -> assertEquals(Difficulty.THREE, BANDS.nearestSupported(GridSize.SIX, Variant.CLASSIC, Difficulty.FIVE)),
			() -> assertEquals(Difficulty.SEVEN, BANDS.nearestSupported(GridSize.SIX, Variant.CLASSIC, Difficulty.SIX))
		);
	}
	
	@Test
	void nearestSupported_bandAboveEverySupportedOne_snapsDownToTheCeiling() {
		assertAll(
			() -> assertEquals(Difficulty.ONE, BANDS.nearestSupported(GridSize.FOUR, Variant.CLASSIC, Difficulty.LISA)),
			() -> assertEquals(Difficulty.EIGHT, BANDS.nearestSupported(GridSize.SIX, Variant.CLASSIC, Difficulty.LISA))
		);
	}
	
	@Test
	void nearestSupported_nullBand_throws() {
		assertThrows(NullPointerException.class, () -> BANDS.nearestSupported(GridSize.NINE, Variant.CLASSIC, null));
	}
	
	@Test
	void classify_noTechniqueRequired_isBandOne() {
		TechniqueReport report = reportOf(SOLVED_FOUR, GridSize.FOUR);
		
		assertEquals(Difficulty.ONE, BANDS.classify(GridSize.FOUR, Variant.CLASSIC, report));
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
		assertEquals(Difficulty.ONE, BANDS.classify(GridSize.FOUR, Variant.CLASSIC, report));
	}
	
	@Test
	void classify_hardReport_isLisaAtNine() {
		assertEquals(Difficulty.LISA, BANDS.classify(GridSize.NINE, Variant.CLASSIC, hardNineReport()));
	}
	
	@Test
	void classify_bandAboveCeiling_clampsToCeiling() {
		// The same report that rates LISA at NINE must clamp to each smaller grid's ceiling.
		TechniqueReport hard = hardNineReport();
		
		assertAll(
			() -> assertEquals(Difficulty.ONE, BANDS.classify(GridSize.FOUR, Variant.CLASSIC, hard)),
			() -> assertEquals(Difficulty.EIGHT, BANDS.classify(GridSize.SIX, Variant.CLASSIC, hard))
		);
	}
	
	@Test
	void classify_reportThatStoppedAtALevelCap_throws() {
		var key = PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.LISA, 0L);
		TechniqueReport capped = TechniqueSolver.solve(PuzzleGenerator.generate(key).puzzle(), 2);
		
		assertTrue(capped.exceededCap());
		assertThrows(IllegalArgumentException.class, () -> BANDS.classify(GridSize.NINE, Variant.CLASSIC, capped));
	}
}
