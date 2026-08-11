package net.luis.sudoku.learn;

import net.luis.sudoku.solver.CandidateGrid;
import net.luis.sudoku.solver.Deduction;
import net.luis.sudoku.solver.Technique;
import net.luis.sudoku.solver.TechniqueSolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link LearnPuzzleGenerator}.
 * <p>
 *     The techniques exercised here are the common ones, which turn up within a few seeds. The rarer techniques are
 *     generated offline by the export task, where a long search costs nobody anything; proving them here would make
 *     the suite take minutes to say what these already say.
 * </p>
 */
class LearnPuzzleGeneratorTest {

	private static final LearnPuzzleGenerator.Budget BUDGET = new LearnPuzzleGenerator.Budget(400, 60_000L);

	private static final List<Technique> COMMON = List.of(
		Technique.NAKED_SINGLE, Technique.HIDDEN_SINGLE_REGION, Technique.HIDDEN_SINGLE_LINE,
		Technique.POINTING, Technique.CLAIMING, Technique.NAKED_PAIR, Technique.HIDDEN_PAIR
	);

	private static LearnPuzzle generate(Technique technique) {
		Optional<LearnPuzzle> puzzle = LearnPuzzleGenerator.generate(technique, 1L, BUDGET);
		assertTrue(puzzle.isPresent(), "No exercise found for " + technique);
		return puzzle.orElseThrow();
	}

	@Test
	void generatesAnExerciseForTheCommonTechniques() {
		for (Technique technique : COMMON) {
			LearnPuzzle puzzle = generate(technique);

			assertEquals(technique, puzzle.technique());
			assertEquals(technique, puzzle.explanation().technique());
			assertTrue(puzzle.emptyCount() > 0);
		}
	}

	@Test
	void theTargetIsAnEmptyCellHoldingItsSolutionDigit() {
		for (Technique technique : COMMON) {
			LearnPuzzle puzzle = generate(technique);

			assertEquals(0, puzzle.board()[puzzle.targetCell()], "Target of " + technique + " is already filled");
			assertEquals(puzzle.solution()[puzzle.targetCell()], puzzle.targetDigit(), "Target of " + technique + " disagrees with the solution");
			assertTrue((puzzle.pencilMarks()[puzzle.targetCell()] & (1 << puzzle.targetDigit())) != 0, "Target digit of " + technique + " is not among its own pencil marks");
		}
	}

	/**
	 * The promise the whole learn area rests on: in the position handed to the player, with the pencil marks it ships
	 * with, nothing <b>easier</b> than the technique being taught applies. If something easier applied, the player
	 * could solve the exercise without ever meeting the technique.
	 * <p>
	 *     Easier means a lower {@link Technique#level()}, not a lower rank. A level is the rating axis and the
	 *     techniques sharing one are equally hard by definition, so a sibling of the same level being available too
	 *     costs the exercise nothing.
	 * </p>
	 */
	@Test
	void nothingEasierThanTheTaughtTechniqueApplies() {
		for (Technique technique : COMMON) {
			LearnPuzzle puzzle = generate(technique);
			CandidateGrid grid = restore(puzzle);

			if (technique.level() > 1) {
				assertTrue(TechniqueSolver.nextDeductionUpTo(grid, technique.level() - 1).isEmpty(), "Something easier than " + technique + " applies in its own exercise");
			}

			Optional<Deduction> next = TechniqueSolver.nextDeduction(grid);
			assertTrue(next.isPresent(), "Nothing applies in the exercise for " + technique);
			assertEquals(technique.level(), next.orElseThrow().technique().level(), "The easiest technique in the exercise for " + technique + " is of another level");
		}
	}

	/**
	 * Walking the exercise forwards from its own pencil marks has to arrive at the stated target, or the app would be
	 * checking the player against a placement the position does not actually lead to.
	 */
	@Test
	void theTargetIsThePlacementTheTechniqueLeadsTo() {
		for (Technique technique : COMMON) {
			LearnPuzzle puzzle = generate(technique);
			CandidateGrid grid = restore(puzzle);

			Deduction.Placement placement = null;
			for (int step = 0; step < 64 && placement == null; step++) {
				Deduction deduction = TechniqueSolver.nextDeductionUpTo(grid, technique.level()).orElseThrow();
				if (deduction instanceof Deduction.Placement found) {
					placement = found;
				} else {
					deduction.applyTo(grid);
				}
			}

			assertNotNull(placement, "No placement is reachable in the exercise for " + technique);
			assertEquals(puzzle.targetCell(), placement.cell(), "Target cell of " + technique);
			assertEquals(puzzle.targetDigit(), placement.digit(), "Target digit of " + technique);
		}
	}

