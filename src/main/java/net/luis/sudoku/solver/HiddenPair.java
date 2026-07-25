package net.luis.sudoku.solver;

import java.util.*;

/**
 * The hidden-pair technique: two digits confined to the same two cells of a unit eliminate every other candidate from
 * those two cells.
 * <p>
 *     When two digits can each only go into the same two cells of a unit, those two cells must hold exactly those two
 *     digits, so any other candidate in them is impossible. The scan is deterministic — units in
 *     {@link CandidateGrid#allUnits()} order, digit pairs in ascending order — and the first hidden pair that removes at
 *     least one candidate is returned.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Technique#HIDDEN_PAIR
 */
public final class HiddenPair implements TechniqueStrategy {
	
	/**
	 * Constructs the hidden-pair strategy. The strategy is stateless and holds no grid.
	 */
	public HiddenPair() {}
	
	@Override
	public Technique technique() {
		return Technique.HIDDEN_PAIR;
	}
	
	/**
	 * Scans every unit for two digits confined to the same two cells and returns the first elimination of the other
	 * candidates in those cells.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first hidden-pair elimination, or empty if the pattern makes no progress anywhere
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		int n = grid.n();
		for (int[] unit : grid.allUnits()) {
			for (int a = 1; a <= n; a++) {
				int maskA = this.cellsWith(grid, unit, a);
				if (Integer.bitCount(maskA) != 2) {
					continue;
				}
				for (int b = a + 1; b <= n; b++) {
					if (this.cellsWith(grid, unit, b) != maskA) {
						continue;
					}
					int keep = (1 << a) | (1 << b);
					Optional<Deduction> elimination = this.eliminate(grid, unit, maskA, keep);
					if (elimination.isPresent()) {
						return elimination;
					}
				}
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Returns a bitmask over unit positions of the cells that carry the given digit as a candidate.
	 */
	private int cellsWith(CandidateGrid grid, int[] unit, int digit) {
		int mask = 0;
		for (int position = 0; position < unit.length; position++) {
			if (grid.hasCandidate(unit[position], digit)) {
				mask |= 1 << position;
			}
		}
		return mask;
	}
	
	private Optional<Deduction> eliminate(CandidateGrid grid, int[] unit, int positionMask, int keep) {
		List<Integer> cells = new ArrayList<>();
		List<Integer> digits = new ArrayList<>();
		int remaining = positionMask;
		while (remaining != 0) {
			int position = Integer.numberOfTrailingZeros(remaining);
			remaining &= remaining - 1;
			int cell = unit[position];
			for (int digit = 1; digit <= grid.n(); digit++) {
				if ((keep & (1 << digit)) == 0 && grid.hasCandidate(cell, digit)) {
					cells.add(cell);
					digits.add(digit);
				}
			}
		}
		if (cells.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new Deduction.Eliminations(Technique.HIDDEN_PAIR, NakedPair.toArray(cells), NakedPair.toArray(digits)));
	}
}
