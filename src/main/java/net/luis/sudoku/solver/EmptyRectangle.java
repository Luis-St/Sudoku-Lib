package net.luis.sudoku.solver;

import java.util.Optional;

/**
 * The Empty Rectangle: a digit whose candidates in a region form an L across one row and one column of that region,
 * combined with a conjugate pair on a crossing line.
 * <p>
 *     The L means the region's copy of the digit lies either somewhere on that row or somewhere on that column — the
 *     rectangle of cells the L leaves free is the "empty" part the technique is named after. Now take a conjugate pair
 *     of the digit in another row, one of whose ends stands in the L's column outside the region. If the L's row cell
 *     in the pair's other column held the digit, that pair would be forced onto the L's column, and both of the
 *     region's options would be ruled out at once — which is impossible. So that cell can be eliminated.
 * </p>
 * <p>
 *     Regions are not assumed to be boxes: the L is found by testing which row and column the region's candidates fall
 *     on, so the technique works unchanged on chaos layouts.
 * </p>
 * <p>
 *     The scan is deterministic — digits ascending, regions ascending, the row-hinge form before the column-hinge
 *     form — and the first configuration that actually removes a candidate is returned.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Technique#EMPTY_RECTANGLE
 */
public final class EmptyRectangle implements TechniqueStrategy {
	
	/**
	 * Constructs the Empty Rectangle strategy. The strategy is stateless and holds no grid.
	 */
	public EmptyRectangle() {}
	
	@Override
	public Technique technique() {
		return Technique.EMPTY_RECTANGLE;
	}
	
	/**
	 * Scans every region for an L-shaped candidate set and pairs it with a conjugate pair on a crossing line.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first Empty Rectangle elimination, or empty if the pattern makes no progress anywhere
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		for (int digit = 1; digit <= grid.n(); digit++) {
			for (int region = 0; region < grid.partition().regionCount(); region++) {
				Optional<Deduction> found = this.scanRegion(grid, digit, region);
				if (found.isPresent()) {
					return found;
				}
			}
		}
		return Optional.empty();
	}
	
	private Optional<Deduction> scanRegion(CandidateGrid grid, int digit, int region) {
		int n = grid.n();
		for (int row = 0; row < n; row++) {
			for (int column = 0; column < n; column++) {
				if (!this.formsHinge(grid, digit, region, row, column)) {
					continue;
				}
				
				Optional<Deduction> byRow = this.eliminate(grid, digit, region, row, column, true);
				if (byRow.isPresent()) {
					return byRow;
				}
				
				Optional<Deduction> byColumn = this.eliminate(grid, digit, region, row, column, false);
				if (byColumn.isPresent()) {
					return byColumn;
				}
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Checks whether the region's candidates for the digit form a genuine L on the given row and column: every
	 * candidate lies on one of the two lines, and each line carries at least one candidate away from their crossing.
	 * <p>
	 *     Requiring both arms is what keeps this apart from {@link Pointing}: a region whose candidates lie on a
	 *     single line is locked candidates and is handled six levels lower.
	 * </p>
	 */
	private boolean formsHinge(CandidateGrid grid, int digit, int region, int row, int column) {
		boolean rowArm = false;
		boolean columnArm = false;
		for (int cell : grid.regionCells(region)) {
			if (!grid.hasCandidate(cell, digit)) {
				continue;
			}
			
			boolean onRow = grid.rowOf(cell) == row;
			boolean onColumn = grid.columnOf(cell) == column;
			if (!onRow && !onColumn) {
				return false;
			}
			
			rowArm |= onRow && !onColumn;
			columnArm |= onColumn && !onRow;
		}
		return rowArm && columnArm;
	}
	
	/**
	 * Looks for the conjugate pair that completes the pattern and returns the elimination it proves.
	 *
	 * @param throughRow True to hinge on the L's row, false to hinge on its column
	 */
	private Optional<Deduction> eliminate(CandidateGrid grid, int digit, int region, int row, int column, boolean throughRow) {
		int n = grid.n();
		for (int line = 0; line < n; line++) {
			// The conjugate pair runs across the L's arm, so it must lie off the L's own line.
			if (throughRow ? line == row : line == column) {
				continue;
			}
			
			int[] lineCells = throughRow ? grid.rowCells(line) : grid.columnCells(line);
			int[] pair = this.pairOf(grid, lineCells, digit);
			if (pair == null) {
				continue;
			}
			
			for (int i = 0; i < 2; i++) {
				int anchor = pair[i];
				int target = pair[1 - i];
				// One end must stand on the L's far arm outside the region ...
				if (grid.regionOf(anchor) == region || (throughRow ? grid.columnOf(anchor) != column : grid.rowOf(anchor) != row)) {
					continue;
				}
				
				// ... and the elimination happens where the other end's line crosses the L's near arm.
				int victim = throughRow ? row * n + grid.columnOf(target) : grid.rowOf(target) * n + column;
				if (grid.regionOf(victim) == region || victim == target) {
					continue;
				}
				
				EliminationBuilder builder = new EliminationBuilder();
				builder.add(grid, victim, digit);
				Optional<Deduction> found = builder.build(Technique.EMPTY_RECTANGLE);
				if (found.isPresent()) {
					return found;
				}
			}
		}
		return Optional.empty();
	}
	
	private int[] pairOf(CandidateGrid grid, int[] lineCells, int digit) {
		int first = -1;
		int second = -1;
		int seen = 0;
		for (int cell : lineCells) {
			if (grid.hasCandidate(cell, digit)) {
				if (seen == 0) {
					first = cell;
				} else {
					second = cell;
				}
				seen++;
			}
		}
		return seen == 2 ? new int[] { first, second } : null;
	}
}
