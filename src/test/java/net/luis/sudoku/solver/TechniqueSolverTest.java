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
		"000803690", "060400000", "054006230", "785000023", "002504076", "410070000", "027140000", "000005709", "000030000"
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
			() -> assertTrue(report.hardestTechnique().orElseThrow().level() <= Technique.HIDDEN_SINGLE_LINE.level(),
				"No technique harder than a hidden single may be needed, but " + report.hardestTechnique().orElseThrow() + " was")
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
		assertTrue(new Pointing().find(fresh).isPresent(), "The fixture must genuinely also offer a harder technique");
		
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
	void solve_hardPuzzleUnderALevelCap_reportsStuckAtTheCap() {
		// The technique set no longer leaves hard puzzles unsolved: level 15 assumes a candidate and plays the
		// position out, so the generator cannot produce a genuinely stuck grid any more. What can still stop the
		// solver short is a level cap, and that is the case worth pinning, because the band search depends on it.
		GeneratedPuzzle hard = PuzzleGenerator.generate(PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.LISA, 0L));
		
		TechniqueReport capped = TechniqueSolver.solve(hard.puzzle(), 2);
		
		assertAll(
			() -> assertTrue(capped.stuck()),
			() -> assertFalse(capped.solved()),
			() -> assertTrue(capped.exceededCap(), "Stopping at a cap must be distinguishable from exhausting the set"),
			() -> assertTrue(TechniqueSolver.solve(hard.puzzle()).solved(), "The same puzzle must solve uncapped")
		);
	}
}
