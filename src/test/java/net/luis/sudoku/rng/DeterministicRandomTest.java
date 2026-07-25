package net.luis.sudoku.rng;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link DeterministicRandom}.
 */
class DeterministicRandomTest {
	
	private static final int[] GOLDEN_NEXT_INT_100 = { 51, 80, 41, 28, 55, 84, 75, 2, 1, 89 };
	private static final long[] GOLDEN_NEXT_LONG = {
		6674089274190705457L, -1236052134575208584L, -3078921119283744887L, 6022414958441676900L,
		4344647195749500666L, 6440041613324510652L, 8265573421575953197L, -6674900032466492448L,
		2919502189498201214L, -4263262838430303025L
	};
	
	/**
	 * Draws a long, mixed sequence of {@code nextInt}, {@code nextLong} and {@code nextBoolean} values.
	 * <p>
	 *     The draw pattern only depends on the position in the sequence, so two generators fed through this
	 *     method make exactly the same calls in exactly the same order.
	 * </p>
	 *
	 * @param random The generator to draw from
	 * @param draws The number of values to draw
	 * @return The drawn values, widened to long
	 */
	private static long[] mixedSequence(DeterministicRandom random, int draws) {
		long[] values = new long[draws];
		for (int i = 0; i < draws; i++) {
			values[i] = switch (i % 4) {
				case 0 -> random.nextInt(1000);
				case 1 -> random.nextLong();
				case 2 -> random.nextBoolean() ? 1L : 0L;
				default -> random.nextInt(-50, 50);
			};
		}
		return values;
	}
	
	/**
	 * Shuffles the given array with a hand-written downward Fisher-Yates loop driven by a plain {@link Random}.
	 * <p>
	 *     This is the independent oracle for {@link DeterministicRandom#shuffle(int[])}, written out longhand so
	 *     that a future refactor of the production method cannot silently change the permutation.
	 * </p>
	 *
	 * @param values The array to shuffle in place
	 * @param seed The seed of the oracle generator
	 */
	private static void referenceShuffle(int[] values, long seed) {
		Random random = new Random(seed);
		for (int i = values.length - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			int swap = values[i];
			values[i] = values[j];
			values[j] = swap;
		}
	}
	
	/**
	 * Builds the ascending array {@code 0..size - 1}.
	 *
	 * @param size The number of elements
	 * @return The ascending array
	 */
	private static int[] range(int size) {
		int[] values = new int[size];
		for (int i = 0; i < size; i++) {
			values[i] = i;
		}
		return values;
	}
	
	@Test
	void deterministicRandom_sameSeed_producesIdenticalMixedSequences() {
		long[] first = mixedSequence(new DeterministicRandom(2024L), 1000);
		long[] second = mixedSequence(new DeterministicRandom(2024L), 1000);
		for (int i = 0; i < first.length; i++) {
			assertEquals(first[i], second[i], "draw " + i);
		}
	}
	
	@Test
	void deterministicRandom_sameSeedAcrossManySeeds_producesIdenticalMixedSequences() {
		for (long seed : new long[] { 0L, 1L, -1L, 42L, Long.MIN_VALUE, Long.MAX_VALUE }) {
			assertArrayEquals(
				mixedSequence(new DeterministicRandom(seed), 1000),
				mixedSequence(new DeterministicRandom(seed), 1000),
				"seed " + seed
			);
		}
	}
	
	@Test
	void deterministicRandom_differentSeeds_produceDifferentSequences() {
		long[] first = mixedSequence(new DeterministicRandom(1L), 1000);
		long[] second = mixedSequence(new DeterministicRandom(2L), 1000);
		long[] third = mixedSequence(new DeterministicRandom(Long.MIN_VALUE), 1000);
		assertAll(
			() -> assertFalse(Arrays.equals(first, second)),
			() -> assertFalse(Arrays.equals(first, third)),
			() -> assertFalse(Arrays.equals(second, third))
		);
	}
	
	@Test
	void deterministicRandom_twoInstances_doNotShareState() {
		DeterministicRandom first = new DeterministicRandom(7L);
		DeterministicRandom second = new DeterministicRandom(7L);
		first.nextLong();
		first.nextLong();
		assertEquals(new DeterministicRandom(7L).nextLong(), second.nextLong());
	}
	
