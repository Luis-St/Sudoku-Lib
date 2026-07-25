package net.luis.sudoku.solver;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.generation.GeneratedPuzzle;
import net.luis.sudoku.generation.PuzzleGenerator;
import net.luis.sudoku.grid.*;
import net.luis.sudoku.key.PuzzleKey;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link TechniqueSolver}.
 */
class TechniqueSolverTest {
	
	/**
	 * A generated, uniquely solvable 9x9 puzzle that the technique solver finishes using nothing harder than a
	 * hidden single.
	 */
	private static final String[] SINGLES_ONLY = {
		"908702000", "700006300", "000400059", "400208000", "003000602", "007009000", "002300000", "000000105", "000807000"
	};
	/**
	 * A generated, uniquely solvable 9x9 puzzle the solver finishes but only after applying a naked pair, so its
	 * hardest technique is {@link Technique#NAKED_PAIR}.
	 */
	private static final String[] NEEDS_SUBSET = {
		"002095000", "800000000", "000000004", "000056000", "050010270", "048900600", "000000743", "000803002", "006000010"
	};
	/**
	 * A generated, uniquely solvable 9x9 puzzle whose fresh candidate grid offers both a naked single (cell 11,
	 * digit 2) and several harder techniques (a naked pair and a pointing pair), so it pins the solver's
	 * lowest-technique preference. It is also a puzzle the modelled technique set cannot finish, so the same grid
	 * doubles as a stuck fixture reached deterministically by hand rather than through the generator.
	 */
	private static final String[] MIXED_AND_STUCK = {
		"050000700", "070005046", "018070002", "009004001", "000009060", "000710030", "003090085", "000000000", "501000300"
	};
	
	private static int[] parse(String[] rows) {
		int[] values = new int[81];
		for (int row = 0; row < 9; row++) {
			for (int column = 0; column < 9; column++) {
				values[row * 9 + column] = rows[row].charAt(column) - '0';
			}
		}
		return values;
	}
	
	private static Puzzle puzzle(String[] rows) {
		return Puzzle.classicOfGivens(GridSize.NINE, parse(rows));
	}
	
	@Test
	void solve_nullPuzzle_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> TechniqueSolver.solve(null));
	}
	
	@Test
	void solve_singlesOnlyPuzzle_solvesWithoutAnythingHarderThanAHiddenSingle() {
		TechniqueReport report = TechniqueSolver.solve(puzzle(SINGLES_ONLY));
		
		assertAll(
			() -> assertTrue(report.solved(), "The singles-only puzzle must solve"),
			() -> assertFalse(report.stuck()),
			() -> assertTrue(report.hardestTechnique().orElseThrow().rank() <= Technique.HIDDEN_SINGLE.rank(),
				"No technique harder than a hidden single may be needed")
		);
	}
	
	@Test
	void solve_singlesOnlyPuzzle_reachesTheBacktrackingSolution() {
		Puzzle puzzle = puzzle(SINGLES_ONLY);
		int[] oracle = BacktrackingSolver.solve(puzzle).orElseThrow();
		
		TechniqueReport report = TechniqueSolver.solve(puzzle);
		
		assertArrayEquals(oracle, report.solution());
	}
	
	@Test
	void solve_puzzleNeedingASubset_recordsThatTechnique() {
		TechniqueReport report = TechniqueSolver.solve(puzzle(NEEDS_SUBSET));
		
		assertAll(
			() -> assertTrue(report.solved()),
			() -> assertEquals(Optional.of(Technique.NAKED_PAIR), report.hardestTechnique()),
			() -> assertTrue(report.count(Technique.NAKED_PAIR) >= 1, "The subset technique must be counted")
		);
	}
	
	@Test
	void nextStep_gridOfferingASingleAndHarderTechniques_prefersTheSingle() {
		Puzzle puzzle = puzzle(MIXED_AND_STUCK);
		CandidateGrid fresh = new CandidateGrid(puzzle);
		assertTrue(new PointingPair().find(fresh).isPresent(), "The fixture must genuinely also offer a harder technique");
		
		Optional<SolveStep> step = TechniqueSolver.nextStep(puzzle);
		
		assertEquals(Optional.of(new SolveStep(11, 2, Technique.NAKED_SINGLE)), step);
	}
	
	@Test
	void nextStep_solvablePuzzle_placesTheTrueSolutionDigit() {
		Puzzle puzzle = puzzle(SINGLES_ONLY);
		int[] oracle = BacktrackingSolver.solve(puzzle).orElseThrow();
		
		SolveStep step = TechniqueSolver.nextStep(puzzle).orElseThrow();
		
		assertEquals(oracle[step.cellIndex()], step.digit(), "The placement must agree with the unique solution");
	}
	
	@Test
	void nextStep_alreadySolvedPuzzle_returnsEmpty() {
		int[] solution = BacktrackingSolver.solve(puzzle(SINGLES_ONLY)).orElseThrow();
		Puzzle solved = Puzzle.classicOfGivens(GridSize.NINE, solution);
		
		assertTrue(TechniqueSolver.nextStep(solved).isEmpty());
	}
	
	@Test
	void solve_anyPuzzle_doesNotMutateTheInput() {
		Puzzle puzzle = puzzle(SINGLES_ONLY);
		int[] before = puzzle.values();
		
		TechniqueSolver.solve(puzzle);
		
		assertArrayEquals(before, puzzle.values());
	}
	
	@Test
	void solve_theSamePuzzleTwice_producesEqualUsageAndSolution() {
		TechniqueReport first = TechniqueSolver.solve(puzzle(NEEDS_SUBSET));
		TechniqueReport second = TechniqueSolver.solve(puzzle(NEEDS_SUBSET));
		
		assertAll(
			() -> assertEquals(first.usage(), second.usage(), "Usage must be deterministic"),
			() -> assertArrayEquals(first.solution(), second.solution(), "The solution must be deterministic")
		);
	}
	
	@Test
	void solve_generatedEasyPuzzle_reachesTheBacktrackingSolution() {
		GeneratedPuzzle solvable = null;
		for (long seed = 0; seed < 40 && solvable == null; seed++) {
			GeneratedPuzzle candidate = PuzzleGenerator.generate(PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.ONE, seed));
			if (TechniqueSolver.solve(candidate.puzzle()).solved()) {
				solvable = candidate;
			}
		}
		assertNotNull(solvable, "No technique-solvable easy puzzle was generated in the searched range");
		
		GeneratedPuzzle found = solvable;
		TechniqueReport report = TechniqueSolver.solve(found.puzzle());
		int[] oracle = BacktrackingSolver.solve(found.puzzle()).orElseThrow();
		
		assertAll(
			() -> assertTrue(report.solved()),
			() -> assertArrayEquals(oracle, report.solution()),
			() -> assertArrayEquals(found.solution(), report.solution(), "The technique solution must be the carried solution")
		);
	}
	
	@Test
	void solve_hardPuzzleBeyondTheTechniqueSet_reportsStuckAndNotSolved() {
		TechniqueReport stuck = null;
		for (long seed = 0; seed <= 40 && stuck == null; seed++) {
			GeneratedPuzzle candidate = PuzzleGenerator.generate(PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.FIVE, seed));
			TechniqueReport report = TechniqueSolver.solve(candidate.puzzle());
			if (report.stuck()) {
				stuck = report;
			}
		}
		assertNotNull(stuck, "No stuck Difficulty.FIVE puzzle was found in seeds 0..40");
		
		TechniqueReport report = stuck;
		assertAll(
			() -> assertTrue(report.stuck()),
			() -> assertFalse(report.solved())
		);
	}
}
