package net.luis.sudoku.generation;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.grid.Variant;
import net.luis.sudoku.key.PuzzleKey;
import net.luis.sudoku.version.GenVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Golden-fixture regression tests: a handful of {@code (genVersion=3, size, variant, difficulty, seed)} keys pinned to
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
		assertEquals(3, GenVersion.CURRENT, "The golden fixtures were pinned for genVersion 3");
		GeneratedPuzzle generated = PuzzleGenerator.generate(PuzzleKey.of(size, variant, difficulty, seed));
		
		assertAll(size + "/" + variant + "/" + difficulty + "/seed=" + seed,
			() -> assertArrayEquals(cells(givens), generated.puzzle().values(), "Givens drifted"),
			() -> assertArrayEquals(cells(solution), generated.solution(), "Solution drifted")
		);
	}
	
	@Test
	void generate_nineClassicThree_matchesGolden() {
		assertGolden(GridSize.NINE, Variant.CLASSIC, Difficulty.THREE, 42L,
			"071000069000600000050908034609000008080007000700000016308020650040036090000000800",
			"871543269934672581256918734619254378483167925725389416398421657547836192162795843");
	}
	
	@Test
	void generate_fourClassicOne_matchesGolden() {
		assertGolden(GridSize.FOUR, Variant.CLASSIC, Difficulty.ONE, 7L,
			"4012124021303020",
			"4312124321343421");
	}
	
	@Test
	void generate_sixChaosTwo_matchesGolden() {
		assertGolden(GridSize.SIX, Variant.CHAOS, Difficulty.TWO, 99L,
			"000003030020241000006000023064400000",
			"512643634125241356356412123564465231");
	}
	
	@Test
	void generate_nineChaosThree_matchesGolden() {
		assertGolden(GridSize.NINE, Variant.CHAOS, Difficulty.THREE, 1L,
			"020000000750203190010000008360805002000430009040106000030901060000000304600300920",
			"823619547754283196916754238367895412281437659549126783432971865198562374675348921");
	}
}
