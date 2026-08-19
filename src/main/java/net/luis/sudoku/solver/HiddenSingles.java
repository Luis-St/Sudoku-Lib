package net.luis.sudoku.solver;

import java.util.*;

/**
 * The shared scan behind {@link HiddenSingleRegion} and {@link HiddenSingleLine}.
 * <p>
 *     The two techniques differ only in which units they look at, and in the difficulty level that difference earns
 *     them; the deduction itself is the same argument, so it is written once here.
 * </p>
 */
final class HiddenSingles {
	
	private HiddenSingles() {}
	
	/**
	 * Scans the given units for a digit that is a candidate of exactly one of a unit's cells.
	 *
	 * @param grid The working grid; never mutated
	 * @param units The units to scan, in the order they should be visited
	 * @param technique The technique to attribute the placement to
	 * @return The first forced placement, or empty if no digit is confined to a single cell of any of the units
	 */
	static Optional<Deduction> scan(CandidateGrid grid, List<int[]> units, Technique technique) {
		for (int[] unit : units) {
			for (int digit = 1; digit <= grid.n(); digit++) {
				int seen = 0;
				int target = -1;
				for (int cell : unit) {
					if (grid.hasCandidate(cell, digit)) {
						seen++;
						target = cell;
					}
				}
				
				if (seen == 1) {
					return Optional.of(new Deduction.Placement(technique, target, digit));
				}
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Runs {@link #scan(CandidateGrid, List, Technique)} and explains its result as cross-hatching.
	 * <p>
	 *     The explanation is the argument a player actually makes: the unit is outlined, then every other empty cell
	 *     of it is shown together with the placed digit elsewhere that rules the digit out of it. What is left over is
	 *     the placement. Showing the blockers rather than only the answer is the whole difference between a hint and
	 *     a lesson.
	 * </p>
	 *
	 * @param grid The working grid; never mutated
	 * @param units The units to scan, in the order they should be visited
	 * @param technique The technique to attribute the placement to
	 * @return The first forced placement and its explanation, or empty if there is none
	 */
	static Optional<ExplainedDeduction> scanExplained(CandidateGrid grid, List<int[]> units, Technique technique) {
		return scan(grid, units, technique).map(deduction -> {
			Deduction.Placement placement = (Deduction.Placement) deduction;
			int digit = placement.digit();
			int[] unit = unitContaining(units, placement.cell());
			
			List<PatternCell> blocked = new ArrayList<>();
			for (int cell : unit) {
				if (cell == placement.cell() || !grid.isEmpty(cell)) {
					continue;
				}
				
				blocked.add(PatternCell.of(cell, CellRole.CONTEXT, digit));
				int blocker = Explanations.peerHolding(grid, cell, digit);
				if (blocker >= 0) {
					blocked.add(PatternCell.of(blocker, CellRole.BASE, digit));
				}
			}
			
			return new ExplainedDeduction(deduction, Explanation.builder(technique)
				.focusDigit(digit)
				.focusUnits(digit, List.of(Explanations.refOf(grid, unit, placement.cell())))
				.implication(digit, blocked)
				.conclusion(deduction)
				.build());
		});
	}
	
	/**
	 * Returns the first of the scanned units that holds the given cell, which is the unit the scan argued in.
	 *
	 * @param units The units that were scanned, in scan order
	 * @param cell The placed cell
	 * @return The unit's cells
	 */
	private static int[] unitContaining(List<int[]> units, int cell) {
		for (int[] unit : units) {
			for (int member : unit) {
				if (member == cell) {
					return unit;
				}
			}
		}
		// Unreachable: the placement came from one of these very units.
		throw new IllegalStateException("Placed cell " + cell + " lies in none of the scanned units");
	}
	
}
