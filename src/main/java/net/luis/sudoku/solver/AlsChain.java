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
					
					Optional<Deduction> found = this.eliminate(grid, first, last, leftJunction | rightJunction);
					if (found.isPresent()) {
						return found;
					}
				}
			}
		}
		return Optional.empty();
	}
	
	private Optional<Deduction> eliminate(CandidateGrid grid, AlmostLockedSets.Als first, AlmostLockedSets.Als last, int junctions) {
		int shared = first.mask() & last.mask() & ~junctions;
		while (shared != 0) {
			int digit = Integer.numberOfTrailingZeros(shared);
			shared &= shared - 1;
			
			Optional<Deduction> found = AlmostLockedSets.eliminateSeenBy(grid, digit, Technique.ALS_CHAIN, first, last);
			if (found.isPresent()) {
				return found;
			}
		}
		return Optional.empty();
	}
}
