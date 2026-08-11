package net.luis.sudoku.solver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Sue de Coq: a region-line intersection whose candidates are shared out between one cell on the line and one in
 * the region.
 * <p>
 *     Take the cells where a region and a line cross, and suppose they list two more digits than they have cells.
 *     Find one further cell on the line and one further cell in the region, both drawing only on those digits, with no
 *     digit in common between them. Now count: the intersection cells plus those two hold exactly as many cells as
 *     there are digits, and no digit can repeat among them — the line keeps the intersection and its own extra cell
 *     distinct, the region does the same, and the two extra cells were chosen not to overlap. Every digit is therefore
 *     used exactly once inside the pattern.
 * </p>
 * <p>
 *     That settles three sets of eliminations. The line's extra cell shares its digits only with intersection cells,
 *     so those digits are used up on the line; the region's extra cell likewise; and any digit the intersection alone
 *     carries is used in a cell belonging to both, so it is used up in the region and on the line at once.
 * </p>
 *
 * @see TechniqueStrategy
 * @see AlsXz
 * @see Technique#SUE_DE_COQ
 */
public final class SueDeCoq implements TechniqueStrategy {
	
	/**
	 * Constructs the Sue de Coq strategy. The strategy is stateless and holds no grid.
	 */
	public SueDeCoq() {}
	
	@Override
	public Technique technique() {
		return Technique.SUE_DE_COQ;
	}
	
	/**
	 * Scans every region-line intersection for the pattern.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first Sue de Coq elimination, or empty if no intersection makes progress
	 */
	@Override
	public Optional<Deduction> find(CandidateGrid grid) {
		return this.search(grid, null);
	}

	/**
	 * Explains the pattern by showing the intersection, the two extra cells that complete it, and the fact the whole
	 * argument rests on: as many cells as digits, none of which can repeat, so every digit is used up inside.
	 *
	 * @param grid The working grid; never mutated
	 * @return The eliminations and their explanation, or empty if no intersection makes progress
	 */
	@Override
	public Optional<ExplainedDeduction> findExplained(CandidateGrid grid) {
		Explanation.Builder builder = Explanation.builder(Technique.SUE_DE_COQ);
		return this.search(grid, builder).map(deduction -> new ExplainedDeduction(deduction, builder.conclusion(deduction).build()));
	}

	private Optional<Deduction> search(CandidateGrid grid, Explanation.Builder explanation) {
		int n = grid.n();
		for (int region = 0; region < grid.partition().regionCount(); region++) {
			for (int line = 0; line < n; line++) {
				Optional<Deduction> byRow = this.scan(grid, region, grid.rowCells(line), explanation);
				if (byRow.isPresent()) {
					return byRow;
				}
				
				Optional<Deduction> byColumn = this.scan(grid, region, grid.columnCells(line), explanation);
				if (byColumn.isPresent()) {
					return byColumn;
				}
			}
		}
		return Optional.empty();
	}
	
