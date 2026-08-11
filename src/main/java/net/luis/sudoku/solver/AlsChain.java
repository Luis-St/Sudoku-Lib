package net.luis.sudoku.solver;

import java.util.List;
import java.util.Optional;

/**
 * The ALS chain: three almost-locked sets joined by restricted common digits, eliminating on a digit the two ends
 * share.
 * <p>
 *     Each junction works exactly as in the {@link AlsXz}: a restricted common digit that only one of the two sets it
 *     joins can use. Threaded through a middle set, the argument reaches from the first set to the last, and any digit
 *     both ends list is certainly used by one of them. No cell outside seeing every occurrence of that digit in both
 *     end sets can hold it.
 * </p>
 * <p>
 *     The two junction digits must differ, or the middle set would be asked to give the same digit away twice. Chains
 *     of exactly three sets are searched: they are what a player can still follow, and each further set multiplies the
 *     search cost.
 * </p>
 *
 * @see TechniqueStrategy
 * @see AlsXz
 * @see Technique#ALS_CHAIN
 */
public final class AlsChain implements TechniqueStrategy {
	
	/**
	 * Constructs the ALS-chain strategy. The strategy is stateless and holds no grid.
	 */
	public AlsChain() {}
	
	@Override
	public Technique technique() {
		return Technique.ALS_CHAIN;
	}
	
	/**
	 * Scans every ordered triple of almost-locked sets joined by two distinct restricted common digits.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first ALS-chain elimination, or empty if no chain makes progress
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		return this.scan(grid, null);
	}

	/**
	 * Explains the chain by showing the three sets, the two junction digits that each pass the argument along, and the
	 * digit the two ends share and one of them therefore holds.
	 *
	 * @param grid The working grid; never mutated
	 * @return The eliminations and their explanation, or empty if no chain makes progress
	 */
	@Override
	public Optional<ExplainedDeduction> findExplained(CandidateGrid grid) {
		Explanation.Builder builder = Explanation.builder(Technique.ALS_CHAIN);
		return this.scan(grid, builder).map(deduction -> new ExplainedDeduction(deduction, builder.conclusion(deduction).build()));
	}

	private Optional<Deduction> scan(CandidateGrid grid, Explanation.Builder explanation) {
		List<AlmostLockedSets.Als> sets = AlmostLockedSets.of(grid);
		for (AlmostLockedSets.Als first : sets) {
			for (AlmostLockedSets.Als middle : sets) {
				if (first.overlaps(middle)) {
					continue;
				}
				
				int leftJunction = AlmostLockedSets.restrictedCommons(grid, first, middle);
				if (leftJunction == 0) {
					continue;
				}
				
				for (AlmostLockedSets.Als last : sets) {
					if (last.overlaps(middle) || last.overlaps(first)) {
						continue;
					}
					
					int rightJunction = AlmostLockedSets.restrictedCommons(grid, middle, last);
					// The middle set can only give one digit away per junction, so the two must differ.
					if (rightJunction == 0 || Integer.bitCount(leftJunction | rightJunction) < 2) {
						continue;
					}
					
					Optional<Deduction> found = this.eliminate(grid, first, middle, last, leftJunction, rightJunction, explanation);
					if (found.isPresent()) {
						return found;
					}
				}
			}
		}
		return Optional.empty();
	}
	
	private Optional<Deduction> eliminate(CandidateGrid grid, AlmostLockedSets.Als first, AlmostLockedSets.Als middle, AlmostLockedSets.Als last, int leftJunction, int rightJunction, Explanation.Builder explanation) {
		int shared = first.mask() & last.mask() & ~(leftJunction | rightJunction);
		while (shared != 0) {
			int digit = Integer.numberOfTrailingZeros(shared);
			shared &= shared - 1;
			
			Optional<Deduction> found = AlmostLockedSets.eliminateSeenBy(grid, digit, Technique.ALS_CHAIN, first, last);
			// Only a chain that removes something is the deduction being returned, so only that one is worth
			// explaining: any earlier one was looked at and rejected.
			if (found.isPresent()) {
				if (explanation != null) {
					this.explain(grid, first, middle, last, leftJunction, rightJunction, digit, explanation);
				}
				return found;
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Records the pattern: the three sets in chain order, each junction digit with the places it occurs across the two
	 * sets it joins, and the shared digit one of the two ends is left holding.
	 *
	 * @param grid The working grid
	 * @param first The set the chain starts at
	 * @param middle The set threading the two junctions together
	 * @param last The set the chain ends at
	 * @param leftJunction The restricted commons of the first junction
	 * @param rightJunction The restricted commons of the second
	 * @param digit The shared digit being eliminated elsewhere
	 * @param explanation The explanation to record into
	 */
	private void explain(CandidateGrid grid, AlmostLockedSets.Als first, AlmostLockedSets.Als middle, AlmostLockedSets.Als last, int leftJunction, int rightJunction, int digit, Explanation.Builder explanation) {
		// The middle set can only give one digit away per junction, so the two junctions have to be shown on different
		// digits. Either junction may be the one with a digit to spare, which is why both orders are tried: the
		// search only guarantees that two distinct digits exist across the pair, not which side holds the choice.
		int left = Integer.numberOfTrailingZeros(leftJunction);
		int right;
		if ((rightJunction & ~(1 << left)) != 0) {
			right = Integer.numberOfTrailingZeros(rightJunction & ~(1 << left));
		} else {
			right = Integer.numberOfTrailingZeros(rightJunction);
			left = Integer.numberOfTrailingZeros(leftJunction & ~(1 << right));
		}
		
		explanation.pattern(0, AlmostLockedSets.cellsOf(grid, first, CellRole.BASE))
			.pattern(0, AlmostLockedSets.cellsOf(grid, middle, CellRole.PATTERN))
			.pattern(0, AlmostLockedSets.cellsOf(grid, last, CellRole.COVER))
			.link(left, AlmostLockedSets.occurrencesOf(grid, left, CellRole.CONTEXT, first, middle))
			.link(right, AlmostLockedSets.occurrencesOf(grid, right, CellRole.CONTEXT, middle, last))
			.implication(digit, AlmostLockedSets.occurrencesOf(grid, digit, CellRole.LINK_ON, first, last));
	}
}