	@Test
	void reset_afterManyDraws_reproducesTheSameSequence() {
		DeterministicRandom random = new DeterministicRandom(99L);
		long[] first = mixedSequence(random, 1000);
		random.reset();
		long[] second = mixedSequence(random, 1000);
		for (int i = 0; i < first.length; i++) {
			assertEquals(first[i], second[i], "draw " + i);
		}
	}
	
	@Test
	void reset_calledRepeatedly_alwaysRestartsFromTheSeed() {
		DeterministicRandom random = new DeterministicRandom(-13L);
		long expected = random.nextLong();
		assertAll(
			() -> {
				random.reset();
				assertEquals(expected, random.nextLong());
			},
			() -> {
				random.nextLong();
				random.reset();
				assertEquals(expected, random.nextLong());
			}
		);
	}
	
	@Test
	void reset_withoutAnyDraws_isANoOp() {
		DeterministicRandom random = new DeterministicRandom(5L);
		random.reset();
		assertEquals(new DeterministicRandom(5L).nextLong(), random.nextLong());
	}
	
	@Test
	void reset_afterShuffledRange_reproducesTheSamePermutation() {
		DeterministicRandom random = new DeterministicRandom(31L);
		int[] first = random.shuffledRange(50);
		random.reset();
		assertArrayEquals(first, random.shuffledRange(50));
	}
	
	@Test
	void nextInt_seed12345_matchesTheGoldenValues() {
		DeterministicRandom random = new DeterministicRandom(12345L);
		int[] actual = new int[GOLDEN_NEXT_INT_100.length];
		for (int i = 0; i < actual.length; i++) {
			actual[i] = random.nextInt(100);
		}
		assertArrayEquals(GOLDEN_NEXT_INT_100, actual);
	}
	
	@Test
	void nextLong_seed12345_matchesTheGoldenValues() {
		DeterministicRandom random = new DeterministicRandom(12345L);
		long[] actual = new long[GOLDEN_NEXT_LONG.length];
		for (int i = 0; i < actual.length; i++) {
			actual[i] = random.nextLong();
		}
		assertArrayEquals(GOLDEN_NEXT_LONG, actual);
	}
	
	@Test
	void nextInt_seed12345_matchesAPlainJavaUtilRandom() {
		DeterministicRandom random = new DeterministicRandom(12345L);
		Random oracle = new Random(12345L);
		for (int i = 0; i < GOLDEN_NEXT_INT_100.length; i++) {
			assertEquals(oracle.nextInt(100), random.nextInt(100), "draw " + i);
		}
	}
	
	@Test
	void nextLong_seed12345_matchesAPlainJavaUtilRandom() {
		DeterministicRandom random = new DeterministicRandom(12345L);
		Random oracle = new Random(12345L);
		for (int i = 0; i < GOLDEN_NEXT_LONG.length; i++) {
			assertEquals(oracle.nextLong(), random.nextLong(), "draw " + i);
		}
	}
	
	@Test
	void nextInt_positiveBound_staysInRange() {
		DeterministicRandom random = new DeterministicRandom(17L);
		for (int i = 0; i < 5000; i++) {
			int bound = 1 + i % 37;
			int value = random.nextInt(bound);
			assertTrue(value >= 0 && value < bound, "draw " + i + " was " + value + " for bound " + bound);
		}
	}
	
	@Test
	void nextInt_boundOfOne_alwaysReturnsZero() {
		DeterministicRandom random = new DeterministicRandom(3L);
		for (int i = 0; i < 100; i++) {
			assertEquals(0, random.nextInt(1), "draw " + i);
		}
	}
	
	@Test
	void nextInt_maximumBound_staysInRange() {
		DeterministicRandom random = new DeterministicRandom(8L);
		for (int i = 0; i < 100; i++) {
			int value = random.nextInt(Integer.MAX_VALUE);
			assertTrue(value >= 0, "draw " + i + " was " + value);
		}
	}
	
	@Test
	void nextInt_everyValueOfASmallBound_isEventuallyProduced() {
		DeterministicRandom random = new DeterministicRandom(4L);
		boolean[] seen = new boolean[6];
		for (int i = 0; i < 1000; i++) {
			seen[random.nextInt(6)] = true;
		}
		for (int value = 0; value < seen.length; value++) {
			assertTrue(seen[value], "value " + value);
		}
	}
	
