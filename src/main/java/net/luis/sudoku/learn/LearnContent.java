package net.luis.sudoku.learn;

import net.luis.sudoku.solver.Technique;

import java.util.List;
import java.util.Objects;

/**
 * The shape of the learn area's content: how many worked examples a technique gets, and how its training is divided
 * into levels and exercises.
 * <p>
 *     These numbers are here rather than in the app because both ends have to agree on them exactly. The export task
 *     generates against them and the app counts progress against them, and a disagreement would show as a player who
 *     can never finish a level.
 * </p>
 */
public final class LearnContent {

	/**
	 * How many worked examples the wiki page of a technique shows.
	 * <p>
	 *     Five, each with a different {@link LearnPuzzle#layoutKey() layout}, so that what is learned is the pattern
	 *     rather than one picture of it.
	 * </p>
	 */
	public static final int EXAMPLES_PER_TECHNIQUE = 5;

	/**
	 * How many training levels a technique has, each giving less help than the one before.
	 */
	public static final int LEVELS = 3;

	/**
	 * How many exercises each training level holds.
	 */
	public static final int SUB_LEVELS = 3;

	/**
	 * How many training exercises a technique has in total.
	 */
	public static final int EXERCISES_PER_TECHNIQUE = LEVELS * SUB_LEVELS;

	/**
	 * How many puzzles have to be generated in total: the examples and the exercises of every taught technique.
	 */
	public static int totalPuzzles() {
		return LearnTechniques.count() * (EXAMPLES_PER_TECHNIQUE + EXERCISES_PER_TECHNIQUE);
	}

	private LearnContent() {}

	/**
	 * How much help a training level gives.
	 * <p>
	 *     The three levels are the same puzzle set under three different amounts of support, which is the whole of the
	 *     teaching design: recognise the pattern, then find it when asked, then find it unprompted.
	 * </p>
	 */
	public enum Assistance {

		/**
		 * Level 1. A button walks through the pattern cells one at a time, in the order the explanation introduces
		 * them, so a player who cannot yet see the shape is shown it rather than left to guess.
		 */
		GUIDED,
		/**
		 * Level 2. Hints only when asked for, and they may mark cells and nothing else. No candidate is highlighted
		 * and nothing says what to do with the marked cells, so the deduction is still the player's to make.
		 */
		ON_REQUEST,
		/**
		 * Level 3. Nothing but the technique's description. This is the level that proves the technique was learned.
		 */
		NONE;

		/**
		 * Returns the assistance of the given training level.
		 *
		 * @param level The one-based level, {@code 1..}{@link #LEVELS}
		 * @return The assistance that level gives
		 * @throws IllegalArgumentException If the level is out of range
		 */
		public static Assistance ofLevel(int level) {
			if (level < 1 || level > LEVELS) {
				throw new IllegalArgumentException("Level " + level + " is not in 1.." + LEVELS);
			}
			return values()[level - 1];
		}
	}

	/**
	 * Returns the seed a given exercise is generated from.
	 * <p>
	 *     Seeds are derived rather than random so that the bundled exercises are the same for every player, and so
	 *     that regenerating the asset after a change produces the same puzzles wherever it did not have to change.
	 *     The technique's ordinal is mixed in so that two techniques never search the same seeds in the same order,
	 *     which would make their exercises look related when they are not.
	 * </p>
	 *
	 * @param technique The technique
	 * @param level The one-based training level, or {@code 0} for the worked examples
	 * @param index The zero-based index within the level
	 * @return The seed to start the search at
	 * @throws NullPointerException If the technique is null
	 */
	public static long seedFor(Technique technique, int level, int index) {
		Objects.requireNonNull(technique, "Technique must not be null");

		return (technique.ordinal() + 1L) * 1_000_000L + level * 1_000L + index * 37L + 1L;
	}

	/**
	 * Returns every technique the export task has to generate for.
	 *
	 * @return The taught techniques
	 */
	public static List<Technique> techniques() {
		return LearnTechniques.taught();
	}
}
