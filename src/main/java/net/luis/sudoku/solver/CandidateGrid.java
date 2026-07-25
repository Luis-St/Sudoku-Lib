package net.luis.sudoku.solver;

import net.luis.sudoku.grid.*;

import java.util.List;

/**
 * The mutable working state every technique reads and mutates: the current value of each cell plus its remaining
 * candidate set as an {@code int} bitmask.
 * <p>
 *     A grid is snapshotted from a {@link Puzzle} and is fully independent of it afterwards — the puzzle is never
 *     touched. Every empty cell starts with every digit that does not already appear in its row, its column or its
 *     region; a filled cell carries a candidate mask of {@code 0}. The candidate convention matches {@link Cell}: bit
 *     {@code d} set means digit {@code d} is a candidate, bit {@code 0} is unused, and a full mask for size {@code n}
 *     is {@code ((1 << n) - 1) << 1}.
 * </p>
 * <p>
 *     The unit lists ({@link #rows()}, {@link #columns()}, {@link #regions()}), the region lookup and the peer arrays
 *     are all precomputed once in the constructor and never depend on a hash order, so every scan a strategy runs over
 *     this grid is deterministic and byte-identical on every JVM. The unit and peer arrays are cached and must not be
 *     mutated by callers.
 * </p>
 *
 * @see TechniqueStrategy
 * @see Deduction
 */
public final class CandidateGrid {
	
	private final GridSize size;
	private final RegionPartition partition;
	private final int n;
	private final int cellCount;
	private final int fullMask;
	private final int[] values;
	private final int[] candidates;
	private final int[] regionOf;
	private final int[][] rowUnits;
	private final int[][] columnUnits;
	private final int[][] regionUnits;
	private final int[][] peers;
	private final List<int[]> rowList;
	private final List<int[]> columnList;
	private final List<int[]> regionList;
	private final List<int[]> allUnitList;
	
	/**
	 * Builds a candidate grid from the given puzzle, snapshotting its values and deriving every candidate set.
	 * <p>
	 *     The puzzle is read through {@link Puzzle#values()} and is never modified. For every empty cell the candidate
	 *     mask is every digit not already present in the cell's row, column or region; filled cells get a mask of
	 *     {@code 0}.
	 * </p>
	 *
	 * @param puzzle The puzzle to snapshot
	 */
	public CandidateGrid(Puzzle puzzle) {
		this.size = puzzle.size();
		this.partition = puzzle.partition();
		this.n = this.size.n();
		this.cellCount = this.size.cellCount();
		this.fullMask = ((1 << this.n) - 1) << 1;
		this.values = puzzle.values();
		this.regionOf = new int[this.cellCount];
		for (int cell = 0; cell < this.cellCount; cell++) {
			this.regionOf[cell] = this.partition.regionOf(cell);
		}
		this.rowUnits = new int[this.n][this.n];
		this.columnUnits = new int[this.n][this.n];
		for (int line = 0; line < this.n; line++) {
			for (int position = 0; position < this.n; position++) {
				this.rowUnits[line][position] = line * this.n + position;
				this.columnUnits[line][position] = position * this.n + line;
			}
		}
		this.regionUnits = new int[this.n][];
		for (int regionIndex = 0; regionIndex < this.n; regionIndex++) {
			this.regionUnits[regionIndex] = this.partition.region(regionIndex).cells();
		}
		this.candidates = new int[this.cellCount];
		int[] rowUsed = new int[this.n];
		int[] columnUsed = new int[this.n];
		int[] regionUsed = new int[this.partition.regionCount()];
		for (int cell = 0; cell < this.cellCount; cell++) {
			int value = this.values[cell];
			if (value != 0) {
				int bit = 1 << value;
				rowUsed[cell / this.n] |= bit;
				columnUsed[cell % this.n] |= bit;
				regionUsed[this.regionOf[cell]] |= bit;
			}
		}
		for (int cell = 0; cell < this.cellCount; cell++) {
			if (this.values[cell] == 0) {
				this.candidates[cell] = this.fullMask & ~(rowUsed[cell / this.n] | columnUsed[cell % this.n] | regionUsed[this.regionOf[cell]]);
			}
		}
		this.peers = new int[this.cellCount][];
		boolean[] mark = new boolean[this.cellCount];
		for (int cell = 0; cell < this.cellCount; cell++) {
			this.buildPeers(cell, mark);
		}
		this.rowList = List.of(this.rowUnits);
		this.columnList = List.of(this.columnUnits);
		this.regionList = List.of(this.regionUnits);
		int[][] all = new int[3 * this.n][];
		System.arraycopy(this.rowUnits, 0, all, 0, this.n);
		System.arraycopy(this.columnUnits, 0, all, this.n, this.n);
		System.arraycopy(this.regionUnits, 0, all, 2 * this.n, this.n);
		this.allUnitList = List.of(all);
	}
	