	@Test
	void nextInt_boundOfZeroOrNegative_throws() {
		DeterministicRandom random = new DeterministicRandom(1L);
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> random.nextInt(0)),
			() -> assertThrows(IllegalArgumentException.class, () -> random.nextInt(-1)),
			() -> assertThrows(IllegalArgumentException.class, () -> random.nextInt(-100)),
			() -> assertThrows(IllegalArgumentException.class, () -> random.nextInt(Integer.MIN_VALUE))
		);
	}
	
	@Test
	void nextInt_originAndBound_staysInRange() {
		DeterministicRandom random = new DeterministicRandom(23L);
		for (int i = 0; i < 5000; i++) {
			int origin = -100 + i % 13;
			int bound = origin + 1 + i % 29;
			int value = random.nextInt(origin, bound);
			assertTrue(value >= origin && value < bound, "draw " + i + " was " + value + " for [" + origin + ", " + bound + ")");
		}
	}
	
	@Test
	void nextInt_originAndBoundOneApart_alwaysReturnsTheOrigin() {
		DeterministicRandom random = new DeterministicRandom(6L);
		assertAll(
			() -> assertEquals(0, random.nextInt(0, 1)),
			() -> assertEquals(5, random.nextInt(5, 6)),
			() -> assertEquals(-7, random.nextInt(-7, -6)),
			() -> assertEquals(Integer.MAX_VALUE - 1, random.nextInt(Integer.MAX_VALUE - 1, Integer.MAX_VALUE)),
			() -> assertEquals(Integer.MIN_VALUE, random.nextInt(Integer.MIN_VALUE, Integer.MIN_VALUE + 1))
		);
	}
	
	@Test
	void nextInt_negativeOriginAndBound_staysInRange() {
		DeterministicRandom random = new DeterministicRandom(11L);
		for (int i = 0; i < 1000; i++) {
			int value = random.nextInt(-20, -5);
			assertTrue(value >= -20 && value < -5, "draw " + i + " was " + value);
		}
	}
	
	@Test
	void nextInt_spanExceedingTheIntRange_staysInRange() {
		DeterministicRandom random = new DeterministicRandom(77L);
		for (int i = 0; i < 1000; i++) {
			int value = random.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
			assertTrue(value >= Integer.MIN_VALUE && value < Integer.MAX_VALUE, "draw " + i + " was " + value);
		}
	}
	
	@Test
	void nextInt_equalOriginAndBound_throws() {
		DeterministicRandom random = new DeterministicRandom(1L);
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> random.nextInt(0, 0)),
			() -> assertThrows(IllegalArgumentException.class, () -> random.nextInt(5, 5)),
			() -> assertThrows(IllegalArgumentException.class, () -> random.nextInt(-3, -3)),
			() -> assertThrows(IllegalArgumentException.class, () -> random.nextInt(Integer.MIN_VALUE, Integer.MIN_VALUE))
		);
	}
	
	@Test
	void nextInt_originGreaterThanBound_throws() {
		DeterministicRandom random = new DeterministicRandom(1L);
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> random.nextInt(1, 0)),
			() -> assertThrows(IllegalArgumentException.class, () -> random.nextInt(10, -10)),
			() -> assertThrows(IllegalArgumentException.class, () -> random.nextInt(-5, -10)),
			() -> assertThrows(IllegalArgumentException.class, () -> random.nextInt(Integer.MAX_VALUE, Integer.MIN_VALUE))
		);
	}
	
	@Test
	void nextBoolean_manyDraws_producesBothValues() {
		DeterministicRandom random = new DeterministicRandom(21L);
		boolean sawTrue = false;
		boolean sawFalse = false;
		for (int i = 0; i < 100; i++) {
			if (random.nextBoolean()) {
				sawTrue = true;
			} else {
				sawFalse = true;
			}
		}
		boolean actualTrue = sawTrue;
		boolean actualFalse = sawFalse;
		assertAll(
			() -> assertTrue(actualTrue),
			() -> assertTrue(actualFalse)
		);
	}
	
	@Test
	void nextBoolean_sameSeed_producesTheSameFlags() {
		DeterministicRandom first = new DeterministicRandom(64L);
		DeterministicRandom second = new DeterministicRandom(64L);
		for (int i = 0; i < 200; i++) {
			assertEquals(first.nextBoolean(), second.nextBoolean(), "draw " + i);
		}
	}
	
	@Test
	void shuffle_sameSeed_shufflesEqualArraysIdentically() {
		int[] first = range(100);
		int[] second = range(100);
		new DeterministicRandom(1234L).shuffle(first);
		new DeterministicRandom(1234L).shuffle(second);
		assertArrayEquals(first, second);
	}
	
	@Test
	void shuffle_differentSeeds_shuffleEqualArraysDifferently() {
		int[] first = range(100);
		int[] second = range(100);
		new DeterministicRandom(1L).shuffle(first);
		new DeterministicRandom(2L).shuffle(second);
		assertFalse(Arrays.equals(first, second));
	}
	
	@Test
	void shuffle_anyArray_matchesAHandWrittenFisherYates() {
		for (int size : new int[] { 2, 3, 9, 16, 81, 256 }) {
			for (long seed : new long[] { 0L, 1L, -1L, 12345L }) {
				int[] actual = range(size);
				new DeterministicRandom(seed).shuffle(actual);
				int[] expected = range(size);
				referenceShuffle(expected, seed);
				assertArrayEquals(expected, actual, "size " + size + ", seed " + seed);
			}
		}
	}
	
	@Test
	void shuffle_arrayWithDuplicatesAndNegatives_matchesAHandWrittenFisherYates() {
		int[] actual = { 5, -3, 5, 0, 17, -3, 8, 8, 1 };
		int[] expected = actual.clone();
		new DeterministicRandom(555L).shuffle(actual);
		referenceShuffle(expected, 555L);
		assertArrayEquals(expected, actual);
	}
	
	@Test
	void shuffle_anyArray_keepsTheSameMultiset() {
		DeterministicRandom random = new DeterministicRandom(9L);
		for (int size = 0; size < 60; size++) {
			int[] values = range(size);
			random.shuffle(values);
			int[] sorted = values.clone();
			Arrays.sort(sorted);
			assertArrayEquals(range(size), sorted, "size " + size);
		}
	}
	
	@Test
	void shuffle_arrayWithDuplicates_keepsTheSameMultiset() {
		int[] values = { 3, 3, 3, 1, 1, 7, -2, -2, -2, -2 };
		int[] expected = values.clone();
		Arrays.sort(expected);
		new DeterministicRandom(88L).shuffle(values);
		int[] sorted = values.clone();
		Arrays.sort(sorted);
		assertArrayEquals(expected, sorted);
	}
	
	@Test
	void shuffle_emptyArray_isANoOp() {
		int[] values = new int[0];
		DeterministicRandom random = new DeterministicRandom(2L);
		random.shuffle(values);
		assertAll(
			() -> assertEquals(0, values.length),
			() -> assertEquals(new DeterministicRandom(2L).nextLong(), random.nextLong())
		);
	}
	
	@Test
	void shuffle_singleElementArray_isANoOp() {
		int[] values = { 42 };
		DeterministicRandom random = new DeterministicRandom(2L);
		random.shuffle(values);
		assertAll(
			() -> assertArrayEquals(new int[] { 42 }, values),
			() -> assertEquals(new DeterministicRandom(2L).nextLong(), random.nextLong())
		);
	}
	
	@Test
	void shuffle_twoElementArray_eventuallySwaps() {
		DeterministicRandom random = new DeterministicRandom(2L);
		boolean sawSwapped = false;
		boolean sawUnchanged = false;
		for (int i = 0; i < 100; i++) {
			int[] values = { 0, 1 };
			random.shuffle(values);
			if (values[0] == 1) {
				sawSwapped = true;
			} else {
				sawUnchanged = true;
			}
		}
		boolean actualSwapped = sawSwapped;
		boolean actualUnchanged = sawUnchanged;
		assertAll(
			() -> assertTrue(actualSwapped),
			() -> assertTrue(actualUnchanged)
		);
	}
	
	@Test
	void shuffle_largeArray_doesNotLeaveTheOriginalOrder() {
		int[] values = range(1000);
		new DeterministicRandom(1L).shuffle(values);
		assertFalse(Arrays.equals(range(1000), values));
	}
	
	@Test
	void shuffle_anyArray_mutatesTheGivenArrayInPlace() {
		int[] values = range(20);
		int[] expected = range(20);
		new DeterministicRandom(3L).shuffle(expected);
		new DeterministicRandom(3L).shuffle(values);
		assertAll(
			() -> assertArrayEquals(expected, values),
			() -> assertFalse(Arrays.equals(range(20), values))
		);
	}
	
	@Test
	void shuffle_nullArray_throws() {
		DeterministicRandom random = new DeterministicRandom(1L);
		assertThrows(NullPointerException.class, () -> random.shuffle(null));
	}
	
	@Test
	void shuffledRange_variousSizes_returnsAPermutation() {
		DeterministicRandom random = new DeterministicRandom(1_000L);
		for (int size : new int[] { 2, 3, 4, 6, 9, 12, 16, 81, 144, 256, 500 }) {
			int[] values = random.shuffledRange(size);
			int[] sorted = values.clone();
			Arrays.sort(sorted);
			assertAll(
				() -> assertEquals(size, values.length, "size " + size),
				() -> assertArrayEquals(range(size), sorted, "size " + size)
			);
		}
	}
	
	@Test
	void shuffledRange_sizeOfZero_returnsAnEmptyArray() {
		assertArrayEquals(new int[0], new DeterministicRandom(1L).shuffledRange(0));
	}
	
	@Test
	void shuffledRange_sizeOfOne_returnsZeroOnly() {
		assertArrayEquals(new int[] { 0 }, new DeterministicRandom(1L).shuffledRange(1));
	}
	
	@Test
	void shuffledRange_sameSeed_returnsTheSamePermutation() {
		assertArrayEquals(
			new DeterministicRandom(4321L).shuffledRange(200),
			new DeterministicRandom(4321L).shuffledRange(200)
		);
	}
	
	@Test
	void shuffledRange_differentSeeds_returnDifferentPermutations() {
		assertFalse(Arrays.equals(
			new DeterministicRandom(1L).shuffledRange(200),
			new DeterministicRandom(2L).shuffledRange(200)
		));
	}
	
	@Test
	void shuffledRange_largeSize_doesNotReturnTheAscendingOrder() {
		assertFalse(Arrays.equals(range(1000), new DeterministicRandom(1L).shuffledRange(1000)));
	}
	
	@Test
	void shuffledRange_matchesShuffleOfTheAscendingArray() {
		int[] expected = range(64);
		new DeterministicRandom(64L).shuffle(expected);
		assertArrayEquals(expected, new DeterministicRandom(64L).shuffledRange(64));
	}
	
	@Test
	void shuffledRange_repeatedCalls_returnIndependentArrays() {
		DeterministicRandom random = new DeterministicRandom(12L);
		int[] first = random.shuffledRange(10);
		int[] second = random.shuffledRange(10);
		first[0] = -1;
		assertAll(
			() -> assertNotSame(first, second),
			() -> assertNotEquals(-1, second[0])
		);
	}
	
	@Test
	void shuffledRange_negativeSize_throws() {
		DeterministicRandom random = new DeterministicRandom(1L);
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, () -> random.shuffledRange(-1)),
			() -> assertThrows(IllegalArgumentException.class, () -> random.shuffledRange(-100)),
			() -> assertThrows(IllegalArgumentException.class, () -> random.shuffledRange(Integer.MIN_VALUE))
		);
	}
	
	@Test
	void seed_anySeed_returnsTheConstructorArgument() {
		assertAll(
			() -> assertEquals(0L, new DeterministicRandom(0L).seed()),
			() -> assertEquals(1L, new DeterministicRandom(1L).seed()),
			() -> assertEquals(12345L, new DeterministicRandom(12345L).seed()),
			() -> assertEquals(-1L, new DeterministicRandom(-1L).seed()),
			() -> assertEquals(-987654321L, new DeterministicRandom(-987654321L).seed()),
			() -> assertEquals(Long.MIN_VALUE, new DeterministicRandom(Long.MIN_VALUE).seed()),
			() -> assertEquals(Long.MAX_VALUE, new DeterministicRandom(Long.MAX_VALUE).seed())
		);
	}
	
	@Test
	void seed_afterDrawingAndResetting_isUnchanged() {
		DeterministicRandom random = new DeterministicRandom(-42L);
		random.nextLong();
		random.shuffledRange(20);
		assertEquals(-42L, random.seed());
		random.reset();
		assertEquals(-42L, random.seed());
	}
	
	@Test
	void toString_anySeed_containsTheSeed() {
		assertAll(
			() -> assertEquals("DeterministicRandom[seed=12345]", new DeterministicRandom(12345L).toString()),
			() -> assertEquals("DeterministicRandom[seed=-1]", new DeterministicRandom(-1L).toString())
		);
	}
}
