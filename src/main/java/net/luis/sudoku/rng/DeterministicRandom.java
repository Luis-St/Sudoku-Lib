package net.luis.sudoku.rng;

import java.util.Random;

/**
 * The single source of randomness for puzzle generation, seeded from a puzzle key.
 * <p>
 *     A puzzle is a pure function of its key, so every draw the generator makes has to be reproducible on any
 *     device, forever. This class is a thin wrapper over {@link Random} because {@link Random} is the only
 *     JDK generator whose algorithm is written down: its Javadoc specifies the exact 48-bit linear congruential
 *     formula, the multiplier, the addend and how {@code next(int)} feeds every other method. An implementation
 *     that deviates is not a conforming {@link Random}. That makes the sequence portable across JVM vendors,
 *     across JVM versions and across Android, which is what lets the client and the server produce byte-identical
 *     puzzles from the same key (spec §3.2).
 * </p>
 * <p>
 *     {@code SplittableRandom} and {@code ThreadLocalRandom} leave their algorithms unspecified — their Javadoc
 *     documents statistical quality, not the bit sequence — so two runtimes may legally disagree on what they
 *     emit. They are forbidden by spec §3.2 and must never appear anywhere in this library. For the same reason
 *     this class deliberately does not expose {@code doubles}, {@code ints} or any other stream: streams invite
 *     parallelism, and parallelism destroys reproducibility.
 * </p>
 * <p>
 *     Instances are not thread-safe in any useful sense. Sharing one across threads would make the draw order
 *     depend on scheduling and therefore make generation non-deterministic, so an instance belongs to exactly
 *     one generation run.
 * </p>
 *
 * @apiNote The exact algorithm of {@link #shuffle(int[])} is part of this library's observable output. Changing
 * it — the iteration direction, the bound passed to {@link #nextInt(int)}, or which draws are consumed — changes
 * every puzzle this library has ever generated and therefore requires incrementing the generator version, so
 * that saved games and share codes created by an older build are still recognised as belonging to an older
 * generator. The same applies to {@link #shuffledRange(int)} and to the number of draws any method consumes.
 */
public final class DeterministicRandom {
	
	private final Random random;
	private final long seed;
	
	/**
	 * Constructs a generator seeded with the given value.
	 * <p>
	 *     Two instances constructed with the same seed produce the same sequence, on every JVM and forever.
	 * </p>
	 *
	 * @param seed The seed, typically derived from a puzzle key
	 */
	public DeterministicRandom(long seed) {
		this.random = new Random(seed);
		this.seed = seed;
	}
	
	/**
	 * Returns the seed this instance was constructed with.
	 *
	 * @return The seed
	 */
	public long seed() {
		return this.seed;
	}
	
	/**
	 * Re-seeds this generator back to {@link #seed()}, discarding every draw made so far.
	 * <p>
	 *     A bounded retry loop uses this to restart an attempt from an identical state, so that the n-th attempt
	 *     depends only on the key and on n, never on how many draws the previous attempts happened to consume.
	 * </p>
	 */
	public void reset() {
		this.random.setSeed(this.seed);
	}
	
	/**
	 * Returns the next pseudorandom integer between zero inclusive and the given bound exclusive.
	 *
	 * @param bound The exclusive upper bound
	 * @return A value in {@code [0, bound)}
	 * @throws IllegalArgumentException If the bound is less than one
	 */
	public int nextInt(int bound) {
		if (bound < 1) {
			throw new IllegalArgumentException("Bound " + bound + " must be positive");
		}
		return this.random.nextInt(bound);
	}
	
	/**
	 * Returns the next pseudorandom integer between the given origin inclusive and the given bound exclusive.
	 * <p>
	 *     This is implemented on top of {@link #nextInt(int)} rather than delegating to {@code Random}'s own
	 *     two-argument overload, because that overload is inherited from the {@code RandomGenerator} interface,
	 *     whose rejection scheme is not part of the specified {@link Random} algorithm and is not available on
	 *     every Android release.
	 * </p>
	 *
	 * @param origin The inclusive lower bound
	 * @param bound The exclusive upper bound
	 * @return A value in {@code [origin, bound)}
	 * @throws IllegalArgumentException If the origin is greater than or equal to the bound
	 */
	public int nextInt(int origin, int bound) {
		if (origin >= bound) {
			throw new IllegalArgumentException("Origin " + origin + " must be less than bound " + bound);
		}
		long span = (long) bound - origin;
		if (span <= Integer.MAX_VALUE) {
			return origin + this.nextInt((int) span);
		}
		int value;
		do {
			value = this.random.nextInt();
		} while (value < origin || value >= bound);
		return value;
	}
	
	/**
	 * Returns the next pseudorandom long, uniformly distributed over the whole 64-bit range.
	 *
	 * @return The next long
	 */
	public long nextLong() {
		return this.random.nextLong();
	}
	
	/**
	 * Returns the next pseudorandom boolean.
	 *
	 * @return The next boolean
	 */
	public boolean nextBoolean() {
		return this.random.nextBoolean();
	}
	
	/**
	 * Shuffles the given array in place.
	 * <p>
	 *     This is a plain Fisher-Yates shuffle: {@code i} runs downward from {@code values.length - 1} to
	 *     {@code 1}, and the element at {@code i} is swapped with the element at {@code nextInt(i + 1)}. An array
	 *     of fewer than two elements consumes no draws at all.
	 * </p>
	 * <p>
	 *     The permutation produced is part of the generator's observable output, so this algorithm must never
	 *     change silently — see the API note on this class.
	 * </p>
	 *
	 * @param values The array to shuffle
	 */
	public void shuffle(int[] values) {
		for (int i = values.length - 1; i > 0; i--) {
			int j = this.nextInt(i + 1);
			int swap = values[i];
			values[i] = values[j];
			values[j] = swap;
		}
	}
	
	/**
	 * Returns a shuffled permutation of {@code 0..size - 1}.
	 * <p>
	 *     This is the canonical way to pick a deterministic order over candidate digits, cells or regions.
	 * </p>
	 *
	 * @param size The number of elements
	 * @return A freshly allocated array holding every value in {@code [0, size)} exactly once, shuffled
	 * @throws IllegalArgumentException If the size is negative
	 */
	public int[] shuffledRange(int size) {
		if (size < 0) {
			throw new IllegalArgumentException("Size " + size + " must not be negative");
		}
		int[] values = new int[size];
		for (int i = 0; i < size; i++) {
			values[i] = i;
		}
		this.shuffle(values);
		return values;
	}
	
	@Override
	public String toString() {
		return "DeterministicRandom[seed=" + this.seed + "]";
	}
}
