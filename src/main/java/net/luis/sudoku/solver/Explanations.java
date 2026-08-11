package net.luis.sudoku.solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Small shared helpers for building an {@link Explanation}, used by the strategies that can re-derive their pattern
 * from the deduction rather than having to record it while they search.
 * <p>
 *     Re-deriving is only correct where the pattern is uniquely determined by the conclusion, which is true of the
 *     singles and of the locked-candidate techniques: given the placed cell and digit there is exactly one
 *     cross-hatching argument to show. Every technique whose conclusion could have come from more than one pattern
 *     records its pattern during the search instead, because a re-derived explanation could otherwise show a
 *     different pattern from the one the solver actually used.
 * </p>
 */
final class Explanations {

	private Explanations() {}

	/**
	 * Returns references to the row, the column and the region the given cell lies in.
	 *
	 * @param grid The working grid
	 * @param cell The row-major cell index
	 * @return The three units the cell belongs to
	 */
	static List<UnitRef> unitsOf(CandidateGrid grid, int cell) {
		return List.of(UnitRef.row(grid.rowOf(cell)), UnitRef.column(grid.columnOf(cell)), UnitRef.region(grid.regionOf(cell)));
	}

	/**
	 * Finds the peer of the given cell that already holds the given digit.
	 * <p>
	 *     This is the cell a player points at when they say "it cannot go there, it is already here", so it is what
	 *     makes an elimination visible rather than merely asserted.
	 * </p>
	 *
	 * @param grid The working grid
	 * @param cell The cell whose candidate is being explained away
	 * @param digit The digit that cannot go there
	 * @return The peer holding the digit, or {@code -1} if none does
	 */
	static int peerHolding(CandidateGrid grid, int cell, int digit) {
		for (int peer : grid.peers(cell)) {
			if (grid.value(peer) == digit) {
				return peer;
			}
		}
		return -1;
	}

	/**
	 * Collects, for every digit other than the one being placed, the peer that rules it out of the cell.
	 *
	 * @param grid The working grid
	 * @param cell The cell being placed
	 * @param digit The digit being placed
	 * @return One context cell per ruled-out digit, in ascending digit order
	 */
	static List<PatternCell> blockersFor(CandidateGrid grid, int cell, int digit) {
		List<PatternCell> blockers = new ArrayList<>();
		for (int other = 1; other <= grid.n(); other++) {
			if (other == digit) {
				continue;
			}

			int blocker = peerHolding(grid, cell, other);
			if (blocker >= 0) {
				blockers.add(PatternCell.of(blocker, CellRole.CONTEXT, other));
			}
		}
		return blockers;
	}

	/**
	 * Wraps every given cell as a pattern cell of one role, about one digit.
	 *
	 * @param cells The row-major cell indices
	 * @param role The role to give each of them
	 * @param digit The digit they are about, or {@code 0} for the whole cell
	 * @return The pattern cells, in the order given
	 */
	static List<PatternCell> cells(int[] cells, CellRole role, int digit) {
		List<PatternCell> result = new ArrayList<>(cells.length);
		for (int cell : cells) {
			result.add(digit == 0 ? PatternCell.of(cell, role) : PatternCell.of(cell, role, digit));
		}
		return result;
	}

	/**
	 * Names the given unit, by checking which of a member cell's three units has the same membership.
	 * <p>
	 *     A strategy that scans {@link CandidateGrid#allUnits()} holds a unit only as an array of cells, with nothing
	 *     saying whether it came from the rows, the columns or the regions. An explanation has to say which, because
	 *     "this row" and "this box" are different claims to a player.
	 * </p>
	 *
	 * @param grid The working grid
	 * @param unit The unit's cells
	 * @param cell Any cell of the unit
	 * @return The reference naming it
	 * @throws IllegalStateException If the cells are not one of that cell's three units
	 */
	static UnitRef refOf(CandidateGrid grid, int[] unit, int cell) {
		for (UnitRef candidate : unitsOf(grid, cell)) {
			if (Arrays.equals(candidate.cells(grid), unit)) {
				return candidate;
			}
		}
		throw new IllegalStateException("Unit holding cell " + cell + " is none of its row, column or region");
	}

	/**
	 * Names a unit two cells both lie in, which is what "these two see each other" means on screen.
	 *
	 * @param grid The working grid
	 * @param first One cell
	 * @param second The other
	 * @return The first unit they share, or null if they are not peers
	 */
	static UnitRef sharedUnit(CandidateGrid grid, int first, int second) {
		for (UnitRef unit : unitsOf(grid, first)) {
			if (contains(unit.cells(grid), second)) {
				return unit;
			}
		}
		return null;
	}

