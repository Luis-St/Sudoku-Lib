package net.luis.sudoku.grid;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link RegionPartition}.
 */
class RegionPartitionTest {
	
	private static final Region FOUR_BOX_0 = new Region(0, 1, 4, 5);
	private static final Region FOUR_BOX_1 = new Region(2, 3, 6, 7);
	private static final Region FOUR_BOX_2 = new Region(8, 9, 12, 13);
	private static final Region FOUR_BOX_3 = new Region(10, 11, 14, 15);
	
	private static List<Region> fourBoxes() {
		return List.of(FOUR_BOX_0, FOUR_BOX_1, FOUR_BOX_2, FOUR_BOX_3);
	}
	
	private static RegionPartition fourBoxPartition() {
		return new RegionPartition(GridSize.FOUR, fourBoxes());
	}
	
	private static List<Region> fourRows() {
		return List.of(new Region(0, 1, 2, 3), new Region(4, 5, 6, 7), new Region(8, 9, 10, 11), new Region(12, 13, 14, 15));
	}
	
	@Test
	void regionPartition_validHandWrittenFourGrid_isAccepted() {
		RegionPartition partition = fourBoxPartition();
		assertAll(
			() -> assertSame(GridSize.FOUR, partition.size()),
			() -> assertEquals(4, partition.regionCount()),
			() -> assertEquals(fourBoxes(), partition.regions())
		);
	}
	
	@Test
	void regionPartition_validRowPartition_isAccepted() {
		RegionPartition partition = new RegionPartition(GridSize.FOUR, fourRows());
		assertAll(
			() -> assertSame(GridSize.FOUR, partition.size()),
			() -> assertEquals(4, partition.regionCount()),
			() -> assertEquals(0, partition.regionOf(3)),
			() -> assertEquals(3, partition.regionOf(12))
		);
	}
	
