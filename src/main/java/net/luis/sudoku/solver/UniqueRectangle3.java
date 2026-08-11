package net.luis.sudoku.solver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Unique rectangle type 3: the roof's extra candidates behave as a single virtual cell, which can form a naked subset
 * with the other cells of a unit.
 * <p>
 *     The two floor corners hold nothing but the shared pair, so one of the two roof corners must use one of its
 *     extra candidates — otherwise the rectangle becomes the deadly pattern. The pair of roof corners therefore acts
 *     exactly like one cell whose candidates are the union of their extras. Dropped into a unit that contains both of
 *     them, that virtual cell can complete a naked pair, triple or quad with the unit's ordinary cells, and the
 *     subset's digits come out of everything else in the unit.
 * </p>
 * <p>
 *     Subsets of two, three and four are searched, the virtual cell always being one member. The scan is
 *     deterministic — units in {@link CandidateGrid#allUnits()} order, subset sizes ascending, cell combinations
 *     ascending — and the first subset that removes a candidate is returned.
 * </p>
 *
 * @see TechniqueStrategy
 * @see UniqueRectangle
 * @see NakedSubset
 * @see Technique#UNIQUE_RECTANGLE_3
 */
public final class UniqueRectangle3 extends UniqueRectangle {
	
	/**
	 * Constructs the type 3 strategy. The strategy is stateless and holds no grid.
	 */
	public UniqueRectangle3() {
		super(Technique.UNIQUE_RECTANGLE_3);
	}
	
	@Override
	Optional<Deduction> test(CandidateGrid grid, int[] corners, int pair, Explanation.Builder explanation) {
		int[] roof = this.roofOf(grid, corners, pair);
		if (roof == null) {
			return Optional.empty();
		}
		
		int extras = (grid.candidates(roof[0]) | grid.candidates(roof[1])) & ~pair;
		// A single extra digit across both roof corners is type 2, which is a rank lower and fires first.
		if (Integer.bitCount(extras) < 2) {
			return Optional.empty();
		}
		
		for (int[] unit : grid.allUnits()) {
			if (!this.contains(unit, roof[0]) || !this.contains(unit, roof[1])) {
				continue;
			}
			
			for (int subsetSize = 2; subsetSize <= 4; subsetSize++) {
				int[] chosen = new int[subsetSize - 1];
				Optional<Deduction> found = this.search(grid, unit, roof, chosen, extras, subsetSize, 0, 0, extras, corners, pair, explanation);
				if (found.isPresent()) {
					return found;
				}
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Picks the ordinary cells that join the virtual cell in the subset, in ascending positional order.
	 */
	private Optional<Deduction> search(CandidateGrid grid, int[] unit, int[] roof, int[] chosen, int extras, int subsetSize, int depth, int start, int union, int[] corners, int pair, Explanation.Builder explanation) {
		if (depth == chosen.length) {
			return Integer.bitCount(union) == subsetSize ? this.eliminate(grid, unit, roof, chosen, union, extras, corners, pair, explanation) : Optional.empty();
		}
		if (Integer.bitCount(union) > subsetSize) {
			return Optional.empty();
		}
		
		for (int position = start; position <= unit.length - (chosen.length - depth); position++) {
			int cell = unit[position];
			int count = grid.candidateCount(cell);
			if (cell == roof[0] || cell == roof[1] || count < 2 || count > subsetSize) {
				continue;
			}
			
			chosen[depth] = cell;
			Optional<Deduction> found = this.search(grid, unit, roof, chosen, extras, subsetSize, depth + 1, position + 1, union | grid.candidates(cell), corners, pair, explanation);
			if (found.isPresent()) {
				return found;
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Removes the subset's digits from every cell of the unit outside the subset and outside the roof.
	 */
	private Optional<Deduction> eliminate(CandidateGrid grid, int[] unit, int[] roof, int[] chosen, int union, int extras, int[] corners, int pair, Explanation.Builder explanation) {
		EliminationBuilder builder = new EliminationBuilder();
		for (int cell : unit) {
			if (cell == roof[0] || cell == roof[1] || this.contains(chosen, cell)) {
				continue;
			}
			
			builder.addAll(grid, cell, union);
		}
		Optional<Deduction> deduction = builder.build(Technique.UNIQUE_RECTANGLE_3);
		// Only a subset that removes something is the deduction being returned, so only that one is worth explaining:
		// any earlier one was looked at and rejected.
		if (deduction.isPresent() && explanation != null) {
			this.explain(grid, unit, roof, chosen, union, extras, corners, pair, explanation);
		}
		return deduction;
	}
	
	/**
	 * Records the pattern: the rectangle, the unit the subset lives in, the roof corners acting as one virtual cell
	 * alongside the ordinary members of the subset, and the cells the subset's digits are therefore gone from.
	 *
	 * @param grid The working grid
	 * @param unit The unit the subset lives in
	 * @param roof The two roof corners
	 * @param chosen The ordinary cells of the subset
	 * @param union The digits the subset spans
	 * @param extras The roof's extra candidates, which are the virtual cell's
	 * @param corners The rectangle's four corners
	 * @param pair The bitmask of the two shared digits
	 * @param explanation The explanation to record into
	 */
	private void explain(CandidateGrid grid, int[] unit, int[] roof, int[] chosen, int union, int extras, int[] corners, int pair, Explanation.Builder explanation) {
		this.explainRectangle(grid, corners, pair, explanation);
		
		// The two roof corners are shown carrying only their extras: that is the virtual cell, and the pair digits
		// they also list play no part in the subset.
		List<PatternCell> subset = new ArrayList<>();
		subset.add(new PatternCell(roof[0], CellRole.ROOF, extras));
		subset.add(new PatternCell(roof[1], CellRole.ROOF, extras));
		for (int cell : chosen) {
			subset.add(new PatternCell(cell, CellRole.PATTERN, grid.candidates(cell)));
		}
		
		List<PatternCell> rest = new ArrayList<>();
		for (int cell : unit) {
			if (cell == roof[0] || cell == roof[1] || this.contains(chosen, cell) || !grid.isEmpty(cell)) {
				continue;
			}
			
			rest.add(new PatternCell(cell, CellRole.CONTEXT, union & grid.candidates(cell)));
		}
		
		explanation.focusUnits(0, List.of(Explanations.refOf(grid, unit, roof[0])))
			.pattern(0, subset)
			.implication(0, rest);
	}
	
	private boolean contains(int[] cells, int cell) {
		for (int member : cells) {
			if (member == cell) {
				return true;
			}
		}
		return false;
	}
}
