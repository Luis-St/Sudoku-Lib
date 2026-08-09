package net.luis.sudoku.solver;

import java.util.Optional;

/**
 * The shared naked-subset scan: {@code k} cells of a unit whose candidates together span exactly {@code k} digits use
 * those digits up within the unit, so no other cell of the unit may hold any of them.
 * <p>
 *     Pairs, triples and quads are the same deduction at three sizes, and they are three separate techniques only
 *     because they cost a player increasingly more effort to spot. Writing the scan once and parameterizing it by
 *     {@code k} keeps that difference where it belongs — in the {@link Technique} the subclass reports — instead of in
 *     three near-identical copies of the search.
 * </p>
 * <p>
 *     A cell may take part only if it has between two and {@code k} candidates: a single-candidate cell is a
 *     {@link NakedSingle} and would let a subset fire on a pattern the solver has already handled at a lower rank.
 *     The search is deterministic — units in {@link CandidateGrid#allUnits()} order, cell combinations in ascending
 *     positional order — and returns the first subset that actually removes a candidate.
 * </p>
 *
 * @see NakedPair
 * @see NakedTriple
 * @see NakedQuad
 */
abstract sealed class NakedSubset implements TechniqueStrategy permits NakedPair, NakedTriple, NakedQuad {
	
	private final Technique technique;
	private final int subsetSize;
	
	/**
	 * Constructs a naked-subset strategy of the given size.
	 *
	 * @param technique The technique to attribute the eliminations to
	 * @param subsetSize The number of cells and digits the subset spans
	 */
	NakedSubset(Technique technique, int subsetSize) {
		this.technique = technique;
		this.subsetSize = subsetSize;
	}
	
	@Override
	public final Technique technique() {
		return this.technique;
	}
	
	/**
	 * Scans every unit for {@code k} cells spanning exactly {@code k} candidates and returns the first elimination it
	 * enables in the rest of that unit.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first naked-subset elimination, or empty if the pattern makes no progress anywhere
	 */
	@Override
	public final Optional<Deduction> find(CandidateGrid grid) {
		for (int[] unit : grid.allUnits()) {
			int[] chosen = new int[this.subsetSize];
			Optional<Deduction> found = this.search(grid, unit, chosen, 0, 0, 0);
			if (found.isPresent()) {
				return found;
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Extends the current combination of unit positions by one cell and recurses, testing a complete combination for
	 * the subset property.
	 *
	 * @param grid The working grid
	 * @param unit The unit being scanned
	 * @param chosen The positions picked so far, the first {@code depth} entries being meaningful
	 * @param depth How many positions have been picked
	 * @param start The first unit position this level may pick, which keeps combinations ascending
	 * @param union The union of the picked cells' candidate masks
	 * @return The first elimination found below this node, or empty
	 */
	private Optional<Deduction> search(CandidateGrid grid, int[] unit, int[] chosen, int depth, int start, int union) {
		if (depth == this.subsetSize) {
			return Integer.bitCount(union) == this.subsetSize ? this.eliminate(grid, unit, chosen, union) : Optional.empty();
		}
		
		// Prune as soon as the union has already outgrown the subset: adding cells can only widen it further.
		if (Integer.bitCount(union) > this.subsetSize) {
			return Optional.empty();
		}
		
		for (int position = start; position <= unit.length - (this.subsetSize - depth); position++) {
			int cell = unit[position];
			int count = grid.candidateCount(cell);
			if (count < 2 || count > this.subsetSize) {
				continue;
			}
			
			chosen[depth] = position;
			Optional<Deduction> found = this.search(grid, unit, chosen, depth + 1, position + 1, union | grid.candidates(cell));
			if (found.isPresent()) {
				return found;
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Removes the subset's digits from every cell of the unit outside the subset.
	 *
	 * @param grid The working grid
	 * @param unit The unit the subset lives in
	 * @param chosen The unit positions the subset occupies
	 * @param union The digits the subset spans
	 * @return The eliminations, or empty if the subset removes nothing
	 */
	private Optional<Deduction> eliminate(CandidateGrid grid, int[] unit, int[] chosen, int union) {
		EliminationBuilder builder = new EliminationBuilder();
		for (int position = 0; position < unit.length; position++) {
			if (this.contains(chosen, position)) {
				continue;
			}
			
			builder.addAll(grid, unit[position], union);
		}
		return builder.build(this.technique);
	}
	
	private boolean contains(int[] chosen, int position) {
		for (int picked : chosen) {
			if (picked == position) {
				return true;
			}
		}
		return false;
	}
}
