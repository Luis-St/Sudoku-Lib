package net.luis.sudoku.generation;

import net.luis.sudoku.grid.*;
import net.luis.sudoku.rng.DeterministicRandom;
import net.luis.sudoku.solver.BacktrackingSolver;

/**
 * Digs holes into a complete solution grid, producing a puzzle whose only solution is that very grid.
 * <p>
 *     This is spec §3.3 steps 3–4. Starting from a complete valid solution, the digger walks the cells in a
 *     seeded random order and tries to blank each one. A removal is kept only when the resulting puzzle still has
 *     exactly one solution; otherwise the digit is put back before the walk continues. Because uniqueness is
 *     re-checked after every single removal and any removal that would introduce a second solution is undone, the
 *     returned givens are guaranteed to have exactly one solution, and that solution is the input grid.
 * </p>
 * <p>
 *     All randomness comes from a single up-front {@link DeterministicRandom#shuffledRange(int)} that fixes the
 *     order in which cells are visited. There is no further drawing, so the number of draws consumed depends only
 *     on the cell count, never on how many holes end up being dug. That keeps the digger byte-identical across
 *     JVMs for a given seed, exactly as the generator requires (spec §3.2).
 * </p>
 * <p>
 *     The digger never mutates the {@code solution} it is handed and never builds a mutable {@link Puzzle} it has
 *     to unset cells of. It works on a throwaway {@code int[]} of the current givens and rebuilds a fresh
 *     {@link Puzzle} for each uniqueness check, because {@link Puzzle#ofGivens} marks every clue as a given and
 *     {@link Puzzle#setValue} refuses to clear a given.
 * </p>
 *
 * @see BacktrackingSolver
 * @see Puzzle
 */
public final class HoleDigger {
	
	private HoleDigger() {}
	
	/**
	 * Digs as many holes as uniqueness allows, equivalent to {@link #dig(RegionPartition, int[], DeterministicRandom, int)}
	 * with an unbounded hole budget.
	 *
	 * @param partition The region partition the solution is laid out on
	 * @param solution The complete row-major solution grid, every value in {@code 1..n}
	 * @param random The source of the seeded cell visit order; the caller owns the stream
	 * @return A new row-major givens array, {@code 0} marking a hole, whose puzzle has exactly one solution
	 * @throws IllegalArgumentException If the solution length does not match the partition, if any value is outside
	 *        {@code 1..n}, or if the solution is not a valid complete grid
	 * @throws NullPointerException If the partition, the solution or the random is null
	 */
	public static int[] dig(RegionPartition partition, int[] solution, DeterministicRandom random) {
		return dig(partition, solution, random, Integer.MAX_VALUE);
	}
	
	/**
	 * Digs up to {@code maxHoles} holes into the given solution, keeping each removal only while the puzzle stays
	 * uniquely solvable.
	 * <p>
	 *     The cells are visited in the order given by {@code random.shuffledRange(cellCount)}, a single shuffle
	 *     drawn up front. Each visited cell is blanked, the resulting puzzle is checked for uniqueness with a
	 *     solution cap of two, and the removal is undone the moment a second solution appears. Once {@code maxHoles}
	 *     holes have been dug the walk stops early, but the full shuffle has already been drawn, so the draw count
	 *     depends only on the cell count.
	 * </p>
	 * <p>
	 *     The uniqueness check is built as {@code Puzzle.ofGivens(size, Variant.CLASSIC, partition, working)}. The
	 *     variant is irrelevant to counting solutions — it only names the region layout, which the {@code partition}
	 *     already carries — and {@link Variant#CLASSIC} is the one variant legal at every size, whereas
	 *     {@link Variant#CHAOS} is rejected at {@link GridSize#FOUR}.
	 * </p>
	 *
	 * @param partition The region partition the solution is laid out on
	 * @param solution The complete row-major solution grid, every value in {@code 1..n}
	 * @param random The source of the seeded cell visit order; the caller owns the stream
	 * @param maxHoles The maximum number of holes to dig; {@code 0} returns the solution unchanged
	 * @return A new row-major givens array, {@code 0} marking a hole, whose puzzle has exactly one solution
	 * @throws IllegalArgumentException If the solution length does not match the partition, if any value is outside
	 *        {@code 1..n}, if the solution is not a valid complete grid, or if {@code maxHoles} is negative
	 * @throws NullPointerException If the partition, the solution or the random is null
	 */
	public static int[] dig(RegionPartition partition, int[] solution, DeterministicRandom random, int maxHoles) {
		GridSize size = partition.size();
		int cellCount = size.cellCount();
		if (solution.length != cellCount) {
			throw new IllegalArgumentException("Solution length " + solution.length + " does not match the " + cellCount + " cells of a " + size + " grid");
		}
		if (maxHoles < 0) {
			throw new IllegalArgumentException("The maximum number of holes must not be negative, but is " + maxHoles);
		}
		
		for (int cellIndex = 0; cellIndex < cellCount; cellIndex++) {
			int value = solution[cellIndex];
			if (!size.isValidDigit(value)) {
				throw new IllegalArgumentException("Solution value " + value + " at cell " + cellIndex + " is not in 1.." + size.n());
			}
		}
		
		// Every value is a legal digit at this point, so ofGivens cannot throw here; isSolved rejects a grid that
		// is not complete-and-valid, i.e. one carrying a duplicate in some row, column or region.
		Puzzle solved = Puzzle.ofGivens(size, Variant.CLASSIC, partition, solution);
		if (!solved.isSolved()) {
			throw new IllegalArgumentException("The given grid is not a valid complete solution");
		}
		
		int[] working = solution.clone();
		int[] order = random.shuffledRange(cellCount);
		int dug = 0;
		for (int cellIndex : order) {
			if (dug >= maxHoles) {
				break;
			}
			
			int digit = working[cellIndex];
			working[cellIndex] = 0;
			// CLASSIC is arbitrary: only the partition drives region membership, and CLASSIC is legal at every size.
			Puzzle candidate = Puzzle.ofGivens(size, Variant.CLASSIC, partition, working);
			if (BacktrackingSolver.countSolutions(candidate, 2) == 1) {
				dug++;
			} else {
				working[cellIndex] = digit;
			}
		}
		return working;
	}
}