	/**
	 * The pencil marks are the puzzle, not a decoration: they must be exactly the candidate state the solver left
	 * behind. Candidates re-derived from the board alone would restore what the earlier eliminations removed.
	 */
	@Test
	void pencilMarksCoverEveryEmptyCellAndNoFilledOne() {
		for (Technique technique : COMMON) {
			LearnPuzzle puzzle = generate(technique);
			int[] board = puzzle.board();
			int[] marks = puzzle.pencilMarks();

			for (int cell = 0; cell < LearnPuzzle.CELL_COUNT; cell++) {
				if (board[cell] == 0) {
					assertTrue(marks[cell] != 0, "Empty cell " + cell + " has no pencil marks in " + technique);
					assertTrue((marks[cell] & (1 << puzzle.solution()[cell])) != 0, "Pencil marks of cell " + cell + " lost the solution digit in " + technique);
				} else {
					assertEquals(0, marks[cell], "Filled cell " + cell + " carries pencil marks in " + technique);
					assertEquals(puzzle.solution()[cell], board[cell], "Filled cell " + cell + " disagrees with the solution in " + technique);
				}
			}
		}
	}

	@Test
	void theExplanationShowsARealPattern() {
		for (Technique technique : COMMON) {
			LearnPuzzle puzzle = generate(technique);

			assertFalse(puzzle.explanation().isConclusionOnly(), "Exercise for " + technique + " explains nothing but its answer");
			assertTrue(puzzle.patternCells().length > 0, "Exercise for " + technique + " marks no pattern cells");
			for (int cell : puzzle.patternCells()) {
				assertTrue(cell >= 0 && cell < LearnPuzzle.CELL_COUNT);
			}
		}
	}

	@Test
	void generateSetReturnsDistinctLayouts() {
		List<LearnPuzzle> puzzles = LearnPuzzleGenerator.generateSet(Technique.HIDDEN_SINGLE_REGION, 5, 1L, BUDGET);

		assertEquals(5, puzzles.size());
		assertEquals(5, puzzles.stream().map(LearnPuzzle::layoutKey).distinct().count(), "Two examples show the same layout");
		for (LearnPuzzle puzzle : puzzles) {
			assertEquals(Technique.HIDDEN_SINGLE_REGION, puzzle.technique());
		}
	}

	@Test
	void generateRejectsATechniqueTheLearnAreaDoesNotTeach() {
		assertThrows(IllegalArgumentException.class, () -> LearnPuzzleGenerator.generate(Technique.LAW_OF_LEFTOVERS, 1L, BUDGET));
		assertThrows(IllegalArgumentException.class, () -> LearnPuzzleGenerator.generate(Technique.NISHIO, 1L, BUDGET));
		// Dominated by 3D Medusa, so no position ever has it as the easiest technique: see LearnTechniques.
		assertThrows(IllegalArgumentException.class, () -> LearnPuzzleGenerator.generate(Technique.MULTI_COLOURING, 1L, BUDGET));
	}

	@Test
	void generateGivesUpRatherThanRunningForever() {
		// One attempt of one millisecond cannot find anything, and must come back empty rather than hang.
		Optional<LearnPuzzle> puzzle = LearnPuzzleGenerator.generate(Technique.ALS_CHAIN, 1L, new LearnPuzzleGenerator.Budget(1, 1L));

		assertNotNull(puzzle);
	}

	@Test
	void budgetRejectsNonsense() {
		assertThrows(IllegalArgumentException.class, () -> new LearnPuzzleGenerator.Budget(0, 1000L));
		assertThrows(IllegalArgumentException.class, () -> new LearnPuzzleGenerator.Budget(10, 0L));
	}

	/**
	 * Rebuilds the exercise exactly as the app will: the position as givens, then the shipped pencil marks written
	 * over the freshly derived candidates.
	 *
	 * @param puzzle The exercise
	 * @return The working grid the player faces
	 */
	private static CandidateGrid restore(LearnPuzzle puzzle) {
		CandidateGrid grid = new CandidateGrid(LearnPuzzleGenerator.asPuzzle(puzzle));
		int[] marks = puzzle.pencilMarks();
		for (int cell = 0; cell < LearnPuzzle.CELL_COUNT; cell++) {
			if (!grid.isEmpty(cell)) {
				continue;
			}

			int surplus = grid.candidates(cell) & ~marks[cell];
			while (surplus != 0) {
				grid.eliminate(cell, Integer.numberOfTrailingZeros(surplus));
				surplus &= surplus - 1;
			}
		}
		return grid;
	}
}
