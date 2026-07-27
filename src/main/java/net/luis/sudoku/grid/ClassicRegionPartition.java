package net.luis.sudoku.grid;

import java.util.*;

/**
 * Builds the {@link RegionPartition} of rectangular boxes belonging to a {@link GridSize}.
 * <p>
 *     The layout is computed directly from the size's {@link GridSize#boxWidth()} and
 *     {@link GridSize#boxHeight()}: regions are numbered left to right, then top to bottom, so region 0 is
 *     always the top-left box.
 * </p>
 * <p>
 *     Results are cached per size in an {@link EnumMap}. Partitions are immutable, so sharing one instance
 *     across puzzles is safe and keeps repeated generation allocation-free.
 * </p>
 */
public final class ClassicRegionPartition {
	
	private static final Map<GridSize, RegionPartition> CACHE = new EnumMap<>(GridSize.class);
	
	private ClassicRegionPartition() {}
	
	/**
	 * Returns the classic box partition for the given grid size.
	 *
	 * @param size The grid size
	 * @return The cached, immutable partition
	 */
	public static RegionPartition of(GridSize size) {
		return Objects.requireNonNull(CACHE.get(size), "No classic partition for grid size " + size);
	}
	
	private static RegionPartition build(GridSize size) {
		int n = size.n();
		int boxWidth = size.boxWidth();
		int boxHeight = size.boxHeight();
		int boxesPerRow = n / boxWidth;
		int[][] cells = new int[n][n];
		int[] filled = new int[n];
		
		for (int row = 0; row < n; row++) {
			for (int column = 0; column < n; column++) {
				int regionIndex = row / boxHeight * boxesPerRow + column / boxWidth;
				cells[regionIndex][filled[regionIndex]++] = row * n + column;
			}
		}
		
		List<Region> regions = new ArrayList<>(n);
		for (int regionIndex = 0; regionIndex < n; regionIndex++) {
			regions.add(new Region(cells[regionIndex]));
		}
		return new RegionPartition(size, regions);
	}
	
	static {
		for (GridSize size : GridSize.values()) {
			CACHE.put(size, build(size));
		}
	}
}
