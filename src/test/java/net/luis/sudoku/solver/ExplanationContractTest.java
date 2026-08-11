package net.luis.sudoku.solver;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.generation.GeneratedPuzzle;
import net.luis.sudoku.generation.PuzzleGenerator;
import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.grid.Puzzle;
import net.luis.sudoku.grid.Variant;
import net.luis.sudoku.key.PuzzleKey;
import net.luis.sudoku.learn.LearnTechniques;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the contract between {@link TechniqueStrategy#find(CandidateGrid)} and
 * {@link TechniqueStrategy#findExplained(CandidateGrid)}.
 * <p>
 *     These are the invariants the learn area rests on. The individual per-technique tests prove that each strategy
 *     finds the right deduction; this one proves that asking the same strategy to explain itself never changes the
 *     answer, which is what stops a lesson from teaching a pattern the solver did not actually use.
 * </p>
 */
class ExplanationContractTest {

	/**
	 * Positions to run every strategy against: a spread of generated puzzles, walked forwards by the solver so that
	 * the harder techniques get grids they can actually fire on. A single fixture would exercise the singles and
	 * nothing else.
	 */
	private static List<CandidateGrid> positions() {
		List<CandidateGrid> positions = new ArrayList<>();
		for (long seed = 1; seed <= 6; seed++) {
			// Bands 6, 9 and 12: low enough to generate quickly, spread far enough apart that the subsets, the
			// fish and the wings all show up somewhere in the walk.
			for (Difficulty difficulty : List.of(Difficulty.ofIndex(6), Difficulty.ofIndex(9), Difficulty.ofIndex(12))) {
				GeneratedPuzzle generated = PuzzleGenerator.generate(PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, difficulty, seed));
				Puzzle puzzle = generated.puzzle();
				CandidateGrid grid = new CandidateGrid(puzzle);
				positions.add(grid.copy());

				// Walk the position forwards, keeping a snapshot every few deductions: a technique of level 9 only
				// ever appears in a grid that the easier ones have already worked over.
				for (int step = 0; step < 40; step++) {
					Optional<Deduction> next = Optional.empty();
					for (TechniqueStrategy strategy : TechniqueSolver.STRATEGIES) {
						next = strategy.find(grid);
						if (next.isPresent()) {
							break;
						}
					}
					if (next.isEmpty()) {
						break;
					}

					next.get().applyTo(grid);
					if (step % 4 == 0) {
						positions.add(grid.copy());
					}
				}
			}
		}
		return positions;
	}

	@Test
	void findExplainedReturnsTheSameDeductionAsFind() {
		List<CandidateGrid> positions = positions();
		for (TechniqueStrategy strategy : TechniqueSolver.STRATEGIES) {
			for (CandidateGrid position : positions) {
				Optional<Deduction> plain = strategy.find(position);
				Optional<ExplainedDeduction> explained = strategy.findExplained(position);

				assertEquals(plain.isPresent(), explained.isPresent(), "Presence differs for " + strategy.technique());
				plain.ifPresent(deduction -> assertEquals(deduction, explained.orElseThrow().deduction(), "Deduction differs for " + strategy.technique()));
			}
		}
	}

	@Test
	void findExplainedNeverMutatesTheGrid() {
		for (TechniqueStrategy strategy : TechniqueSolver.STRATEGIES) {
			for (CandidateGrid position : positions()) {
				int[] before = position.values();
				int[] candidatesBefore = new int[position.cellCount()];
				for (int cell = 0; cell < position.cellCount(); cell++) {
					candidatesBefore[cell] = position.candidates(cell);
				}

				strategy.findExplained(position);

				assertArrayEquals(before, position.values(), "Values changed by " + strategy.technique());
				for (int cell = 0; cell < position.cellCount(); cell++) {
					assertEquals(candidatesBefore[cell], position.candidates(cell), "Candidates of cell " + cell + " changed by " + strategy.technique());
				}
			}
		}
	}

	@Test
	void explanationAlwaysAgreesWithItsDeduction() {
		for (TechniqueStrategy strategy : TechniqueSolver.STRATEGIES) {
			for (CandidateGrid position : positions()) {
				strategy.findExplained(position).ifPresent(explained -> {
					assertEquals(explained.deduction().technique(), explained.explanation().technique());
					assertFalse(explained.explanation().steps().isEmpty(), "Empty explanation for " + strategy.technique());

					// The conclusion is always the last beat, so a consumer can play the steps in order and end on
					// the answer rather than having to hunt for it.
					StepKind last = explained.explanation().steps().get(explained.explanation().steps().size() - 1).kind();
					assertTrue(last == StepKind.PLACEMENT || last == StepKind.ELIMINATION, "Explanation of " + strategy.technique() + " does not end on its conclusion");
				});
			}
		}
	}

	@Test
	void explanationCellsAreWithinTheGrid() {
		for (TechniqueStrategy strategy : TechniqueSolver.STRATEGIES) {
			for (CandidateGrid position : positions()) {
				strategy.findExplained(position).ifPresent(explained -> {
					for (PatternCell cell : explained.explanation().allCells()) {
						assertTrue(cell.cell() < position.cellCount(), "Cell " + cell.cell() + " out of range for " + strategy.technique());
					}
					for (ExplanationStep step : explained.explanation().steps()) {
						assertTrue(step.digit() <= position.n(), "Digit " + step.digit() + " out of range for " + strategy.technique());
						for (UnitRef unit : step.units()) {
							assertNotNull(unit.cells(position), "Unit " + unit + " does not resolve for " + strategy.technique());
						}
					}
				});
			}
		}
	}

	/**
	 * The techniques that have been taught to explain themselves so far. Every one of them must produce a real
	 * pattern rather than the {@link Explanation#conclusionOnly(Deduction)} fallback, or the learn area would show a
	 * lesson consisting of nothing but the answer.
	 */
	private static final Set<Technique> EXPLAINING = EnumSet.of(
		Technique.FULL_HOUSE, Technique.LAST_DIGIT, Technique.NAKED_SINGLE,
		Technique.HIDDEN_SINGLE_REGION, Technique.HIDDEN_SINGLE_LINE,
		Technique.POINTING, Technique.CLAIMING,
		Technique.NAKED_PAIR, Technique.NAKED_TRIPLE, Technique.NAKED_QUAD,
		Technique.HIDDEN_PAIR, Technique.HIDDEN_TRIPLE, Technique.HIDDEN_QUAD,
		Technique.X_WING, Technique.SWORDFISH, Technique.JELLYFISH,
		Technique.FINNED_X_WING, Technique.FINNED_SWORDFISH, Technique.SASHIMI_SWORDFISH,
		Technique.XY_WING, Technique.XYZ_WING, Technique.BUG_PLUS_ONE,
		Technique.SKYSCRAPER, Technique.TWO_STRING_KITE, Technique.CRANE, Technique.EMPTY_RECTANGLE,
		Technique.W_WING, Technique.WXYZ_WING,
		Technique.UNIQUE_RECTANGLE_1, Technique.UNIQUE_RECTANGLE_2, Technique.UNIQUE_RECTANGLE_3, Technique.UNIQUE_RECTANGLE_4,
		Technique.SIMPLE_COLOURING, Technique.MEDUSA_3D,
		Technique.X_CHAIN, Technique.XY_CHAIN, Technique.AIC, Technique.GROUPED_AIC,
		Technique.ALS_XZ, Technique.ALS_CHAIN, Technique.SUE_DE_COQ
	);

	/**
	 * Every technique the learn area teaches must now explain itself, so the set above has to be the taught set
	 * exactly. Without this the set could quietly fall behind {@link LearnTechniques} and a technique would ship a
	 * lesson made of nothing but its answer.
	 */
	@Test
	void everyTaughtTechniqueIsExpectedToExplainItself() {
		assertEquals(LearnTechniques.taught().size(), EXPLAINING.size());
		for (Technique technique : LearnTechniques.taught()) {
			assertTrue(EXPLAINING.contains(technique), technique + " is taught but is not expected to explain itself");
		}
	}

	@Test
	void taughtStrategiesExplainTheirPattern() {
		List<CandidateGrid> positions = positions();
		Set<Technique> seen = EnumSet.noneOf(Technique.class);
		for (TechniqueStrategy strategy : TechniqueSolver.STRATEGIES) {
			if (!EXPLAINING.contains(strategy.technique())) {
				continue;
			}

			for (CandidateGrid position : positions) {
				strategy.findExplained(position).ifPresent(explained -> {
					assertFalse(explained.explanation().isConclusionOnly(), strategy.technique() + " fell back to the conclusion only");
					seen.add(strategy.technique());
				});
			}
		}

		// A guard against the test quietly proving nothing: the fixtures have to actually reach these techniques.
		assertFalse(seen.isEmpty(), "No explaining technique fired on any fixture");
	}

	@Test
	void everyTaughtTechniqueHasAStrategy() {
		for (Technique technique : LearnTechniques.taught()) {
			assertTrue(TechniqueSolver.STRATEGIES.stream().anyMatch(strategy -> strategy.technique() == technique), "No strategy implements " + technique);
		}
	}

	@Test
	void learnTechniquesExcludesWhatCannotBeTaught() {
		assertFalse(LearnTechniques.isTaught(Technique.LAW_OF_LEFTOVERS));
		assertFalse(LearnTechniques.isTaught(Technique.MULTI_COLOURING));
		assertFalse(LearnTechniques.isTaught(Technique.NISHIO));
		assertFalse(LearnTechniques.isTaught(Technique.FORCING_CHAIN));
		assertFalse(LearnTechniques.isTaught(Technique.FORCING_NET));
		assertFalse(LearnTechniques.isTaught(Technique.DEATH_BLOSSOM));
		assertFalse(LearnTechniques.isTaught(Technique.DYNAMIC_CONTRADICTION_CHAIN));

		assertTrue(LearnTechniques.isTaught(Technique.NAKED_SINGLE));
		assertTrue(LearnTechniques.isTaught(Technique.GROUPED_AIC));
		assertTrue(LearnTechniques.isTaught(Technique.ALS_CHAIN));
		assertEquals(LearnTechniques.taught().size(), LearnTechniques.count());
	}
}
