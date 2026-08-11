package net.luis.sudoku.solver;

import java.util.ArrayList;
import java.util.List;

/**
 * The enumeration of almost-locked sets, shared by {@link AlsXz}, {@link AlsChain} and the {@link DeathBlossom}.
 * <p>
 *     An almost-locked set is {@code k} cells inside one unit whose candidates together span {@code k + 1} digits. It
 *     is one digit away from being a naked subset: take any single digit out of it and the remaining {@code k} digits
 *     are locked into the {@code k} cells. Equivalently, the set holds all but one of its digits for certain — which
 *     is exactly the kind of near-certainty a chain can hang off.
 * </p>
 * <p>
 *     Two sets are joined by a <b>restricted common digit</b>: a digit both of them list, all of whose occurrences
 *     across the two sets see each other, so it can be used by at most one of the two. If it is used by neither, both
 *     sets would be over-full, so exactly one of them takes it — and the other set is then a genuine locked set.
 * </p>
 * <p>
 *     Sets of up to {@value #MAX_SIZE} cells are enumerated, in a deterministic order: units in
 *     {@link CandidateGrid#allUnits()} order, cell combinations ascending. Larger sets exist but cost far more to
 *     enumerate than the eliminations they add are worth.
 * </p>
 */
final class AlmostLockedSets {
	
	/**
	 * The largest almost-locked set the enumeration produces, counted in cells.
	 */
	static final int MAX_SIZE = 3;
	
	private AlmostLockedSets() {}
	
	/**
	 * Enumerates every almost-locked set of the grid.
	 *
	 * @param grid The working grid; never mutated
	 * @return The sets, in a deterministic order
	 */
	static List<Als> of(CandidateGrid grid) {
		List<Als> sets = new ArrayList<>();
		for (int[] unit : grid.allUnits()) {
			for (int size = 1; size <= MAX_SIZE; size++) {
				collect(grid, unit, new int[size], 0, 0, 0, sets);
			}
		}
		return sets;
	}
	
	private static void collect(CandidateGrid grid, int[] unit, int[] chosen, int depth, int start, int mask, List<Als> sets) {
		if (depth == chosen.length) {
			if (Integer.bitCount(mask) == chosen.length + 1) {
				sets.add(new Als(chosen.clone(), mask));
			}
			return;
		}
		if (Integer.bitCount(mask) > chosen.length + 1) {
			return;
		}
		
		for (int position = start; position <= unit.length - (chosen.length - depth); position++) {
			int cell = unit[position];
			if (!grid.isEmpty(cell)) {
				continue;
			}
			
			chosen[depth] = cell;
			collect(grid, unit, chosen, depth + 1, position + 1, mask | grid.candidates(cell), sets);
		}
	}
	
	/**
	 * Returns the bitmask of digits that are restricted commons of the two sets: listed by both, and with every
	 * occurrence across the two sets seeing every other.
	 *
	 * @param grid The working grid
	 * @param first The first set
	 * @param second The second set
	 * @return The bitmask of restricted common digits, possibly zero
	 */
	static int restrictedCommons(CandidateGrid grid, Als first, Als second) {
		int commons = first.mask() & second.mask();
		int restricted = 0;
		int remaining = commons;
		while (remaining != 0) {
			int digit = Integer.numberOfTrailingZeros(remaining);
			remaining &= remaining - 1;
			if (isRestricted(grid, first, second, digit)) {
				restricted |= 1 << digit;
			}
		}
		return restricted;
	}
	
	private static boolean isRestricted(CandidateGrid grid, Als first, Als second, int digit) {
		for (int cell : first.cells()) {
			if (!grid.hasCandidate(cell, digit)) {
				continue;
			}
			
			for (int otherCell : second.cells()) {
				if (grid.hasCandidate(otherCell, digit) && !grid.peers(cell, otherCell)) {
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Removes the given digit from every cell that sees each of the digit's occurrences in all of the given sets.
	 *
	 * @param grid The working grid
	 * @param digit The digit to remove
	 * @param technique The technique to attribute the eliminations to
	 * @param sets The sets whose occurrences of the digit must all be seen
	 * @return The eliminations, or empty if nothing is removed
	 */
	static java.util.Optional<Deduction> eliminateSeenBy(CandidateGrid grid, int digit, Technique technique, Als... sets) {
		EliminationBuilder builder = new EliminationBuilder();
		for (int cell = 0; cell < grid.cellCount(); cell++) {
			boolean inside = false;
			boolean seesAll = true;
			for (Als set : sets) {
				inside |= set.contains(cell);
				for (int member : set.cells()) {
					if (grid.hasCandidate(member, digit) && !grid.peers(cell, member)) {
						seesAll = false;
						break;
					}
				}
			}
			
			if (!inside && seesAll) {
				builder.add(grid, cell, digit);
			}
		}
		return builder.build(technique);
	}
	
	/**
	 * Shows one set's cells with the digits each of them carries, so a consumer can draw the set as a group rather
	 * than as loose cells.
	 *
	 * @param grid The working grid
	 * @param set The set
	 * @param role The role to give its cells
	 * @return The pattern cells, in the set's own order
	 */
	static List<PatternCell> cellsOf(CandidateGrid grid, Als set, CellRole role) {
		List<PatternCell> cells = new ArrayList<>(set.cells().length);
		for (int cell : set.cells()) {
			cells.add(new PatternCell(cell, role, grid.candidates(cell)));
		}
		return cells;
	}
	
	/**
	 * Shows every place one digit occurs across the given sets, which is what a claim about that digit is made of.
	 *
	 * @param grid The working grid
	 * @param digit The digit
	 * @param role The role to give the occurrences
	 * @param sets The sets to look through
	 * @return The pattern cells, set by set
	 */
	static List<PatternCell> occurrencesOf(CandidateGrid grid, int digit, CellRole role, Als... sets) {
		List<PatternCell> cells = new ArrayList<>();
		for (Als set : sets) {
			for (int cell : set.cells()) {
				if (grid.hasCandidate(cell, digit)) {
					cells.add(PatternCell.of(cell, role, digit));
				}
			}
		}
		return cells;
	}
	
	/**
	 * One almost-locked set.
	 *
	 * @param cells The ascending cell indices the set occupies
	 * @param mask The bitmask of the digits the set spans, which holds one bit more than it has cells
	 */
	record Als(int[] cells, int mask) {
		
		/**
		 * Checks whether this set and the other share a cell, which disqualifies them from being chained.
		 */
		boolean overlaps(Als other) {
			for (int cell : this.cells) {
				for (int otherCell : other.cells) {
					if (cell == otherCell) {
						return true;
					}
				}
			}
			return false;
		}
		
		/**
		 * Checks whether the given cell belongs to this set.
		 */
		boolean contains(int cell) {
			for (int member : this.cells) {
				if (member == cell) {
					return true;
				}
			}
			return false;
		}
	}
}
