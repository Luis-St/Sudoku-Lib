package net.luis.sudoku.solver;

import java.util.List;
import java.util.Optional;

/**
 * The shared machinery behind the techniques that reason by assumption: {@link Nishio}, the {@link ForcingChain}, the
 * {@link ForcingNet} and the {@link DynamicContradictionChain}.
 * <p>
 *     All four work the same way. Take a candidate, suppose it is the answer, and push that supposition as far as a
 *     given set of techniques will carry it on a throwaway copy of the grid. If the supposition runs into a
 *     contradiction, the candidate is impossible. If several suppositions that between them cover every possibility
 *     all reach the same placement, that placement is proved.
 * </p>
 * <p>
 *     What separates the four is only how much reasoning the propagation is allowed to use — singles alone, or the
 *     whole pattern-based technique set — and therefore how much work a player would have to do to follow it. That is
 *     why they sit at the top of the difficulty scale: the deduction is mechanical, but the search is not.
 * </p>
 * <p>
 *     Propagation is bounded by a step budget so that a pathological grid cannot make a single technique run away.
 * </p>
 */
final class Assumptions {
	
	/**
	 * The techniques an assumption may use when only immediate consequences count.
	 */
	static final List<TechniqueStrategy> SINGLES = List.of(new FullHouse(), new LastDigit(), new NakedSingle(), new HiddenSingleRegion(), new HiddenSingleLine());
	
	/**
	 * The techniques an assumption may use when it is allowed to reason as a competent player would: the singles plus
	 * locked candidates and the subsets.
	 */
	static final List<TechniqueStrategy> BASIC = List.of(
		new FullHouse(), new LastDigit(), new NakedSingle(), new HiddenSingleRegion(), new HiddenSingleLine(),
		new Pointing(), new Claiming(),
		new NakedPair(), new HiddenPair(), new NakedTriple(), new HiddenTriple(), new NakedQuad(), new HiddenQuad()
	);
	
	/**
	 * The techniques an assumption may use when the whole pattern-based repertoire is on the table, which is what
	 * makes a contradiction chain "dynamic". Deliberately stops below the chain techniques themselves, so no
	 * assumption ever re-enters an assumption.
	 */
	static final List<TechniqueStrategy> PATTERNS = List.of(
		new FullHouse(), new LastDigit(), new NakedSingle(), new HiddenSingleRegion(), new HiddenSingleLine(),
		new Pointing(), new Claiming(),
		new NakedPair(), new HiddenPair(), new NakedTriple(), new HiddenTriple(),
		new XWing(), new Skyscraper(), new TwoStringKite(),
		new Swordfish(), new Crane(), new XyWing(),
		new XyzWing(), new WWing(), new FinnedXWing(), new NakedQuad(), new HiddenQuad(),
		new EmptyRectangle(), new FinnedSwordfish(), new SashimiSwordfish(), new Jellyfish()
	);
	
	/**
	 * The step budget of a trial that is meant to be a single thread of reasoning rather than an exhaustive one.
	 * <p>
	 *     A Nishio is a <i>chain</i>: the player follows one assumption along until it either dies or peters out, and
	 *     gives up long before exhausting the grid. Bounding it here is what keeps it a level-14 technique instead of
	 *     a brute-force solver that would leave the level-15 techniques with nothing to do.
	 * </p>
	 */
	static final int CHAIN_STEPS = 24;
	
	/**
	 * The step budget of a trial that is allowed to run to exhaustion, which the level-15 techniques are.
	 */
	static final int FULL_STEPS = 512;
	
	private Assumptions() {}
	
	/**
	 * Plays out the assumption that the given cell holds the given digit.
	 *
	 * @param grid The grid to assume on; never mutated, a copy is worked on instead
	 * @param cell The cell to fill
	 * @param digit The digit to assume
	 * @param strategies The techniques the propagation may use
	 * @param maxSteps How many deductions the trial may apply before it is abandoned
	 * @return The resulting grid, or an empty optional if the assumption leads to a contradiction
	 */
	static Optional<CandidateGrid> assume(CandidateGrid grid, int cell, int digit, List<TechniqueStrategy> strategies, int maxSteps) {
		CandidateGrid copy = grid.copy();
		copy.place(cell, digit);
		return propagate(copy, strategies, maxSteps);
	}
	
	/**
	 * Applies the given techniques to the grid until none of them fires, reporting whether the grid stayed consistent.
	 *
	 * @param grid The grid to work on, which is mutated
	 * @param strategies The techniques the propagation may use
	 * @param maxSteps How many deductions the propagation may apply before it stops
	 * @return The grid, or an empty optional if a contradiction surfaced
	 */
	static Optional<CandidateGrid> propagate(CandidateGrid grid, List<TechniqueStrategy> strategies, int maxSteps) {
		for (int step = 0; step < maxSteps; step++) {
			if (isContradictory(grid)) {
				return Optional.empty();
			}
			if (grid.isComplete()) {
				return grid.isSolved() ? Optional.of(grid) : Optional.empty();
			}
			
			Deduction deduction = null;
			for (TechniqueStrategy strategy : strategies) {
				Optional<Deduction> found = strategy.find(grid);
				if (found.isPresent()) {
					deduction = found.orElseThrow();
					break;
				}
			}
			
			if (deduction == null) {
				return Optional.of(grid);
			}
			deduction.applyTo(grid);
		}
		return Optional.of(grid);
	}
	
	/**
	 * Checks whether the grid has become impossible: a cell with nothing left to put in it, or a unit that has lost
	 * every place for one of its digits.
	 *
	 * @param grid The grid to check
	 * @return True if the grid can no longer be completed
	 */
	static boolean isContradictory(CandidateGrid grid) {
		for (int cell = 0; cell < grid.cellCount(); cell++) {
			if (grid.isEmpty(cell) && grid.candidates(cell) == 0) {
				return true;
			}
		}
		
		for (int[] unit : grid.allUnits()) {
			int available = 0;
			for (int cell : unit) {
				available |= grid.isEmpty(cell) ? grid.candidates(cell) : 1 << grid.value(cell);
			}
			
			if (available != (((1 << grid.n()) - 1) << 1)) {
				return true;
			}
		}
		return false;
	}
}