	@Test
	void regionPartition_tooFewRegions_throws() {
		List<Region> regions = List.of(FOUR_BOX_0, FOUR_BOX_1, FOUR_BOX_2);
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> new RegionPartition(GridSize.FOUR, regions)),
			() -> assertThrows(IllegalArgumentException.class, () -> new RegionPartition(GridSize.FOUR, List.of()))
		);
	}
	
	@Test
	void regionPartition_tooManyRegions_throws() {
		List<Region> regions = List.of(FOUR_BOX_0, FOUR_BOX_1, FOUR_BOX_2, FOUR_BOX_3, new Region(0, 1, 2, 3));
		assertThrows(IllegalArgumentException.class, () -> new RegionPartition(GridSize.FOUR, regions));
	}
	
	@Test
	void regionPartition_regionCountMatchingAnotherSize_throws() {
		List<Region> regions = fourBoxes();
		assertThrows(IllegalArgumentException.class, () -> new RegionPartition(GridSize.NINE, regions));
	}
	
	@Test
	void regionPartition_regionWithTooFewCells_throws() {
		List<Region> regions = List.of(FOUR_BOX_0, new Region(2, 3, 6), FOUR_BOX_2, new Region(7, 10, 11, 14, 15));
		assertThrows(IllegalArgumentException.class, () -> new RegionPartition(GridSize.FOUR, regions));
	}
	
	@Test
	void regionPartition_regionWithTooManyCells_throws() {
		List<Region> regions = List.of(new Region(0, 1, 4, 5, 2), new Region(3, 6, 7), FOUR_BOX_2, FOUR_BOX_3);
		assertThrows(IllegalArgumentException.class, () -> new RegionPartition(GridSize.FOUR, regions));
	}
	
	@Test
	void regionPartition_cellCoveredTwice_throws() {
		List<Region> regions = List.of(FOUR_BOX_0, FOUR_BOX_1, FOUR_BOX_2, new Region(0, 10, 11, 14));
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new RegionPartition(GridSize.FOUR, regions));
		assertTrue(exception.getMessage().contains("Cell 0 belongs to region 0 and region 3"), exception.getMessage());
	}
	
	@Test
	void regionPartition_cellCoveredTwiceWithinTheSameRow_throws() {
		List<Region> regions = List.of(new Region(0, 1, 2, 3), new Region(3, 4, 5, 6), new Region(7, 8, 9, 10), new Region(11, 12, 13, 14));
		assertThrows(IllegalArgumentException.class, () -> new RegionPartition(GridSize.FOUR, regions));
	}
	
	@Test
	void regionPartition_cellCoveredByNoRegion_throws() {
		// With n regions of exactly n distinct in-grid cells each, leaving cell 15 uncovered forces some other
		// cell to be covered twice, so the overlap check is what reports the broken coverage.
		List<Region> regions = List.of(FOUR_BOX_0, FOUR_BOX_1, FOUR_BOX_2, new Region(10, 11, 14, 13));
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new RegionPartition(GridSize.FOUR, regions));
		assertAll(
			() -> assertTrue(exception.getMessage().startsWith("Cell "), exception.getMessage()),
			() -> assertFalse(exception.getMessage().contains("Cell 15"), exception.getMessage())
		);
	}
	
	@Test
	void regionPartition_cellIndexAboveTheGrid_throws() {
		List<Region> regions = List.of(FOUR_BOX_0, FOUR_BOX_1, FOUR_BOX_2, new Region(10, 11, 14, 16));
		assertThrows(IndexOutOfBoundsException.class, () -> new RegionPartition(GridSize.FOUR, regions));
	}
	
	@Test
	void regionPartition_cellIndexFarOutsideTheGrid_throws() {
		List<Region> regions = List.of(new Region(0, 1, 4, 1000), FOUR_BOX_1, FOUR_BOX_2, FOUR_BOX_3);
		assertThrows(IndexOutOfBoundsException.class, () -> new RegionPartition(GridSize.FOUR, regions));
	}
	
	@Test
	void regionPartition_fourGridRegionsOnSixGrid_throws() {
		List<Region> regions = new ArrayList<>(fourBoxes());
		regions.add(new Region(16, 17, 18, 19));
		regions.add(new Region(20, 21, 22, 23));
		assertThrows(IllegalArgumentException.class, () -> new RegionPartition(GridSize.SIX, regions));
	}
	
	@Test
	void regionPartition_nullRegionList_throws() {
		assertThrows(NullPointerException.class, () -> new RegionPartition(GridSize.FOUR, null));
	}
	
	@Test
	void regionPartition_mutatingTheInputListAfterwards_doesNotAffectThePartition() {
		List<Region> regions = new ArrayList<>(fourBoxes());
		RegionPartition partition = new RegionPartition(GridSize.FOUR, regions);
		regions.clear();
		assertAll(
			() -> assertEquals(4, partition.regionCount()),
			() -> assertEquals(fourBoxes(), partition.regions())
		);
	}
	
	@Test
	void size_fourGridPartition_returnsFour() {
		assertSame(GridSize.FOUR, fourBoxPartition().size());
	}
	
	@Test
	void regionCount_everySize_equalsN() {
		for (GridSize size : GridSize.values()) {
			RegionPartition partition = ClassicRegionPartition.of(size);
			assertEquals(size.n(), partition.regionCount(), "region count of " + size);
		}
	}
	
	@Test
	void regions_anyPartition_isUnmodifiable() {
		List<Region> regions = fourBoxPartition().regions();
		assertAll(
			() -> assertThrows(UnsupportedOperationException.class, () -> regions.add(new Region(0))),
			() -> assertThrows(UnsupportedOperationException.class, () -> regions.remove(0)),
			() -> assertThrows(UnsupportedOperationException.class, () -> regions.set(0, new Region(0))),
			() -> assertThrows(UnsupportedOperationException.class, regions::clear)
		);
	}
	
	@Test
	void regions_anyPartition_keepsConstructionOrder() {
		List<Region> regions = List.of(FOUR_BOX_2, FOUR_BOX_0, FOUR_BOX_3, FOUR_BOX_1);
		RegionPartition partition = new RegionPartition(GridSize.FOUR, regions);
		assertEquals(regions, partition.regions());
	}
	
	@Test
	void region_everyIndex_returnsTheConstructedRegion() {
		RegionPartition partition = fourBoxPartition();
		assertAll(
			() -> assertEquals(FOUR_BOX_0, partition.region(0)),
			() -> assertEquals(FOUR_BOX_1, partition.region(1)),
			() -> assertEquals(FOUR_BOX_2, partition.region(2)),
			() -> assertEquals(FOUR_BOX_3, partition.region(3))
		);
	}
	
	@Test
	void region_indexOutsideThePartition_throws() {
		RegionPartition partition = fourBoxPartition();
		assertAll(
			() -> assertThrows(IndexOutOfBoundsException.class, () -> partition.region(-1)),
			() -> assertThrows(IndexOutOfBoundsException.class, () -> partition.region(4)),
			() -> assertThrows(IndexOutOfBoundsException.class, () -> partition.region(100))
		);
	}
	
	@Test
	void regionOf_everyCellOfTheFourGrid_returnsTheBoxIndex() {
		RegionPartition partition = fourBoxPartition();
		int[] expected = { 0, 0, 1, 1, 0, 0, 1, 1, 2, 2, 3, 3, 2, 2, 3, 3 };
		for (int cellIndex = 0; cellIndex < expected.length; cellIndex++) {
			assertEquals(expected[cellIndex], partition.regionOf(cellIndex), "cell " + cellIndex);
		}
	}
	
	@Test
	void regionOf_negativeCellIndex_throws() {
		RegionPartition partition = fourBoxPartition();
		assertThrows(IndexOutOfBoundsException.class, () -> partition.regionOf(-1));
	}
	
	@Test
	void regionOf_cellIndexAtOrAboveCellCount_throws() {
		RegionPartition partition = fourBoxPartition();
		assertAll(
			() -> assertThrows(IndexOutOfBoundsException.class, () -> partition.regionOf(16)),
			() -> assertThrows(IndexOutOfBoundsException.class, () -> partition.regionOf(Integer.MAX_VALUE))
		);
	}
	
	@Test
	void regionContaining_everyCellOfTheFourGrid_returnsARegionContainingIt() {
		RegionPartition partition = fourBoxPartition();
		for (int cellIndex = 0; cellIndex < GridSize.FOUR.cellCount(); cellIndex++) {
			Region region = partition.regionContaining(cellIndex);
			int index = cellIndex;
			assertAll(
				() -> assertTrue(region.contains(index), "cell " + index),
				() -> assertSame(partition.region(partition.regionOf(index)), region, "cell " + index)
			);
		}
	}
	
	@Test
	void regionContaining_knownCells_returnsTheExpectedBox() {
		RegionPartition partition = fourBoxPartition();
		assertAll(
			() -> assertEquals(FOUR_BOX_0, partition.regionContaining(0)),
			() -> assertEquals(FOUR_BOX_1, partition.regionContaining(3)),
			() -> assertEquals(FOUR_BOX_2, partition.regionContaining(12)),
			() -> assertEquals(FOUR_BOX_3, partition.regionContaining(15))
		);
	}
	
	@Test
	void regionContaining_cellIndexOutsideTheGrid_throws() {
		RegionPartition partition = fourBoxPartition();
		assertAll(
			() -> assertThrows(IndexOutOfBoundsException.class, () -> partition.regionContaining(-1)),
			() -> assertThrows(IndexOutOfBoundsException.class, () -> partition.regionContaining(16))
		);
	}
	
	@Test
	void equals_sameInstance_returnsTrue() {
		RegionPartition partition = fourBoxPartition();
		assertEquals(partition, partition);
	}
	
	@Test
	void equals_equalPartitions_returnsTrue() {
		assertEquals(fourBoxPartition(), fourBoxPartition());
	}
	
	@Test
	void equals_handWrittenBoxesAndClassicPartition_returnsTrue() {
		assertEquals(fourBoxPartition(), ClassicRegionPartition.of(GridSize.FOUR));
	}
	
	@Test
	void equals_differentRegionOrder_returnsFalse() {
		RegionPartition reordered = new RegionPartition(GridSize.FOUR, List.of(FOUR_BOX_1, FOUR_BOX_0, FOUR_BOX_2, FOUR_BOX_3));
		assertNotEquals(fourBoxPartition(), reordered);
	}
	
	@Test
	void equals_differentRegions_returnsFalse() {
		RegionPartition rows = new RegionPartition(GridSize.FOUR, fourRows());
		assertNotEquals(fourBoxPartition(), rows);
	}
	
	@Test
	void equals_differentGridSize_returnsFalse() {
		assertNotEquals(ClassicRegionPartition.of(GridSize.FOUR), ClassicRegionPartition.of(GridSize.SIX));
	}
	
	@Test
	void equals_nullOrOtherType_returnsFalse() {
		RegionPartition partition = fourBoxPartition();
		assertAll(
			() -> assertNotEquals(null, partition),
			() -> assertNotEquals("RegionPartition", partition),
			() -> assertNotEquals(partition, new Object())
		);
	}
	
	@Test
	void hashCode_equalPartitions_areEqual() {
		assertEquals(fourBoxPartition().hashCode(), fourBoxPartition().hashCode());
	}
	
	@Test
	void hashCode_differentPartitions_differ() {
		RegionPartition rows = new RegionPartition(GridSize.FOUR, fourRows());
		assertNotEquals(fourBoxPartition().hashCode(), rows.hashCode());
	}
	
	@Test
	void toString_anyPartition_containsSizeAndRegions() {
		String string = fourBoxPartition().toString();
		assertAll(
			() -> assertTrue(string.startsWith("RegionPartition[size=FOUR"), string),
			() -> assertTrue(string.contains("Region[0, 1, 4, 5]"), string),
			() -> assertTrue(string.contains("Region[10, 11, 14, 15]"), string)
		);
	}
}
