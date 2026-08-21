package net.luis.sudoku.solver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The full-house technique: a unit with exactly one empty cell left must hold the one digit it is missing.
 * <p>
 *     This is the easiest deduction in the game — it needs no candidate bookkeeping at all, only the observation that
 *     a unit is one cell short of complete. It is ranked below {@link NakedSingle} because a solver, like a player,
 *     sees it without looking at candidates.
 * </p>
 * <p>
 *     The scan is deterministic: units are visited in {@link CandidateGrid#allUnits()} order (rows, then columns, then
 *     regions) and the first unit with a single empty cell is filled.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Technique#FULL_HOUSE
 */
public final class FullHouse implements TechniqueStrategy {
	
	/**
	 * Constructs the full-house strategy. The strategy is stateless and holds no grid.
	 */
	public FullHouse() {}
	
	@Override
	public Technique technique() {
		return Technique.FULL_HOUSE;
	}
	
	/**
	 * Scans every unit for the case of a single remaining empty cell and returns its forced placement.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first full house, or empty if every unit has none or several empty cells
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		for (int[] unit : grid.allUnits()) {
			int empty = 0;
			int target = -1;
			int used = 0;
			for (int cell : unit) {
				int value = grid.value(cell);
				if (value == 0) {
					empty++;
					target = cell;
				} else {
					used |= 1 << value;
				}
			}
			
			if (empty != 1) {
				continue;
			}
			
			int missing = (((1 << grid.n()) - 1) << 1) & ~used;
			// A unit whose filled cells already repeat a digit is not solvable; such a grid never reaches the
			// solver, but the guard keeps the technique from inventing a placement for a broken unit.
			if (Integer.bitCount(missing) == 1) {
				return Optional.of(new Deduction.Placement(Technique.FULL_HOUSE, target, Integer.numberOfTrailingZeros(missing)));
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Explains the full house by outlining the unit that is one cell short and showing every digit already in it, so
	 * the missing one is the only thing left to say.
	 *
	 * @param grid The working grid; never mutated
	 * @return The placement and its explanation, or empty if no unit has a single empty cell
	 */
	@Override
	public Optional<ExplainedDeduction> findExplained(CandidateGrid grid) {
		return this.find(grid).map(deduction -> this.explain(grid, deduction));
	}
	
	/**
	 * Returns every full house of the grid, not only the first: a position regularly has several units one cell
	 * short at once, and each of them is this technique applied.
	 *
	 * @param grid The working grid; never mutated
	 * @return The placements, in {@link CandidateGrid#allUnits()} order, one per cell
	 */
	@Override
	public List<ExplainedDeduction> findAllPlacements(CandidateGrid grid) {
		List<ExplainedDeduction> found = new ArrayList<>();
		boolean[] seen = new boolean[grid.cellCount()];
		for (int[] unit : grid.allUnits()) {
			int empty = 0;
			int target = -1;
			int used = 0;
			for (int cell : unit) {
				int value = grid.value(cell);
				if (value == 0) {
					empty++;
					target = cell;
				} else {
					used |= 1 << value;
				}
			}
			
			if (empty != 1 || seen[target]) {
				continue;
			}
			
			int missing = (((1 << grid.n()) - 1) << 1) & ~used;
			if (Integer.bitCount(missing) == 1) {
				// The same cell is the last empty of its row and of its region often enough; the argument is the same
				// one either way, so it is made once, in the first unit the scan met it in.
				seen[target] = true;
				found.add(this.explain(grid, new Deduction.Placement(Technique.FULL_HOUSE, target, Integer.numberOfTrailingZeros(missing))));
			}
		}
		return found;
	}
	
	/**
	 * Builds the argument for one full house: the unit that is one cell short, and every digit already in it.
	 *
	 * @param grid The working grid; never mutated
	 * @param deduction The placement to explain
	 * @return The placement and its explanation
	 */
	private ExplainedDeduction explain(CandidateGrid grid, Deduction deduction) {
		Deduction.Placement placement = (Deduction.Placement) deduction;
		int cell = placement.cell();
		// The unit the argument was made in, found the same way the scan found it: the first of the cell's three
		// units with exactly one empty cell. Rows are checked before columns before regions, matching allUnits().
		UnitRef unit = null;
		int[] unitCells = null;
		for (UnitRef candidate : Explanations.unitsOf(grid, cell)) {
			int[] cells = candidate.cells(grid);
			int empty = 0;
			for (int member : cells) {
				if (grid.isEmpty(member)) {
					empty++;
				}
			}
			if (empty == 1) {
				unit = candidate;
				unitCells = cells;
				break;
			}
		}
		
		return new ExplainedDeduction(deduction, Explanation.builder(Technique.FULL_HOUSE)
			.focusUnits(0, List.of(unit))
			.pattern(0, Explanations.filledOf(grid, unitCells))
			.conclusion(deduction)
			.build());
	}
}