	/**
	 * Paints a two-coloured cluster as pattern cells, one colour shown as assumed true and the other as assumed false.
	 * <p>
	 *     Which colour is which is exactly what a colouring does not know, and does not need to: the argument works
	 *     whichever way round it falls. The two roles are used because they are the vocabulary a consumer already
	 *     draws as two opposed colours, not as a claim about which one is the true one.
	 * </p>
	 *
	 * @param cells The cell of each candidate
	 * @param digits The digit of each candidate
	 * @param colours The colour of each candidate, {@code 0} or {@code 1}
	 * @return The painted cells, in cluster order
	 */
	static List<PatternCell> painted(int[] cells, int[] digits, int[] colours) {
		List<PatternCell> result = new ArrayList<>(cells.length);
		for (int index = 0; index < cells.length; index++) {
			result.add(PatternCell.of(cells[index], colours[index] == 0 ? CellRole.LINK_ON : CellRole.LINK_OFF, digits[index]));
		}
		return result;
	}

	/**
	 * Names the unit that makes two cells a strong link for one digit: the unit they share in which the digit has
	 * exactly two candidates.
	 * <p>
	 *     Two cells can share as many as two units, a row and a region among them, and only one of the two is
	 *     necessarily the one that makes the link strong. Reporting the wrong one would have an animation outline a
	 *     unit in which the digit sits in four places and claim it forces something.
	 * </p>
	 *
	 * @param grid The working grid
	 * @param digit The digit the link is about
	 * @param first One end of the link
	 * @param second The other end
	 * @return The unit making them a strong link, or the first unit they share if none does
	 */
	static UnitRef strongLinkUnit(CandidateGrid grid, int digit, int first, int second) {
		UnitRef shared = null;
		for (UnitRef unit : unitsOf(grid, first)) {
			if (!contains(unit.cells(grid), second)) {
				continue;
			}
			if (shared == null) {
				shared = unit;
			}

			int count = 0;
			for (int cell : unit.cells(grid)) {
				if (grid.hasCandidate(cell, digit)) {
					count++;
				}
			}
			if (count == 2) {
				return unit;
			}
		}
		return shared;
	}

	/**
	 * Builds the steps every single-digit chain shares: the digit, the units its strong links live in, one beat per
	 * link, and the implication that one of the two ends holds the digit.
	 * <p>
	 *     The chain is given end to end, so {@code chain[0]} and the last entry are the ends the conclusion rests on,
	 *     and every consecutive pair is one link. The cells alternate between assumed false and assumed true, which is
	 *     the argument itself played out: suppose the first end does not hold the digit, then its strong partner does,
	 *     which rules it out of the next cell, which forces the one after, down to the far end. Either the first end
	 *     holds the digit or the last one does, which is what the implication step then says.
	 * </p>
	 *
	 * @param grid The working grid
	 * @param digit The digit being chained
	 * @param chain The chain cells, end to end, an even number of them
	 * @param explanation The explanation to record into
	 */
	static void chain(CandidateGrid grid, int digit, int[] chain, Explanation.Builder explanation) {
		List<UnitRef> units = new ArrayList<>();
		for (int index = 0; index + 1 < chain.length; index += 2) {
			UnitRef unit = strongLinkUnit(grid, digit, chain[index], chain[index + 1]);
			if (unit != null) {
				units.add(unit);
			}
		}

		explanation.focusDigit(digit);
		if (!units.isEmpty()) {
			explanation.focusUnits(digit, units);
		}
		for (int index = 0; index + 1 < chain.length; index += 2) {
			explanation.link(digit, List.of(
				PatternCell.of(chain[index], CellRole.LINK_OFF, digit),
				PatternCell.of(chain[index + 1], CellRole.LINK_ON, digit)
			));
		}
		explanation.implication(digit, List.of(
			PatternCell.of(chain[0], CellRole.LINK_ON, digit),
			PatternCell.of(chain[chain.length - 1], CellRole.LINK_ON, digit)
		));
	}

	private static boolean contains(int[] cells, int cell) {
		for (int candidate : cells) {
			if (candidate == cell) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the cells of the given unit that already hold a digit.
	 *
	 * @param grid The working grid
	 * @param unit The unit's cells
	 * @return The filled cells, as context
	 */
	static List<PatternCell> filledOf(CandidateGrid grid, int[] unit) {
		List<PatternCell> result = new ArrayList<>();
		for (int cell : unit) {
			int value = grid.value(cell);
			if (value != 0) {
				result.add(PatternCell.of(cell, CellRole.CONTEXT, value));
			}
		}
		return result;
	}
}
