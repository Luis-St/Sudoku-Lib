package net.luis.sudoku.hint;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.generation.GeneratedPuzzle;
import net.luis.sudoku.generation.PuzzleGenerator;
import net.luis.sudoku.grid.*;
import net.luis.sudoku.key.PuzzleKey;
import net.luis.sudoku.solver.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class HintEngineTest {
	
	/**
	 * The board a hint once explained wrongly: a pointing pair on 9 in box 2 was drawn while the cell it marked, r1c8,
	 * is solved by a different pointing pair on 2 in box 6. Givens only, as the solver reads them.
	 */
	private static final String REPORTED_BOARD = "059700000030065018102040570008000600043002000500400300975030062000009030300000890";
	
	/**
	 * Fixed puzzles, each its givens followed by its solution, walked to the end one hint at a time.
	 * <p>
	 *     Generated once and kept here rather than generated per run: a hint is a statement about one position, and a
	 *     fixture that changed whenever the generator does would stop testing the positions a failure was found on.
	 *     Picked so that their walks between them reach 34 of the techniques, the chains and ALS techniques included;
	 *     the rest are covered by the learn area's shipped positions in the app's tests.
	 * </p>
	 */
	private static final String[] WALKED = {
		"000060080100000200040002307504600001009070002020001008097000000050090010000053004732564189185739246946182357574628931819375462623941578397416825458297613261853794",
		"010000000074200500026083000001820003000004020203106040080040000050038069000000007315467892874291536926583174741829653568374921293156748687942315152738469439615287",
		"000015000009000008020070006000900802687200400002000030904700000005000010700020000476815329359642178821379546143967852687253491592184637914736285265498713738521964",
		"080400209905000000070000800000900001800600000000540600760000020100806074000000510683417259925368147471259863546982731812673495397541682764135928159826374238794516",
		"200400000083071400001806007600008700000000001009040500000000630500007100030014000267435819983271456451896327645128793378659241129743568714982635592367184836514972",
		"004800000000960000071500309000001405049006080000700000000000002307000048098000000964813257523967814871542369682391475749256183135784926456138792317629548298475631"
	};
	
	private static int[] digits(String text) {
		int[] digits = new int[text.length()];
		for (int index = 0; index < text.length(); index++) {
			digits[index] = text.charAt(index) - '0';
		}
		return digits;
	}
	
	private static CandidateGrid gridOf(String givens) {
		return new CandidateGrid(Puzzle.classicOfGivens(GridSize.NINE, digits(givens)));
	}
	
	private static int cell(int row, int column) {
		return (row - 1) * 9 + column - 1;
	}
	
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
	
	@Test
	void next_onTheReportedBoard_explainsTheFirstEliminationRatherThanWalkingToACell() {
		CandidateGrid grid = gridOf(REPORTED_BOARD);
		
		ExplainedDeduction first = HintEngine.next(grid).orElseThrow();
		
		assertEquals(Technique.POINTING, first.deduction().technique());
		assertEquals(new Deduction.Eliminations(Technique.POINTING, new int[] { cell(4, 4), cell(5, 4) }, new int[] { 9, 9 }), first.deduction());
		assertFalse(first.explanation().isConclusionOnly());
	}
	
	@Test
	void next_onTheReportedBoard_reachesTheMarkedCellOneStepAtATime() {
		CandidateGrid grid = gridOf(REPORTED_BOARD);
		HintEngine.next(grid).orElseThrow().deduction().applyTo(grid);
		
		// The step that really settles r1c8, which the old walk applied without ever showing it.
		Deduction second = HintEngine.next(grid).orElseThrow().deduction();
		assertEquals(new Deduction.Eliminations(Technique.POINTING, new int[] { cell(1, 8) }, new int[] { 2 }), second);
		second.applyTo(grid);
		
		Deduction third = HintEngine.next(grid).orElseThrow().deduction();
		assertEquals(new Deduction.Placement(Technique.NAKED_SINGLE, cell(1, 8), 4), third);
	}
	
	@Test
	void next_withTheEliminationAlreadyMadeInTheNotes_doesNotSuggestItAgain() {
		CandidateGrid grid = gridOf(REPORTED_BOARD);
		grid.eliminate(cell(4, 4), 9);
		grid.eliminate(cell(5, 4), 9);
		
		Deduction deduction = HintEngine.next(grid).orElseThrow().deduction();
		
		assertEquals(new Deduction.Eliminations(Technique.POINTING, new int[] { cell(1, 8) }, new int[] { 2 }), deduction);
	}
	
	@Test
	void next_agreesWithTheDriverAndLeavesTheGridAlone() {
		CandidateGrid grid = gridOf(REPORTED_BOARD);
		CandidateGrid before = grid.copy();
		
		ExplainedDeduction explained = HintEngine.next(grid).orElseThrow();
		
		assertEquals(TechniqueSolver.nextDeduction(before).orElseThrow(), explained.deduction());
		for (int cell = 0; cell < grid.cellCount(); cell++) {
			assertEquals(before.candidates(cell), grid.candidates(cell), "Candidates of cell " + cell + " changed");
		}
	}
	
	@Test
	void next_onACompleteGrid_isEmpty() {
		GeneratedPuzzle generated = easyNine(3L);
		
		assertEquals(Optional.empty(), HintEngine.next(new CandidateGrid(Puzzle.classicOfGivens(GridSize.NINE, generated.solution()))));
	}
	
	@Test
	void next_nullGrid_throws() {
		assertThrows(NullPointerException.class, () -> HintEngine.next(null));
	}
	
	/**
	 * Every hint on the way through a whole puzzle, checked for what a player is shown: the step is sound, its
	 * explanation ends on exactly that step, and the pattern is drawn on candidates the grid actually holds, which is
	 * what the old walk broke by explaining a step from further down the road.
	 */
	@Test
	void next_walkedThroughFixedPuzzles_isSoundAndDrawnOnTheCandidatesItWasGiven() {
		List<String> failures = new ArrayList<>();
		Set<Technique> seen = EnumSet.noneOf(Technique.class);
		for (int fixture = 0; fixture < WALKED.length; fixture++) {
			CandidateGrid grid = gridOf(WALKED[fixture].substring(0, 81));
			int[] solution = digits(WALKED[fixture].substring(81));
			
			for (int step = 0; !grid.isComplete(); step++) {
				String label = "fixture " + fixture + " step " + step;
				Optional<ExplainedDeduction> next = HintEngine.next(grid);
				if (next.isEmpty()) {
					failures.add(label + ": no hint on an unfinished grid");
					break;
				}
				Deduction deduction = next.get().deduction();
				Explanation explanation = next.get().explanation();
				seen.add(deduction.technique());
				
				if (explanation.technique() != deduction.technique()) {
					failures.add(label + ": explains " + explanation.technique() + " for a " + deduction.technique());
				}
				if (!explanation.steps().equals(conclusionTail(explanation, deduction))) {
					failures.add(label + ": explanation of " + deduction.technique() + " does not end on its own conclusion");
				}
				if (deduction instanceof Deduction.Placement placement && solution[placement.cell()] != placement.digit()) {
					failures.add(label + ": " + deduction.technique() + " places a wrong digit");
				}
				if (deduction instanceof Deduction.Eliminations eliminations) {
					for (int index = 0; index < eliminations.cells().length; index++) {
						int cell = eliminations.cells()[index];
						int digit = eliminations.digits()[index];
						if (!grid.hasCandidate(cell, digit)) {
							failures.add(label + ": " + deduction.technique() + " removes " + digit + " from cell " + cell + ", which is not a candidate");
						}
						if (solution[cell] == digit) {
							failures.add(label + ": " + deduction.technique() + " removes the solution of cell " + cell);
						}
					}
				}
				for (PatternCell cell : explanation.allCells()) {
					// A unique rectangle's roof is about the extra candidates of both roof cells together.
					if (cell.role() == CellRole.CONTEXT || cell.role() == CellRole.ROOF || !grid.isEmpty(cell.cell())) {
						continue;
					}
					if ((cell.digits() & grid.candidates(cell.cell())) != cell.digits()) {
						failures.add(label + ": " + deduction.technique() + " draws " + cell.role() + " cell " + cell.cell() + " on digits it does not hold");
					}
				}
				
				deduction.applyTo(grid);
			}
			if (grid.isComplete() && !Arrays.equals(solution, grid.values())) {
				failures.add("fixture " + fixture + ": the walk ends on a grid other than the solution");
			}
		}
		
		assertTrue(failures.isEmpty(), failures.size() + " failures:\n" + String.join("\n", failures));
		// A guard against the fixtures quietly shrinking to singles: they were picked to reach the chains.
		assertTrue(seen.contains(Technique.AIC) && seen.contains(Technique.ALS_XZ) && seen.contains(Technique.X_WING), "The fixtures no longer reach the harder techniques: " + seen);
	}
	
	/** The explanation's steps with its conclusion replaced by the one the deduction itself builds. */
	private static List<ExplanationStep> conclusionTail(Explanation explanation, Deduction deduction) {
		List<ExplanationStep> conclusion = Explanation.conclusionOnly(deduction).steps();
		List<ExplanationStep> steps = explanation.steps();
		if (steps.size() < conclusion.size()) {
			return conclusion;
		}
		List<ExplanationStep> expected = new ArrayList<>(steps.subList(0, steps.size() - conclusion.size()));
		expected.addAll(conclusion);
		return expected;
	}
}
