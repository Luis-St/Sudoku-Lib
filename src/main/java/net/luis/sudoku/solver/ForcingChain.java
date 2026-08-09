package net.luis.sudoku.solver;

import java.util.Optional;

/**
 * The forcing chain: every candidate of one cell leads, by singles alone, to the same placement.
 * <p>
 *     A cell must hold one of its candidates. Follow each of them in turn as far as the singles carry it. If all of
 *     those separate lines of play end up putting the same digit in the same cell, that digit is settled without ever
 *     resolving the cell the branching started from.
 * </p>
 * <p>
 *     Where {@link Nishio} refutes a candidate by contradiction, this proves a placement by agreement. Both need the
 *     same kind of bookkeeping, which is why they sit close together, but a forcing chain has to be carried out for
 *     every candidate at once rather than abandoned at the first contradiction, and that is what puts it a level
 *     higher.
 * </p>
 *
 * @see TechniqueStrategy
 * @see ForcingNet
 * @see Technique#FORCING_CHAIN
 */
public final class ForcingChain implements TechniqueStrategy {
	
	/**
	 * Constructs the forcing-chain strategy. The strategy is stateless and holds no grid.
	 */
	public ForcingChain() {}
	
	@Override
	public Technique technique() {
		return Technique.FORCING_CHAIN;
	}
	
	/**
	 * Branches on every multi-candidate cell and returns the first placement all of its branches agree on.
	 *
	 * @param grid The working grid; never mutated
	 * @return The forced placement, or empty if no cell forces one
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		return Forcing.force(grid, Assumptions.SINGLES, Assumptions.FULL_STEPS, Technique.FORCING_CHAIN, false);
	}
}
