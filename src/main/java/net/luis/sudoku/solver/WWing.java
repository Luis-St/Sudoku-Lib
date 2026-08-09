package net.luis.sudoku.solver;

import java.util.Optional;

/**
 * The W-Wing: two cells with the identical two-candidate set, joined by a conjugate pair of one of their digits.
 * <p>
 *     Both cells hold {@code {a, b}} and see no common unit themselves. Somewhere else in the grid, a unit confines
 *     {@code a} to two cells, one seeing the first bi-value cell and the other seeing the second. Exactly one of those
 *     two cells holds {@code a}, and whichever it is drives the bi-value cell it sees off {@code a} and onto
 *     {@code b}. One of the two bi-value cells is therefore {@code b}, so no cell seeing both of them can be.
 * </p>
 * <p>
 *     The scan is deterministic — bi-value cell pairs ascending, then conjugate pairs in the order
 *     {@link ConjugateLinks} reports them — and the first configuration that removes at least one candidate is
 *     returned.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Technique#W_WING
 */
public final class WWing implements TechniqueStrategy {
	
	/**
	 * Constructs the W-Wing strategy. The strategy is stateless and holds no grid.
	 */
	public WWing() {}
	
	@Override
	public Technique technique() {
		return Technique.W_WING;
	}
	
	/**
	 * Scans every pair of identical bi-value cells for a conjugate pair linking them.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first W-Wing elimination, or empty if the pattern makes no progress anywhere
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		for (int first = 0; first < grid.cellCount(); first++) {
			int mask = grid.candidates(first);
			if (Integer.bitCount(mask) != 2) {
				continue;
			}
			
			for (int second = first + 1; second < grid.cellCount(); second++) {
				// Two cells that already see each other are a naked pair, which is handled far below this rank.
				if (grid.candidates(second) != mask || grid.peers(first, second)) {
					continue;
				}
				
				Optional<Deduction> found = this.scanLinks(grid, first, second, mask);
				if (found.isPresent()) {
					return found;
				}
			}
		}
		return Optional.empty();
	}
	
	private Optional<Deduction> scanLinks(CandidateGrid grid, int first, int second, int mask) {
		int digitA = Integer.numberOfTrailingZeros(mask);
		int digitB = Integer.numberOfTrailingZeros(mask & (mask - 1));
		for (int linked : new int[] { digitA, digitB }) {
			int eliminated = linked == digitA ? digitB : digitA;
			for (int[] link : ConjugateLinks.of(grid, linked)) {
				if (link[0] == first || link[0] == second || link[1] == first || link[1] == second) {
					continue;
				}
				
				boolean straight = grid.peers(link[0], first) && grid.peers(link[1], second);
				boolean crossed = grid.peers(link[0], second) && grid.peers(link[1], first);
				if (!straight && !crossed) {
					continue;
				}
				
				Optional<Deduction> found = ConjugateLinks.eliminateSeenByBoth(grid, eliminated, first, second, Technique.W_WING, link[0], link[1]);
				if (found.isPresent()) {
					return found;
				}
			}
		}
		return Optional.empty();
	}
}
