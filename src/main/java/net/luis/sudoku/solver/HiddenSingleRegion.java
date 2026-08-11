package net.luis.sudoku.solver;

import java.util.Optional;

/**
 * The hidden-single technique restricted to regions: within a region, a digit that is a candidate of exactly one cell
 * must be placed there.
 * <p>
 *     This is cross-hatching, the first technique a player learns after the singles, and it is deliberately rated
 *     easier than the same argument along a line ({@link HiddenSingleLine}): a region is a compact block, so the rows
 *     and columns striking through it are visible at a glance, while scanning a full line for a missing digit takes
 *     real effort. Splitting the two is what makes difficulty levels 2 and 3 distinguishable.
 * </p>
 * <p>
 *     The scan is deterministic — regions in ascending index order, digits ascending — and the first digit confined to
 *     a single cell of a region is placed.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Technique#HIDDEN_SINGLE_REGION
 */
public final class HiddenSingleRegion implements TechniqueStrategy {
	
	/**
	 * Constructs the region hidden-single strategy. The strategy is stateless and holds no grid.
	 */
	public HiddenSingleRegion() {}
	
	@Override
	public Technique technique() {
		return Technique.HIDDEN_SINGLE_REGION;
	}
	
	/**
	 * Scans every region for a digit that is a candidate of exactly one of its cells and returns that forced placement.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first region hidden single, or empty if no digit is confined to a single cell of any region
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		return HiddenSingles.scan(grid, grid.regions(), Technique.HIDDEN_SINGLE_REGION);
	}
	
	/**
	 * Explains the single as cross-hatching: the region, and the digit already placed in the row or column of every
	 * other empty cell of it.
	 *
	 * @param grid The working grid; never mutated
	 * @return The placement and its explanation, or empty if there is none
	 */
	@Override
	public Optional<ExplainedDeduction> findExplained(CandidateGrid grid) {
		return HiddenSingles.scanExplained(grid, grid.regions(), Technique.HIDDEN_SINGLE_REGION);
	}
}