	private Optional<Deduction> scan(CandidateGrid grid, int region, int[] lineCells, Explanation.Builder explanation) {
		int[] intersection = this.intersectionOf(grid, region, lineCells);
		// Two or three shared cells: one is not an intersection worth the name, four leaves no room for the extras.
		if (intersection.length < 2 || intersection.length > 3) {
			return Optional.empty();
		}
		
		int digits = 0;
		for (int cell : intersection) {
			digits |= grid.candidates(cell);
		}
		if (Integer.bitCount(digits) != intersection.length + 2) {
			return Optional.empty();
		}
		
		int[] regionCells = grid.regionCells(region);
		for (int lineCell : lineCells) {
			if (!this.isExtra(grid, lineCell, intersection, digits, region, true)) {
				continue;
			}
			
			for (int regionCell : regionCells) {
				if (!this.isExtra(grid, regionCell, intersection, digits, region, false)) {
					continue;
				}
				if ((grid.candidates(lineCell) & grid.candidates(regionCell)) != 0) {
					continue;
				}
				
				Optional<Deduction> found = this.eliminate(grid, intersection, lineCells, regionCells, lineCell, regionCell, digits, region, explanation);
				if (found.isPresent()) {
					return found;
				}
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Checks whether a cell can serve as the extra cell on its side: empty, outside the intersection, drawing only on
	 * the intersection's digits, and — for the line's extra — genuinely off the region.
	 */
	private boolean isExtra(CandidateGrid grid, int cell, int[] intersection, int digits, int region, boolean onLine) {
		if (!grid.isEmpty(cell) || this.contains(intersection, cell)) {
			return false;
		}
		if (onLine == (grid.regionOf(cell) == region)) {
			return false;
		}
		return (grid.candidates(cell) & ~digits) == 0;
	}
	
	private Optional<Deduction> eliminate(CandidateGrid grid, int[] intersection, int[] lineCells, int[] regionCells, int lineCell, int regionCell, int digits, int region, Explanation.Builder explanation) {
		int lineDigits = grid.candidates(lineCell);
		int regionDigits = grid.candidates(regionCell);
		int intersectionOnly = digits & ~lineDigits & ~regionDigits;
		
		EliminationBuilder builder = new EliminationBuilder();
		for (int cell : lineCells) {
			if (cell == lineCell || this.contains(intersection, cell)) {
				continue;
			}
			builder.addAll(grid, cell, lineDigits | intersectionOnly);
		}
		
		for (int cell : regionCells) {
			if (cell == regionCell || this.contains(intersection, cell)) {
				continue;
			}
			builder.addAll(grid, cell, regionDigits | intersectionOnly);
		}
		
		builder.sortByCell();
		Optional<Deduction> deduction = builder.build(Technique.SUE_DE_COQ);
		// Only a pattern that removes something is the deduction being returned, so only that one is worth explaining:
		// any earlier one was looked at and rejected.
		if (deduction.isPresent() && explanation != null) {
			this.explain(grid, intersection, lineCells, lineCell, regionCell, digits, region, explanation);
		}
		return deduction;
	}
	
	/**
	 * Records the pattern: the region and the line that cross, the intersection cells, the two extra cells that draw
	 * on the same digits, and the counting argument that uses every one of those digits up.
	 *
	 * @param grid The working grid
	 * @param intersection The cells the region and the line share
	 * @param lineCells The line's cells
	 * @param lineCell The extra cell on the line
	 * @param regionCell The extra cell in the region
	 * @param digits The digits the intersection spans
	 * @param region The region
	 * @param explanation The explanation to record into
	 */
	private void explain(CandidateGrid grid, int[] intersection, int[] lineCells, int lineCell, int regionCell, int digits, int region, Explanation.Builder explanation) {
		List<PatternCell> shared = new ArrayList<>(intersection.length);
		List<PatternCell> used = new ArrayList<>();
		for (int cell : intersection) {
			shared.add(new PatternCell(cell, CellRole.BASE, grid.candidates(cell)));
			used.add(new PatternCell(cell, CellRole.LINK_ON, grid.candidates(cell)));
		}
		used.add(new PatternCell(lineCell, CellRole.LINK_ON, grid.candidates(lineCell)));
		used.add(new PatternCell(regionCell, CellRole.LINK_ON, grid.candidates(regionCell)));
		
		explanation.focusUnits(0, List.of(UnitRef.region(region), Explanations.refOf(grid, lineCells, lineCells[0])))
			.pattern(0, shared)
			// The two extra cells draw only on the intersection's digits and share none with each other.
			.pattern(0, List.of(new PatternCell(lineCell, CellRole.PATTERN, grid.candidates(lineCell)), new PatternCell(regionCell, CellRole.PATTERN, grid.candidates(regionCell))))
			// As many cells as digits, and no digit can repeat among them, so every one of the digits is used up here.
			.implication(0, used);
	}
	
	private int[] intersectionOf(CandidateGrid grid, int region, int[] lineCells) {
		int[] shared = new int[lineCells.length];
		int count = 0;
		for (int cell : lineCells) {
			if (grid.regionOf(cell) == region && grid.isEmpty(cell)) {
				shared[count++] = cell;
			}
		}
		return java.util.Arrays.copyOf(shared, count);
	}
	
	private boolean contains(int[] cells, int cell) {
		for (int member : cells) {
			if (member == cell) {
				return true;
			}
		}
		return false;
	}
}
