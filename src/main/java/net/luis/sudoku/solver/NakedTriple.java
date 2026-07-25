package net.luis.sudoku.solver;

import java.util.*;

/**
 * The naked-triple technique: three cells of a unit confined to a common three-candidate set eliminate those three
 * digits from the rest of the unit.
 * <p>
 *     Three cells whose candidates are all drawn from the same three digits use those three digits up within the unit,
 *     even when no single cell holds all three. Each of the three cells has two or three of the digits and their union
 *     is exactly three. The scan is deterministic — units in {@link CandidateGrid#allUnits()} order, cell triples in
 *     ascending index order — and the first triple that removes at least one candidate is returned.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Technique#NAKED_TRIPLE
 */
public final class NakedTriple implements TechniqueStrategy {
	
	/**
	 * Constructs the naked-triple strategy. The strategy is stateless and holds no grid.
	 */
	public NakedTriple() {}
	
	@Override
	public Technique technique() {
		return Technique.NAKED_TRIPLE;
	}
	
	/**
	 * Scans every unit for three cells whose candidates share a common three-digit set and returns the first
	 * elimination it enables in the rest of that unit.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first naked-triple elimination, or empty if the pattern makes no progress anywhere
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		for (int[] unit : grid.allUnits()) {
			for (int i = 0; i < unit.length; i++) {
				if (!this.eligible(grid, unit[i])) {
					continue;
				}
				for (int j = i + 1; j < unit.length; j++) {
					if (!this.eligible(grid, unit[j])) {
						continue;
					}
					for (int k = j + 1; k < unit.length; k++) {
						if (!this.eligible(grid, unit[k])) {
							continue;
						}
						int union = grid.candidates(unit[i]) | grid.candidates(unit[j]) | grid.candidates(unit[k]);
						if (Integer.bitCount(union) != 3) {
							continue;
						}
						Optional<Deduction> elimination = this.eliminate(grid, unit, union, unit[i], unit[j], unit[k]);
						if (elimination.isPresent()) {
							return elimination;
						}
					}
				}
			}
		}
		return Optional.empty();
	}
	
	private boolean eligible(CandidateGrid grid, int cell) {
		int count = grid.candidateCount(cell);
		return count == 2 || count == 3;
	}
	
	private Optional<Deduction> eliminate(CandidateGrid grid, int[] unit, int union, int first, int second, int third) {
		List<Integer> cells = new ArrayList<>();
		List<Integer> digits = new ArrayList<>();
		for (int cell : unit) {
			if (cell == first || cell == second || cell == third) {
				continue;
			}
			for (int digit = 1; digit <= grid.n(); digit++) {
				if ((union & (1 << digit)) != 0 && grid.hasCandidate(cell, digit)) {
					cells.add(cell);
					digits.add(digit);
				}
			}
		}
		if (cells.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new Deduction.Eliminations(Technique.NAKED_TRIPLE, NakedPair.toArray(cells), NakedPair.toArray(digits)));
	}
}
