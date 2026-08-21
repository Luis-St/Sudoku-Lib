package net.luis.sudoku.learn;

import net.luis.sudoku.grid.GridSize;
import net.luis.sudoku.grid.Puzzle;
import net.luis.sudoku.solver.*;

import java.util.*;

/**
 * One exercise of the learn area: a 9x9 position in which a chosen technique is exactly the next thing to do.
 * <p>
 *     A learn puzzle is not a puzzle in the ordinary sense. It is a <b>position</b>, taken from part-way through a
 *     real solve, chosen so that every technique easier than the target has already been exhausted. That is what makes
 *     it teach: in this position the target technique is not merely <i>possible</i>, it is the easiest thing that
 *     works, so a player who cannot see it has nothing else to fall back on.
 * </p>
 * <p>
 *     Two consequences follow, and both are why this record carries more than a grid of givens:
 * </p>
 * <ul>
 *     <li>The {@link #pencilMarks() pencil marks} are part of the puzzle, not a convenience. They are the candidate
 *         state <i>as the solver left it</i>, after the easier techniques did their eliminating. Re-deriving
 *         candidates from the board alone would put back candidates that a locked-candidate step had already removed,
 *         and the target technique would no longer be the next step.</li>
 *     <li>The {@link #targetCell()} and {@link #targetDigit()} are a single placement, and hitting them is how the
 *         app knows the technique was used. For a technique that only eliminates, the target is the placement its
 *         eliminations <i>unlock</i>: the player does the eliminating in their head or in pencil, and proves it by
 *         filling the cell it frees.</li>
 * </ul>
 *
 * @param technique The technique this exercise teaches
 * @param board The position, row-major, {@code 0} for an empty cell
 * @param solution The full solution of the position, row-major
 * @param pencilMarks Per cell, the candidate bitmask to pre-fill, {@code 0} for a filled cell
 * @param targetCell The row-major index of the cell the technique leads to
 * @param targetDigit The digit that belongs in the target cell
 * @param explanation Why the technique applies here, for the worked examples and the training hints
 *
 * @see LearnPuzzleGenerator
 */
public record LearnPuzzle(Technique technique, int[] board, int[] solution, int[] pencilMarks, int targetCell, int targetDigit, Explanation explanation) {
	
	/**
	 * The edge length of every learn puzzle.
	 * <p>
	 *     The learn area is 9x9 classic throughout. A pattern has to be recognisable as the same shape from one
	 *     exercise to the next, and a technique shown on a 6x6 grid one time and a 16x16 grid the next teaches the
	 *     grid rather than the technique.
	 * </p>
	 */
	public static final int SIZE = 9;
	
	/**
	 * The number of cells of every learn puzzle.
	 */
	public static final int CELL_COUNT = SIZE * SIZE;
	
	/**
	 * Constructs a learn puzzle, copying every array so the record stays an immutable value.
	 *
	 * @throws NullPointerException If the technique, the explanation or any array is null
	 * @throws IllegalArgumentException If an array is the wrong length, if the target is out of range, if the target
	 *         cell is not empty on the board, or if the target digit disagrees with the solution
	 */
	public LearnPuzzle {
		Objects.requireNonNull(technique, "Technique must not be null");
		Objects.requireNonNull(explanation, "Explanation must not be null");
		Objects.requireNonNull(board, "Board must not be null");
		Objects.requireNonNull(solution, "Solution must not be null");
		Objects.requireNonNull(pencilMarks, "Pencil marks must not be null");
		
		if (board.length != CELL_COUNT || solution.length != CELL_COUNT || pencilMarks.length != CELL_COUNT) {
			throw new IllegalArgumentException("Every array must have " + CELL_COUNT + " entries");
		}
		if (targetCell < 0 || targetCell >= CELL_COUNT) {
			throw new IllegalArgumentException("Target cell " + targetCell + " is not on the board");
		}
		if (targetDigit < 1 || targetDigit > SIZE) {
			throw new IllegalArgumentException("Target digit " + targetDigit + " is not in 1.." + SIZE);
		}
		if (board[targetCell] != 0) {
			throw new IllegalArgumentException("Target cell " + targetCell + " is already filled");
		}
		if (solution[targetCell] != targetDigit) {
			throw new IllegalArgumentException("Target digit " + targetDigit + " disagrees with the solution at cell " + targetCell);
		}
		if (technique != explanation.technique()) {
			throw new IllegalArgumentException("Explanation is for " + explanation.technique() + ", not " + technique);
		}
		
		board = board.clone();
		solution = solution.clone();
		pencilMarks = pencilMarks.clone();
	}
	
	/**
	 * Returns a copy of the position.
	 *
	 * @return The board, row-major, {@code 0} for an empty cell
	 */
	@Override
	public int[] board() {
		return this.board.clone();
	}
	
	/**
	 * Returns a copy of the solution.
	 *
	 * @return The solution, row-major
	 */
	@Override
	public int[] solution() {
		return this.solution.clone();
	}
	
