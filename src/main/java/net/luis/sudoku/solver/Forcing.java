package net.luis.sudoku.solver;

import java.util.List;
import java.util.Optional;

/**
 * The shared machinery behind the {@link ForcingChain} and the {@link ForcingNet}.
 * <p>
 *     A forcing argument works by exhausting the possibilities. Take something that must be true in one of several
 *     ways — a cell, which must hold one of its candidates, or a digit in a unit, which must go in one of its
 *     places — and play out every one of those ways. If they all end up placing the same digit in the same cell, that
 *     placement holds no matter which way the original choice falls, and it can be made without ever deciding the
 *     choice itself.
 * </p>
 * <p>
 *     Branches that run into a contradiction are not treated as agreement: a contradictory branch is a refutation, and
 *     refutations belong to {@link Nishio} and the {@link DynamicContradictionChain}, which are ranked below these.
 *     Requiring every branch to survive keeps each technique responsible for its own kind of deduction, which is what
 *     makes the difficulty rating meaningful.
 * </p>
 */
final class Forcing {
	
	private Forcing() {}
	
	/**
	 * Runs a forcing argument over every cell and every unit and returns the first placement all branches agree on.
	 *
	 * @param grid The working grid; never mutated
	 * @param strategies The techniques each branch may use while propagating
	 * @param maxSteps How many deductions each branch may apply
	 * @param technique The technique to attribute the placement to
	 * @param unitBased True to also branch on where a digit goes within a unit, not only on what a cell holds
	 * @return The agreed placement, or empty if nothing is forced
	 */
	static Optional<Deduction> force(CandidateGrid grid, List<TechniqueStrategy> strategies, int maxSteps, Technique technique, boolean unitBased) {
		for (int cell = 0; cell < grid.cellCount(); cell++) {
			if (!grid.isEmpty(cell) || grid.candidateCount(cell) < 2) {
				continue;
			}
			
			int[] digits = grid.candidateDigits(cell);
			int[][] branches = new int[digits.length][];
			for (int index = 0; index < digits.length; index++) {
				Optional<CandidateGrid> outcome = Assumptions.assume(grid, cell, digits[index], strategies, maxSteps);
				if (outcome.isEmpty()) {
					branches = null;
					break;
				}
				branches[index] = outcome.orElseThrow().values();
			}
			
			Optional<Deduction> agreed = agreement(grid, branches, technique);
			if (agreed.isPresent()) {
				return agreed;
			}
		}
		
		if (!unitBased) {
			return Optional.empty();
		}
		return forceUnits(grid, strategies, maxSteps, technique);
	}
	
	/**
	 * Branches on where a digit goes inside a unit rather than on what a cell holds, which reaches conclusions the
	 * cell form does not.
	 */
	private static Optional<Deduction> forceUnits(CandidateGrid grid, List<TechniqueStrategy> strategies, int maxSteps, Technique technique) {
		for (int[] unit : grid.allUnits()) {
			for (int digit = 1; digit <= grid.n(); digit++) {
				int[] places = placesOf(grid, unit, digit);
				if (places.length < 2) {
					continue;
				}
				
				int[][] branches = new int[places.length][];
				for (int index = 0; index < places.length; index++) {
					Optional<CandidateGrid> outcome = Assumptions.assume(grid, places[index], digit, strategies, maxSteps);
					if (outcome.isEmpty()) {
						branches = null;
						break;
					}
					branches[index] = outcome.orElseThrow().values();
				}
				
				Optional<Deduction> agreed = agreement(grid, branches, technique);
				if (agreed.isPresent()) {
					return agreed;
				}
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Returns the first cell that was empty before the branching and holds the same digit in every branch.
	 *
	 * @param branches The resulting value arrays, or null if any branch was contradictory
	 */
	private static Optional<Deduction> agreement(CandidateGrid grid, int[][] branches, Technique technique) {
		if (branches == null) {
			return Optional.empty();
		}
		
		for (int cell = 0; cell < grid.cellCount(); cell++) {
			if (!grid.isEmpty(cell)) {
				continue;
			}
			
			int digit = branches[0][cell];
			if (digit == 0) {
				continue;
			}
			
			boolean agreed = true;
			for (int[] branch : branches) {
				agreed &= branch[cell] == digit;
			}
			
			if (agreed) {
				return Optional.of(new Deduction.Placement(technique, cell, digit));
			}
		}
		return Optional.empty();
	}
	
	private static int[] placesOf(CandidateGrid grid, int[] unit, int digit) {
		int[] places = new int[unit.length];
		int count = 0;
		for (int cell : unit) {
			if (grid.hasCandidate(cell, digit)) {
				places[count++] = cell;
			}
		}
		return java.util.Arrays.copyOf(places, count);
	}
}
