package net.luis.sudoku.version;

/**
 * The version stamp of the puzzle generation pipeline.
 * <p>
 *     {@link #CURRENT} must be incremented whenever the generator, the region builder, the hole-digging order or
 *     the difficulty rater changes. The rater in particular is expected to be tuned repeatedly, and every one of
 *     those changes makes the same key produce a different puzzle.
 * </p>
 * <p>
 *     Saved games store the full puzzle key including its generator version, so a game resumed after the
 *     generator changed still reproduces its original puzzle. Share codes store it too, which is what lets anyone
 *     regenerate the identical puzzle offline.
 * </p>
 * <p>
 *     Client and server must agree on the generator version. A mismatch means the two sides would generate
 *     divergent puzzles from the same key, so it is refused at connect time rather than silently tolerated.
 * </p>
 */
public final class GenVersion {
	
	/**
	 * The generator version this build produces puzzles with.
	 * <p>
	 *     Increment this on every change to the generator, the region builder, the hole-digging order or the
	 *     difficulty rater.
	 * </p>
	 */
	public static final int CURRENT = 2;
	
	private GenVersion() {}
}
