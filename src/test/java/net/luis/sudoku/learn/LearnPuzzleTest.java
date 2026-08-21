package net.luis.sudoku.learn;

import net.luis.sudoku.solver.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * Test class for {@link LearnPuzzle}, and mainly for what the training judges a solve by.
 * <p>
 *     The exercises are generated rather than written out by hand: a hand-built position that happens to hold two
 *     full houses proves the arithmetic of one case, while the generator produces the positions the app actually
 *     ships, which is where the several-right-answers case comes from in the first place.
 * </p>
 */
class LearnPuzzleTest {
	
	private static final LearnPuzzleGenerator.Budget BUDGET = new LearnPuzzleGenerator.Budget(400, 60_000L);
	
	private static final List<Technique> EASY = List.of(
		Technique.FULL_HOUSE, Technique.NAKED_SINGLE, Technique.HIDDEN_SINGLE_REGION, Technique.HIDDEN_SINGLE_LINE
	);
	
	private static LearnPuzzle generate(Technique technique, long seed) {
		Optional<LearnPuzzle> puzzle = LearnPuzzleGenerator.generate(technique, seed, BUDGET);
		assertTrue(puzzle.isPresent(), "No exercise found for " + technique);
		return puzzle.orElseThrow();
	}
	
	@Test
	void gridRestoresTheShippedPencilMarksRatherThanDerivingThem() {
		for (Technique technique : EASY) {
			LearnPuzzle puzzle = generate(technique, 1L);
			CandidateGrid grid = puzzle.grid();
			int[] marks = puzzle.pencilMarks();
			
			for (int cell = 0; cell < LearnPuzzle.CELL_COUNT; cell++) {
				if (puzzle.board()[cell] == 0) {
					assertEquals(marks[cell], grid.candidates(cell), "Candidates of cell " + cell + " in " + technique);
				} else {
					assertFalse(grid.isEmpty(cell), "Cell " + cell + " of " + technique + " lost its digit");
				}
			}
		}
	}
	
	@Test
	void theTargetIsAlwaysAPlacementCell() {
		for (Technique technique : EASY) {
			LearnPuzzle puzzle = generate(technique, 1L);
			
			int[] cells = puzzle.placementCells();
			assertTrue(cells.length > 0);
			assertEquals(puzzle.targetCell(), cells[0], "Target of " + technique + " is not the first placement");
			assertTrue(puzzle.proves(puzzle.targetCell(), puzzle.targetDigit()), "Target of " + technique + " is not proved");
		}
	}
	
	/**
	 * Every cell the technique places has to be one the technique really places in <i>this</i> position: solving it
	 * has to be the first thing its own strategy finds on the grid the player is given.
	 */
	@Test
	void everyPlacementCellIsOneTheTechniqueFindsInThePositionItself() {
		for (Technique technique : EASY) {
			LearnPuzzle puzzle = generate(technique, 1L);
			
			for (int cell : puzzle.placementCells()) {
				assertEquals(0, puzzle.board()[cell], "Placement cell " + cell + " of " + technique + " is filled");
				assertTrue(puzzle.proves(cell, puzzle.solution()[cell]), "Placement cell " + cell + " of " + technique + " is not proved");
			}
		}
	}
	
	/**
	 * The reason the method exists: the easy techniques regularly apply in more than one place at once, and each of
	 * those places is the technique being used.
	 */
	@Test
	void aPositionWithSeveralPlacementsAcceptsEveryOneOfThem() {
		List<Integer> several = new ArrayList<>();
		for (long seed = 1L; seed <= 12L && several.isEmpty(); seed++) {
			LearnPuzzle puzzle = generate(Technique.FULL_HOUSE, seed);
			int[] cells = puzzle.placementCells();
			if (cells.length < 2) {
				continue;
			}
			
			for (int cell : cells) {
				several.add(cell);
				assertTrue(puzzle.proves(cell, puzzle.solution()[cell]), "Cell " + cell + " is a full house of this position and was refused");
			}
			assertNotEquals(cells[0], cells[1]);
		}
		assertFalse(several.isEmpty(), "No full-house exercise with several placements was generated in twelve seeds");
	}
	
	@Test
	void explainsTheTargetWithTheShippedExplanation() {
		for (Technique technique : EASY) {
			LearnPuzzle puzzle = generate(technique, 1L);
			
			assertEquals(puzzle.explanation(), puzzle.explanationOf(puzzle.targetCell()).orElseThrow());
		}
	}
	
