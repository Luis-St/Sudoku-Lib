package net.luis.sudoku.solver;

import java.util.Optional;

/**
 * The XY-Wing: a bi-value pivot with two bi-value wings eliminates the shared third digit from every cell both wings
 * see.
 * <p>
 *     The pivot holds candidates {@code {x, y}}. One wing is a peer of the pivot holding {@code {x, z}}, the other a
 *     peer holding {@code {y, z}}. Whichever digit the pivot takes, one of the wings is forced to {@code z}, so any
 *     cell seen by <b>both</b> wings cannot hold {@code z}.
 * </p>
 * <p>
 *     The scan is deterministic — pivot cells ascending, then wing cells ascending among the pivot's peers — and the
 *     first configuration that removes at least one candidate is returned.
 * </p>
 *
 * @see TechniqueStrategy
 * @see XyzWing
 * @see Technique#XY_WING
 */
public final class XyWing implements TechniqueStrategy {
	
	/**
	 * Constructs the XY-Wing strategy. The strategy is stateless and holds no grid.
	 */
	public XyWing() {}
	
	@Override
	public Technique technique() {
		return Technique.XY_WING;
	}
	
	/**
	 * Scans every bi-value pivot for a matching pair of bi-value wings and returns the first elimination of the shared
	 * third digit from the cells both wings see.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first XY-Wing elimination, or empty if the pattern makes no progress anywhere
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		for (int pivot = 0; pivot < grid.cellCount(); pivot++) {
			if (grid.candidateCount(pivot) != 2) {
				continue;
			}
			
			int pivotMask = grid.candidates(pivot);
			int[] peers = grid.peers(pivot);
			for (int wingA : peers) {
				if (grid.candidateCount(wingA) != 2) {
					continue;
				}
				
				int maskA = grid.candidates(wingA);
				int shared = maskA & pivotMask;
				if (Integer.bitCount(shared) != 1) {
					continue;
				}
				
				int z = maskA & ~pivotMask;
				int wantB = (pivotMask & ~shared) | z;
				for (int wingB : peers) {
					if (wingB == wingA || grid.candidates(wingB) != wantB) {
						continue;
					}
					
					Optional<Deduction> found = this.eliminate(grid, pivot, wingA, wingB, Integer.numberOfTrailingZeros(z));
					if (found.isPresent()) {
						return found;
					}
				}
			}
		}
		return Optional.empty();
	}
	
	private Optional<Deduction> eliminate(CandidateGrid grid, int pivot, int wingA, int wingB, int digit) {
		EliminationBuilder builder = new EliminationBuilder();
		for (int cell = 0; cell < grid.cellCount(); cell++) {
			if (cell == pivot || cell == wingA || cell == wingB) {
				continue;
			}
			
			if (grid.peers(cell, wingA) && grid.peers(cell, wingB)) {
				builder.add(grid, cell, digit);
			}
		}
		return builder.build(Technique.XY_WING);
	}
}
