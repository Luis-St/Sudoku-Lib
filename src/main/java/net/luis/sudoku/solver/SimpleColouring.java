package net.luis.sudoku.solver;

import java.util.List;
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
		return this.scan(grid, null);
	}

	/**
	 * Explains the colouring by painting the whole cluster in its two colours and then showing which of the two rules
	 * fired: a colour that meets itself in a unit, or a cell outside the cluster that sees both colours.
	 *
	 * @param grid The working grid; never mutated
	 * @return The eliminations and their explanation, or empty if no colouring makes progress
	 */
	@Override
	public Optional<ExplainedDeduction> findExplained(CandidateGrid grid) {
		Explanation.Builder builder = Explanation.builder(Technique.SIMPLE_COLOURING);
		return this.scan(grid, builder).map(deduction -> new ExplainedDeduction(deduction, builder.conclusion(deduction).build()));
	}

	private Optional<Deduction> scan(CandidateGrid grid, Explanation.Builder explanation) {
		for (int digit = 1; digit <= grid.n(); digit++) {
			for (Colourings.Cluster cluster : Colourings.ofDigit(grid, digit)) {
				Optional<Deduction> contradiction = this.selfContradiction(grid, cluster, digit, explanation);
				if (contradiction.isPresent()) {
					return contradiction;
				}
				
				Optional<Deduction> trapped = this.trapped(grid, cluster, digit, explanation);
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
	private Optional<Deduction> selfContradiction(CandidateGrid grid, Colourings.Cluster cluster, int digit, Explanation.Builder explanation) {
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
				// Only a colouring that removes something is the deduction being returned, so only that one is worth
				// explaining: any earlier one was looked at and rejected.
				if (found.isPresent()) {
					if (explanation != null) {
						this.paint(grid, cluster, digit, explanation);
						UnitRef shared = Explanations.sharedUnit(grid, cells[i], cells[j]);
						if (shared != null) {
							explanation.focusUnits(digit, List.of(shared));
						}
						// Two cells of one colour in a single unit: that colour cannot be the true one, so every cell
						// wearing it loses the digit.
						explanation.implication(digit, List.of(PatternCell.of(cells[i], CellRole.CONTEXT, digit), PatternCell.of(cells[j], CellRole.CONTEXT, digit)));
					}
					return found;
				}
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Removes the digit from every cell outside the cluster that sees both colours.
	 */
	private Optional<Deduction> trapped(CandidateGrid grid, Colourings.Cluster cluster, int digit, Explanation.Builder explanation) {
		EliminationBuilder builder = new EliminationBuilder();
		for (int cell = 0; cell < grid.cellCount(); cell++) {
			if (cluster.contains(cell, digit)) {
				continue;
			}
			
			if (this.sees(grid, cluster, cell, 0) && this.sees(grid, cluster, cell, 1)) {
				builder.add(grid, cell, digit);
			}
		}
		Optional<Deduction> deduction = builder.build(Technique.SIMPLE_COLOURING);
		if (deduction.isPresent() && explanation != null) {
			this.paint(grid, cluster, digit, explanation);
			// One trapped cell is enough to show the argument: it sees a cell of each colour, so whichever colour is
			// the true one has already used the digit where this cell can see it.
			int trapped = ((Deduction.Eliminations) deduction.orElseThrow()).cells()[0];
			explanation.implication(digit, List.of(
				PatternCell.of(this.witness(grid, cluster, trapped, 0), CellRole.CONTEXT, digit),
				PatternCell.of(this.witness(grid, cluster, trapped, 1), CellRole.CONTEXT, digit)
			));
		}
		return deduction;
	}
	
	/**
	 * Records the cluster itself: the digit it is about and every candidate in it, painted in the two colours.
	 */
	private void paint(CandidateGrid grid, Colourings.Cluster cluster, int digit, Explanation.Builder explanation) {
		explanation.focusDigit(digit)
			.pattern(digit, Explanations.painted(cluster.cells(), cluster.digits(), cluster.colours()));
	}
	
	/**
	 * Returns the cluster cell of the given colour that the trapped cell sees.
	 */
	private int witness(CandidateGrid grid, Colourings.Cluster cluster, int cell, int colour) {
		int[] cells = cluster.cells();
		int[] colours = cluster.colours();
		for (int index = 0; index < cells.length; index++) {
			if (colours[index] == colour && grid.peers(cell, cells[index])) {
				return cells[index];
			}
		}
		throw new IllegalStateException("Cell " + cell + " was trapped without seeing colour " + colour);
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