	/**
	 * The reason {@code explanationOf} exists: the board drawn once the exercise is over has to argue for the cell the
	 * player filled, and the shipped explanation argues for the target and for nothing else.
	 */
	@Test
	void explainsEveryOtherPlacementWithItsOwnArgument() {
		boolean seenASecondPlacement = false;
		for (long seed = 1L; seed <= 12L && !seenASecondPlacement; seed++) {
			LearnPuzzle puzzle = generate(Technique.FULL_HOUSE, seed);
			int[] cells = puzzle.placementCells();
			if (cells.length < 2) {
				continue;
			}
			
			seenASecondPlacement = true;
			for (int index = 1; index < cells.length; index++) {
				int cell = cells[index];
				Explanation explanation = puzzle.explanationOf(cell).orElseThrow(() -> new AssertionError("No explanation for placement cell " + cell));
				
				assertEquals(puzzle.technique(), explanation.technique());
				assertNotEquals(puzzle.explanation(), explanation, "Cell " + cell + " was explained with the target's argument");
				
				boolean namesItsOwnCell = false;
				for (PatternCell pattern : explanation.allCells()) {
					namesItsOwnCell |= pattern.cell() == cell;
				}
				assertTrue(namesItsOwnCell, "The explanation of cell " + cell + " never mentions that cell");
			}
		}
		assertTrue(seenASecondPlacement, "No full-house exercise with several placements was generated in twelve seeds");
	}
	
	@Test
	void explainsNothingForACellTheTechniqueDoesNotPlace() {
		LearnPuzzle puzzle = generate(Technique.HIDDEN_SINGLE_REGION, 1L);
		int[] board = puzzle.board();
		List<Integer> placements = new ArrayList<>();
		for (int cell : puzzle.placementCells()) {
			placements.add(cell);
		}
		
		for (int cell = 0; cell < LearnPuzzle.CELL_COUNT; cell++) {
			if (board[cell] == 0 && !placements.contains(cell)) {
				assertTrue(puzzle.explanationOf(cell).isEmpty(), "Cell " + cell + " is not this technique's to place and was explained");
			}
		}
	}
	
	/**
	 * The bug this file's method was rewritten for. Full house exercise 3 of level 2 has two of them: the target in
	 * column 3, and the last empty cell of the top-right box. The first walked the technique forwards, applying each
	 * find to reach the next, and the cascade of new full houses that opened up filled the box's cell before the scan
	 * ever reached the regions, so the player who solved it was told they had not used the technique.
	 */
	@Test
	void acceptsAFullHouseTheScanReachesLateInTheBundledExerciseThatReportedIt() {
		LearnPuzzle puzzle = LearnAsset.read(bundled("full_house")).exercise(2, 2);
		int cell = 1 * LearnPuzzle.SIZE + 6;
		
		assertEquals(6 * LearnPuzzle.SIZE + 2, puzzle.targetCell(), "The exercise is not the one the report was about");
		assertTrue(puzzle.proves(cell, puzzle.solution()[cell]), "The second full house of the position was refused");
		assertTrue(puzzle.explanationOf(cell).isPresent(), "The second full house of the position has no argument of its own");
	}
	
	/**
	 * Reads a bundled learn asset from the Android app, which is where the exported content lives.
	 *
	 * @param technique The asset's file name without its extension
	 * @return The asset's JSON
	 */
	private static String bundled(String technique) {
		Path asset = Path.of(System.getProperty("user.dir")).getParent().resolve("Sudoku-Android/app/src/main/assets/learn/" + technique + ".json");
		assumeTrue(Files.isReadable(asset), "The exported learn assets are not next to this checkout");
		try {
			return Files.readString(asset);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	@Test
	void refusesACellThatIsNotTheTechniquesToPlace() {
		LearnPuzzle puzzle = generate(Technique.HIDDEN_SINGLE_REGION, 1L);
		int[] board = puzzle.board();
		int[] solution = puzzle.solution();
		List<Integer> placements = new ArrayList<>();
		for (int cell : puzzle.placementCells()) {
			placements.add(cell);
		}
		
		for (int cell = 0; cell < LearnPuzzle.CELL_COUNT; cell++) {
			if (board[cell] != 0 || placements.contains(cell)) {
				continue;
			}
			
			assertFalse(puzzle.proves(cell, solution[cell]), "Cell " + cell + " is not this technique's to place and was accepted");
		}
	}
	
	@Test
	void refusesADigitThatIsNotTheSolutionAndACellThatIsNotOnTheBoard() {
		LearnPuzzle puzzle = generate(Technique.NAKED_SINGLE, 1L);
		int target = puzzle.targetCell();
		int wrong = puzzle.targetDigit() == LearnPuzzle.SIZE ? 1 : puzzle.targetDigit() + 1;
		
		assertFalse(puzzle.proves(target, wrong));
		assertFalse(puzzle.proves(-1, puzzle.targetDigit()));
		assertFalse(puzzle.proves(LearnPuzzle.CELL_COUNT, puzzle.targetDigit()));
		
		int filled = -1;
		for (int cell = 0; cell < LearnPuzzle.CELL_COUNT && filled < 0; cell++) {
			if (puzzle.board()[cell] != 0) {
				filled = cell;
			}
		}
		assertFalse(puzzle.proves(filled, puzzle.solution()[filled]), "A cell that is already filled was accepted");
	}
}
