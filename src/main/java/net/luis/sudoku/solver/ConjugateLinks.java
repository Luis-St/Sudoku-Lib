package net.luis.sudoku.solver;

import java.util.ArrayList;
import java.util.List;

/**
 * The strong links of a single digit: the pairs of cells that are the only two places a digit can go within some unit.
 * <p>
 *     A conjugate pair is the raw material of every single-digit chain. Exactly one of its two cells holds the digit,
 *     so knowing that one of them does not immediately proves the other does. The {@link Skyscraper},
 *     {@link TwoStringKite}, {@link Crane} and {@link XChain} are all chains of these links, differing only in length
 *     and in how the links are allowed to meet.
 * </p>
 * <p>
 *     Links are returned in a deterministic order — units in {@link CandidateGrid#allUnits()} order, cells ascending
 *     within a unit — and each pair is reported once, even when the same two cells are conjugate in more than one unit.
 * </p>
 */
final class ConjugateLinks {
	
	private ConjugateLinks() {}
	
	/**
	 * Returns every conjugate pair of the given digit.
	 *
	 * @param grid The working grid; never mutated
	 * @param digit The digit to collect links for
	 * @return The links, each a two-element array of ascending cell indices
	 */
	static List<int[]> of(CandidateGrid grid, int digit) {
		List<int[]> links = new ArrayList<>();
		for (int[] unit : grid.allUnits()) {
			int first = -1;
			int second = -1;
			int seen = 0;
			for (int cell : unit) {
				if (grid.hasCandidate(cell, digit)) {
					if (seen == 0) {
						first = cell;
					} else {
						second = cell;
					}
					seen++;
				}
			}
			
			if (seen == 2 && !contains(links, first, second)) {
				links.add(new int[] { first, second });
			}
		}
		return links;
	}
	
	private static boolean contains(List<int[]> links, int first, int second) {
		for (int[] link : links) {
			if (link[0] == first && link[1] == second) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Collects the digit's eliminations for every cell that sees both ends of a chain.
	 * <p>
	 *     This is the conclusion every single-digit chain of even link count shares: one of the two ends holds the
	 *     digit, so no cell seeing both of them can.
	 * </p>
	 *
	 * @param grid The working grid
	 * @param digit The digit being chained
	 * @param endA The first end of the chain
	 * @param endB The other end of the chain
	 * @param technique The technique to attribute the eliminations to
	 * @param excluded The cells belonging to the pattern itself, which are never eliminated from
	 * @return The eliminations, or empty if the chain removes nothing
	 */
	static java.util.Optional<Deduction> eliminateSeenByBoth(CandidateGrid grid, int digit, int endA, int endB, Technique technique, int... excluded) {
		EliminationBuilder builder = new EliminationBuilder();
		for (int cell = 0; cell < grid.cellCount(); cell++) {
			if (cell == endA || cell == endB || contains(excluded, cell)) {
				continue;
			}
			
			if (grid.peers(cell, endA) && grid.peers(cell, endB)) {
				builder.add(grid, cell, digit);
			}
		}
		return builder.build(technique);
	}
	
	private static boolean contains(int[] cells, int cell) {
		for (int candidate : cells) {
			if (candidate == cell) {
				return true;
			}
		}
		return false;
	}
}
