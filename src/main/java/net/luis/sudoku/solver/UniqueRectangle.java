package net.luis.sudoku.solver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The shared rectangle hunt behind the four unique-rectangle techniques.
 * <p>
 *     Four cells standing on two rows, two columns and exactly two regions, every one of them holding the same two
 *     candidates {@code {a, b}} and nothing else, are a <b>deadly pattern</b>: the two ways of filling them are both
 *     consistent, so a puzzle containing that configuration would have two solutions. Every puzzle this library
 *     generates has exactly one, so the configuration cannot occur — and whatever candidate would have to be removed
 *     to create it must therefore stay, while whatever would complete it must go.
 * </p>
 * <p>
 *     The four types differ only in how far the rectangle already is from the deadly pattern and what that leaves to
 *     conclude:
 * </p>
 * <ul>
 *     <li>{@link UniqueRectangle1} — three corners are bare, so the fourth must use one of its extra candidates.</li>
 *     <li>{@link UniqueRectangle2} — both roof corners carry the same single extra, which must be used somewhere.</li>
 *     <li>{@link UniqueRectangle3} — the roof's extras act as one virtual cell in a naked subset.</li>
 *     <li>{@link UniqueRectangle4} — a pair digit is locked to the roof, so the other one cannot sit there.</li>
 * </ul>
 * <p>
 *     Because these are uniqueness arguments rather than logical ones, they only hold for a properly formed puzzle.
 *     The scan is deterministic — row pairs, then column pairs, then digit pairs, all ascending — and the first
 *     rectangle that removes at least one candidate is returned.
 * </p>
 *
 * @see BugPlusOne
 */
abstract sealed class UniqueRectangle implements TechniqueStrategy permits UniqueRectangle1, UniqueRectangle2, UniqueRectangle3, UniqueRectangle4 {
	
	private final Technique technique;
	
	/**
	 * Constructs a unique-rectangle strategy.
	 *
	 * @param technique The technique to attribute the eliminations to
	 */
	UniqueRectangle(Technique technique) {
		this.technique = technique;
	}
	
	@Override
	public final Technique technique() {
		return this.technique;
	}
	
	/**
	 * Enumerates every candidate rectangle and digit pair and hands each to the concrete type's test.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first elimination this unique-rectangle type proves, or empty
	 */
	@Override
	public final Optional<Deduction> find(CandidateGrid grid) {
		return this.scan(grid, null);
	}

	/**
	 * Explains the rectangle: the four corners and the pair that would make them deadly, followed by whatever the
	 * concrete type concludes from how far this rectangle already is from that.
	 *
	 * @param grid The working grid; never mutated
	 * @return The eliminations and their explanation, or empty if this type proves nothing anywhere
	 */
	@Override
	public final Optional<ExplainedDeduction> findExplained(CandidateGrid grid) {
		Explanation.Builder builder = Explanation.builder(this.technique);
		return this.scan(grid, builder).map(deduction -> new ExplainedDeduction(deduction, builder.conclusion(deduction).build()));
	}

