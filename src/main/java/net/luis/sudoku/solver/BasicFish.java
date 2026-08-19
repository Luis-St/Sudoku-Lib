package net.luis.sudoku.solver;

import java.util.*;

/**
 * The shared basic-fish scan behind the X-Wing, Swordfish and Jellyfish and their finned and sashimi forms.
 * <p>
 *     A fish argues about one digit at a time. Pick {@code k} base lines — rows, say — in which the digit's candidates
 *     are confined to the same {@code k} columns. Each of those rows must place the digit in one of those columns, and
 *     there are as many rows as columns, so the {@code k} columns are used up by the {@code k} rows: the digit can be
 *     removed from those columns in every other row. Swapping the roles of rows and columns gives the mirror form.
 *     {@code k} of two, three and four are the X-Wing, the Swordfish and the Jellyfish.
 * </p>
 * <p>
 *     <b>Fins.</b> If the base lines carry a few extra candidates outside the cover columns, the argument survives in
 *     weakened form: either the fish holds, or the digit sits on one of the extra candidates. When every one of those
 *     extras — the fins — lies in a single region, both cases put the digit inside that region, so the eliminations
 *     still hold for cover cells inside the fin's region. A finned fish whose base lines all keep at least two
 *     candidates inside the cover is a plain <i>finned</i> fish; one where a base line is down to a single cover
 *     candidate is a <i>sashimi</i> fish, which is harder to see because the fish it is built on is incomplete.
 * </p>
 * <p>
 *     Fins are searched for across at most {@value #MAX_FIN_LINES} cover lines. That bound is deliberate: a fish
 *     whose fins are spread wider than that is not something a player finds, and enumerating those covers would cost
 *     far more than the puzzles it would rate differently are worth.
 * </p>
 * <p>
 *     The scan is deterministic — digits ascending, the row form before the column form, base-line combinations in
 *     ascending order — and the first fish that actually removes a candidate is returned.
 * </p>
 *
 * @see XWing
 * @see Swordfish
 * @see Jellyfish
 */
abstract sealed class BasicFish implements TechniqueStrategy permits XWing, Swordfish, Jellyfish, FinnedXWing, FinnedSwordfish, SashimiSwordfish {
	
	/**
	 * The widest fin the scan considers, measured in cover lines the fins stick out into.
	 */
	static final int MAX_FIN_LINES = 2;
	
	private final Technique technique;
	private final int fishSize;
	private final FinMode finMode;
	
	/**
	 * Constructs a fish strategy.
	 *
	 * @param technique The technique to attribute the eliminations to
	 * @param fishSize The number of base lines, {@code 2} for an X-Wing through {@code 4} for a Jellyfish
	 * @param finMode Which of the finless, finned and sashimi forms this strategy accepts
	 */
	BasicFish(Technique technique, int fishSize, FinMode finMode) {
		this.technique = technique;
		this.fishSize = fishSize;
		this.finMode = finMode;
	}
	
	@Override
	public final Technique technique() {
		return this.technique;
	}
	
	/**
	 * Scans for a fish of this strategy's size and fin form, row form before column form.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first fish elimination, or empty if no fish makes progress
	 */
	@Override
	public final Optional<Deduction> find(CandidateGrid grid) {
		Optional<Deduction> rows = this.scan(grid, true, null);
		if (rows.isPresent()) {
			return rows;
		}
		return this.scan(grid, false, null);
	}
	
	/**
	 * Explains the fish by showing the digit, the base lines it is confined to, the cover lines it therefore fills,
	 * and any fin that spoils the exact pattern without spoiling the conclusion.
	 *
	 * @param grid The working grid; never mutated
	 * @return The eliminations and their explanation, or empty if no fish makes progress
	 */
	@Override
	public final Optional<ExplainedDeduction> findExplained(CandidateGrid grid) {
		Explanation.Builder builder = Explanation.builder(this.technique);
		Optional<Deduction> rows = this.scan(grid, true, builder);
		Optional<Deduction> found = rows.isPresent() ? rows : this.scan(grid, false, builder);
		return found.map(deduction -> new ExplainedDeduction(deduction, builder.conclusion(deduction).build()));
	}
	
