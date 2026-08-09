package net.luis.sudoku.bench;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.generation.GeneratedPuzzle;
import net.luis.sudoku.generation.PuzzleGenerator;
import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.grid.Variant;
import net.luis.sudoku.key.PuzzleKey;
import net.luis.sudoku.solver.*;

import java.util.EnumMap;
import java.util.Map;

/**
 * Finds, for every technique, a generated puzzle whose solve actually applies it, and prints it as a pinned fixture.
 * <p>
 *     This is the source of the per-strategy regression fixtures: a technique that no reachable puzzle exercises
 *     cannot be given an honest fixture test, and this says plainly which ones those are instead of inventing a grid.
 * </p>
 */
public final class TechniqueCoverage {
	
	private TechniqueCoverage() {}
	
	public static void main(String[] args) {
		int seeds = Integer.getInteger("seeds", 12);
		Map<Technique, String> fixtures = new EnumMap<>(Technique.class);
		Map<Technique, Integer> counts = new EnumMap<>(Technique.class);
		
		for (GridSize size : new GridSize[] { GridSize.NINE, GridSize.TWELVE }) {
			for (Variant variant : Variant.values()) {
				for (Difficulty difficulty : Difficulty.values()) {
					for (long seed = 0; seed < seeds; seed++) {
						PuzzleKey key = PuzzleKey.of(size, variant, difficulty, seed);
						GeneratedPuzzle generated = PuzzleGenerator.generate(key);
						TechniqueReport report = TechniqueSolver.solve(generated.puzzle());
						String fixture = size + "|" + variant + "|" + difficulty + "|" + seed + "|" + hex(generated.puzzle().values());
						for (Map.Entry<Technique, Integer> entry : report.usage().entrySet()) {
							counts.merge(entry.getKey(), entry.getValue(), Integer::sum);
							fixtures.putIfAbsent(entry.getKey(), fixture);
						}
					}
				}
			}
		}
		
		System.out.println("== covered ==");
		for (Technique technique : Technique.values()) {
			if (fixtures.containsKey(technique)) {
				System.out.println(technique + "|" + counts.get(technique) + "|" + fixtures.get(technique));
			}
		}
		System.out.println("== never fired ==");
		for (Technique technique : Technique.values()) {
			if (!fixtures.containsKey(technique)) {
				System.out.println(technique);
			}
		}
	}
	
	private static String hex(int[] values) {
		StringBuilder builder = new StringBuilder(values.length);
		for (int value : values) {
			builder.append(Character.forDigit(value, 16));
		}
		return builder.toString();
	}
}
