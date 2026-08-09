package net.luis.sudoku.solver;

import java.util.List;
import java.util.Optional;

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
}
