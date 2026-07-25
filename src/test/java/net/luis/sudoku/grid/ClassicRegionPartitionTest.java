package net.luis.sudoku.grid;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link ClassicRegionPartition}.
 */
class ClassicRegionPartitionTest {
	
	@Test
	void of_everySize_returnsAPartitionForThatSize() {
		for (GridSize size : GridSize.values()) {
			assertSame(size, ClassicRegionPartition.of(size).size(), "size of " + size);
		}
	}
	
	@Test
	void of_everySize_hasNRegions() {
		for (GridSize size : GridSize.values()) {
			RegionPartition partition = ClassicRegionPartition.of(size);
			assertAll(
				() -> assertEquals(size.n(), partition.regionCount(), "region count of " + size),
				() -> assertEquals(size.n(), partition.regions().size(), "region list size of " + size)
			);
		}
	}
	
	@Test
	void of_everySize_hasRegionsOfExactlyNCells() {
		for (GridSize size : GridSize.values()) {
			RegionPartition partition = ClassicRegionPartition.of(size);
			for (int regionIndex = 0; regionIndex < partition.regionCount(); regionIndex++) {
				assertEquals(size.n(), partition.region(regionIndex).size(), "region " + regionIndex + " of " + size);
			}
		}
	}
	
	@Test
	void of_everySize_coversEveryCellExactlyOnce() {
		for (GridSize size : GridSize.values()) {
			RegionPartition partition = ClassicRegionPartition.of(size);
			int[] coverage = new int[size.cellCount()];
			for (int regionIndex = 0; regionIndex < partition.regionCount(); regionIndex++) {
				Region region = partition.region(regionIndex);
				for (int position = 0; position < region.size(); position++) {
					int cellIndex = region.cell(position);
					assertTrue(cellIndex >= 0 && cellIndex < size.cellCount(), "cell " + cellIndex + " of " + size + " is outside the grid");
					coverage[cellIndex]++;
				}
			}
			for (int cellIndex = 0; cellIndex < coverage.length; cellIndex++) {
				assertEquals(1, coverage[cellIndex], "coverage of cell " + cellIndex + " of " + size);
			}
		}
	}
	
	@Test
	void of_everySize_regionMembershipMatchesTheBoxFormula() {
		for (GridSize size : GridSize.values()) {
			RegionPartition partition = ClassicRegionPartition.of(size);
			int n = size.n();
			int boxWidth = size.boxWidth();
			int boxHeight = size.boxHeight();
			int boxesPerRow = n / boxWidth;
			for (int cellIndex = 0; cellIndex < size.cellCount(); cellIndex++) {
				int row = cellIndex / n;
				int column = cellIndex % n;
				int expected = row / boxHeight * boxesPerRow + column / boxWidth;
				int index = cellIndex;
				assertAll(
					() -> assertEquals(expected, partition.regionOf(index), "regionOf cell " + index + " of " + size),
					() -> assertTrue(partition.region(expected).contains(index), "region " + expected + " of " + size + " must contain cell " + index),
					() -> assertTrue(partition.regionContaining(index).contains(index), "regionContaining cell " + index + " of " + size)
				);
			}
		}
	}
	
	@Test
	void of_everySize_regionsAreRectangularBoxesNumberedLeftToRightThenTopToBottom() {
		for (GridSize size : GridSize.values()) {
			RegionPartition partition = ClassicRegionPartition.of(size);
			int n = size.n();
			int boxWidth = size.boxWidth();
			int boxHeight = size.boxHeight();
			int boxesPerRow = n / boxWidth;
			for (int regionIndex = 0; regionIndex < n; regionIndex++) {
				int boxRow = regionIndex / boxesPerRow;
				int boxColumn = regionIndex % boxesPerRow;
				int[] expected = new int[n];
				int filled = 0;
				for (int row = boxRow * boxHeight; row < boxRow * boxHeight + boxHeight; row++) {
					for (int column = boxColumn * boxWidth; column < boxColumn * boxWidth + boxWidth; column++) {
						expected[filled++] = row * n + column;
					}
				}
				Arrays.sort(expected);
				assertArrayEquals(expected, partition.region(regionIndex).cells(), "region " + regionIndex + " of " + size);
			}
		}
	}
	
	@Test
	void of_four_hasTheExpectedTopLeftBox() {
		assertArrayEquals(new int[] { 0, 1, 4, 5 }, ClassicRegionPartition.of(GridSize.FOUR).region(0).cells());
	}
	
	@Test
	void of_six_hasTheExpectedTopLeftBox() {
		assertArrayEquals(new int[] { 0, 1, 2, 6, 7, 8 }, ClassicRegionPartition.of(GridSize.SIX).region(0).cells());
	}
	
