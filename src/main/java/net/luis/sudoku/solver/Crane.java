package net.luis.sudoku.solver;

import java.util.List;
import java.util.Optional;

/**
 * The Crane: the three-link single-digit chain.
 * <p>
 *     Three conjugate pairs of one digit are joined end to end, each junction being two cells that see each other. If
 *     the first chain end does not hold the digit, its partner does; that partner rules the digit out of the cell it
 *     sees in the second link, which forces that link's partner, and so on down the chain until the far end is forced
 *     to hold the digit. One of the two ends therefore always does, so no cell seeing both of them can.
 * </p>
 * <p>
 *     It is the same argument as the {@link Skyscraper} and the {@link TwoStringKite} one link longer, and one link is
 *     exactly what makes it noticeably harder to trace by eye. The unrestricted-length form of this chain is the
 *     {@link XChain}, several levels above.
 * </p>
 * <p>
 *     The scan is deterministic — digits ascending, then link triples in the order {@link ConjugateLinks} reports
 *     them — and the first chain that actually removes a candidate is returned.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Technique#CRANE
 */
public final class Crane implements TechniqueStrategy {
	
	/**
	 * Constructs the Crane strategy. The strategy is stateless and holds no grid.
	 */
	public Crane() {}
	
	@Override
	public Technique technique() {
		return Technique.CRANE;
	}
	
	/**
	 * Scans every ordered triple of conjugate pairs that can be chained together.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first Crane elimination, or empty if the pattern makes no progress anywhere
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		return this.scan(grid, null);
	}
	
	/**
	 * Explains the Crane as the three-link chain it is: the digit, the units its links live in, each link in turn, and
	 * the pair of ends one of which must therefore hold it.
	 *
	 * @param grid The working grid; never mutated
	 * @return The eliminations and their explanation, or empty if the pattern makes no progress anywhere
	 */
	@Override
	public Optional<ExplainedDeduction> findExplained(CandidateGrid grid) {
		Explanation.Builder builder = Explanation.builder(Technique.CRANE);
		return this.scan(grid, builder).map(deduction -> new ExplainedDeduction(deduction, builder.conclusion(deduction).build()));
	}
	
	private Optional<Deduction> scan(CandidateGrid grid, Explanation.Builder explanation) {
		for (int digit = 1; digit <= grid.n(); digit++) {
			List<int[]> links = ConjugateLinks.of(grid, digit);
			for (int[] first : links) {
				for (int[] second : links) {
					if (first == second) {
						continue;
					}
					
					for (int[] third : links) {
						if (third == first || third == second) {
							continue;
						}
						
						Optional<Deduction> found = this.test(grid, digit, first, second, third, explanation);
						if (found.isPresent()) {
							return found;
						}
					}
				}
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Tries every orientation of the three links and returns the first chain whose ends eliminate something.
	 */
	private Optional<Deduction> test(CandidateGrid grid, int digit, int[] first, int[] second, int[] third, Explanation.Builder explanation) {
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 2; j++) {
				for (int k = 0; k < 2; k++) {
					int startEnd = first[i];
					int startInner = first[1 - i];
					int middleLeft = second[j];
					int middleRight = second[1 - j];
					int finishInner = third[k];
					int finishEnd = third[1 - k];
					
					if (!this.distinct(startEnd, startInner, middleLeft, middleRight, finishInner, finishEnd)) {
						continue;
					}
					// The junctions are weak links: two cells that see each other cannot both hold the digit.
					if (!grid.peers(startInner, middleLeft) || !grid.peers(middleRight, finishInner)) {
						continue;
					}
					
					Optional<Deduction> found = ConjugateLinks.eliminateSeenByBoth(grid, digit, startEnd, finishEnd, Technique.CRANE, startInner, middleLeft, middleRight, finishInner);
					// Only a chain that removes something is the deduction being returned, so only that one is worth
					// explaining: any earlier one was looked at and rejected.
					if (found.isPresent()) {
						if (explanation != null) {
							Explanations.chain(grid, digit, new int[] { startEnd, startInner, middleLeft, middleRight, finishInner, finishEnd }, explanation);
						}
						return found;
					}
				}
			}
		}
		return Optional.empty();
	}
	
	private boolean distinct(int... cells) {
		for (int i = 0; i < cells.length; i++) {
			for (int j = i + 1; j < cells.length; j++) {
				if (cells[i] == cells[j]) {
					return false;
				}
			}
		}
		return true;
	}
}
