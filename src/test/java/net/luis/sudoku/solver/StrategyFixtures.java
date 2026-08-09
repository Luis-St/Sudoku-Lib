package net.luis.sudoku.solver;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.generation.PuzzleGenerator;
import net.luis.sudoku.grid.*;
import net.luis.sudoku.key.PuzzleKey;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Shared scaffolding for the per-strategy test classes.
 * <p>
 *     Every strategy is tested the same way, against a pinned puzzle that genuinely needs it, so the mechanics live
 *     here rather than being copied into three dozen classes. The interesting part of each test class is its fixture
 *     and the technique it names.
 * </p>
 */
final class StrategyFixtures {
	
	private StrategyFixtures() {}
	
	/**
	 * Parses a pinned classic puzzle written one hex character per cell, {@code 0} for an empty cell.
	 *
	 * @param size The grid size the string describes
	 * @param hex The row-major cell values
	 * @return The puzzle
	 */
	static Puzzle classic(GridSize size, String hex) {
		int[] values = new int[hex.length()];
		for (int index = 0; index < hex.length(); index++) {
			values[index] = Character.digit(hex.charAt(index), 16);
		}
		return Puzzle.classicOfGivens(size, values);
	}
	
	/**
	 * Regenerates a pinned puzzle from its key.
	 * <p>
	 *     Used for the chaos fixtures only. A jigsaw puzzle is not described by its givens alone — the region layout
	 *     is half of it — so those fixtures pin the key and let the generator rebuild the layout, which makes them a
	 *     regression on the generator as well.
	 * </p>
	 */
	static Puzzle generated(GridSize size, Variant variant, Difficulty difficulty, long seed) {
		return PuzzleGenerator.generate(PuzzleKey.of(size, variant, difficulty, seed)).puzzle();
	}
	
	static Puzzle empty(GridSize size) {
		return Puzzle.empty(size, Variant.CLASSIC, ClassicRegionPartition.of(size));
	}
	
	/**
	 * Returns the puzzle's own solution as a complete grid, on which no technique may find anything.
	 */
	static Puzzle solved(Puzzle puzzle) {
		return Puzzle.ofGivens(puzzle.size(), Variant.CLASSIC, puzzle.partition(), BacktrackingSolver.solve(puzzle).orElseThrow());
	}
	
	/**
	 * Replays the solver over the puzzle and returns the first deduction the given strategy offers on any state the
	 * solver passes through.
	 * <p>
	 *     Walking the solve matters: the harder techniques cannot fire on a fresh grid at all, because they need the
	 *     candidates the easier techniques have already narrowed.
	 * </p>
	 *
	 * @param strategy The strategy to watch for
	 * @param puzzle The puzzle to walk
	 * @return The first deduction the strategy offers, or empty if it never applies anywhere in the solve
	 */
	static Optional<Deduction> firstFiring(TechniqueStrategy strategy, Puzzle puzzle) {
		CandidateGrid grid = new CandidateGrid(puzzle);
		while (!grid.isComplete()) {
			Optional<Deduction> watched = strategy.find(grid);
			if (watched.isPresent()) {
				return watched;
			}
			
			Deduction next = null;
			for (TechniqueStrategy candidate : TechniqueSolver.STRATEGIES) {
				Optional<Deduction> found = candidate.find(grid);
				if (found.isPresent()) {
					next = found.orElseThrow();
					break;
				}
			}
			
			if (next == null) {
				return Optional.empty();
			}
			next.applyTo(grid);
		}
		return Optional.empty();
	}
	
	/**
	 * Asserts that a deduction only claims what the puzzle's one true solution agrees with: a placement must be the
	 * solution's digit, and an elimination must never remove it.
	 */
	static void assertAgreesWithSolution(Deduction deduction, Puzzle puzzle) {
		int[] solution = BacktrackingSolver.solve(puzzle).orElseThrow();
		if (deduction instanceof Deduction.Placement placement) {
			assertEquals(solution[placement.cell()], placement.digit(),
				deduction.technique() + " placed the wrong digit in cell " + placement.cell());
			return;
		}
		
		Deduction.Eliminations eliminations = (Deduction.Eliminations) deduction;
		int[] cells = eliminations.cells();
		int[] digits = eliminations.digits();
		assertTrue(cells.length > 0, deduction.technique() + " returned an empty elimination set");
		for (int index = 0; index < cells.length; index++) {
			assertNotEquals(solution[cells[index]], digits[index],
				deduction.technique() + " removed the solution digit from cell " + cells[index]);
		}
	}
}