	@Test
	void of_six_numbersBoxesLeftToRightThenTopToBottom() {
		RegionPartition partition = ClassicRegionPartition.of(GridSize.SIX);
		assertAll(
			() -> assertArrayEquals(new int[] { 0, 1, 2, 6, 7, 8 }, partition.region(0).cells()),
			() -> assertArrayEquals(new int[] { 3, 4, 5, 9, 10, 11 }, partition.region(1).cells()),
			() -> assertArrayEquals(new int[] { 12, 13, 14, 18, 19, 20 }, partition.region(2).cells()),
			() -> assertArrayEquals(new int[] { 15, 16, 17, 21, 22, 23 }, partition.region(3).cells()),
			() -> assertArrayEquals(new int[] { 24, 25, 26, 30, 31, 32 }, partition.region(4).cells()),
			() -> assertArrayEquals(new int[] { 27, 28, 29, 33, 34, 35 }, partition.region(5).cells())
		);
	}
	
	@Test
	void of_nine_hasTheExpectedTopLeftAndBottomRightBoxes() {
		RegionPartition partition = ClassicRegionPartition.of(GridSize.NINE);
		assertAll(
			() -> assertArrayEquals(new int[] { 0, 1, 2, 9, 10, 11, 18, 19, 20 }, partition.region(0).cells()),
			() -> assertArrayEquals(new int[] { 60, 61, 62, 69, 70, 71, 78, 79, 80 }, partition.region(8).cells())
		);
	}
	
	@Test
	void of_twelve_hasTheExpectedTopLeftBox() {
		assertArrayEquals(new int[] { 0, 1, 2, 3, 12, 13, 14, 15, 24, 25, 26, 27 }, ClassicRegionPartition.of(GridSize.TWELVE).region(0).cells());
	}
	
	@Test
	void of_sixteen_hasTheExpectedTopLeftBox() {
		assertArrayEquals(new int[] { 0, 1, 2, 3, 16, 17, 18, 19, 32, 33, 34, 35, 48, 49, 50, 51 }, ClassicRegionPartition.of(GridSize.SIXTEEN).region(0).cells());
	}
	
	@Test
	void of_everySize_regionZeroIsTheTopLeftBox() {
		for (GridSize size : GridSize.values()) {
			Region region = ClassicRegionPartition.of(size).region(0);
			assertAll(
				() -> assertEquals(0, region.cell(0), "first cell of region 0 of " + size),
				() -> assertTrue(region.contains(size.indexOf(size.boxHeight() - 1, size.boxWidth() - 1)), "bottom right corner of region 0 of " + size),
				() -> assertFalse(region.contains(size.indexOf(0, size.boxWidth())), "cell right of region 0 of " + size),
				() -> assertFalse(region.contains(size.indexOf(size.boxHeight(), 0)), "cell below region 0 of " + size)
			);
		}
	}
	
	@Test
	void of_repeatedCalls_returnTheCachedInstance() {
		for (GridSize size : GridSize.values()) {
			RegionPartition first = ClassicRegionPartition.of(size);
			RegionPartition second = ClassicRegionPartition.of(size);
			assertAll(
				() -> assertSame(first, second, "cached instance of " + size),
				() -> assertSame(first, ClassicRegionPartition.of(size), "cached instance of " + size)
			);
		}
	}
	
	@Test
	void of_differentSizes_returnDifferentInstances() {
		assertNotSame(ClassicRegionPartition.of(GridSize.FOUR), ClassicRegionPartition.of(GridSize.SIX));
	}
	
	@Test
	void of_nullSize_throws() {
		assertThrows(NullPointerException.class, () -> ClassicRegionPartition.of(null));
	}
	
	@Test
	void of_everySize_equalsAnIndependentlyBuiltPartition() {
		for (GridSize size : GridSize.values()) {
			int n = size.n();
			int boxWidth = size.boxWidth();
			int boxHeight = size.boxHeight();
			int boxesPerRow = n / boxWidth;
			int[][] cells = new int[n][n];
			int[] filled = new int[n];
			for (int row = 0; row < n; row++) {
				for (int column = 0; column < n; column++) {
					int regionIndex = row / boxHeight * boxesPerRow + column / boxWidth;
					cells[regionIndex][filled[regionIndex]++] = size.indexOf(row, column);
				}
			}
			List<Region> regions = new ArrayList<>(n);
			for (int regionIndex = 0; regionIndex < n; regionIndex++) {
				regions.add(new Region(cells[regionIndex]));
			}
			assertEquals(new RegionPartition(size, regions), ClassicRegionPartition.of(size), "partition of " + size);
		}
	}
	
	@Test
	void classicRegionPartition_asUtilityClass_hasOnlyAPrivateConstructor() {
		Constructor<?>[] constructors = ClassicRegionPartition.class.getDeclaredConstructors();
		assertAll(
			() -> assertEquals(1, constructors.length),
			() -> assertTrue(Modifier.isPrivate(constructors[0].getModifiers())),
			() -> assertEquals(0, constructors[0].getParameterCount()),
			() -> assertTrue(Modifier.isFinal(ClassicRegionPartition.class.getModifiers()))
		);
	}
}
