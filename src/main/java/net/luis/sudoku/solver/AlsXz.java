package net.luis.sudoku.solver;

import java.util.List;
import java.util.Optional;

/**
 * The ALS-XZ: two almost-locked sets sharing a restricted common digit eliminate any second common digit.
 * <p>
 *     Call the restricted common {@code x}. Only one of the two sets can use it, so the other is left holding all of
 *     its digits in all of its cells — a genuine locked set. Whichever way round that falls, every other digit
 *     {@code z} the two sets share is certainly used inside one of them, so no cell outside that sees every
 *     occurrence of {@code z} in both sets can hold it.
 * </p>
 * <p>
 *     This is the first technique in the solver whose building block is a set rather than a cell or a line, and it
 *     subsumes a good deal of what the wings do — the XY-Wing, for instance, is an ALS-XZ between two bi-value cells.
 * </p>
 *
 * @see TechniqueStrategy
 * @see AlmostLockedSets
 * @see Technique#ALS_XZ
 */
public final class AlsXz implements TechniqueStrategy {
	
	/**
	 * Constructs the ALS-XZ strategy. The strategy is stateless and holds no grid.
	 */
	public AlsXz() {}
	
	@Override
	public Technique technique() {
		return Technique.ALS_XZ;
	}
	
	/**
	 * Scans every pair of almost-locked sets for a restricted common digit and a second shared digit to eliminate.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first ALS-XZ elimination, or empty if no pair makes progress
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		return this.scan(grid, null);
	}
	
	/**
	 * Explains the pair by showing both sets with the digits they span, the restricted common only one of them can
	 * use, and the second shared digit that one of them therefore certainly holds.
	 *
	 * @param grid The working grid; never mutated
	 * @return The eliminations and their explanation, or empty if no pair makes progress
	 */
	@Override
	public Optional<ExplainedDeduction> findExplained(CandidateGrid grid) {
		Explanation.Builder builder = Explanation.builder(Technique.ALS_XZ);
		return this.scan(grid, builder).map(deduction -> new ExplainedDeduction(deduction, builder.conclusion(deduction).build()));
	}
	
	private Optional<Deduction> scan(CandidateGrid grid, Explanation.Builder explanation) {
		List<AlmostLockedSets.Als> sets = AlmostLockedSets.of(grid);
		for (int i = 0; i < sets.size(); i++) {
			AlmostLockedSets.Als first = sets.get(i);
			for (int j = i + 1; j < sets.size(); j++) {
				AlmostLockedSets.Als second = sets.get(j);
				if (first.overlaps(second)) {
					continue;
				}
				
				int restricted = AlmostLockedSets.restrictedCommons(grid, first, second);
				if (restricted == 0) {
					continue;
				}
				
				Optional<Deduction> found = this.eliminate(grid, first, second, restricted, explanation);
				if (found.isPresent()) {
					return found;
				}
			}
		}
		return Optional.empty();
	}
	
	private Optional<Deduction> eliminate(CandidateGrid grid, AlmostLockedSets.Als first, AlmostLockedSets.Als second, int restricted, Explanation.Builder explanation) {
		// Any shared digit other than the restricted common is certainly used by one of the two sets.
		int candidates = first.mask() & second.mask() & ~restricted;
		while (candidates != 0) {
			int digit = Integer.numberOfTrailingZeros(candidates);
			candidates &= candidates - 1;
			
			Optional<Deduction> found = AlmostLockedSets.eliminateSeenBy(grid, digit, Technique.ALS_XZ, first, second);
			// Only a pair that removes something is the deduction being returned, so only that one is worth
			// explaining: any earlier one was looked at and rejected.
			if (found.isPresent()) {
				if (explanation != null) {
					this.explain(grid, first, second, Integer.numberOfTrailingZeros(restricted), digit, explanation);
				}
				return found;
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Records the pattern: the two sets, the restricted common that only one of them can use, and the shared digit
	 * that one of them is therefore left holding.
	 *
	 * @param grid The working grid
	 * @param first The first set
	 * @param second The other set
	 * @param restricted The restricted common digit
	 * @param digit The shared digit being eliminated elsewhere
	 * @param explanation The explanation to record into
	 */
	private void explain(CandidateGrid grid, AlmostLockedSets.Als first, AlmostLockedSets.Als second, int restricted, int digit, Explanation.Builder explanation) {
		explanation.pattern(0, AlmostLockedSets.cellsOf(grid, first, CellRole.BASE))
			.pattern(0, AlmostLockedSets.cellsOf(grid, second, CellRole.COVER))
			// Every occurrence of the restricted common across the two sets sees every other, so at most one set uses
			// it - which leaves the other one locked.
			.focusDigit(restricted)
			.link(restricted, AlmostLockedSets.occurrencesOf(grid, restricted, CellRole.CONTEXT, first, second))
			.implication(digit, AlmostLockedSets.occurrencesOf(grid, digit, CellRole.LINK_ON, first, second));
	}
}
