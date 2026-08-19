package net.luis.sudoku.solver;

import java.util.*;

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
		return this.search(grid, group, 0, 0, 0, null);
	}
	
	/**
	 * Explains the wing by showing the four cells and the four digits they span, and naming the one digit that is not
	 * used up by the others, which one of the cells therefore has to hold.
	 *
	 * @param grid The working grid; never mutated
	 * @return The eliminations and their explanation, or empty if the pattern makes no progress anywhere
	 */
	@Override
	public Optional<ExplainedDeduction> findExplained(CandidateGrid grid) {
		Explanation.Builder builder = Explanation.builder(Technique.WXYZ_WING);
		int[] group = new int[4];
		return this.search(grid, group, 0, 0, 0, builder).map(deduction -> new ExplainedDeduction(deduction, builder.conclusion(deduction).build()));
	}
	
	private Optional<Deduction> search(CandidateGrid grid, int[] group, int depth, int start, int union, Explanation.Builder explanation) {
		if (depth == 4) {
			return Integer.bitCount(union) == 4 ? this.test(grid, group, union, explanation) : Optional.empty();
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
			Optional<Deduction> found = this.search(grid, group, depth + 1, cell + 1, union | grid.candidates(cell), explanation);
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
	private Optional<Deduction> test(CandidateGrid grid, int[] group, int union, Explanation.Builder explanation) {
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
		return this.eliminate(grid, group, unrestricted, explanation);
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
	
	private Optional<Deduction> eliminate(CandidateGrid grid, int[] group, int digit, Explanation.Builder explanation) {
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
		Optional<Deduction> deduction = builder.build(Technique.WXYZ_WING);
		// Only a group that removes something is the deduction being returned, so only that one is worth explaining:
		// any earlier one was looked at and rejected.
		if (deduction.isPresent() && explanation != null) {
			this.explain(grid, group, digit, explanation);
		}
		return deduction;
	}
	
	/**
	 * Records the pattern: the four cells with the digits they span, and the cells among them that could hold the
	 * unrestricted digit, one of which has to.
	 *
	 * @param grid The working grid
	 * @param group The four cells
	 * @param digit The unrestricted digit
	 * @param explanation The explanation to record into
	 */
	private void explain(CandidateGrid grid, int[] group, int digit, Explanation.Builder explanation) {
		List<PatternCell> cells = new ArrayList<>(group.length);
		List<PatternCell> holders = new ArrayList<>();
		for (int member : group) {
			boolean holds = grid.hasCandidate(member, digit);
			cells.add(new PatternCell(member, holds ? CellRole.WING : CellRole.PATTERN, grid.candidates(member)));
			if (holds) {
				holders.add(PatternCell.of(member, CellRole.LINK_ON, digit));
			}
		}
		
		explanation.pattern(0, cells)
			.focusDigit(digit)
			// The other three digits can be used once each across the group, so they cannot fill four cells on their
			// own: one of the cells listing this digit has to hold it.
			.implication(digit, holders);
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
