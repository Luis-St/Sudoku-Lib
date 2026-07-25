package net.luis.sudoku.grid;

import java.util.*;

/**
 * A complete partition of a grid's cells into {@link GridSize#n()} regions of exactly {@link GridSize#n()}
 * cells each.
 * <p>
 *     The same type carries both classic and chaos layouts, which is what allows the solver, the generator and
 *     the rater to be written once against the generalized region model. Classic partitions are produced by
 *     {@link ClassicRegionPartition}; chaos partitions are built cell by cell by the region generator.
 * </p>
 * <p>
 *     The instance is immutable and its region order is the order it was constructed with, never a hash order.
 * </p>
 *
 * @see Region
 * @see ClassicRegionPartition
 */
public final class RegionPartition {
	
	private final GridSize size;
	private final List<Region> regions;
	private final int[] regionOf;
	
	/**
	 * Constructs a partition from the given regions.
	 * <p>
	 *     The regions are validated to cover every cell of the grid exactly once. The list is copied, so the
	 *     caller may reuse it afterwards.
	 * </p>
	 *
	 * @param size The grid size the regions belong to
	 * @param regions The regions, in the order they should be reported in
	 * @throws IllegalArgumentException If the region count, any region's size, or the cell coverage is wrong
	 */
	public RegionPartition(GridSize size, List<Region> regions) {
		int n = size.n();
		if (regions.size() != n) {
			throw new IllegalArgumentException("Expected " + n + " regions for grid size " + size + ", got " + regions.size());
		}
		int[] lookup = new int[size.cellCount()];
		Arrays.fill(lookup, -1);
		for (int regionIndex = 0; regionIndex < n; regionIndex++) {
			Region region = regions.get(regionIndex);
			if (region.size() != n) {
				throw new IllegalArgumentException("Region " + regionIndex + " must contain " + n + " cells, but contains " + region.size());
			}
			for (int position = 0; position < n; position++) {
				int cellIndex = region.cell(position);
				size.checkCellIndex(cellIndex);
				if (lookup[cellIndex] != -1) {
					throw new IllegalArgumentException("Cell " + cellIndex + " belongs to region " + lookup[cellIndex] + " and region " + regionIndex);
				}
				lookup[cellIndex] = regionIndex;
			}
		}
		for (int cellIndex = 0; cellIndex < lookup.length; cellIndex++) {
			if (lookup[cellIndex] == -1) {
				throw new IllegalArgumentException("Cell " + cellIndex + " belongs to no region");
			}
		}
		this.size = size;
		this.regions = List.copyOf(regions);
		this.regionOf = lookup;
	}
	
	/**
	 * Returns the grid size this partition covers.
	 *
	 * @return The grid size
	 */
	public GridSize size() {
		return this.size;
	}
	
	/**
	 * Returns the number of regions, which equals {@link GridSize#n()}.
	 *
	 * @return The region count
	 */
	public int regionCount() {
		return this.regions.size();
	}
	
	/**
	 * Returns all regions in their construction order.
	 *
	 * @return An unmodifiable list of the regions
	 */
	public List<Region> regions() {
		return this.regions;
	}
	
	/**
	 * Returns the region at the given index.
	 *
	 * @param regionIndex The zero-based region index
	 * @return The region
	 * @throws IndexOutOfBoundsException If the region index is outside the partition
	 */
	public Region region(int regionIndex) {
		return this.regions.get(regionIndex);
	}
	
	/**
	 * Returns the index of the region the given cell belongs to.
	 *
	 * @param cellIndex The row-major cell index
	 * @return The zero-based region index
	 * @throws IndexOutOfBoundsException If the cell index is outside the grid
	 */
	public int regionOf(int cellIndex) {
		this.size.checkCellIndex(cellIndex);
		return this.regionOf[cellIndex];
	}
	
	/**
	 * Returns the region the given cell belongs to.
	 *
	 * @param cellIndex The row-major cell index
	 * @return The region containing the cell
	 * @throws IndexOutOfBoundsException If the cell index is outside the grid
	 */
	public Region regionContaining(int cellIndex) {
		return this.regions.get(this.regionOf(cellIndex));
	}
	
	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		return object instanceof RegionPartition partition && this.size == partition.size && this.regions.equals(partition.regions);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.size, this.regions);
	}
	
	@Override
	public String toString() {
		return "RegionPartition[size=" + this.size + ", regions=" + this.regions + "]";
	}
}
