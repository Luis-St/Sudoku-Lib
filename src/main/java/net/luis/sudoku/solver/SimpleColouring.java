package net.luis.sudoku.solver;

import java.util.Optional;

/**
 * Simple colouring: a two-colouring of one digit's conjugate-pair graph.
 * <p>
 *     Follow the chain of conjugate pairs of a digit and paint alternate cells in two colours. One whole colour is
 *     true and the other whole colour is false. Two conclusions follow directly:
 * </p>
 * <ul>
 *     <li><b>The colour contradicts itself.</b> If two cells of the same colour share a unit, that colour cannot be
 *         the true one, so the digit comes out of every cell painted with it.</li>
 *     <li><b>An outside cell is trapped.</b> Any cell seeing a cell of each colour is seen by the true colour whichever
 *         it turns out to be, so it cannot hold the digit.</li>
 * </ul>
 * <p>
 *     The scan is deterministic — digits ascending, clusters in the order {@link Colourings} produces them, the
 *     self-contradiction rule before the trap rule.
 * </p>
 *
 * @see TechniqueStrategy
 * @see MultiColouring
 * @see Technique#SIMPLE_COLOURING
 */
public final class SimpleColouring implements TechniqueStrategy {
	
	/**
	 * Constructs the simple-colouring strategy. The strategy is stateless and holds no grid.
	 */
	public SimpleColouring() {}
	
	@Override
	public Technique technique() {
		return Technique.SIMPLE_COLOURING;
	}
	
	/**
	 * Colours every digit's conjugate-pair graph and applies the two colouring rules.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first colouring elimination, or empty if no colouring makes progress
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		for (int digit = 1; digit <= grid.n(); digit++) {
			for (Colourings.Cluster cluster : Colourings.ofDigit(grid, digit)) {
				Optional<Deduction> contradiction = this.selfContradiction(grid, cluster, digit);
				if (contradiction.isPresent()) {
					return contradiction;
				}
				
				Optional<Deduction> trapped = this.trapped(grid, cluster, digit);
				if (trapped.isPresent()) {
					return trapped;
				}
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Removes the digit from a colour that appears twice in one unit, which proves that colour false.
	 */
	private Optional<Deduction> selfContradiction(CandidateGrid grid, Colourings.Cluster cluster, int digit) {
		int[] cells = cluster.cells();
		int[] colours = cluster.colours();
		for (int i = 0; i < cells.length; i++) {
			for (int j = i + 1; j < cells.length; j++) {
				if (colours[i] != colours[j] || !grid.peers(cells[i], cells[j])) {
					continue;
				}
				
				EliminationBuilder builder = new EliminationBuilder();
				for (int index = 0; index < cells.length; index++) {
					if (colours[index] == colours[i]) {
						builder.add(grid, cells[index], digit);
					}
				}
				
				Optional<Deduction> found = builder.build(Technique.SIMPLE_COLOURING);
				if (found.isPresent()) {
					return found;
				}
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Removes the digit from every cell outside the cluster that sees both colours.
	 */
	private Optional<Deduction> trapped(CandidateGrid grid, Colourings.Cluster cluster, int digit) {
		EliminationBuilder builder = new EliminationBuilder();
		for (int cell = 0; cell < grid.cellCount(); cell++) {
			if (cluster.contains(cell, digit)) {
				continue;
			}
			
			if (this.sees(grid, cluster, cell, 0) && this.sees(grid, cluster, cell, 1)) {
				builder.add(grid, cell, digit);
			}
		}
		return builder.build(Technique.SIMPLE_COLOURING);
	}
	
	private boolean sees(CandidateGrid grid, Colourings.Cluster cluster, int cell, int colour) {
		int[] cells = cluster.cells();
		int[] colours = cluster.colours();
		for (int index = 0; index < cells.length; index++) {
			if (colours[index] == colour && grid.peers(cell, cells[index])) {
				return true;
			}
		}
		return false;
	}
}
