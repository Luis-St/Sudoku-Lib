package net.luis.sudoku.solver;

import java.util.*;

/**
 * The naked-pair technique: two cells of a unit sharing the identical two-candidate set eliminate those two digits
 * from the rest of the unit.
 * <p>
 *     If two cells of a unit can only hold the same two digits, those two digits are used up by that pair within the
 *     unit, so no other cell of the unit may hold either of them. The scan is deterministic — units in
 *     {@link CandidateGrid#allUnits()} order, cell pairs in ascending index order — and the first pair that removes at
 *     least one candidate is returned.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Technique#NAKED_PAIR
 */
public final class NakedPair implements TechniqueStrategy {
	
	/**
	 * Constructs the naked-pair strategy. The strategy is stateless and holds no grid.
	 */
	public NakedPair() {}
	
	static int[] toArray(List<Integer> list) {
		int[] array = new int[list.size()];
		for (int index = 0; index < array.length; index++) {
			array[index] = list.get(index);
		}
		return array;
	}
	
	@Override
	public Technique technique() {
		return Technique.NAKED_PAIR;
	}
	
	/**
	 * Scans every unit for two cells sharing an identical two-candidate set and returns the first elimination it
	 * enables in the rest of that unit.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first naked-pair elimination, or empty if the pattern makes no progress anywhere
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		for (int[] unit : grid.allUnits()) {
			for (int i = 0; i < unit.length; i++) {
				int first = unit[i];
				if (grid.candidateCount(first) != 2) {
					continue;
				}
				
				int mask = grid.candidates(first);
				for (int j = i + 1; j < unit.length; j++) {
					int second = unit[j];
					if (grid.candidates(second) != mask) {
						continue;
					}
					
					Optional<Deduction> elimination = this.eliminate(grid, unit, mask, first, second);
					if (elimination.isPresent()) {
						return elimination;
					}
				}
			}
		}
		return Optional.empty();
	}
	
	private Optional<Deduction> eliminate(CandidateGrid grid, int[] unit, int mask, int first, int second) {
		List<Integer> cells = new ArrayList<>();
		List<Integer> digits = new ArrayList<>();
		for (int cell : unit) {
			if (cell == first || cell == second) {
				continue;
			}
			
			for (int digit = 1; digit <= grid.n(); digit++) {
				if ((mask & (1 << digit)) != 0 && grid.hasCandidate(cell, digit)) {
					cells.add(cell);
					digits.add(digit);
				}
			}
		}
		if (cells.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new Deduction.Eliminations(Technique.NAKED_PAIR, toArray(cells), toArray(digits)));
	}
}
