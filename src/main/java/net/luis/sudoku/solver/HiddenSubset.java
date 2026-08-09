package net.luis.sudoku.solver;

import java.util.Optional;

/**
 * The shared hidden-subset scan: {@code k} digits of a unit confined to the same {@code k} cells own those cells
 * outright, so every other candidate can be removed from them.
 * <p>
 *     This is the dual of {@link NakedSubset} — it constrains the same {@code k} cells by counting digits rather than
 *     candidates — and it is consistently harder for a player to see, because the subset is hidden behind the extra
 *     candidates the technique goes on to remove. Pairs, triples and quads share this one scan, parameterized by
 *     {@code k}.
 * </p>
 * <p>
 *     A digit takes part only if it has at least two candidate cells in the unit: a digit with one is a hidden single
 *     and is handled several ranks lower. The search is deterministic — units in {@link CandidateGrid#allUnits()}
 *     order, digit combinations ascending — and returns the first subset that actually removes a candidate.
 * </p>
 *
 * @see HiddenPair
 * @see HiddenTriple
 * @see HiddenQuad
 */
abstract sealed class HiddenSubset implements TechniqueStrategy permits HiddenPair, HiddenTriple, HiddenQuad {
	
	private final Technique technique;
	private final int subsetSize;
	
	/**
	 * Constructs a hidden-subset strategy of the given size.
	 *
	 * @param technique The technique to attribute the eliminations to
	 * @param subsetSize The number of digits and cells the subset spans
	 */
	HiddenSubset(Technique technique, int subsetSize) {
		this.technique = technique;
		this.subsetSize = subsetSize;
	}
	
	@Override
	public final Technique technique() {
		return this.technique;
	}
	
	/**
	 * Scans every unit for {@code k} digits confined to exactly {@code k} cells and returns the first elimination it
	 * enables inside those cells.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first hidden-subset elimination, or empty if the pattern makes no progress anywhere
	 */
	@Override
	public final Optional<Deduction> find(CandidateGrid grid) {
		for (int[] unit : grid.allUnits()) {
			int[] positions = new int[grid.n() + 1];
			for (int digit = 1; digit <= grid.n(); digit++) {
				positions[digit] = this.positionsOf(grid, unit, digit);
			}
			
			Optional<Deduction> found = this.search(grid, unit, positions, 0, 1, 0, 0);
			if (found.isPresent()) {
				return found;
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Extends the current combination of digits by one and recurses, testing a complete combination for the subset
	 * property.
	 *
	 * @param grid The working grid
	 * @param unit The unit being scanned
	 * @param positions For each digit, the bitmask of unit positions it is a candidate in
	 * @param depth How many digits have been picked
	 * @param start The first digit this level may pick, which keeps combinations ascending
	 * @param digitMask The digits picked so far
	 * @param union The union of the picked digits' position masks
	 * @return The first elimination found below this node, or empty
	 */
	private Optional<Deduction> search(CandidateGrid grid, int[] unit, int[] positions, int depth, int start, int digitMask, int union) {
		if (depth == this.subsetSize) {
			return Integer.bitCount(union) == this.subsetSize ? this.eliminate(grid, unit, union, digitMask) : Optional.empty();
		}
		
		// Prune once the cells the digits occupy already outnumber the subset; more digits only add cells.
		if (Integer.bitCount(union) > this.subsetSize) {
			return Optional.empty();
		}
		
		for (int digit = start; digit <= grid.n() - (this.subsetSize - depth - 1); digit++) {
			if (Integer.bitCount(positions[digit]) < 2) {
				continue;
			}
			
			Optional<Deduction> found = this.search(grid, unit, positions, depth + 1, digit + 1, digitMask | (1 << digit), union | positions[digit]);
			if (found.isPresent()) {
				return found;
			}
		}
		return Optional.empty();
	}
	
	private int positionsOf(CandidateGrid grid, int[] unit, int digit) {
		int mask = 0;
		for (int position = 0; position < unit.length; position++) {
			if (grid.hasCandidate(unit[position], digit)) {
				mask |= 1 << position;
			}
		}
		return mask;
	}
	
	/**
	 * Removes every digit outside the subset from the cells the subset occupies.
	 *
	 * @param grid The working grid
	 * @param unit The unit the subset lives in
	 * @param positionMask The unit positions the subset occupies
	 * @param digitMask The digits the subset consists of, which are the ones to keep
	 * @return The eliminations, or empty if the subset removes nothing
	 */
	private Optional<Deduction> eliminate(CandidateGrid grid, int[] unit, int positionMask, int digitMask) {
		EliminationBuilder builder = new EliminationBuilder();
		int remaining = positionMask;
		while (remaining != 0) {
			int position = Integer.numberOfTrailingZeros(remaining);
			remaining &= remaining - 1;
			builder.addAll(grid, unit[position], ~digitMask);
		}
		return builder.build(this.technique);
	}
}