	private void buildPeers(int cell, boolean[] mark) {
		int row = cell / this.n;
		int column = cell % this.n;
		int region = this.regionOf[cell];
		int count = 0;
		for (int position = 0; position < this.n; position++) {
			count += this.markPeer(this.rowUnits[row][position], cell, mark) ? 1 : 0;
			count += this.markPeer(this.columnUnits[column][position], cell, mark) ? 1 : 0;
		}
		for (int member : this.regionUnits[region]) {
			count += this.markPeer(member, cell, mark) ? 1 : 0;
		}
		int[] peerArray = new int[count];
		int index = 0;
		for (int candidate = 0; candidate < this.cellCount; candidate++) {
			if (mark[candidate]) {
				peerArray[index++] = candidate;
				mark[candidate] = false;
			}
		}
		this.peers[cell] = peerArray;
	}
	
	private boolean markPeer(int candidate, int cell, boolean[] mark) {
		if (candidate == cell || mark[candidate]) {
			return false;
		}
		mark[candidate] = true;
		return true;
	}
	
	/**
	 * Returns the edge length of the grid.
	 *
	 * @return The edge length {@code n}
	 */
	public int n() {
		return this.n;
	}
	
	/**
	 * Returns the total number of cells in the grid.
	 *
	 * @return The cell count, {@code n * n}
	 */
	public int cellCount() {
		return this.cellCount;
	}
	
	/**
	 * Returns the grid size of this grid.
	 *
	 * @return The grid size
	 */
	public GridSize size() {
		return this.size;
	}
	
	/**
	 * Returns the region partition of this grid.
	 *
	 * @return The region partition
	 */
	public RegionPartition partition() {
		return this.partition;
	}
	
	/**
	 * Returns the current value of the given cell.
	 *
	 * @param cell The row-major cell index
	 * @return The digit, or {@code 0} if the cell is empty
	 */
	public int value(int cell) {
		return this.values[cell];
	}
	
	/**
	 * Checks whether the given cell currently holds no digit.
	 *
	 * @param cell The row-major cell index
	 * @return True if the cell is empty, false otherwise
	 */
	public boolean isEmpty(int cell) {
		return this.values[cell] == 0;
	}
	
	/**
	 * Returns the candidate bitmask of the given cell, with bit {@code d} set for a candidate digit {@code d}.
	 *
	 * @param cell The row-major cell index
	 * @return The candidate bitmask, {@code 0} for a filled cell
	 */
	public int candidates(int cell) {
		return this.candidates[cell];
	}
	
	/**
	 * Checks whether the given digit is a candidate of the given cell.
	 *
	 * @param cell The row-major cell index
	 * @param digit The digit to test
	 * @return True if the digit is a candidate, false otherwise
	 */
	public boolean hasCandidate(int cell, int digit) {
		return (this.candidates[cell] & (1 << digit)) != 0;
	}
	
	/**
	 * Returns how many candidates the given cell has.
	 *
	 * @param cell The row-major cell index
	 * @return The candidate count
	 */
	public int candidateCount(int cell) {
		return Integer.bitCount(this.candidates[cell]);
	}
	
	/**
	 * Returns the candidate digits of the given cell in ascending order.
	 *
	 * @param cell The row-major cell index
	 * @return A new array of the candidate digits, empty for a filled cell
	 */
	public int[] candidateDigits(int cell) {
		int mask = this.candidates[cell];
		int[] digits = new int[Integer.bitCount(mask)];
		int index = 0;
		for (int digit = 1; digit <= this.n; digit++) {
			if ((mask & (1 << digit)) != 0) {
				digits[index++] = digit;
			}
		}
		return digits;
	}
	
	/**
	 * Places the given digit into the given cell, clears the cell's candidates and removes the digit from every peer.
	 *
	 * @param cell The row-major cell index
	 * @param digit The digit to place
	 * @return True, as a placement always changes the grid
	 * @throws IllegalStateException If the cell is already filled
	 * @throws IllegalArgumentException If the digit was not a candidate of the cell
	 */
	public boolean place(int cell, int digit) {
		if (this.values[cell] != 0) {
			throw new IllegalStateException("Cell " + cell + " already holds " + this.values[cell]);
		}
		int bit = 1 << digit;
		if ((this.candidates[cell] & bit) == 0) {
			throw new IllegalArgumentException("Digit " + digit + " is not a candidate of cell " + cell);
		}
		this.values[cell] = digit;
		this.candidates[cell] = 0;
		for (int peer : this.peers[cell]) {
			this.candidates[peer] &= ~bit;
		}
		return true;
	}
	
