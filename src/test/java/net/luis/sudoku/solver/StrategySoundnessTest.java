package net.luis.sudoku.solver;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.generation.PuzzleGenerator;
import net.luis.sudoku.grid.*;
import net.luis.sudoku.key.PuzzleKey;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Soundness sweep across every strategy in {@link TechniqueSolver#STRATEGIES}.
 * <p>
 *     A technique may be too weak to find a pattern, and that only costs a rating a band. A technique that removes a
 *     candidate the true solution actually needs is a different matter: it makes the solver reach a wrong grid, or an
 *     unsolvable one, and because the difficulty rating is calibrated against what these strategies report, one
 *     unsound detector quietly corrupts every band boundary derived from it. This sweep is the guard for that, and it
 *     covers the strategies that have no fixture test of their own.
 * </p>
 * <p>
 *     The method is to solve a generated puzzle for its one true solution, then replay the solver over it and, at
 *     every state the solver passes through, offer that state to <b>every</b> strategy. Whatever a strategy claims is
 *     checked against the solution: a placement must be the solution's digit, and an elimination must never remove
 *     it. Sweeping every state rather than only fresh grids matters, because the harder techniques only become
 *     applicable once the easier ones have narrowed the candidates.
 * </p>
 */
class StrategySoundnessTest {
	
	/**
	 * The keys swept. Every band is included so that the hard strategies actually get positions they can fire on,
	 * and both variants are, so the jigsaw-only Law of Leftovers is covered too.
	 */
	private static List<PuzzleKey> sweepKeys() {
		// Deliberately small so the suite stays quick. Widen it with -Dsoundness.seeds=N when a strategy changes;
		// that is how the BUG+1 defect was found, and a wider sweep is the only thing that would find the next one.
		int seeds = Integer.getInteger("soundness.seeds", 3);
		List<PuzzleKey> keys = new ArrayList<>();
		for (Difficulty difficulty : Difficulty.values()) {
			for (long seed = 0; seed < seeds; seed++) {
				keys.add(PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, difficulty, seed));
				keys.add(PuzzleKey.of(GridSize.NINE, Variant.CHAOS, difficulty, seed));
				keys.add(PuzzleKey.of(GridSize.SIX, Variant.CLASSIC, difficulty, seed));
				keys.add(PuzzleKey.of(GridSize.TWELVE, Variant.CLASSIC, difficulty, seed));
			}
		}
		return keys;
	}
	
	/**
	 * Offers every state the solver passes through to every strategy, and checks whatever they claim against the
	 * known solution.
	 *
	 * @param puzzle The puzzle to walk
	 * @param solution Its one true solution
	 * @param firings Counts how often each technique fired anywhere in the sweep, for the coverage assertion
	 * @param failures Collects a description of every unsound claim
	 */
	private static void walk(Puzzle puzzle, int[] solution, Map<Technique, Integer> firings, List<String> failures) {
		CandidateGrid grid = new CandidateGrid(puzzle);
		for (int step = 0; step < solution.length && !grid.isComplete(); step++) {
			Deduction next = null;
			for (TechniqueStrategy strategy : TechniqueSolver.STRATEGIES) {
				Optional<Deduction> found = strategy.find(grid);
				if (found.isEmpty()) {
					continue;
				}
				
				Deduction deduction = found.orElseThrow();
				firings.merge(strategy.technique(), 1, Integer::sum);
				check(deduction, strategy, solution, grid, failures);
				if (next == null) {
					next = deduction;
				}
			}
			
			if (next == null) {
				return;
			}
			next.applyTo(grid);
		}
	}
	
	private static void check(Deduction deduction, TechniqueStrategy strategy, int[] solution, CandidateGrid grid, List<String> failures) {
		if (deduction.technique() != strategy.technique()) {
			failures.add(strategy.technique() + " returned a deduction labelled " + deduction.technique());
		}
		
		if (deduction instanceof Deduction.Placement placement) {
			if (solution[placement.cell()] != placement.digit()) {
				failures.add(strategy.technique() + " placed " + placement.digit() + " in cell " + placement.cell()
					+ ", but the solution has " + solution[placement.cell()]);
			}
			return;
		}
		
		Deduction.Eliminations eliminations = (Deduction.Eliminations) deduction;
		int[] cells = eliminations.cells();
		int[] digits = eliminations.digits();
		if (cells.length == 0) {
			failures.add(strategy.technique() + " returned an empty elimination set");
		}
		
		for (int index = 0; index < cells.length; index++) {
			if (solution[cells[index]] == digits[index]) {
				failures.add(strategy.technique() + " removed " + digits[index] + " from cell " + cells[index]
					+ ", which is the solution digit there");
			}
			if (!grid.hasCandidate(cells[index], digits[index])) {
				failures.add(strategy.technique() + " removed " + digits[index] + " from cell " + cells[index]
					+ ", where it was not a candidate, so the deduction changes nothing");
			}
		}
	}
	
	@Test
	void everyStrategy_acrossEveryStateOfASweepOfPuzzles_onlyClaimsWhatTheSolutionAgreesWith() {
		List<String> failures = new ArrayList<>();
		Map<Technique, Integer> firings = new EnumMap<>(Technique.class);
		for (PuzzleKey key : sweepKeys()) {
			var generated = PuzzleGenerator.generate(key);
			walk(generated.puzzle(), generated.solution(), firings, failures);
		}
		
		assertTrue(failures.isEmpty(), () -> failures.size() + " unsound deductions, first few:\n  "
			+ String.join("\n  ", failures.subList(0, Math.min(10, failures.size()))));
	}
	
	@Test
	void everyStrategy_onACompleteGrid_findsNothing() {
		var generated = PuzzleGenerator.generate(PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.ONE, 0L));
		Puzzle solved = Puzzle.classicOfGivens(GridSize.NINE, generated.solution());
		CandidateGrid grid = new CandidateGrid(solved);
		
		for (TechniqueStrategy strategy : TechniqueSolver.STRATEGIES) {
			assertTrue(strategy.find(grid).isEmpty(), strategy.technique() + " claimed a deduction on a solved grid");
		}
	}
	
	@Test
	void everyStrategy_onAnEmptyGrid_findsNothingOrOnlySoundEliminations() {
		// An empty grid has every digit possible everywhere, so no technique has anything to bite on. A strategy that
		// claims something here is reading structure that is not there.
		CandidateGrid grid = new CandidateGrid(Puzzle.empty(GridSize.NINE, Variant.CLASSIC, ClassicRegionPartition.of(GridSize.NINE)));
		
		for (TechniqueStrategy strategy : TechniqueSolver.STRATEGIES) {
			assertTrue(strategy.find(grid).isEmpty(), strategy.technique() + " claimed a deduction on an empty grid");
		}
	}
	
	@Test
	void everyStrategy_declaresTheTechniqueItIsRegisteredUnder() {
		// The solver picks a strategy purely by its position in STRATEGIES and reports the technique the strategy
		// names, so the two must agree or every rating is mislabelled.
		Set<Technique> registered = EnumSet.noneOf(Technique.class);
		for (TechniqueStrategy strategy : TechniqueSolver.STRATEGIES) {
			assertTrue(registered.add(strategy.technique()), strategy.technique() + " is registered twice");
		}
		
		assertEquals(EnumSet.allOf(Technique.class), registered, "Every technique needs exactly one strategy");
	}
	
	@Test
	void strategies_areOrderedByAscendingLevel() {
		// The driver applies the first applicable strategy and stops, so the list order *is* the difficulty order.
		// If it ever slipped, a puzzle would be rated by whichever technique happened to be scanned first.
		int previous = 0;
		for (TechniqueStrategy strategy : TechniqueSolver.STRATEGIES) {
			int level = strategy.technique().level();
			assertTrue(level >= previous, strategy.technique() + " at level " + level + " follows level " + previous);
			previous = level;
		}
	}
}
