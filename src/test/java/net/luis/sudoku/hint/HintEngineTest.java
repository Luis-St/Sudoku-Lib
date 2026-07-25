package net.luis.sudoku.hint;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.generation.GeneratedPuzzle;
import net.luis.sudoku.generation.PuzzleGenerator;
import net.luis.sudoku.grid.*;
import net.luis.sudoku.key.PuzzleKey;
import net.luis.sudoku.solver.SolveStep;
import net.luis.sudoku.solver.TechniqueSolver;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class HintEngineTest {
	
	private static GeneratedPuzzle easyNine(long seed) {
		return PuzzleGenerator.generate(PuzzleKey.of(GridSize.NINE, Variant.CLASSIC, Difficulty.ONE, seed));
	}
	
	@Test
	void peek_onSolvablePuzzle_returnsTheNextStepsCellAndTechnique() {
		GeneratedPuzzle generated = easyNine(1L);
		SolveStep step = TechniqueSolver.nextStep(generated.puzzle()).orElseThrow();
		
		HintCandidate candidate = HintEngine.peek(generated.puzzle()).orElseThrow();
		
		assertAll(
			() -> assertEquals(step.cellIndex(), candidate.cellIndex()),
			() -> assertEquals(step.technique(), candidate.technique())
		);
	}
	
	@Test
	void peek_doesNotRevealTheDigit_butConsumeDoesAndItMatchesTheSolution() {
		GeneratedPuzzle generated = easyNine(2L);
		
		HintCandidate candidate = HintEngine.peek(generated.puzzle()).orElseThrow();
		HintResult result = HintEngine.consume(generated.puzzle(), candidate);
		
		assertAll(
			() -> assertEquals(candidate.cellIndex(), result.cellIndex()),
			() -> assertEquals(candidate.technique(), result.technique()),
			() -> assertTrue(generated.puzzle().cell(result.cellIndex()).isEmpty(), "Hint points at a non-empty cell"),
			() -> assertEquals(generated.solutionAt(result.cellIndex()), result.digit(), "Hint digit disagrees with the solution")
		);
	}
	
	@Test
	void peek_onSolvedPuzzle_isEmpty() {
		GeneratedPuzzle generated = easyNine(3L);
		Puzzle solved = Puzzle.classicOfGivens(GridSize.NINE, generated.solution());
		
		assertEquals(Optional.empty(), HintEngine.peek(solved));
	}
	
	@Test
	void peek_doesNotMutateThePuzzle() {
		GeneratedPuzzle generated = easyNine(4L);
		int[] before = generated.puzzle().values();
		
		HintEngine.peek(generated.puzzle());
		
		assertArrayEquals(before, generated.puzzle().values());
	}
	
	@Test
	void consume_onSolvedPuzzle_throwsBecauseNoHintIsAvailable() {
		GeneratedPuzzle generated = easyNine(5L);
		Puzzle solved = Puzzle.classicOfGivens(GridSize.NINE, generated.solution());
		
		assertThrows(IllegalStateException.class, () -> HintEngine.consume(solved, new HintCandidate(0, net.luis.sudoku.solver.Technique.NAKED_SINGLE)));
	}
	
	@Test
	void consume_withStaleCandidateForADifferentCell_throws() {
		GeneratedPuzzle generated = easyNine(6L);
		HintCandidate real = HintEngine.peek(generated.puzzle()).orElseThrow();
		// A candidate pointing at a different empty cell than the true next step.
		int otherCell = (real.cellIndex() + 1) % generated.puzzle().size().cellCount();
		HintCandidate stale = new HintCandidate(otherCell, real.technique());
		
		if (otherCell != real.cellIndex()) {
			assertThrows(IllegalStateException.class, () -> HintEngine.consume(generated.puzzle(), stale));
		}
	}
	
	@Test
	void peek_nullPuzzle_throws() {
		assertThrows(NullPointerException.class, () -> HintEngine.peek(null));
	}
	
	@Test
	void consume_nullArguments_throw() {
		GeneratedPuzzle generated = easyNine(7L);
		HintCandidate candidate = HintEngine.peek(generated.puzzle()).orElseThrow();
		
		assertAll(
			() -> assertThrows(NullPointerException.class, () -> HintEngine.consume(null, candidate)),
			() -> assertThrows(NullPointerException.class, () -> HintEngine.consume(generated.puzzle(), null))
		);
	}
}
