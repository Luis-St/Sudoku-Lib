package net.luis.sudoku.generation;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.grid.Variant;
import net.luis.sudoku.key.PuzzleKey;
import net.luis.sudoku.version.GenVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Golden-fixture regression tests: a handful of {@code (genVersion=2, size, variant, difficulty, seed)} keys pinned to
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
		assertEquals(2, GenVersion.CURRENT, "The golden fixtures were pinned for genVersion 2");
		GeneratedPuzzle generated = PuzzleGenerator.generate(PuzzleKey.of(size, variant, difficulty, seed));
		
		assertAll(size + "/" + variant + "/" + difficulty + "/seed=" + seed,
			() -> assertArrayEquals(cells(givens), generated.puzzle().values(), "Givens drifted"),
			() -> assertArrayEquals(cells(solution), generated.solution(), "Solution drifted")
		);
	}
	
	@Test
	void generate_nineClassicThree_matchesGolden() {
		assertGolden(GridSize.NINE, Variant.CLASSIC, Difficulty.THREE, 42L,
			"090008000005046000000000090007209000001080270800000904030007609070400003286000017",
			"693728541715946382428135796347259168961384275852671934534817629179462853286593417");
	}
	
	@Test
	void generate_fourClassicOne_matchesGolden() {
		assertGolden(GridSize.FOUR, Variant.CLASSIC, Difficulty.ONE, 7L,
			"4123000112043010",
			"4123234112343412");
	}
	
	@Test
	void generate_sixChaosTwo_matchesGolden() {
		assertGolden(GridSize.SIX, Variant.CHAOS, Difficulty.TWO, 99L,
			"000025000300000000605410100004040000",
			"364125512346431652625413156234243561");
	}
	
	@Test
	void generate_nineChaosThree_matchesGolden() {
		assertGolden(GridSize.NINE, Variant.CHAOS, Difficulty.THREE, 1L,
			"007000000201703004300840070500000016000001008002006000000008700105020040090000025",
			"847165932261793584359842671584937216736251498912486357423518769175629843698374125");
	}
}
