package net.luis.sudoku.solver;

import java.util.Optional;

/**
 * The claiming half of locked candidates: a digit whose candidates within a row or column all lie in one region must
 * be placed in that region, so it can be removed from the rest of the region off the line.
 * <p>
 *     This is the mirror of {@link Pointing} — here the line constrains the region. The digit has to go somewhere on
 *     the line, every place it could go is inside the same region, and therefore the region carries the digit on that
 *     line and nowhere else. The two halves are one technique to a player, which is why they share a difficulty
 *     level.
 * </p>
 * <p>
 *     The scan is deterministic — every row in ascending order first, then every column, digits ascending — and the
 *     first configuration that actually removes a candidate is returned.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Pointing
 * @see Technique#CLAIMING
 */
public final class Claiming implements TechniqueStrategy {
	
	/**
	 * Constructs the claiming strategy. The strategy is stateless and holds no grid.
	 */
	public Claiming() {}
	
	@Override
	public Technique technique() {
		return Technique.CLAIMING;
	}
	
	/**
	 * Scans every row and column for a digit whose candidates are confined to a single region and returns the first
	 * elimination it can make inside that region off the line.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first claiming elimination, or empty if the pattern makes no progress anywhere
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		for (int row = 0; row < grid.n(); row++) {
			Optional<Deduction> found = this.scanLine(grid, grid.rowCells(row));
			if (found.isPresent()) {
				return found;
			}
		}
		
		for (int column = 0; column < grid.n(); column++) {
			Optional<Deduction> found = this.scanLine(grid, grid.columnCells(column));
			if (found.isPresent()) {
				return found;
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Tests a single line for every digit and returns the first region-confined elimination.
	 *
	 * @param grid The working grid
	 * @param lineCells The ascending cell indices of the line
	 * @return The first elimination on this line, or empty
	 */
	private Optional<Deduction> scanLine(CandidateGrid grid, int[] lineCells) {
		for (int digit = 1; digit <= grid.n(); digit++) {
			int seen = 0;
			int region = -1;
			boolean sameRegion = true;
			for (int cell : lineCells) {
				if (!grid.hasCandidate(cell, digit)) {
					continue;
				}
				
				if (seen == 0) {
					region = grid.regionOf(cell);
				} else {
					sameRegion &= grid.regionOf(cell) == region;
				}
				seen++;
			}
			
			if (seen < 2 || !sameRegion) {
				continue;
			}
			
			EliminationBuilder builder = new EliminationBuilder();
			for (int cell : grid.regionCells(region)) {
				if (!this.inLine(lineCells, cell)) {
					builder.add(grid, cell, digit);
				}
			}
			
			Optional<Deduction> found = builder.build(Technique.CLAIMING);
			if (found.isPresent()) {
				return found;
			}
		}
		return Optional.empty();
	}
	
	private boolean inLine(int[] lineCells, int cell) {
		for (int lineCell : lineCells) {
			if (lineCell == cell) {
				return true;
			}
		}
		return false;
	}
}