	/**
	 * Returns a copy of the pencil marks.
	 *
	 * @return Per cell, the candidate bitmask, bit {@code d} for digit {@code d}
	 */
	@Override
	public int[] pencilMarks() {
		return this.pencilMarks.clone();
	}
	
	/**
	 * Returns the candidates to pre-fill into the given cell.
	 *
	 * @param cell The row-major cell index
	 * @return The digits, ascending, empty for a filled cell
	 * @throws IllegalArgumentException If the cell is not on the board
	 */
	public int[] candidatesAt(int cell) {
		if (cell < 0 || cell >= CELL_COUNT) {
			throw new IllegalArgumentException("Cell " + cell + " is not on the board");
		}
		
		int mask = this.pencilMarks[cell];
		int[] digits = new int[Integer.bitCount(mask)];
		int index = 0;
		while (mask != 0) {
			digits[index++] = Integer.numberOfTrailingZeros(mask);
			mask &= mask - 1;
		}
		return digits;
	}
	
	/**
	 * Returns how many cells of the position are still empty.
	 *
	 * @return The number of empty cells
	 */
	public int emptyCount() {
		int empty = 0;
		for (int value : this.board) {
			if (value == 0) {
				empty++;
			}
		}
		return empty;
	}
	
	/**
	 * Returns a key describing the <i>shape</i> of this exercise, used to keep a set of examples visually varied.
	 * <p>
	 *     Five worked examples of one technique are only worth five if they look different. Two puzzles with the same
	 *     key show the pattern in the same orientation, in the same band and stack of the grid, for the same digit,
	 *     which is exactly the sameness that would teach a player to recognise one picture rather than the idea
	 *     behind it. The key deliberately ignores everything else about the grid, because the rest of the board is
	 *     noise as far as recognising the pattern goes.
	 * </p>
	 *
	 * @return The layout key
	 */
	public String layoutKey() {
		StringJoiner key = new StringJoiner("|");
		key.add(String.valueOf(this.targetDigit));
		
		TreeSet<String> units = new TreeSet<>();
		for (ExplanationStep step : this.explanation.steps()) {
			for (UnitRef unit : step.units()) {
				units.add(unit.kind().name().charAt(0) + String.valueOf(unit.index() / 3));
			}
		}
		key.add(String.join(",", units));
		
		TreeSet<String> blocks = new TreeSet<>();
		for (PatternCell cell : this.explanation.allCells()) {
			if (cell.role() == CellRole.CONTEXT) {
				continue;
			}
			
			blocks.add((cell.cell() / SIZE / 3) + ":" + (cell.cell() % SIZE / 3));
		}
		key.add(String.join(",", blocks));
		return key.toString();
	}
	
	/**
	 * Rebuilds the position as the player meets it: the board as givens, with the shipped candidates written over the
	 * freshly derived ones.
	 * <p>
	 *     Deriving candidates from the board alone is not the same grid. The {@link #pencilMarks() pencil marks} are
	 *     the candidate state the solver left behind, after the easier techniques did their eliminating, and a grid
	 *     without those eliminations is one in which the taught technique is no longer what applies next.
	 * </p>
	 *
	 * @return The working grid of this exercise
	 */
	public CandidateGrid grid() {
		CandidateGrid grid = new CandidateGrid(Puzzle.classicOfGivens(GridSize.NINE, this.board));
		for (int cell = 0; cell < CELL_COUNT; cell++) {
			if (!grid.isEmpty(cell)) {
				continue;
			}
			
			int surplus = grid.candidates(cell) & ~this.pencilMarks[cell];
			while (surplus != 0) {
				grid.eliminate(cell, Integer.numberOfTrailingZeros(surplus));
				surplus &= surplus - 1;
			}
		}
		return grid;
	}
	
	/**
	 * Returns every cell this exercise's technique places a digit in, in this position.
	 * <p>
	 *     {@link #targetCell()} is one of them and usually the only one, but it is not the only <i>right answer</i>. A
	 *     strategy returns the first deduction it finds in scan order, and the easy techniques regularly have several:
	 *     a position can hold three full houses at once, and a player who fills the third of them has used the
	 *     technique exactly as well as one who filled the first.
	 * </p>
	 * <p>
	 *     Only placements found in <i>this</i> position count. The scan continues past each one by placing it, since
	 *     that is the only way to reach the next, but a placement whose explanation rests on a cell an earlier one
	 *     filled is left out: it did not follow from the position the player was given, it followed from the position
	 *     plus a move they have not made.
	 * </p>
	 * <p>
	 *     A technique that only eliminates has no placements of its own, and this returns the target alone: what such
	 *     an exercise asks for is the placement the eliminations unlock, which is the target by construction.
	 * </p>
	 *
	 * @return The row-major cell indices, the target first, without repeats
	 */
	public int[] placementCells() {
		List<ExplainedDeduction> placements = this.placements();
		List<Integer> cells = new ArrayList<>();
		cells.add(this.targetCell);
		for (ExplainedDeduction explained : placements) {
			int cell = ((Deduction.Placement) explained.deduction()).cell();
			if (!cells.contains(cell)) {
				cells.add(cell);
			}
		}
		
		int[] result = new int[cells.size()];
		for (int index = 0; index < result.length; index++) {
			result[index] = cells.get(index);
		}
		return result;
	}
	
