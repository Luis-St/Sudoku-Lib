package net.luis.sudoku.solver;

import java.util.*;

/**
 * The hidden-triple technique: three digits confined to the same three cells of a unit eliminate every other candidate
 * from those three cells.
 * <p>
 *     When three digits can each only appear within the same three cells of a unit, those three cells must hold exactly
 *     those three digits, so any other candidate in them is impossible — even though no single one of the three digits
 *     need occupy all three cells. The scan is deterministic — units in {@link CandidateGrid#allUnits()} order, digit
 *     triples in ascending order — and the first hidden triple that removes at least one candidate is returned.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Technique#HIDDEN_TRIPLE
 */
public final class HiddenTriple implements TechniqueStrategy {
	
	/**
	 * Constructs the hidden-triple strategy. The strategy is stateless and holds no grid.
	 */
	public HiddenTriple() {}
	
	@Override
	public Technique technique() {
		return Technique.HIDDEN_TRIPLE;
	}
	
	/**
	 * Scans every unit for three digits confined to the same three cells and returns the first elimination of the
	 * other candidates in those cells.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first hidden-triple elimination, or empty if the pattern makes no progress anywhere
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		int n = grid.n();
		for (int[] unit : grid.allUnits()) {
			for (int a = 1; a <= n; a++) {
				int maskA = this.cellsWith(grid, unit, a);
				if (maskA == 0) {
					continue;
				}
				
				for (int b = a + 1; b <= n; b++) {
					int maskB = this.cellsWith(grid, unit, b);
					if (maskB == 0) {
						continue;
					}
					
					for (int c = b + 1; c <= n; c++) {
						int maskC = this.cellsWith(grid, unit, c);
						if (maskC == 0) {
							continue;
						}
						
						int union = maskA | maskB | maskC;
						if (Integer.bitCount(union) != 3) {
							continue;
						}
						
						int keep = (1 << a) | (1 << b) | (1 << c);
						Optional<Deduction> elimination = this.eliminate(grid, unit, union, keep);
						if (elimination.isPresent()) {
							return elimination;
						}
					}
				}
			}
		}
		return Optional.empty();
	}
	
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
		return Optional.of(new Deduction.Eliminations(Technique.HIDDEN_TRIPLE, NakedPair.toArray(cells), NakedPair.toArray(digits)));
	}
}
