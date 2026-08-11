package net.luis.sudoku.solver;

import java.util.List;
import java.util.Optional;

/**
 * The XYZ-Wing: a three-candidate pivot with two bi-value wings eliminates the shared digit from every cell the pivot
 * and both wings see.
 * <p>
 *     The pivot holds {@code {x, y, z}} and its two peer wings hold {@code {x, z}} and {@code {y, z}}. One of the
 *     three cells must take {@code z}: if the pivot takes {@code x} the first wing is not forced, but then the second
 *     wing must be {@code y} or {@code z}, and running the three cases through shows {@code z} always lands somewhere
 *     in the trio. The elimination is therefore narrower than the {@link XyWing}'s — a victim must see all three
 *     cells, not just the two wings — which is exactly why the pattern is harder to use.
 * </p>
 * <p>
 *     The scan is deterministic — pivot cells ascending, then wing cells ascending among the pivot's peers — and the
 *     first configuration that removes at least one candidate is returned.
 * </p>
 *
 * @see TechniqueStrategy
 * @see XyWing
 * @see Technique#XYZ_WING
 */
public final class XyzWing implements TechniqueStrategy {
	
	/**
	 * Constructs the XYZ-Wing strategy. The strategy is stateless and holds no grid.
	 */
	public XyzWing() {}
	
	@Override
	public Technique technique() {
		return Technique.XYZ_WING;
	}
	
	/**
	 * Scans every three-candidate pivot for a matching pair of bi-value wings.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first XYZ-Wing elimination, or empty if the pattern makes no progress anywhere
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		return this.scan(grid, null);
	}
	
	/**
	 * Explains the wing by showing the three-candidate pivot, its two wings, and the digit all three share, which is
	 * the one that has to land somewhere in the trio.
	 *
	 * @param grid The working grid; never mutated
	 * @return The eliminations and their explanation, or empty if the pattern makes no progress anywhere
	 */
	@Override
	public Optional<ExplainedDeduction> findExplained(CandidateGrid grid) {
		Explanation.Builder builder = Explanation.builder(Technique.XYZ_WING);
		return this.scan(grid, builder).map(deduction -> new ExplainedDeduction(deduction, builder.conclusion(deduction).build()));
	}
	
	/**
	 * Runs the scan, optionally recording the pattern it found.
	 *
	 * @param grid The working grid
	 * @param explanation The explanation to record into, or null to skip recording entirely
	 * @return The first XYZ-Wing elimination, or empty
	 */
	private Optional<Deduction> scan(CandidateGrid grid, Explanation.Builder explanation) {
		for (int pivot = 0; pivot < grid.cellCount(); pivot++) {
			if (grid.candidateCount(pivot) != 3) {
				continue;
			}
			
			int pivotMask = grid.candidates(pivot);
			int[] peers = grid.peers(pivot);
			for (int i = 0; i < peers.length; i++) {
				int wingA = peers[i];
				int maskA = grid.candidates(wingA);
				if (Integer.bitCount(maskA) != 2 || (maskA | pivotMask) != pivotMask) {
					continue;
				}
				
				for (int j = i + 1; j < peers.length; j++) {
					int wingB = peers[j];
					int maskB = grid.candidates(wingB);
					if (Integer.bitCount(maskB) != 2 || (maskB | pivotMask) != pivotMask || maskB == maskA) {
						continue;
					}
					
					// The two wings overlap in exactly the digit that is forced somewhere in the trio.
					int shared = maskA & maskB;
					if (Integer.bitCount(shared) != 1 || (maskA | maskB) != pivotMask) {
						continue;
					}
					
					Optional<Deduction> found = this.eliminate(grid, pivot, wingA, wingB, Integer.numberOfTrailingZeros(shared), explanation);
					if (found.isPresent()) {
						return found;
					}
				}
			}
		}
		return Optional.empty();
	}
	
	private Optional<Deduction> eliminate(CandidateGrid grid, int pivot, int wingA, int wingB, int digit, Explanation.Builder explanation) {
		EliminationBuilder builder = new EliminationBuilder();
		for (int cell = 0; cell < grid.cellCount(); cell++) {
			if (cell == pivot || cell == wingA || cell == wingB) {
				continue;
			}
			
			if (grid.peers(cell, pivot) && grid.peers(cell, wingA) && grid.peers(cell, wingB)) {
				builder.add(grid, cell, digit);
			}
		}
		Optional<Deduction> deduction = builder.build(Technique.XYZ_WING);
		if (deduction.isPresent() && explanation != null) {
			explanation.pattern(0, List.of(new PatternCell(pivot, CellRole.PIVOT, grid.candidates(pivot))))
				.link(0, List.of(new PatternCell(wingA, CellRole.WING, grid.candidates(wingA)), new PatternCell(wingB, CellRole.WING, grid.candidates(wingB))))
				.implication(digit, List.of(PatternCell.of(pivot, CellRole.PIVOT, digit), PatternCell.of(wingA, CellRole.WING, digit), PatternCell.of(wingB, CellRole.WING, digit)));
		}
		return deduction;
	}
}
