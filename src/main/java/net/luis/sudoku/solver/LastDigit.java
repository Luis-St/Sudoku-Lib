package net.luis.sudoku.solver;

import java.util.*;

/**
 * The last-digit technique: a digit already placed in every region but one has exactly one cell left in the grid that
 * can still hold it.
 * <p>
 *     Like the {@link FullHouse}, this is a counting argument rather than a candidate argument, which is why players
 *     find it immediately: once a digit is nearly used up, its final cell stands out without any cross-hatching. The
 *     technique fires only when the digit occupies {@code n - 1} regions, so the single remaining candidate cell is
 *     forced by the count and not merely by elimination — that stricter form is what keeps it apart from a
 *     {@link HiddenSingleRegion}.
 * </p>
 * <p>
 *     The scan is deterministic: digits ascending, and for each the grid is swept in row-major order.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Technique#LAST_DIGIT
 */
public final class LastDigit implements TechniqueStrategy {
	
	/**
	 * Constructs the last-digit strategy. The strategy is stateless and holds no grid.
	 */
	public LastDigit() {}
	
	@Override
	public Technique technique() {
		return Technique.LAST_DIGIT;
	}
	
	/**
	 * Scans every digit for the case of a single remaining placement in the whole grid and returns it.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first last digit, or empty if no digit is down to its final cell
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		int regionCount = grid.partition().regionCount();
		for (int digit = 1; digit <= grid.n(); digit++) {
			int placed = 0;
			int candidates = 0;
			int target = -1;
			for (int cell = 0; cell < grid.cellCount(); cell++) {
				if (grid.value(cell) == digit) {
					placed++;
				} else if (grid.hasCandidate(cell, digit)) {
					candidates++;
					target = cell;
				}
			}
			
			if (placed == regionCount - 1 && candidates == 1) {
				return Optional.of(new Deduction.Placement(Technique.LAST_DIGIT, target, digit));
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Explains the last digit by showing every cell that already holds it, which is the count the argument rests on.
	 *
	 * @param grid The working grid; never mutated
	 * @return The placement and its explanation, or empty if no digit is down to its final cell
	 */
	@Override
	public Optional<ExplainedDeduction> findExplained(CandidateGrid grid) {
		return this.find(grid).map(deduction -> {
			Deduction.Placement placement = (Deduction.Placement) deduction;
			int digit = placement.digit();
			List<PatternCell> placedCells = new ArrayList<>();
			for (int cell = 0; cell < grid.cellCount(); cell++) {
				if (grid.value(cell) == digit) {
					placedCells.add(PatternCell.of(cell, CellRole.CONTEXT, digit));
				}
			}
			
			return new ExplainedDeduction(deduction, Explanation.builder(Technique.LAST_DIGIT)
				.focusDigit(digit)
				.pattern(digit, placedCells)
				.conclusion(deduction)
				.build());
		});
	}
}
