package net.luis.sudoku.solver;

import java.util.*;

/**
 * The Death Blossom: a stem cell each of whose candidates opens onto an almost-locked set.
 * <p>
 *     Pick a cell — the stem — and give every one of its candidates its own petal: an almost-locked set for which that
 *     candidate is a restricted common with the stem. Whatever digit the stem finally takes, the petal belonging to
 *     that digit is deprived of it and collapses into a genuine locked set. Every other petal keeps all its options.
 *     So a digit listed by <i>every</i> petal is certainly used by one of them, and no cell outside the blossom that
 *     sees all of its occurrences can hold it.
 * </p>
 * <p>
 *     The blossom is the widest-branching technique the solver knows that is still a single deduction rather than a
 *     search, which is why it sits at the top level alongside the forcing chains. Stems of up to three candidates are
 *     searched, which keeps the number of petal combinations bounded.
 * </p>
 *
 * @see TechniqueStrategy
 * @see AlmostLockedSets
 * @see Technique#DEATH_BLOSSOM
 */
public final class DeathBlossom implements TechniqueStrategy {
	
	/**
	 * The widest stem the search considers. Every additional candidate multiplies the petal combinations by the
	 * number of sets that candidate can open onto.
	 */
	private static final int MAX_STEM_CANDIDATES = 3;
	
	/**
	 * Constructs the Death Blossom strategy. The strategy is stateless and holds no grid.
	 */
	public DeathBlossom() {}
	
	@Override
	public Technique technique() {
		return Technique.DEATH_BLOSSOM;
	}
	
	/**
	 * Scans every cell as a stem and tries to hang a petal on each of its candidates.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first Death Blossom elimination, or empty if no blossom makes progress
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		List<AlmostLockedSets.Als> sets = AlmostLockedSets.of(grid);
		for (int stem = 0; stem < grid.cellCount(); stem++) {
			int count = grid.candidateCount(stem);
			if (count < 2 || count > MAX_STEM_CANDIDATES) {
				continue;
			}
			
			int[] digits = grid.candidateDigits(stem);
			List<List<AlmostLockedSets.Als>> petals = this.petalsFor(grid, sets, stem, digits);
			if (petals == null) {
				continue;
			}
			
			Optional<Deduction> found = this.combine(grid, petals, new AlmostLockedSets.Als[digits.length], 0, stem);
			if (found.isPresent()) {
				return found;
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Collects, for each stem candidate, the sets that could serve as its petal, or null if any candidate has none.
	 */
	private List<List<AlmostLockedSets.Als>> petalsFor(CandidateGrid grid, List<AlmostLockedSets.Als> sets, int stem, int[] digits) {
		List<List<AlmostLockedSets.Als>> petals = new ArrayList<>(digits.length);
		for (int digit : digits) {
			List<AlmostLockedSets.Als> matching = new ArrayList<>();
			for (AlmostLockedSets.Als set : sets) {
				if (set.contains(stem) || (set.mask() & (1 << digit)) == 0) {
					continue;
				}
				if (this.isRestricted(grid, set, stem, digit)) {
					matching.add(set);
				}
			}
			
			if (matching.isEmpty()) {
				return null;
			}
			petals.add(matching);
		}
		return petals;
	}
	
	/**
	 * Checks that every occurrence of the digit in the set sees the stem, which is what lets the stem take the digit
	 * away from the set.
	 */
	private boolean isRestricted(CandidateGrid grid, AlmostLockedSets.Als set, int stem, int digit) {
		for (int cell : set.cells()) {
			if (grid.hasCandidate(cell, digit) && !grid.peers(cell, stem)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Picks one petal per stem candidate and tests the finished blossom.
	 */
	private Optional<Deduction> combine(CandidateGrid grid, List<List<AlmostLockedSets.Als>> petals, AlmostLockedSets.Als[] chosen, int depth, int stem) {
		if (depth == chosen.length) {
			return this.eliminate(grid, chosen, stem);
		}
		
		for (AlmostLockedSets.Als petal : petals.get(depth)) {
			boolean clash = false;
			for (int index = 0; index < depth; index++) {
				clash |= chosen[index].overlaps(petal);
			}
			if (clash) {
				continue;
			}
			
			chosen[depth] = petal;
			Optional<Deduction> found = this.combine(grid, petals, chosen, depth + 1, stem);
			if (found.isPresent()) {
				return found;
			}
		}
		return Optional.empty();
	}
	
	private Optional<Deduction> eliminate(CandidateGrid grid, AlmostLockedSets.Als[] petals, int stem) {
		int shared = -1;
		for (AlmostLockedSets.Als petal : petals) {
			shared &= petal.mask();
		}
		shared &= ~grid.candidates(stem);
		
		while (shared != 0) {
			int digit = Integer.numberOfTrailingZeros(shared);
			shared &= shared - 1;
			
			Optional<Deduction> found = AlmostLockedSets.eliminateSeenBy(grid, digit, Technique.DEATH_BLOSSOM, petals);
			if (found.isPresent() && !this.touchesStem(found.orElseThrow(), stem)) {
				return found;
			}
		}
		return Optional.empty();
	}
	
	private boolean touchesStem(Deduction deduction, int stem) {
		for (int cell : ((Deduction.Eliminations) deduction).cells()) {
			if (cell == stem) {
				return true;
			}
		}
		return false;
	}
}
