package net.luis.sudoku.solver;

import java.util.List;
import java.util.Objects;

/**
 * One beat of an {@link Explanation}: what to highlight, and what is being said while it is highlighted.
 * <p>
 *     Steps are meant to be played in order, each adding to what the previous ones showed rather than replacing it,
 *     which is how a pattern assembles itself in front of a player. A consumer that only wants a static picture can
 *     flatten every step into one set of highlights and lose nothing but the timing.
 * </p>
 *
 * @param kind What this step is saying
 * @param digit The digit the step is about, or {@code 0} if it is not about one digit
 * @param cells The cells to highlight, with their roles
 * @param units The units to outline
 *
 * @see StepKind
 */
public record ExplanationStep(StepKind kind, int digit, List<PatternCell> cells, List<UnitRef> units) {

	/**
	 * Constructs a step, copying both lists so the record stays an immutable value.
	 *
	 * @throws NullPointerException If the kind or either list is null, or if either list holds a null element
	 * @throws IllegalArgumentException If the digit is negative
	 */
	public ExplanationStep {
		Objects.requireNonNull(kind, "Step kind must not be null");
		Objects.requireNonNull(cells, "Cells must not be null");
		Objects.requireNonNull(units, "Units must not be null");

		if (digit < 0) {
			throw new IllegalArgumentException("Digit must not be negative, but was " + digit);
		}

		cells = List.copyOf(cells);
		units = List.copyOf(units);
	}

	/**
	 * Creates a step that highlights cells only.
	 *
	 * @param kind What the step is saying
	 * @param digit The digit it is about, or {@code 0}
	 * @param cells The cells to highlight
	 * @return The step
	 */
	public static ExplanationStep of(StepKind kind, int digit, List<PatternCell> cells) {
		return new ExplanationStep(kind, digit, cells, List.of());
	}

	/**
	 * Creates a step that outlines units only.
	 *
	 * @param kind What the step is saying
	 * @param digit The digit it is about, or {@code 0}
	 * @param units The units to outline
	 * @return The step
	 */
	public static ExplanationStep ofUnits(StepKind kind, int digit, List<UnitRef> units) {
		return new ExplanationStep(kind, digit, List.of(), units);
	}
}