	/**
	 * Returns why the technique places a digit in the given cell, in this position.
	 * <p>
	 *     The shipped {@link #explanation()} argues for the target and for nothing else, so it is the wrong picture to
	 *     draw over a board where the player solved one of the <i>other</i> cells the technique proves: it would
	 *     highlight a pattern somewhere else on the grid and write the conclusion into a cell they never touched. This
	 *     builds the argument for the cell that was actually filled, from the same strategy that judged it.
	 * </p>
	 *
	 * @param cell The row-major cell index
	 * @return The explanation of that placement, or empty if the technique does not place a digit there
	 */
	public Optional<Explanation> explanationOf(int cell) {
		if (cell == this.targetCell) {
			return Optional.of(this.explanation);
		}
		
		for (ExplainedDeduction explained : this.placements()) {
			if (((Deduction.Placement) explained.deduction()).cell() == cell) {
				return Optional.of(explained.explanation());
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Asks this exercise's technique what it places in this position.
	 * <p>
	 *     Every placement holds in the position <i>as the player was given it</i>: nothing is applied between the
	 *     finds, so no answer here depends on another move having been made first. That is the whole reason
	 *     {@link TechniqueStrategy#findAllPlacements(CandidateGrid)} exists rather than this walking the technique
	 *     forwards and collecting what it turns up. Walking applies each find to reach the next, and the grid it then
	 *     scans is no longer the player's: cells they could have solved get filled in by the walk before it ever looks
	 *     at the unit that proves them, and the answer is lost.
	 * </p>
	 *
	 * @return The placements, in the technique's own scan order, empty for a technique that only eliminates
	 */
	private List<ExplainedDeduction> placements() {
		for (TechniqueStrategy strategy : TechniqueSolver.STRATEGIES) {
			if (strategy.technique() == this.technique) {
				return strategy.findAllPlacements(this.grid());
			}
		}
		return List.of();
	}
	
	/**
	 * Reports whether writing the given digit into the given cell is this exercise's technique being used.
	 * <p>
	 *     This is what the training judges a solve by, and it is deliberately wider than "is this the target": every
	 *     cell of {@link #placementCells()} is the technique applied to this position, and an exercise that accepted
	 *     only one of several equally correct answers would record a player who used the technique perfectly as having
	 *     solved the position without it.
	 * </p>
	 *
	 * @param cell The row-major cell index the player wrote in
	 * @param digit The digit they wrote
	 * @return True if the technique proves that digit belongs in that cell
	 */
	public boolean proves(int cell, int digit) {
		if (cell < 0 || cell >= CELL_COUNT || this.board[cell] != 0) {
			return false;
		}
		// A digit that is not the solution is not proof of anything, whichever cell it went into.
		if (this.solution[cell] != digit) {
			return false;
		}
		if (cell == this.targetCell) {
			return true;
		}
		
		for (int placement : this.placementCells()) {
			if (placement == cell) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns the cells the technique's pattern occupies, without the merely explanatory ones.
	 * <p>
	 *     This is what the first training level marks step by step, and what the second marks on request. The context
	 *     cells are left out on purpose: they explain <i>why</i> the pattern works, which is more than a hint should
	 *     give away.
	 * </p>
	 *
	 * @return The pattern cells, in the order the explanation introduces them, without repeats
	 */
	public int[] patternCells() {
		List<Integer> cells = new ArrayList<>();
		for (PatternCell cell : this.explanation.allCells()) {
			if (cell.role() != CellRole.CONTEXT && !cells.contains(cell.cell())) {
				cells.add(cell.cell());
			}
		}
		
		int[] result = new int[cells.size()];
		for (int index = 0; index < result.length; index++) {
			result[index] = cells.get(index);
		}
		return result;
	}
	
	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		return object instanceof LearnPuzzle other
			&& this.technique == other.technique
			&& this.targetCell == other.targetCell
			&& this.targetDigit == other.targetDigit
			&& Arrays.equals(this.board, other.board)
			&& Arrays.equals(this.solution, other.solution)
			&& Arrays.equals(this.pencilMarks, other.pencilMarks)
			&& this.explanation.equals(other.explanation);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.technique, this.targetCell, this.targetDigit, Arrays.hashCode(this.board), Arrays.hashCode(this.pencilMarks));
	}
	
	@Override
	public String toString() {
		return "LearnPuzzle[" + this.technique + ", target " + this.targetDigit + " at " + this.targetCell + ", " + this.emptyCount() + " empty]";
	}
}
