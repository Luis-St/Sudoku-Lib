package net.luis.sudoku.generation;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.grid.Variant;
import net.luis.sudoku.key.PuzzleKey;
import net.luis.sudoku.version.GenVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Golden-fixture regression tests: a handful of {@code (genVersion=4, size, variant, difficulty, seed)} keys pinned to
 * their exact generated grids.
 * <p>
 *     These freeze the generator's output so that any future change to the generator, the region builder, the
 *     hole-digging order or the difficulty rater is caught immediately. A deliberate change to any of those is a
 *     {@link GenVersion} bump (spec §3.5); an accidental one is a bug, and one of these assertions will fail. Each grid
 *     is written one hex character per cell ({@code 0} = empty), which is unambiguous here because every pinned size
 *     uses digits {@code 1..9}.
 * </p>
 */
class GenerationGoldenTest {
	
	private static int[] cells(String hex) {
		int[] values = new int[hex.length()];
		for (int index = 0; index < hex.length(); index++) {
			values[index] = Character.digit(hex.charAt(index), 16);
		}
		return values;
	}
	
	private static void assertGolden(GridSize size, Variant variant, Difficulty difficulty, long seed, String givens, String solution) {
		assertEquals(4, GenVersion.CURRENT, "The golden fixtures were pinned for genVersion 4");
		GeneratedPuzzle generated = PuzzleGenerator.generate(PuzzleKey.of(size, variant, difficulty, seed));
		
		assertAll(size + "/" + variant + "/" + difficulty + "/seed=" + seed,
			() -> assertArrayEquals(cells(givens), generated.puzzle().values(), "Givens drifted"),
			() -> assertArrayEquals(cells(solution), generated.solution(), "Solution drifted")
		);
	}
	
	@Test
	void generate_nineClassicThree_matchesGolden() {
		assertGolden(GridSize.NINE, Variant.CLASSIC, Difficulty.THREE, 42L,
			"000006000000050300060000054740280001509040800002031000000900000420060790037000000",
			"375426189294158376168793254743289561519647832682531947856972413421365798937814625");
	}
	
	@Test
	void generate_fourClassicOne_matchesGolden() {
		assertGolden(GridSize.FOUR, Variant.CLASSIC, Difficulty.ONE, 7L,
			"4103204104100034",
			"4123234134121234");
	}
	
	@Test
	void generate_sixChaosTwo_matchesGolden() {
		assertGolden(GridSize.SIX, Variant.CHAOS, Difficulty.TWO, 99L,
			"000204100000300000000405000002000041",
			"536214124653345126612435451362263541");
	}
	
	@Test
	void generate_nineChaosThree_matchesGolden() {
		assertGolden(GridSize.NINE, Variant.CHAOS, Difficulty.THREE, 1L,
			"000000407000300982800090003009002030000084100104570890091740258278005040050009300",
			"963258417745361982812497563589612734637984125124573896391746258278135649456829371");
	}
}
