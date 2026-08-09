package net.luis.sudoku.solver;

import java.util.List;
import java.util.Optional;

/**
 * The shared trial-and-refute scan behind {@link Nishio} and the {@link DynamicContradictionChain}.
 * <p>
 *     Both techniques do the same thing and differ only in how much reasoning the trial is allowed to use. Writing the
 *     scan once keeps that difference — which is the whole of the difficulty difference between them — in one obvious
 *     place.
 * </p>
 */
final class Contradictions {
	
	private Contradictions() {}
	
	/**
	 * Assumes each candidate in turn and returns the elimination of the first one that leads to a contradiction.
	 *
	 * @param grid The working grid; never mutated
	 * @param strategies The techniques the trial may use while propagating
	 * @param maxSteps How many deductions the trial may apply
	 * @param technique The technique to attribute the elimination to
	 * @return The elimination, or empty if no candidate is refuted
	 */
	static Optional<Deduction> refute(CandidateGrid grid, List<TechniqueStrategy> strategies, int maxSteps, Technique technique) {
		for (int cell = 0; cell < grid.cellCount(); cell++) {
			if (!grid.isEmpty(cell) || grid.candidateCount(cell) < 2) {
				continue;
			}
			
			for (int digit : grid.candidateDigits(cell)) {
				if (Assumptions.assume(grid, cell, digit, strategies, maxSteps).isPresent()) {
					continue;
				}
				
				EliminationBuilder builder = new EliminationBuilder();
				builder.add(grid, cell, digit);
				Optional<Deduction> found = builder.build(technique);
				if (found.isPresent()) {
					return found;
				}
			}
		}
		return Optional.empty();
	}
}
