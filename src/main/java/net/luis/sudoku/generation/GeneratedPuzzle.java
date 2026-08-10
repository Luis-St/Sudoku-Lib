package net.luis.sudoku.generation;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.grid.Puzzle;
import net.luis.sudoku.key.PuzzleKey;

import java.util.Arrays;
import java.util.Objects;

/**
 * The finished product of the generator: the key it was built from, the playable puzzle and the known solution.
 * <p>
 *     The solution is carried alongside the puzzle rather than recomputed on demand because the client checks the
 *     player's mistakes against this known solution instead of against local constraints, and the server replays a
 *     recorded solve order against it. Both need the exact grid the generator settled on, so it travels with the
 *     puzzle as a value.
 * </p>
 * <p>
 *     The solution is a row-major {@code int[]}. An {@code int[]} field breaks a record's value semantics unless it
 *     is handled by hand: the generated accessor would hand out the live array, and the generated
 *     {@link #equals(Object)} and {@link #hashCode()} would compare it by identity. This record therefore defensively
 *     copies the array on the way in and on the way out, and overrides {@code equals}, {@code hashCode} and
 *     {@code toString} to compare the solution by content.
 * </p>
 *
 * <p>
 *     <b>{@link #rated()} is not {@code key().difficulty()}.</b> The key states the band that was <i>asked</i> for;
 *     {@code rated} states the band the puzzle the generator settled on actually is. They agree whenever the search
 *     found the target, which is the ordinary case, and they differ whenever it ran out of attempts and returned its
 *     closest candidate instead — at 16x16 chaos above band 10 that is every time, by five bands or more. Anything
 *     that labels a puzzle for a player, prices it, or files it in a pool must read {@code rated}: taking the band
 *     off the key there is how a band-8 grid ends up sold as a band-13 one.
 * </p>
 *
 * @param key The key this puzzle was generated from
 * @param puzzle The playable puzzle, with its clues marked as givens
 * @param solution The complete row-major solution grid, with a value in {@code 1..n} in every cell
 * @param rated The band this puzzle actually rated, which is the requested band only when the search found it
 *
 * @see PuzzleGenerator
 */
public record GeneratedPuzzle(PuzzleKey key, Puzzle puzzle, int[] solution, Difficulty rated) {
	
	/**
	 * Constructs a generated puzzle.
	 * <p>
	 *     The solution array is copied, so a later mutation of the caller's array cannot change this record.
	 * </p>
	 *
	 * @throws NullPointerException If the key, the puzzle, the solution or the rated band is null
	 * @throws IllegalArgumentException If the solution length does not match the grid's cell count
	 */
	public GeneratedPuzzle {
		Objects.requireNonNull(key, "Key must not be null");
		Objects.requireNonNull(puzzle, "Puzzle must not be null");
		Objects.requireNonNull(solution, "Solution must not be null");
		Objects.requireNonNull(rated, "Rated band must not be null");
		
		if (solution.length != key.size().cellCount()) {
			throw new IllegalArgumentException("Expected " + key.size().cellCount() + " solution cells for grid size " + key.size() + ", got " + solution.length);
		}
		solution = solution.clone();
	}
	
	/**
	 * Returns the complete row-major solution grid.
	 * <p>
	 *     The returned array is a copy, so mutating it does not change this record. Prefer {@link #solutionAt(int)} in
	 *     hot loops, as it does not copy.
	 * </p>
	 *
	 * @return A copy of the solution
	 */
	@Override
	public int[] solution() {
		return this.solution.clone();
	}
	
	/**
	 * Returns the solution digit at the given row-major cell index without copying the backing array.
	 *
	 * @param cellIndex The row-major cell index
	 * @return The solution digit, in {@code 1..n}
	 * @throws IndexOutOfBoundsException If the cell index is outside the grid
	 */
	public int solutionAt(int cellIndex) {
		this.key.size().checkCellIndex(cellIndex);
		return this.solution[cellIndex];
	}
	
	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		return object instanceof GeneratedPuzzle other && this.key.equals(other.key) && this.puzzle.equals(other.puzzle)
			&& Arrays.equals(this.solution, other.solution) && this.rated == other.rated;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.key, this.puzzle, Arrays.hashCode(this.solution), this.rated);
	}
	
	@Override
	public String toString() {
		return "GeneratedPuzzle[key=" + this.key + ", puzzle=" + this.puzzle + ", solution=" + Arrays.toString(this.solution) + ", rated=" + this.rated + "]";
	}
}
