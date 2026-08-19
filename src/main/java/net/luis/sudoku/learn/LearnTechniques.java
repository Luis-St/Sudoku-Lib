package net.luis.sudoku.learn;

import net.luis.sudoku.solver.Technique;

import java.util.*;

/**
 * Which {@link Technique techniques} the learn area teaches, and therefore which ones puzzles are generated for,
 * wiki pages are written for and achievements exist for.
 * <p>
 *     This is the single source of truth for that question. The puzzle export task, the technique list the app shows
 *     and the denominator of the "mastered" counter all read it, so a technique can be added or withdrawn in one
 *     place. In particular the counter must be computed from {@link #count()} rather than stored, or adding a
 *     technique later would make an already earned "all techniques" achievement read as incomplete.
 * </p>
 * <p>
 *     Two groups of the solver's techniques are deliberately absent:
 * </p>
 * <ul>
 *     <li>{@link Technique#LAW_OF_LEFTOVERS} argues over irregular regions and cannot occur in the 9x9 classic grid
 *         the learn area is built on, so there is no puzzle that could teach it here.</li>
 *     <li>{@link Technique#MULTI_COLOURING} is dominated by {@link Technique#MEDUSA_3D}, which colours candidates
 *         rather than cells and therefore reaches every conclusion multi-colouring could, from a lower rank. No
 *         position was ever found in which multi-colouring is the easiest technique that applies, over searches of
 *         thousands of seeds across five difficulty bands, so there is no exercise that could honestly teach it.</li>
 *     <li>The level 15 techniques assume a candidate and play the position out. They have no pattern to recognise,
 *         which is what every lesson in the learn area is built around, and forcing one of them and nothing harder is
 *         far too slow to generate on a phone.</li>
 * </ul>
 *
 * @see Technique
 */
public final class LearnTechniques {
	
	private static final Set<Technique> EXCLUDED = EnumSet.of(Technique.LAW_OF_LEFTOVERS, Technique.MULTI_COLOURING);
	private static final List<Technique> TAUGHT = List.copyOf(EnumSet.allOf(Technique.class).stream()
		// Qualified: an unqualified MAX_LEVEL here is a forward reference to a constant declared further down, which
		// javac rejects outright.
		.filter(technique -> technique.level() <= LearnTechniques.MAX_LEVEL)
		.filter(technique -> !EXCLUDED.contains(technique))
		.toList());
	/**
	 * The hardest {@link Technique#level() level} the learn area covers.
	 * <p>
	 *     Level 14 is the last level whose techniques still argue forwards, without assuming anything, so a pattern
	 *     highlight is still an honest description of what a player has to see.
	 * </p>
	 */
	public static final int MAX_LEVEL = 14;
	
	private LearnTechniques() {}
	
	/**
	 * Returns every technique the learn area teaches, in {@link Technique} declaration order, which is difficulty
	 * order.
	 *
	 * @return The taught techniques, hardest last
	 */
	public static List<Technique> taught() {
		return TAUGHT;
	}
	
	/**
	 * Returns how many techniques the learn area teaches.
	 * <p>
	 *     This is the denominator of the "mastered" counter, and the reason that counter must never be hardcoded.
	 * </p>
	 *
	 * @return The number of taught techniques
	 */
	public static int count() {
		return TAUGHT.size();
	}
	
	/**
	 * Checks whether the learn area teaches the given technique.
	 *
	 * @param technique The technique to check
	 * @return True if it is taught
	 * @throws NullPointerException If the technique is null
	 */
	public static boolean isTaught(Technique technique) {
		Objects.requireNonNull(technique, "Technique must not be null");
		
		return TAUGHT.contains(technique);
	}
}
