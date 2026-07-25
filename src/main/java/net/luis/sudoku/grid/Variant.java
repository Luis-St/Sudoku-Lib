package net.luis.sudoku.grid;

/**
 * The region layout variants a puzzle can use.
 *
 * @see GridSize
 * @see RegionPartition
 */
public enum Variant {
	
	/**
	 * Rectangular boxes of the size's {@link GridSize#boxWidth()} by {@link GridSize#boxHeight()} dimensions.
	 * Supported at every grid size.
	 */
	CLASSIC,
	/**
	 * Irregular but contiguous regions, also known as jigsaw. Supported at every grid size except
	 * {@link GridSize#FOUR}, where nearly every valid partition degenerates into the rows or columns themselves.
	 */
	CHAOS;
	
	/**
	 * Checks whether this variant is available at the given grid size.
	 *
	 * @param size The grid size to check
	 * @return True if the combination is supported, false otherwise
	 */
	public boolean isSupportedAt(GridSize size) {
		return this != CHAOS || size != GridSize.FOUR;
	}
	
	/**
	 * Throws if this variant is not available at the given grid size.
	 *
	 * @param size The grid size to check
	 * @throws IllegalArgumentException If the combination is not supported
	 */
	public void checkSupportedAt(GridSize size) {
		if (!this.isSupportedAt(size)) {
			throw new IllegalArgumentException("Variant " + this + " is not supported at grid size " + size);
		}
	}
}