	/**
	 * Removes the given digit from the given cell's candidates.
	 *
	 * @param cell The row-major cell index
	 * @param digit The digit to remove
	 * @return True if the digit was a candidate and got removed, false if nothing changed
	 */
	public boolean eliminate(int cell, int digit) {
		int bit = 1 << digit;
		if ((this.candidates[cell] & bit) == 0) {
			return false;
		}
		this.candidates[cell] &= ~bit;
		return true;
	}
	
	/**
	 * Checks whether every cell of the grid holds a digit.
	 *
	 * @return True if no cell is empty, false otherwise
	 */
	public boolean isComplete() {
		for (int cell = 0; cell < this.cellCount; cell++) {
			if (this.values[cell] == 0) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Checks whether the grid is completely and correctly filled.
	 *
	 * @return True if every cell is filled and no unit holds a duplicate, false otherwise
	 */
	public boolean isSolved() {
		return this.isComplete() && this.isValid();
	}
	
	private boolean isValid() {
		for (int[] unit : this.allUnitList) {
			int seen = 0;
			for (int cell : unit) {
				int value = this.values[cell];
				if (value == 0) {
					continue;
				}
				int bit = 1 << value;
				if ((seen & bit) != 0) {
					return false;
				}
				seen |= bit;
			}
		}
		return true;
	}
	
	/**
	 * Returns a row-major snapshot of the current values.
	 *
	 * @return A new array of the current values, {@code 0} for an empty cell
	 */
	public int[] values() {
		return this.values.clone();
	}
	
	/**
	 * Returns the cached ascending cell indices of the given row. The array must not be mutated.
	 *
	 * @param row The zero-based row
	 * @return The cached row cell indices
	 */
	public int[] rowCells(int row) {
		return this.rowUnits[row];
	}
	
	/**
	 * Returns the cached ascending cell indices of the given column. The array must not be mutated.
	 *
	 * @param column The zero-based column
	 * @return The cached column cell indices
	 */
	public int[] columnCells(int column) {
		return this.columnUnits[column];
	}
	
	/**
	 * Returns the cached ascending cell indices of the given region. The array must not be mutated.
	 *
	 * @param regionIndex The zero-based region index
	 * @return The cached region cell indices
	 */
	public int[] regionCells(int regionIndex) {
		return this.regionUnits[regionIndex];
	}
	
	/**
	 * Returns the {@code n} rows of the grid, each an ascending cell-index array.
	 *
	 * @return An unmodifiable list of the row units
	 */
	public List<int[]> rows() {
		return this.rowList;
	}
	
	/**
	 * Returns the {@code n} columns of the grid, each an ascending cell-index array.
	 *
	 * @return An unmodifiable list of the column units
	 */
	public List<int[]> columns() {
		return this.columnList;
	}
	
	/**
	 * Returns the {@code n} regions of the grid, each an ascending cell-index array.
	 *
	 * @return An unmodifiable list of the region units
	 */
	public List<int[]> regions() {
		return this.regionList;
	}
	
	/**
	 * Returns every unit of the grid: the rows first, then the columns, then the regions, {@code 3n} units in all.
	 *
	 * @return An unmodifiable list of all units
	 */
	public List<int[]> allUnits() {
		return this.allUnitList;
	}
	
	/**
	 * Returns the row of the given cell.
	 *
	 * @param cell The row-major cell index
	 * @return The zero-based row
	 */
	public int rowOf(int cell) {
		return cell / this.n;
	}
	
	/**
	 * Returns the column of the given cell.
	 *
	 * @param cell The row-major cell index
	 * @return The zero-based column
	 */
	public int columnOf(int cell) {
		return cell % this.n;
	}
	
	/**
	 * Returns the region index of the given cell.
	 *
	 * @param cell The row-major cell index
	 * @return The zero-based region index
	 */
	public int regionOf(int cell) {
		return this.regionOf[cell];
	}
	
	/**
	 * Returns the cached ascending peers of the given cell: every other cell sharing its row, column or region. The
	 * array must not be mutated.
	 *
	 * @param cell The row-major cell index
	 * @return The cached peer cell indices, excluding the cell itself
	 */
	public int[] peers(int cell) {
		return this.peers[cell];
	}
	
	/**
	 * Checks whether the two given cells are distinct peers, sharing a row, a column or a region.
	 *
	 * @param a The first row-major cell index
	 * @param b The second row-major cell index
	 * @return True if the cells differ and share a unit, false otherwise
	 */
	public boolean peers(int a, int b) {
		return a != b && (a / this.n == b / this.n || a % this.n == b % this.n || this.regionOf[a] == this.regionOf[b]);
	}
}
