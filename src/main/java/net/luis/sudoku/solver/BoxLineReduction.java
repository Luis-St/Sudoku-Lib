package net.luis.sudoku.solver;

import java.util.Arrays;
import java.util.Optional;

/**
 * The box-line-reduction intersection technique, the dual of the {@link PointingPair pointing pair}.
 * <p>
 *     Within a single row or column, if every candidate of a digit is confined to one region, then the digit
 *     must ultimately be placed in that region on this line. It can therefore be eliminated from the rest of the
 *     region — the cells that share the region but lie outside the line.
 * </p>
 * <p>
 *     The scan is fully deterministic: rows are visited first in ascending order, then columns in ascending
 *     order, and digits in ascending value order within each line. The first elimination that actually removes a
 *     candidate is returned; a match that would remove nothing is skipped so that {@link #find} never produces a
 *     no-op {@link Deduction.Eliminations}.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Technique#BOX_LINE_REDUCTION
 */
public final class BoxLineReduction implements TechniqueStrategy {
	
	/**
	 * Constructs the box-line-reduction strategy. The strategy is stateless and holds no grid.
	 */
	public BoxLineReduction() {}
	
	@Override
	public Technique technique() {
		return Technique.BOX_LINE_REDUCTION;
	}
	
	/**
	 * Scans every row and column for a digit whose candidates are confined to a single region and returns the
	 * first elimination it can make inside that region outside the line.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first box-line-reduction elimination, or empty if the pattern makes no progress anywhere
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		int n = grid.n();
		for (int row = 0; row < n; row++) {
			Optional<Deduction> found = this.scanLine(grid, grid.rowCells(row));
			if (found.isPresent()) {
				return found;
			}
		}
		
		for (int column = 0; column < n; column++) {
			Optional<Deduction> found = this.scanLine(grid, grid.columnCells(column));
			if (found.isPresent()) {
				return found;
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Tests a single line (a row or a column) for every digit and returns the first region-confined elimination.
	 *
	 * @param grid The working grid
	 * @param lineCells The ascending cell indices of the line
	 * @return The first elimination on this line, or empty
	 */
	private Optional<Deduction> scanLine(CandidateGrid grid, int[] lineCells) {
		int n = grid.n();
		int[] cells = new int[grid.cellCount()];
		int[] digits = new int[grid.cellCount()];
		for (int digit = 1; digit <= n; digit++) {
			int seen = 0;
			int region = -1;
			boolean sameRegion = true;
			for (int cell : lineCells) {
				if (!grid.hasCandidate(cell, digit)) {
					continue;
				}
				
				int cellRegion = grid.regionOf(cell);
				if (seen == 0) {
					region = cellRegion;
				} else {
					sameRegion &= cellRegion == region;
				}
				
				seen++;
			}
			if (seen < 2 || !sameRegion) {
				continue;
			}
			
			int count = 0;
			for (int cell : grid.regionCells(region)) {
				if (!this.inLine(lineCells, cell) && grid.hasCandidate(cell, digit)) {
					cells[count] = cell;
					digits[count] = digit;
					count++;
				}
			}
			
			if (count > 0) {
				return Optional.of(new Deduction.Eliminations(Technique.BOX_LINE_REDUCTION, Arrays.copyOf(cells, count), Arrays.copyOf(digits, count)));
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Checks whether a cell belongs to the given line.
	 *
	 * @param lineCells The ascending cell indices of the line
	 * @param cell The cell to look for
	 * @return True if the cell is part of the line, false otherwise
	 */
	private boolean inLine(int[] lineCells, int cell) {
		for (int lineCell : lineCells) {
			if (lineCell == cell) {
				return true;
			}
		}
		return false;
	}
}
