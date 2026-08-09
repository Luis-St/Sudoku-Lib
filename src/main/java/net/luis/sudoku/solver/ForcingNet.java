package net.luis.sudoku.solver;

import java.util.Optional;

/**
 * The forcing net: a forcing argument whose branches may themselves reason, and which may branch on a unit rather
 * than a cell.
 * <p>
 *     Two things separate this from the {@link ForcingChain}. Each branch is allowed the locked candidates and the
 *     subsets on top of the singles, so a branch is a net of deductions rather than a single thread. And the branching
 *     may start from a digit inside a unit — it has to go in one of its remaining places — as well as from a cell,
 *     which reaches conclusions the cell form cannot.
 * </p>
 * <p>
 *     Between them those two extensions make this the strongest constructive technique the solver knows. What it
 *     cannot place, only a contradiction argument will settle.
 * </p>
 *
 * @see TechniqueStrategy
 * @see ForcingChain
 * @see Technique#FORCING_NET
 */
public final class ForcingNet implements TechniqueStrategy {
	
	/**
	 * Constructs the forcing-net strategy. The strategy is stateless and holds no grid.
	 */
	public ForcingNet() {}
	
	@Override
	public Technique technique() {
		return Technique.FORCING_NET;
	}
	
	/**
	 * Branches on every multi-candidate cell and every unit-bound digit, reasoning with the basic technique set, and
	 * returns the first placement all branches agree on.
	 *
	 * @param grid The working grid; never mutated
	 * @return The forced placement, or empty if nothing is forced
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		return Forcing.force(grid, Assumptions.BASIC, Assumptions.FULL_STEPS, Technique.FORCING_NET, true);
	}
}
