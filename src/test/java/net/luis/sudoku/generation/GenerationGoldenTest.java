package net.luis.sudoku.generation;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.grid.Variant;
import net.luis.sudoku.key.PuzzleKey;
import net.luis.sudoku.version.GenVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Golden-fixture regression tests: a handful of {@code (genVersion=1, size, variant, difficulty, seed)} keys pinned to
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
		assertEquals(1, GenVersion.CURRENT, "The golden fixtures were pinned for genVersion 1");
		GeneratedPuzzle generated = PuzzleGenerator.generate(PuzzleKey.of(size, variant, difficulty, seed));
		
		assertAll(size + "/" + variant + "/" + difficulty + "/seed=" + seed,
			() -> assertArrayEquals(cells(givens), generated.puzzle().values(), "Givens drifted"),
			() -> assertArrayEquals(cells(solution), generated.solution(), "Solution drifted")
		);
	}
	
	@Test
	void generate_nineClassicThree_matchesGolden() {
		assertGolden(GridSize.NINE, Variant.CLASSIC, Difficulty.THREE, 42L,
			"000500000090200700020000300500007023000000601000006500064300007010009008002060000",
			"647593812395218746128674395586147923439825671271936584964382157713459268852761439");
	}
	
	@Test
	void generate_fourClassicOne_matchesGolden() {
		assertGolden(GridSize.FOUR, Variant.CLASSIC, Difficulty.ONE, 7L,
			"0104000000300001",
			"3124421314322341");
	}
	
	@Test
	void generate_sixChaosTwo_matchesGolden() {
		assertGolden(GridSize.SIX, Variant.CHAOS, Difficulty.TWO, 99L,
			"000240024056430605615000060300240500",
			"156243324156432615615432561324243561");
	}
	
	@Test
	void generate_nineChaosThree_matchesGolden() {
		assertGolden(GridSize.NINE, Variant.CHAOS, Difficulty.THREE, 1L,
			"793018000082030000605492080009104070050200610174000009801700030507906001906080547",
			"793518462482637195615492783269154378358279614174863259841725936537946821926381547");
	}
}
