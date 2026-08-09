package net.luis.sudoku.solver;

import java.util.Optional;

/**
 * The pointing half of locked candidates: a digit whose candidates within a region all lie on one line must be placed
 * on that line, so it can be removed from the rest of the line outside the region.
 * <p>
 *     The region constrains the line. The digit has to go somewhere in the region, every place it could go is on the
 *     same row or column, and therefore that row or column carries the digit inside the region — nowhere else on the
 *     line can hold it. Two or three candidate cells are the usual counts, but the argument holds for any number
 *     above one.
 * </p>
 * <p>
 *     The scan is deterministic — regions ascending, digits ascending, the row form before the column form — and the
 *     first configuration that actually removes a candidate is returned.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Claiming
 * @see Technique#POINTING
 */
public final class Pointing implements TechniqueStrategy {
	
	/**
	 * Constructs the pointing strategy. The strategy is stateless and holds no grid.
	 */
	public Pointing() {}
	
	@Override
	public Technique technique() {
		return Technique.POINTING;
	}
	
	/**
	 * Scans every region for a digit whose candidates are confined to a single row or column and returns the first
	 * elimination it can make outside the region on that line.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first pointing elimination, or empty if the pattern makes no progress anywhere
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		for (int region = 0; region < grid.partition().regionCount(); region++) {
			int[] regionCells = grid.regionCells(region);
			for (int digit = 1; digit <= grid.n(); digit++) {
				int seen = 0;
				int row = -1;
				int column = -1;
				boolean sameRow = true;
				boolean sameColumn = true;
				for (int cell : regionCells) {
					if (!grid.hasCandidate(cell, digit)) {
						continue;
					}
					
					if (seen == 0) {
						row = grid.rowOf(cell);
						column = grid.columnOf(cell);
					} else {
						sameRow &= grid.rowOf(cell) == row;
						sameColumn &= grid.columnOf(cell) == column;
					}
					seen++;
				}
				
				if (seen < 2) {
					continue;
				}
				
				if (sameRow) {
					Optional<Deduction> found = this.eliminate(grid, grid.rowCells(row), region, digit);
					if (found.isPresent()) {
						return found;
					}
				}
				if (sameColumn) {
					Optional<Deduction> found = this.eliminate(grid, grid.columnCells(column), region, digit);
					if (found.isPresent()) {
						return found;
					}
				}
			}
		}
		return Optional.empty();
	}
	
	private Optional<Deduction> eliminate(CandidateGrid grid, int[] lineCells, int region, int digit) {
		EliminationBuilder builder = new EliminationBuilder();
		for (int cell : lineCells) {
			if (grid.regionOf(cell) != region) {
				builder.add(grid, cell, digit);
			}
		}
		return builder.build(Technique.POINTING);
	}
}
