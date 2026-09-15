package net.luis.sudoku.solver;

import net.luis.sudoku.grid.Puzzle;

import java.util.*;

/**
 * The outcome of running the {@link TechniqueSolver} over a puzzle: whether it solved, how far it got, and which
 * techniques it needed and how often.
 * <p>
 *     This is the raw material the phase L6 difficulty rater consumes. A puzzle is rated by which techniques it forced
 *     and how frequently, so this report exposes both the {@link #hardestTechnique() hardest} technique and the full
 *     per-technique {@link #usage() usage counts}. A puzzle the technique solver cannot finish without guessing is
 *     reported as {@link #stuck() stuck}; such a puzzle is harder than the modelled technique set.
 * </p>
 *
 * @see TechniqueSolver#solve(Puzzle)
 */
public final class TechniqueReport {
	
	private final boolean solved;
	private final boolean stuck;
	private final boolean exceededCap;
	private final int[] solution;
	private final EnumMap<Technique, Integer> usage;
	private final int openingPlacements;

	/**
	 * Constructs a report. The usage map is copied into an {@link EnumMap} and the solution is defensively copied.
	 *
	 * @param solved Whether the grid was fully solved by techniques alone
	 * @param stuck Whether the solver ran out of applicable techniques before solving
	 * @param exceededCap Whether the solver ran out only because a level cap hid the harder techniques from it
	 * @param solution The final grid values, solved or furthest-progressed
	 * @param usage How many times each technique fired; techniques that never fired may be absent
	 * @param openingPlacements How many cells were placed before the first non-routine deduction
	 */
	TechniqueReport(boolean solved, boolean stuck, boolean exceededCap, int[] solution, Map<Technique, Integer> usage, int openingPlacements) {
		this.solved = solved;
		this.stuck = stuck;
		this.exceededCap = exceededCap;
		this.openingPlacements = openingPlacements;
		this.solution = solution.clone();
		this.usage = new EnumMap<>(Technique.class);
		
		for (Map.Entry<Technique, Integer> entry : usage.entrySet()) {
			if (entry.getValue() != 0) {
				this.usage.put(entry.getKey(), entry.getValue());
			}
		}
	}
	
	/**
	 * Returns whether the technique solver fully solved the puzzle.
	 *
	 * @return True if the grid is completely and correctly filled by techniques alone
	 */
	public boolean solved() {
		return this.solved;
	}
	
	/**
	 * Returns whether the solver got stuck, meaning it could not solve the puzzle and no technique applied to the
	 * furthest-progressed grid. Such a puzzle needs a guess and is beyond the modelled technique set.
	 *
	 * @return True if the solver ran out of applicable techniques before solving
	 */
	public boolean stuck() {
		return this.stuck;
	}
	
	/**
	 * Returns whether the solver stopped only because it was run under a level cap that hid the harder techniques
	 * from it, meaning the puzzle's true rating is <b>above</b> that cap but by an unknown amount.
	 * <p>
	 *     This is the early-abort signal of {@link TechniqueSolver#solve(Puzzle, int)}: it is enough to steer a
	 *     search that only needs a direction, but such a report carries no usable rating and must never be
	 *     classified. An uncapped solve never sets it, so {@link #stuck()} keeps its meaning of "beyond the whole
	 *     modelled technique set".
	 * </p>
	 *
	 * @return True if the solver gave up at the cap rather than at the end of the technique set
	 */
	public boolean exceededCap() {
		return this.exceededCap;
	}
	
	/**
	 * Returns the final grid values: the full solution when {@link #solved()}, otherwise the furthest state the
	 * techniques reached.
	 *
	 * @return A fresh copy of the row-major values
	 */
	public int[] solution() {
		return this.solution.clone();
	}
	
	/**
	 * Returns how often each technique fired, keyed in ascending technique order. Techniques that never fired are
	 * absent from the map, which callers must read as a count of zero.
	 *
	 * @return An unmodifiable view of the per-technique usage counts
	 */
	public Map<Technique, Integer> usage() {
		return Collections.unmodifiableMap(this.usage);
	}
	
	/**
	 * Returns how many times the given technique fired.
	 *
	 * @param technique The technique to look up
	 * @return The count, or {@code 0} if the technique never fired
	 */
	public int count(Technique technique) {
		return this.usage.getOrDefault(technique, 0);
	}
	
	/**
	 * Returns the hardest technique that fired, that is the one of the highest {@link Technique#rank()}.
	 *
	 * @return The hardest technique used, or empty if no technique fired at all
	 */
	public Optional<Technique> hardestTechnique() {
		Technique hardest = null;
		for (Technique technique : this.usage.keySet()) {
			if (hardest == null || technique.rank() > hardest.rank()) {
				hardest = technique;
			}
		}
		return Optional.ofNullable(hardest);
	}
	
	/**
	 * Returns the path score of this solve: every deduction's {@link Technique#score()}, summed.
	 * <p>
	 *     This measures how much non-routine work the puzzle demanded, as opposed to how hard its single hardest step
	 *     was. The two together are the rating; see {@link net.luis.sudoku.difficulty.DifficultyBands}.
	 * </p>
	 *
	 * @return The summed score, {@code 0} for a puzzle that needed nothing beyond singles
	 */
	public int pathScore() {
		int score = 0;
		for (Map.Entry<Technique, Integer> entry : this.usage.entrySet()) {
			score += entry.getKey().score() * entry.getValue();
		}
		return score;
	}
	
	/**
	 * Returns how many deductions of the given level or harder the solver applied.
	 *
	 * @param level The lowest level to count
	 * @return The summed usage count of every technique at or above that level
	 */
	public int countAtOrAbove(int level) {
		int total = 0;
		for (Map.Entry<Technique, Integer> entry : this.usage.entrySet()) {
			if (entry.getKey().level() >= level) {
				total += entry.getValue();
			}
		}
		return total;
	}

	/**
	 * Returns how many cells the solve placed before its first non-routine deduction.
	 * <p>
	 *     This is the singles-only opening of a puzzle: the stretch a player fills in without ever needing a technique
	 *     above {@link Technique#ROUTINE_LEVEL}. The band says nothing about it, and a long opening is what makes a
	 *     hard puzzle feel easy for most of its cells.
	 * </p>
	 *
	 * @return The number of routine placements before the first non-routine deduction, or every placement of a solve
	 *   that never needed one
	 */
	public int openingPlacements() {
		return this.openingPlacements;
	}

	/**
	 * Returns the total number of deductions the solver applied, the sum of every technique's usage count.
	 *
	 * @return The total step count
	 */
	public int totalSteps() {
		int total = 0;
		for (int count : this.usage.values()) {
			total += count;
		}
		return total;
	}
}
