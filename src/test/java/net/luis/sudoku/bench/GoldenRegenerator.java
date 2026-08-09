package net.luis.sudoku.bench;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.generation.GeneratedPuzzle;
import net.luis.sudoku.generation.PuzzleGenerator;
import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.grid.Variant;
import net.luis.sudoku.key.KeyDerivation;
import net.luis.sudoku.key.PuzzleKey;
import net.luis.sudoku.sharecode.ShareCodeCodec;

/**
 * Reprints every pinned regression value that moves when the generator or the difficulty scale changes.
 * <p>
 *     Run it with {@code ./gradlew probe -Pmain=net.luis.sudoku.bench.GoldenRegenerator} after any deliberate change
 *     to the generator, the rater or the band thresholds, and paste the output into {@code GenerationGoldenTest},
 *     {@code KeyDerivationTest} and {@code ShareCodeCodecTest}. Those goldens exist to catch <b>accidental</b> drift,
 *     so regenerating them is only ever correct alongside a {@code GenVersion} bump.
 * </p>
 */
public final class GoldenRegenerator {
	
	private GoldenRegenerator() {}
	
	public static void main(String[] args) {
		System.out.println("== GenerationGoldenTest ==");
		print(GridSize.NINE, Variant.CLASSIC, Difficulty.THREE, 42L);
		print(GridSize.FOUR, Variant.CLASSIC, Difficulty.ONE, 7L);
		print(GridSize.SIX, Variant.CHAOS, Difficulty.TWO, 99L);
		print(GridSize.NINE, Variant.CHAOS, Difficulty.THREE, 1L);
		
		System.out.println("== KeyDerivationTest / ShareCodeCodecTest ==");
		PuzzleKey one = new PuzzleKey(1, GridSize.NINE, Variant.CLASSIC, Difficulty.THREE, 0L);
		PuzzleKey two = new PuzzleKey(2, GridSize.SIXTEEN, Variant.CHAOS, Difficulty.LISA, -1L);
		System.out.println("  deriveState ONE = " + KeyDerivation.deriveState(one) + "L");
		System.out.println("  deriveState TWO = " + KeyDerivation.deriveState(two) + "L");
		System.out.println("  shareCode   ONE = " + ShareCodeCodec.encode(one));
		System.out.println("  shareCode   TWO = " + ShareCodeCodec.encode(two));
	}
	
	private static void print(GridSize size, Variant variant, Difficulty difficulty, long seed) {
		GeneratedPuzzle generated = PuzzleGenerator.generate(PuzzleKey.of(size, variant, difficulty, seed));
		System.out.println(size + "/" + variant + "/" + difficulty + "/seed=" + seed);
		System.out.println("  \"" + hex(generated.puzzle().values()) + "\",");
		System.out.println("  \"" + hex(generated.solution()) + "\");");
	}
	
	private static String hex(int[] values) {
		StringBuilder builder = new StringBuilder(values.length);
		for (int value : values) {
			builder.append(Character.forDigit(value, 16));
		}
		return builder.toString();
	}
}