	/**
	 * Scans one orientation. When {@code rowForm} is true the base sets are rows and the cover sets are columns;
	 * otherwise the roles are swapped.
	 */
	private Optional<Deduction> scan(CandidateGrid grid, boolean rowForm, Explanation.Builder explanation) {
		int n = grid.n();
		int[] lineMasks = new int[n];
		for (int digit = 1; digit <= n; digit++) {
			for (int line = 0; line < n; line++) {
				lineMasks[line] = this.lineMask(grid, rowForm, line, digit);
			}
			
			int[] base = new int[this.fishSize];
			Optional<Deduction> found = this.search(grid, rowForm, digit, lineMasks, base, 0, 0, 0, explanation);
			if (found.isPresent()) {
				return found;
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Enumerates base-line combinations in ascending order and tests each complete one.
	 */
	private Optional<Deduction> search(CandidateGrid grid, boolean rowForm, int digit, int[] lineMasks, int[] base, int depth, int start, int union, Explanation.Builder explanation) {
		if (depth == this.fishSize) {
			return this.test(grid, rowForm, digit, lineMasks, base, union, explanation);
		}
		
		int limit = this.fishSize + (this.finMode == FinMode.NONE ? 0 : MAX_FIN_LINES);
		if (Integer.bitCount(union) > limit) {
			return Optional.empty();
		}
		
		for (int line = start; line <= lineMasks.length - (this.fishSize - depth); line++) {
			// A line with a single candidate is a hidden single and is handled far below this rank; a line with
			// none does not constrain anything.
			if (Integer.bitCount(lineMasks[line]) < 2) {
				continue;
			}
			
			base[depth] = line;
			Optional<Deduction> found = this.search(grid, rowForm, digit, lineMasks, base, depth + 1, line + 1, union | lineMasks[line], explanation);
			if (found.isPresent()) {
				return found;
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Tests one complete base-line combination for this strategy's fin form.
	 */
	private Optional<Deduction> test(CandidateGrid grid, boolean rowForm, int digit, int[] lineMasks, int[] base, int union, Explanation.Builder explanation) {
		int extra = Integer.bitCount(union) - this.fishSize;
		if (this.finMode == FinMode.NONE) {
			return extra == 0 ? this.eliminate(grid, rowForm, digit, base, union, -1, 0, explanation) : Optional.empty();
		}
		if (extra < 1 || extra > MAX_FIN_LINES) {
			return Optional.empty();
		}
		return this.searchFins(grid, rowForm, digit, lineMasks, base, union, extra, explanation);
	}
	
	/**
	 * Enumerates which cover lines the fins stick out into, and tests whether those fins share a single region.
	 */
	private Optional<Deduction> searchFins(CandidateGrid grid, boolean rowForm, int digit, int[] lineMasks, int[] base, int union, int finCount, Explanation.Builder explanation) {
		int[] positions = this.bits(union);
		int[] chosen = new int[finCount];
		return this.searchFinPositions(grid, rowForm, digit, lineMasks, base, union, positions, chosen, 0, 0, explanation);
	}
	
	private Optional<Deduction> searchFinPositions(CandidateGrid grid, boolean rowForm, int digit, int[] lineMasks, int[] base, int union, int[] positions, int[] chosen, int depth, int start, Explanation.Builder explanation) {
		if (depth == chosen.length) {
			int finMask = 0;
			for (int position : chosen) {
				finMask |= 1 << position;
			}
			return this.testFinned(grid, rowForm, digit, lineMasks, base, union & ~finMask, finMask, explanation);
		}
		
		for (int index = start; index <= positions.length - (chosen.length - depth); index++) {
			chosen[depth] = positions[index];
			Optional<Deduction> found = this.searchFinPositions(grid, rowForm, digit, lineMasks, base, union, positions, chosen, depth + 1, index + 1, explanation);
			if (found.isPresent()) {
				return found;
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Tests one split of the union into a cover set and a fin set: the fins must all share a region, every base line
	 * must keep at least one candidate in the cover, and the sashimi form must match what this strategy accepts.
	 */
	private Optional<Deduction> testFinned(CandidateGrid grid, boolean rowForm, int digit, int[] lineMasks, int[] base, int cover, int finMask, Explanation.Builder explanation) {
		int finRegion = -1;
		boolean sashimi = false;
		for (int line : base) {
			int inCover = Integer.bitCount(lineMasks[line] & cover);
			if (inCover == 0) {
				// Every candidate of this base line would be a fin, which leaves nothing for the fish to cover.
				return Optional.empty();
			}
			sashimi |= inCover == 1;
			
			int fins = lineMasks[line] & finMask;
			while (fins != 0) {
				int position = Integer.numberOfTrailingZeros(fins);
				fins &= fins - 1;
				int region = grid.regionOf(this.cellAt(grid, rowForm, line, position));
				if (finRegion == -1) {
					finRegion = region;
				} else if (finRegion != region) {
					return Optional.empty();
				}
			}
		}
		
		if (finRegion == -1) {
			return Optional.empty();
		}
		if (this.finMode != FinMode.ANY_FINNED && sashimi != (this.finMode == FinMode.SASHIMI)) {
			return Optional.empty();
		}
		return this.eliminate(grid, rowForm, digit, base, cover, finRegion, finMask, explanation);
	}
	
	/**
	 * Removes the digit from the cover cells outside the base lines, restricted to the fin's region for a finned fish.
	 *
	 * @param finRegion The region every fin lies in, or {@code -1} for a finless fish
	 * @param finMask The cover positions the fins stick out into, or {@code 0} for a finless fish
	 * @param explanation The explanation to record the pattern into, or null
	 */
	private Optional<Deduction> eliminate(CandidateGrid grid, boolean rowForm, int digit, int[] base, int cover, int finRegion, int finMask, Explanation.Builder explanation) {
		EliminationBuilder builder = new EliminationBuilder();
		int remaining = cover;
		while (remaining != 0) {
			int position = Integer.numberOfTrailingZeros(remaining);
			remaining &= remaining - 1;
			
			int[] coverCells = rowForm ? grid.columnCells(position) : grid.rowCells(position);
			for (int cell : coverCells) {
				int line = rowForm ? grid.rowOf(cell) : grid.columnOf(cell);
				if (this.contains(base, line) || (finRegion != -1 && grid.regionOf(cell) != finRegion)) {
					continue;
				}
				
				builder.add(grid, cell, digit);
			}
		}
		
		builder.sortByCell();
		Optional<Deduction> deduction = builder.build(this.technique);
		// Only a fish that removes something is the deduction being returned; explaining any other would describe a
		// pattern the solver looked at and rejected.
		if (deduction.isPresent() && explanation != null) {
			this.explain(grid, rowForm, digit, base, cover, finMask, explanation);
		}
		return deduction;
	}
	
	/**
	 * Records the fish: the digit, the base lines that confine it, the candidates on them, the cover lines those
	 * candidates fill, and the fins if there are any.
	 *
	 * @param grid The working grid
	 * @param rowForm True if the base sets are rows
	 * @param digit The digit the fish is about
	 * @param base The base lines
	 * @param cover The cover positions
	 * @param finMask The cover positions the fins stick out into, {@code 0} if finless
	 * @param explanation The explanation to record into
	 */
	private void explain(CandidateGrid grid, boolean rowForm, int digit, int[] base, int cover, int finMask, Explanation.Builder explanation) {
		List<UnitRef> baseUnits = new ArrayList<>(base.length);
		for (int line : base) {
			baseUnits.add(rowForm ? UnitRef.row(line) : UnitRef.column(line));
		}
		
		List<UnitRef> coverUnits = new ArrayList<>();
		for (int position : this.bits(cover)) {
			coverUnits.add(rowForm ? UnitRef.column(position) : UnitRef.row(position));
		}
		
		List<PatternCell> corners = new ArrayList<>();
		List<PatternCell> fins = new ArrayList<>();
		for (int line : base) {
			int[] lineCells = rowForm ? grid.rowCells(line) : grid.columnCells(line);
			for (int cell : lineCells) {
				if (!grid.hasCandidate(cell, digit)) {
					continue;
				}
				
				int position = rowForm ? grid.columnOf(cell) : grid.rowOf(cell);
				if ((finMask & (1 << position)) != 0) {
					fins.add(PatternCell.of(cell, CellRole.FIN, digit));
				} else if ((cover & (1 << position)) != 0) {
					corners.add(PatternCell.of(cell, CellRole.BASE, digit));
				}
			}
		}
		
		explanation.focusDigit(digit)
			.focusUnits(digit, baseUnits)
			.pattern(digit, corners)
			.focusUnits(digit, coverUnits);
		// A fin is what separates the finned and sashimi fish from the plain one, so it gets its own beat rather than
		// being folded into the pattern: the conclusion is narrower precisely because of it.
		if (!fins.isEmpty()) {
			explanation.implication(digit, fins);
		}
	}
	
	private int lineMask(CandidateGrid grid, boolean rowForm, int line, int digit) {
		int mask = 0;
		int[] lineCells = rowForm ? grid.rowCells(line) : grid.columnCells(line);
		for (int cell : lineCells) {
			if (grid.hasCandidate(cell, digit)) {
				mask |= 1 << (rowForm ? grid.columnOf(cell) : grid.rowOf(cell));
			}
		}
		return mask;
	}
	
	private int cellAt(CandidateGrid grid, boolean rowForm, int line, int position) {
		return rowForm ? grid.rowCells(line)[position] : grid.columnCells(line)[position];
	}
	
	private int[] bits(int mask) {
		int[] positions = new int[Integer.bitCount(mask)];
		int index = 0;
		int remaining = mask;
		while (remaining != 0) {
			positions[index++] = Integer.numberOfTrailingZeros(remaining);
			remaining &= remaining - 1;
		}
		return positions;
	}
	
	private boolean contains(int[] lines, int line) {
		for (int candidate : lines) {
			if (candidate == line) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Which of the three fin forms a fish strategy accepts.
	 */
	enum FinMode {
		
		/**
		 * A finless fish: the base lines' candidates lie entirely inside the cover set.
		 */
		NONE,
		/**
		 * A finned fish in which every base line keeps at least two candidates inside the cover.
		 */
		FINNED,
		/**
		 * A sashimi fish, in which at least one base line is down to a single cover candidate.
		 */
		SASHIMI,
		/**
		 * Either finned form, used where the two are not told apart as separate techniques.
		 */
		ANY_FINNED
	}
}
