package net.luis.sudoku.solver;

import java.util.List;
import java.util.Optional;

/**
 * Multi-colouring: two separate colour clusters of one digit, joined by a single sighting between them.
 * <p>
 *     Colour two disconnected clusters of the same digit. If any cell of colour {@code a} in the first cluster sees
 *     any cell of colour {@code b} in the second, those two colours cannot both be the true ones — so the opposite
 *     colour of one cluster or the other must be true. A cell seeing a cell of each of those two opposite colours is
 *     therefore covered either way, and the digit comes out of it.
 * </p>
 * <p>
 *     Where {@link SimpleColouring} reasons inside one cluster, this reasons across two, which is what makes it
 *     harder: the player has to hold two independent colourings in mind at once.
 * </p>
 *
 * @see TechniqueStrategy
 * @see SimpleColouring
 * @see Technique#MULTI_COLOURING
 */
public final class MultiColouring implements TechniqueStrategy {
	
	/**
	 * Constructs the multi-colouring strategy. The strategy is stateless and holds no grid.
	 */
	public MultiColouring() {}
	
	@Override
	public Technique technique() {
		return Technique.MULTI_COLOURING;
	}
	
	/**
	 * Colours every digit and tests every pair of clusters for a link between their colours.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first multi-colouring elimination, or empty if no pair of clusters makes progress
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		for (int digit = 1; digit <= grid.n(); digit++) {
			List<Colourings.Cluster> clusters = Colourings.ofDigit(grid, digit);
			for (int i = 0; i < clusters.size(); i++) {
				for (int j = i + 1; j < clusters.size(); j++) {
					Optional<Deduction> found = this.test(grid, clusters.get(i), clusters.get(j), digit);
					if (found.isPresent()) {
						return found;
					}
				}
			}
		}
		return Optional.empty();
	}
	
	private Optional<Deduction> test(CandidateGrid grid, Colourings.Cluster first, Colourings.Cluster second, int digit) {
		for (int firstColour = 0; firstColour < 2; firstColour++) {
			for (int secondColour = 0; secondColour < 2; secondColour++) {
				if (!this.linked(grid, first, firstColour, second, secondColour)) {
					continue;
				}
				
				EliminationBuilder builder = new EliminationBuilder();
				for (int cell = 0; cell < grid.cellCount(); cell++) {
					if (first.contains(cell, digit) || second.contains(cell, digit)) {
						continue;
					}
					
					if (this.sees(grid, first, 1 - firstColour, cell) && this.sees(grid, second, 1 - secondColour, cell)) {
						builder.add(grid, cell, digit);
					}
				}
				
				Optional<Deduction> found = builder.build(Technique.MULTI_COLOURING);
				if (found.isPresent()) {
					return found;
				}
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Checks whether any cell of the one colour sees any cell of the other, which is what rules the two colours out
	 * of being true together.
	 */
	private boolean linked(CandidateGrid grid, Colourings.Cluster first, int firstColour, Colourings.Cluster second, int secondColour) {
		for (int i = 0; i < first.cells().length; i++) {
			if (first.colours()[i] != firstColour) {
				continue;
			}
			
			for (int j = 0; j < second.cells().length; j++) {
				if (second.colours()[j] == secondColour && grid.peers(first.cells()[i], second.cells()[j])) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean sees(CandidateGrid grid, Colourings.Cluster cluster, int colour, int cell) {
		for (int index = 0; index < cluster.cells().length; index++) {
			if (cluster.colours()[index] == colour && grid.peers(cell, cluster.cells()[index])) {
				return true;
			}
		}
		return false;
	}
}
