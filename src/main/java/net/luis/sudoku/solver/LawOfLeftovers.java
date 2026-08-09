package net.luis.sudoku.solver;

import java.util.Optional;

/**
 * The Law of Leftovers: take an equal number of lines and of regions, and the cells each covers that the other does
 * not must hold the same digits.
 * <p>
 *     Every line holds each digit exactly once and so does every region, so {@code k} lines and {@code k} regions
 *     cover each digit exactly {@code k} times between them. Removing the cells they share leaves two sets whose
 *     digit multisets are therefore identical, however oddly the regions are shaped. The immediate consequence is the
 *     one worth searching for: if a digit cannot be placed anywhere in one leftover, it cannot appear in the other
 *     either, and it can be struck from every cell there.
 * </p>
 * <p>
 *     This is the one technique a jigsaw player has that a classic player does not, and the rater needs it for the
 *     same reason: without it a chaos puzzle is forced into a harder technique wherever a human would simply read
 *     the leftovers, and the puzzle is rated above how it plays. It is sound on a classic grid too — the box layout
 *     is just a particularly boring jigsaw — but there the leftovers rarely say anything that pointing and claiming
 *     have not already said.
 * </p>
 * <p>
 *     The search is bounded and deterministic: runs of {@code k} adjacent lines, rows before columns, ascending, and
 *     for each run the {@code k} regions overlapping it most, ties broken by region index. Only leftovers of at most
 *     {@link #MAX_LEFTOVER} cells are considered, since a larger one is neither cheap to check nor something a person
 *     would see.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Technique#LAW_OF_LEFTOVERS
 */
public final class LawOfLeftovers implements TechniqueStrategy {
	
	/**
	 * The largest leftover, in cells, the search will look at.
	 * <p>
	 *     Leftovers grow as the lines and the regions agree less, and a large one carries little information for a
	 *     lot of work: the digit has too many places to hide for "it cannot go anywhere here" to happen. Capping at
	 *     four keeps this to the shapes a player actually reads off the board.
	 * </p>
	 */
	public static final int MAX_LEFTOVER = 4;
	
	/**
	 * Constructs the strategy. The strategy is stateless and holds no grid.
	 */
	public LawOfLeftovers() {}
	
	@Override
	public Technique technique() {
		return Technique.LAW_OF_LEFTOVERS;
	}
	
	/**
	 * Scans runs of adjacent rows and then of adjacent columns, comparing each against the regions it most overlaps,
	 * and returns the first elimination the leftovers prove.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first elimination, or empty if no pair of leftovers rules anything out
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		int n = grid.n();
		for (int lines = 1; lines < n; lines++) {
			for (boolean rows : new boolean[] { true, false }) {
				for (int first = 0; first + lines <= n; first++) {
					Optional<Deduction> found = this.scan(grid, rows, first, lines);
					if (found.isPresent()) {
						return found;
					}
				}
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Compares one run of lines against the regions it overlaps most and returns the first elimination its leftovers
	 * prove.
	 *
	 * @param grid The working grid
	 * @param rows Whether the run is of rows rather than of columns
	 * @param first The index of the first line in the run
	 * @param lines How many adjacent lines the run covers
	 * @return The first elimination, or empty
	 */
	private Optional<Deduction> scan(CandidateGrid grid, boolean rows, int first, int lines) {
		boolean[] inLines = new boolean[grid.cellCount()];
		for (int line = first; line < first + lines; line++) {
			for (int cell : rows ? grid.rowCells(line) : grid.columnCells(line)) {
				inLines[cell] = true;
			}
		}
		
		boolean[] inRegions = new boolean[grid.cellCount()];
		for (int region : this.mostOverlapping(grid, inLines, lines)) {
			for (int cell : grid.regionCells(region)) {
				inRegions[cell] = true;
			}
		}
		
		int[] onlyLines = this.difference(inLines, inRegions);
		int[] onlyRegions = this.difference(inRegions, inLines);
		if (onlyLines.length == 0 || onlyLines.length > MAX_LEFTOVER || onlyRegions.length > MAX_LEFTOVER) {
			// Equal covers leave nothing to say, and the two leftovers always have equal size, so one bound checks
			// both; the second test only guards the degenerate case of differing region counts.
			return Optional.empty();
		}
		
		for (int digit = 1; digit <= grid.n(); digit++) {
			boolean possibleInLines = this.canHold(grid, onlyLines, digit);
			boolean possibleInRegions = this.canHold(grid, onlyRegions, digit);
			if (possibleInLines == possibleInRegions) {
				continue;
			}
			
			// The digit is impossible in one leftover, so it occurs there zero times, so it occurs zero times in the
			// other one too, however many cells there could still have taken it.
			EliminationBuilder builder = new EliminationBuilder();
			for (int cell : possibleInLines ? onlyLines : onlyRegions) {
				builder.add(grid, cell, digit);
			}
			
			Optional<Deduction> found = builder.build(Technique.LAW_OF_LEFTOVERS);
			if (found.isPresent()) {
				return found;
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Returns the indices of the {@code count} regions sharing the most cells with the given cover, ties broken by
	 * ascending region index so the choice is deterministic.
	 */
	private int[] mostOverlapping(CandidateGrid grid, boolean[] cover, int count) {
		int regionCount = grid.regions().size();
		int[] overlaps = new int[regionCount];
		for (int region = 0; region < regionCount; region++) {
			for (int cell : grid.regionCells(region)) {
				if (cover[cell]) {
					overlaps[region]++;
				}
			}
		}
		
		int[] chosen = new int[count];
		boolean[] taken = new boolean[regionCount];
		for (int slot = 0; slot < count; slot++) {
			int best = -1;
			for (int region = 0; region < regionCount; region++) {
				if (!taken[region] && (best == -1 || overlaps[region] > overlaps[best])) {
					best = region;
				}
			}
			
			taken[best] = true;
			chosen[slot] = best;
		}
		return chosen;
	}
	
	/**
	 * Returns the ascending cell indices covered by {@code included} but not by {@code excluded}.
	 */
	private int[] difference(boolean[] included, boolean[] excluded) {
		int size = 0;
		for (int cell = 0; cell < included.length; cell++) {
			if (included[cell] && !excluded[cell]) {
				size++;
			}
		}
		
		int[] cells = new int[size];
		int next = 0;
		for (int cell = 0; cell < included.length; cell++) {
			if (included[cell] && !excluded[cell]) {
				cells[next++] = cell;
			}
		}
		return cells;
	}
	
	/**
	 * Returns whether any of the given cells could still hold the digit, either by already holding it or by keeping
	 * it as a candidate.
	 */
	private boolean canHold(CandidateGrid grid, int[] cells, int digit) {
		for (int cell : cells) {
			if (grid.value(cell) == digit || grid.hasCandidate(cell, digit)) {
				return true;
			}
		}
		return false;
	}
}
