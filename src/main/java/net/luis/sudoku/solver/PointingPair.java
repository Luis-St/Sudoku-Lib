package net.luis.sudoku.solver;

import java.util.Arrays;
import java.util.Optional;

/**
 * The pointing-pair (and pointing-triple) intersection technique.
 * <p>
 *     Within a single region, if every candidate of a digit is confined to one row or one column of that region,
 *     then the digit must ultimately be placed inside the region on that line. It can therefore be eliminated
 *     from the rest of the line — the cells that share the row or column but lie outside the region.
 * </p>
 * <p>
 *     The scan is fully deterministic: regions are visited in ascending index order, digits in ascending value
 *     order, and the row form of a region is tested before its column form. The first elimination that actually
 *     removes a candidate is returned; a match that would remove nothing is skipped so that {@link #find} never
 *     produces a no-op {@link Deduction.Eliminations}.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Technique#POINTING_PAIR
 */
public final class PointingPair implements TechniqueStrategy {
	
	/**
	 * Constructs the pointing-pair strategy. The strategy is stateless and holds no grid.
	 */
	public PointingPair() {}
	
	@Override
	public Technique technique() {
		return Technique.POINTING_PAIR;
	}
	
	/**
	 * Scans every region for a digit whose candidates are confined to a single row or column and returns the
	 * first elimination it can make outside the region on that line.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first pointing-pair elimination, or empty if the pattern makes no progress anywhere
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		int n = grid.n();
		int[] cells = new int[grid.cellCount()];
		int[] digits = new int[grid.cellCount()];
		for (int region = 0; region < grid.partition().regionCount(); region++) {
			int[] regionCells = grid.regionCells(region);
			for (int digit = 1; digit <= n; digit++) {
				int seen = 0;
				int row = -1;
				int column = -1;
				boolean sameRow = true;
				boolean sameColumn = true;
				for (int cell : regionCells) {
					if (!grid.hasCandidate(cell, digit)) {
						continue;
					}
					
					int cellRow = grid.rowOf(cell);
					int cellColumn = grid.columnOf(cell);
					if (seen == 0) {
						row = cellRow;
						column = cellColumn;
					} else {
						sameRow &= cellRow == row;
						sameColumn &= cellColumn == column;
					}
					
					seen++;
				}
				if (seen < 2) {
					continue;
				}
				if (sameRow) {
					int count = 0;
					for (int cell : grid.rowCells(row)) {
						if (grid.regionOf(cell) != region && grid.hasCandidate(cell, digit)) {
							cells[count] = cell;
							digits[count] = digit;
							count++;
						}
					}
					
					if (count > 0) {
						return Optional.of(new Deduction.Eliminations(Technique.POINTING_PAIR, Arrays.copyOf(cells, count), Arrays.copyOf(digits, count)));
					}
				}
				if (sameColumn) {
					int count = 0;
					for (int cell : grid.columnCells(column)) {
						if (grid.regionOf(cell) != region && grid.hasCandidate(cell, digit)) {
							cells[count] = cell;
							digits[count] = digit;
							count++;
						}
					}
					
					if (count > 0) {
						return Optional.of(new Deduction.Eliminations(Technique.POINTING_PAIR, Arrays.copyOf(cells, count), Arrays.copyOf(digits, count)));
					}
				}
			}
		}
		return Optional.empty();
	}
}