	private Optional<Deduction> scan(CandidateGrid grid, Explanation.Builder explanation) {
		int n = grid.n();
		for (int topRow = 0; topRow < n; topRow++) {
			for (int bottomRow = topRow + 1; bottomRow < n; bottomRow++) {
				for (int leftColumn = 0; leftColumn < n; leftColumn++) {
					for (int rightColumn = leftColumn + 1; rightColumn < n; rightColumn++) {
						int[] corners = {
							topRow * n + leftColumn,
							topRow * n + rightColumn,
							bottomRow * n + leftColumn,
							bottomRow * n + rightColumn
						};
						
						if (!this.spansTwoRegions(grid, corners)) {
							continue;
						}
						
						Optional<Deduction> found = this.scanPairs(grid, corners, explanation);
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
	 * Checks that the four corners lie in exactly two regions, two corners each. Without that the two fillings are
	 * not interchangeable and the uniqueness argument does not apply.
	 */
	private boolean spansTwoRegions(CandidateGrid grid, int[] corners) {
		int first = grid.regionOf(corners[0]);
		int second = -1;
		int firstCount = 0;
		for (int corner : corners) {
			int region = grid.regionOf(corner);
			if (region == first) {
				firstCount++;
			} else if (second == -1) {
				second = region;
			} else if (region != second) {
				return false;
			}
		}
		return second != -1 && firstCount == 2;
	}
	
	private Optional<Deduction> scanPairs(CandidateGrid grid, int[] corners, Explanation.Builder explanation) {
		for (int corner : corners) {
			if (!grid.isEmpty(corner)) {
				return Optional.empty();
			}
		}
		
		int shared = grid.candidates(corners[0]) & grid.candidates(corners[1]) & grid.candidates(corners[2]) & grid.candidates(corners[3]);
		for (int a = 1; a <= grid.n(); a++) {
			if ((shared & (1 << a)) == 0) {
				continue;
			}
			
			for (int b = a + 1; b <= grid.n(); b++) {
				if ((shared & (1 << b)) == 0) {
					continue;
				}
				
				Optional<Deduction> found = this.test(grid, corners, (1 << a) | (1 << b), explanation);
				if (found.isPresent()) {
					return found;
				}
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Tests one rectangle against one digit pair.
	 *
	 * @param grid The working grid
	 * @param corners The four corners, ordered top-left, top-right, bottom-left, bottom-right
	 * @param pair The bitmask of the two shared digits
	 * @param explanation The explanation to record the pattern into, or null to skip recording entirely
	 * @return The elimination this type proves for the rectangle, or empty
	 */
	abstract Optional<Deduction> test(CandidateGrid grid, int[] corners, int pair, Explanation.Builder explanation);

	/**
	 * Records the beats every type shares: the two rows and two columns the rectangle stands on, and its four corners
	 * split into the floor that is bare and the roof that carries the extra candidates.
	 * <p>
	 *     A type 1 rectangle has three floor corners and one roof corner, the others have two of each, so the split is
	 *     read off the candidates rather than assumed.
	 * </p>
	 *
	 * @param grid The working grid
	 * @param corners The four corners
	 * @param pair The bitmask of the two shared digits
	 * @param explanation The explanation to record into
	 */
	final void explainRectangle(CandidateGrid grid, int[] corners, int pair, Explanation.Builder explanation) {
		List<PatternCell> cells = new ArrayList<>(corners.length);
		for (int corner : corners) {
			boolean bare = (grid.candidates(corner) & ~pair) == 0;
			cells.add(new PatternCell(corner, bare ? CellRole.FLOOR : CellRole.ROOF, grid.candidates(corner)));
		}
		
		explanation.focusUnits(0, List.of(
			UnitRef.row(grid.rowOf(corners[0])), UnitRef.row(grid.rowOf(corners[2])),
			UnitRef.column(grid.columnOf(corners[0])), UnitRef.column(grid.columnOf(corners[1]))
		)).pattern(0, cells);
	}
	
	/**
	 * Returns the two corners that carry candidates beyond the pair, or null unless there are exactly two of them and
	 * they share a row or a column.
	 * <p>
	 *     Types 2 to 4 all need that shape: the two bare corners are the floor the deadly pattern would rest on, and
	 *     the two others form the roof, which has to lie in a common unit for the type's argument to run.
	 * </p>
	 */
	final int[] roofOf(CandidateGrid grid, int[] corners, int pair) {
		int firstRoof = -1;
		int secondRoof = -1;
		for (int corner : corners) {
			if ((grid.candidates(corner) & ~pair) == 0) {
				continue;
			}
			
			if (firstRoof == -1) {
				firstRoof = corner;
			} else if (secondRoof == -1) {
				secondRoof = corner;
			} else {
				return null;
			}
		}
		
		if (secondRoof == -1) {
			return null;
		}
		boolean aligned = grid.rowOf(firstRoof) == grid.rowOf(secondRoof) || grid.columnOf(firstRoof) == grid.columnOf(secondRoof);
		return aligned ? new int[] { firstRoof, secondRoof } : null;
	}
}
