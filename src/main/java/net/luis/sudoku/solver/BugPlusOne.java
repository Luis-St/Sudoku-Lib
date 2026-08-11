package net.luis.sudoku.solver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The BUG+1 technique: a grid one candidate away from the bi-value universal grave must place that extra candidate.
 * <p>
 *     A grid in which every unsolved cell has exactly two candidates, and every digit appears exactly twice in every
 *     unit, has no unique solution — the two possibilities can always be swapped, so it is a "grave" no valid puzzle
 *     can be in. If the grid is one candidate away from that state, with a single cell holding three candidates and
 *     everything else paired off, then that extra candidate is precisely what saves the puzzle from ambiguity: it must
 *     be the digit that cell holds.
 * </p>
 * <p>
 *     The argument rests on the puzzle having exactly one solution, which every puzzle this library generates does.
 *     Like the unique-rectangle techniques it is therefore a uniqueness argument rather than a logical one, and it is
 *     rated as a mid-level technique because spotting the near-grave takes a whole-grid look rather than a local one.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Technique#BUG_PLUS_ONE
 */
public final class BugPlusOne implements TechniqueStrategy {
	
	/**
	 * Constructs the BUG+1 strategy. The strategy is stateless and holds no grid.
	 */
	public BugPlusOne() {}
	
	@Override
	public Technique technique() {
		return Technique.BUG_PLUS_ONE;
	}
	
	/**
	 * Checks whether the grid is one candidate short of the bi-value universal grave and, if so, returns the forced
	 * placement in the single three-candidate cell.
	 *
	 * @param grid The working grid; never mutated
	 * @return The forced placement, or empty if the grid is not in the BUG+1 shape
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		int extraCell = -1;
		for (int cell = 0; cell < grid.cellCount(); cell++) {
			if (!grid.isEmpty(cell)) {
				continue;
			}
			
			int count = grid.candidateCount(cell);
			if (count == 2) {
				continue;
			}
			if (count != 3 || extraCell != -1) {
				return Optional.empty();
			}
			extraCell = cell;
		}
		
		if (extraCell == -1) {
			return Optional.empty();
		}
		
		int target = extraCell;
		return this.oddDigit(grid, target).map(digit -> new Deduction.Placement(Technique.BUG_PLUS_ONE, target, digit));
	}
	
	/**
	 * Explains the grave by showing the extra cell against the sea of two-candidate cells around it, which is the
	 * observation the whole argument rests on.
	 *
	 * @param grid The working grid; never mutated
	 * @return The placement and its explanation, or empty if the grid is not in the BUG+1 shape
	 */
	@Override
	public Optional<ExplainedDeduction> findExplained(CandidateGrid grid) {
		return this.find(grid).map(deduction -> {
			Deduction.Placement placement = (Deduction.Placement) deduction;
			List<PatternCell> biValue = new ArrayList<>();
			for (int cell = 0; cell < grid.cellCount(); cell++) {
				if (grid.isEmpty(cell) && cell != placement.cell()) {
					biValue.add(new PatternCell(cell, CellRole.CONTEXT, grid.candidates(cell)));
				}
			}
			
			return new ExplainedDeduction(deduction, Explanation.builder(Technique.BUG_PLUS_ONE)
				.pattern(0, biValue)
				.implication(placement.digit(), List.of(new PatternCell(placement.cell(), CellRole.PATTERN, grid.candidates(placement.cell()))))
				.conclusion(deduction)
				.build());
		});
	}
	
	/**
	 * Returns the digit the grave argument forces into the extra cell, after checking that the grid really is one
	 * candidate short of a bi-value universal grave.
	 * <p>
	 *     The check has to cover <b>every</b> unit of the grid, not merely the three the extra cell sits in. Bi-value
	 *     cells alone do not make a near-grave: the grave state also requires each digit to appear exactly twice, or
	 *     not at all, in every single unit. A grid where some distant unit carries a digit three times is not one step
	 *     from a grave, the swap argument does not apply to it, and concluding a placement there is simply wrong.
	 * </p>
	 * <p>
	 *     So every unit must be paired off, except that the three units containing the extra cell each carry exactly
	 *     one three-fold digit, and it must be the <i>same</i> digit in all three, and a candidate of the extra cell.
	 *     That digit is the one the grave cannot survive without.
	 * </p>
	 */
	private Optional<Integer> oddDigit(CandidateGrid grid, int extraCell) {
		int saving = 0;
		for (int[] unit : grid.allUnits()) {
			boolean holdsExtraCell = this.contains(unit, extraCell);
			int threeFold = 0;
			for (int digit = 1; digit <= grid.n(); digit++) {
				int count = this.count(grid, unit, digit);
				if (count == 0 || count == 2) {
					continue;
				}
				// Anything other than a pair is only allowed as the single three-fold digit of the extra cell's own
				// units; any other count means this is not a near-grave.
				if (count != 3 || !holdsExtraCell || threeFold != 0) {
					return Optional.empty();
				}
				threeFold = digit;
			}
			
			if (!holdsExtraCell) {
				continue;
			}
			if (threeFold == 0 || (saving != 0 && saving != threeFold)) {
				return Optional.empty();
			}
			saving = threeFold;
		}
		return saving != 0 && grid.hasCandidate(extraCell, saving) ? Optional.of(saving) : Optional.empty();
	}
	
	private int count(CandidateGrid grid, int[] unit, int digit) {
		int count = 0;
		for (int cell : unit) {
			if (grid.hasCandidate(cell, digit)) {
				count++;
			}
		}
		return count;
	}
	
	private boolean contains(int[] unit, int cell) {
		for (int member : unit) {
			if (member == cell) {
				return true;
			}
		}
		return false;
	}
}
