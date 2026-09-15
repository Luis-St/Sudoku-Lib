package net.luis.sudoku.generation;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.grid.Variant;
import net.luis.sudoku.key.PuzzleKey;
import net.luis.sudoku.version.GenVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Golden-fixture regression tests: a handful of {@code (genVersion=5, size, variant, difficulty, seed)} keys pinned to
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
		assertEquals(5, GenVersion.CURRENT, "The golden fixtures were pinned for genVersion 5");
		GeneratedPuzzle generated = PuzzleGenerator.generate(PuzzleKey.of(size, variant, difficulty, seed));
		
		assertAll(size + "/" + variant + "/" + difficulty + "/seed=" + seed,
			() -> assertArrayEquals(cells(givens), generated.puzzle().values(), "Givens drifted"),
			() -> assertArrayEquals(cells(solution), generated.solution(), "Solution drifted")
		);
	}
	
	@Test
	void generate_nineClassicThree_matchesGolden() {
		assertGolden(GridSize.NINE, Variant.CLASSIC, Difficulty.THREE, 42L,
			"000000070000390206260700000300040810000100609059600704500030027001500003000000000",
			"914862375785391246263754981376945812842173659159628734598436127621587493437219568");
	}
	
	@Test
	void generate_fourClassicOne_matchesGolden() {
		assertGolden(GridSize.FOUR, Variant.CLASSIC, Difficulty.ONE, 7L,
			"1004041023410103",
			"1234341223414123");
	}
	
	@Test
	void generate_sixChaosTwo_matchesGolden() {
		assertGolden(GridSize.SIX, Variant.CHAOS, Difficulty.TWO, 99L,
			"000000000600160204230000000000000050",
			"426513351642165234234165513426642351");
	}
	
	@Test
	void generate_nineChaosThree_matchesGolden() {
		assertGolden(GridSize.NINE, Variant.CHAOS, Difficulty.THREE, 1L,
			"370500680200098000000100000032400000080000096100050040000800004003905008900000052",
			"371542689254698731869173425632489517485317296197256843526831974743925168918764352");
	}
}
