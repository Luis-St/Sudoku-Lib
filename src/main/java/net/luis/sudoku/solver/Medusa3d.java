package net.luis.sudoku.solver;

import java.util.Optional;

/**
 * The 3D Medusa: a two-colouring of the strong-link graph across every digit at once.
 * <p>
 *     {@link SimpleColouring} paints one digit's conjugate pairs. The Medusa adds the other kind of strong link — the
 *     two candidates of a bi-value cell, exactly one of which is true — so a single cluster now runs through digits as
 *     well as through cells. The third dimension is what the name refers to, and it is what makes the technique both
 *     stronger and much harder to hold in one's head.
 * </p>
 * <p>
 *     One colour is entirely true and the other entirely false, which gives five conclusions:
 * </p>
 * <ul>
 *     <li>A colour appearing twice in one cell is false, since a cell holds one digit.</li>
 *     <li>A colour appearing twice for one digit in one unit is false, for the same reason as in simple colouring.</li>
 *     <li>A cell carrying both colours holds one of them, so its uncoloured candidates go.</li>
 *     <li>An uncoloured candidate seeing both colours of its own digit is covered either way.</li>
 *     <li>An uncoloured candidate whose cell carries one colour and which sees the other colour of its digit is too.</li>
 * </ul>
 *
 * @see TechniqueStrategy
 * @see SimpleColouring
 * @see Technique#MEDUSA_3D
 */
public final class Medusa3d implements TechniqueStrategy {
	
	/**
	 * Constructs the 3D Medusa strategy. The strategy is stateless and holds no grid.
	 */
	public Medusa3d() {}
	
	@Override
	public Technique technique() {
		return Technique.MEDUSA_3D;
	}
	
	/**
	 * Colours the whole grid's strong-link graph and applies the five Medusa rules in order.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first Medusa elimination, or empty if no cluster makes progress
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		for (Colourings.Cluster cluster : Colourings.ofGrid(grid)) {
			Optional<Deduction> contradiction = this.contradiction(grid, cluster);
			if (contradiction.isPresent()) {
				return contradiction;
			}
			
			Optional<Deduction> trapped = this.trapped(grid, cluster);
			if (trapped.isPresent()) {
				return trapped;
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Finds a colour that contradicts itself — twice in one cell, or twice for one digit in one unit — and removes
	 * every candidate painted with it.
	 */
	private Optional<Deduction> contradiction(CandidateGrid grid, Colourings.Cluster cluster) {
		int[] cells = cluster.cells();
		int[] digits = cluster.digits();
		int[] colours = cluster.colours();
		for (int i = 0; i < cells.length; i++) {
			for (int j = i + 1; j < cells.length; j++) {
				if (colours[i] != colours[j]) {
					continue;
				}
				
				boolean sameCell = cells[i] == cells[j];
				boolean sameDigitSeen = digits[i] == digits[j] && grid.peers(cells[i], cells[j]);
				if (!sameCell && !sameDigitSeen) {
					continue;
				}
				
				EliminationBuilder builder = new EliminationBuilder();
				for (int index = 0; index < cells.length; index++) {
					if (colours[index] == colours[i]) {
						builder.add(grid, cells[index], digits[index]);
					}
				}
				
				Optional<Deduction> found = builder.build(Technique.MEDUSA_3D);
				if (found.isPresent()) {
					return found;
				}
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Removes every uncoloured candidate that the true colour covers whichever colour that turns out to be.
	 */
	private Optional<Deduction> trapped(CandidateGrid grid, Colourings.Cluster cluster) {
		EliminationBuilder builder = new EliminationBuilder();
		for (int cell = 0; cell < grid.cellCount(); cell++) {
			if (!grid.isEmpty(cell)) {
				continue;
			}
			
			boolean cellHasFirst = this.inCell(cluster, cell, 0);
			boolean cellHasSecond = this.inCell(cluster, cell, 1);
			for (int digit = 1; digit <= grid.n(); digit++) {
				if (!grid.hasCandidate(cell, digit) || cluster.contains(cell, digit)) {
					continue;
				}
				
				boolean seesFirst = this.sees(grid, cluster, cell, digit, 0);
				boolean seesSecond = this.sees(grid, cluster, cell, digit, 1);
				// Covered by both colours of its own digit, or by one colour in its cell and the other along its line.
				if ((seesFirst && seesSecond) || (cellHasFirst && cellHasSecond) || (cellHasFirst && seesSecond) || (cellHasSecond && seesFirst)) {
					builder.add(grid, cell, digit);
				}
			}
		}
		return builder.build(Technique.MEDUSA_3D);
	}
	
	private boolean inCell(Colourings.Cluster cluster, int cell, int colour) {
		for (int index = 0; index < cluster.cells().length; index++) {
			if (cluster.cells()[index] == cell && cluster.colours()[index] == colour) {
				return true;
			}
		}
		return false;
	}
	
	private boolean sees(CandidateGrid grid, Colourings.Cluster cluster, int cell, int digit, int colour) {
		for (int index = 0; index < cluster.cells().length; index++) {
			if (cluster.digits()[index] == digit && cluster.colours()[index] == colour && grid.peers(cell, cluster.cells()[index])) {
				return true;
			}
		}
		return false;
	}
}
