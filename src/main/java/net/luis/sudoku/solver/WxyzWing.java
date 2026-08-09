package net.luis.sudoku.solver;

import java.util.Optional;

/**
 * The WXYZ-Wing: four cells spanning exactly four candidates, all but one of which are locked across the group.
 * <p>
 *     Call the four digits {@code w, x, y, z}. Three of them are <i>restricted</i>: every pair of the four cells that
 *     could hold such a digit sees each other, so the digit can be used at most once across the group. Suppose no cell
 *     of the group held {@code z}. Then four cells would have to be filled from the three restricted digits, each
 *     usable once — which is impossible. So one of the cells holds {@code z}, and any cell seeing every group cell
 *     that lists {@code z} cannot hold it.
 * </p>
 * <p>
 *     This is the four-cell generalization of the {@link XyzWing}, and the first technique in the solver that argues
 *     about an almost-locked set rather than a fixed geometric shape.
 * </p>
 * <p>
 *     The scan is deterministic — cell combinations in ascending order — and the first group that removes at least one
 *     candidate is returned.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Technique#WXYZ_WING
 */
public final class WxyzWing implements TechniqueStrategy {
	
	/**
	 * Constructs the WXYZ-Wing strategy. The strategy is stateless and holds no grid.
	 */
	public WxyzWing() {}
	
	@Override
	public Technique technique() {
		return Technique.WXYZ_WING;
	}
	
	/**
	 * Scans every combination of four cells spanning four candidates for the wing property.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first WXYZ-Wing elimination, or empty if the pattern makes no progress anywhere
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		int[] group = new int[4];
		return this.search(grid, group, 0, 0, 0);
	}
	
	private Optional<Deduction> search(CandidateGrid grid, int[] group, int depth, int start, int union) {
		if (depth == 4) {
			return Integer.bitCount(union) == 4 ? this.test(grid, group, union) : Optional.empty();
		}
		if (Integer.bitCount(union) > 4) {
			return Optional.empty();
		}
		
		for (int cell = start; cell <= grid.cellCount() - (4 - depth); cell++) {
			int count = grid.candidateCount(cell);
			if (count < 2 || count > 4) {
				continue;
			}
			// The group has to hang together: every cell but the first must see one already picked, which keeps
			// the search from wandering across four unrelated corners of the grid.
			if (depth > 0 && !this.touches(grid, group, depth, cell)) {
				continue;
			}
			
			group[depth] = cell;
			Optional<Deduction> found = this.search(grid, group, depth + 1, cell + 1, union | grid.candidates(cell));
			if (found.isPresent()) {
				return found;
			}
		}
		return Optional.empty();
	}
	
	private boolean touches(CandidateGrid grid, int[] group, int depth, int cell) {
		for (int index = 0; index < depth; index++) {
			if (grid.peers(group[index], cell)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Tests a complete group: exactly one of its four digits may be unrestricted, and that digit is the one the wing
	 * eliminates.
	 */
	private Optional<Deduction> test(CandidateGrid grid, int[] group, int union) {
		int unrestricted = 0;
		int remaining = union;
		while (remaining != 0) {
			int digit = Integer.numberOfTrailingZeros(remaining);
			remaining &= remaining - 1;
			if (!this.isRestricted(grid, group, digit)) {
				if (unrestricted != 0) {
					return Optional.empty();
				}
				unrestricted = digit;
			}
		}
		
		if (unrestricted == 0) {
			return Optional.empty();
		}
		return this.eliminate(grid, group, unrestricted);
	}
	
	/**
	 * Checks whether a digit can be used at most once across the group, which holds when every pair of group cells
	 * listing it sees each other.
	 */
	private boolean isRestricted(CandidateGrid grid, int[] group, int digit) {
		for (int i = 0; i < group.length; i++) {
			if (!grid.hasCandidate(group[i], digit)) {
				continue;
			}
			
			for (int j = i + 1; j < group.length; j++) {
				if (grid.hasCandidate(group[j], digit) && !grid.peers(group[i], group[j])) {
					return false;
				}
			}
		}
		return true;
	}
	
	private Optional<Deduction> eliminate(CandidateGrid grid, int[] group, int digit) {
		EliminationBuilder builder = new EliminationBuilder();
		for (int cell = 0; cell < grid.cellCount(); cell++) {
			if (this.inGroup(group, cell)) {
				continue;
			}
			
			boolean seesAll = true;
			for (int member : group) {
				if (grid.hasCandidate(member, digit) && !grid.peers(cell, member)) {
					seesAll = false;
					break;
				}
			}
			
			if (seesAll) {
				builder.add(grid, cell, digit);
			}
		}
		return builder.build(Technique.WXYZ_WING);
	}
	
	private boolean inGroup(int[] group, int cell) {
		for (int member : group) {
			if (member == cell) {
				return true;
			}
		}
		return false;
	}
}
