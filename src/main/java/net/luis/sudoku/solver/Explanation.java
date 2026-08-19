package net.luis.sudoku.solver;

import java.util.*;

/**
 * Why a technique's {@link Deduction} follows, as an ordered list of highlight steps.
 * <p>
 *     A deduction says <i>what</i> to do; an explanation says <i>why</i>, which is everything the solver already knew
 *     while it searched and then threw away. It exists for teaching: an animation that reveals a pattern step by
 *     step, a hint that marks the cells to look at without giving the answer, and a check that the player found the
 *     pattern rather than guessed all read the same structure.
 * </p>
 * <p>
 *     An explanation is never user-facing text. It carries cells, digits, units and {@link StepKind narration beats},
 *     and a consumer turns those into sentences in its own language.
 * </p>
 *
 * @param technique The technique being explained
 * @param steps The steps, in the order they should be shown
 *
 * @see ExplainedDeduction
 */
public record Explanation(Technique technique, List<ExplanationStep> steps) {
	
	/**
	 * Constructs an explanation, copying the step list so the record stays an immutable value.
	 *
	 * @throws NullPointerException If the technique or the step list is null, or the list holds a null element
	 * @throws IllegalArgumentException If there are no steps
	 */
	public Explanation {
		Objects.requireNonNull(technique, "Technique must not be null");
		Objects.requireNonNull(steps, "Steps must not be null");
		
		if (steps.isEmpty()) {
			throw new IllegalArgumentException("An explanation must have at least one step");
		}
		
		steps = List.copyOf(steps);
	}
	
	/**
	 * Builds the explanation every strategy can give without any extra work: the conclusion alone.
	 * <p>
	 *     This is the fallback {@link TechniqueStrategy#findExplained(CandidateGrid)} uses for a strategy that has not
	 *     been taught to explain itself yet. It is honest rather than useful — it highlights what changes and claims
	 *     nothing about why — so a consumer that needs a real pattern must check {@link #isConclusionOnly()}.
	 * </p>
	 *
	 * @param deduction The deduction to describe
	 * @return The explanation
	 * @throws NullPointerException If the deduction is null
	 */
	public static Explanation conclusionOnly(Deduction deduction) {
		Objects.requireNonNull(deduction, "Deduction must not be null");
		
		if (deduction instanceof Deduction.Placement placement) {
			return new Explanation(placement.technique(), List.of(
				ExplanationStep.of(StepKind.PLACEMENT, placement.digit(), List.of(PatternCell.of(placement.cell(), CellRole.TARGET, placement.digit())))
			));
		}
		
		Deduction.Eliminations eliminations = (Deduction.Eliminations) deduction;
		int[] cells = eliminations.cells();
		int[] digits = eliminations.digits();
		List<PatternCell> targets = new ArrayList<>(cells.length);
		for (int index = 0; index < cells.length; index++) {
			targets.add(PatternCell.of(cells[index], CellRole.TARGET, digits[index]));
		}
		return new Explanation(eliminations.technique(), List.of(ExplanationStep.of(StepKind.ELIMINATION, 0, targets)));
	}
	
	/**
	 * Creates a builder for an explanation of the given technique.
	 *
	 * @param technique The technique being explained
	 * @return A fresh builder
	 * @throws NullPointerException If the technique is null
	 */
	public static Builder builder(Technique technique) {
		return new Builder(technique);
	}
	
	/**
	 * Checks whether this explanation only restates the conclusion, without showing the pattern that proves it.
	 * <p>
	 *     True means the strategy has no real explanation yet: there is exactly one step and it is the conclusion. A
	 *     consumer that teaches a technique must treat this as "cannot teach this one" rather than showing a step of
	 *     one.
	 * </p>
	 *
	 * @return True if the only step is the conclusion
	 */
	public boolean isConclusionOnly() {
		if (this.steps.size() != 1) {
			return false;
		}
		StepKind kind = this.steps.get(0).kind();
		return kind == StepKind.PLACEMENT || kind == StepKind.ELIMINATION;
	}
	
	/**
	 * Returns every cell any step highlights, flattened, keeping the order the steps introduced them in.
	 * <p>
	 *     A cell that appears in several steps appears several times, once per role it was shown in, because a cell
	 *     can genuinely change role as an argument develops.
	 * </p>
	 *
	 * @return The pattern cells of every step
	 */
	public List<PatternCell> allCells() {
		List<PatternCell> result = new ArrayList<>();
		for (ExplanationStep step : this.steps) {
			result.addAll(step.cells());
		}
		return List.copyOf(result);
	}
	
	/**
	 * Accumulates the steps of an explanation, so a strategy can describe its pattern in the order it found it.
	 */
	public static final class Builder {
		
		private final Technique technique;
		private final List<ExplanationStep> steps = new ArrayList<>();
		
		private Builder(Technique technique) {
			this.technique = Objects.requireNonNull(technique, "Technique must not be null");
		}
		
		/**
		 * Adds a step that names the digit the whole argument is about.
		 *
		 * @param digit The digit
		 * @return This builder
		 */
		public Builder focusDigit(int digit) {
			this.steps.add(ExplanationStep.ofUnits(StepKind.FOCUS_DIGIT, digit, List.of()));
			return this;
		}
		
		/**
		 * Adds a step that outlines the units the pattern is defined on.
		 *
		 * @param digit The digit the units are about, or {@code 0}
		 * @param units The units
		 * @return This builder
		 */
		public Builder focusUnits(int digit, List<UnitRef> units) {
			this.steps.add(ExplanationStep.ofUnits(StepKind.FOCUS_UNIT, digit, units));
			return this;
		}
		
		/**
		 * Adds a step that shows part of the pattern.
		 *
		 * @param digit The digit the cells are about, or {@code 0}
		 * @param cells The cells
		 * @return This builder
		 */
		public Builder pattern(int digit, List<PatternCell> cells) {
			this.steps.add(ExplanationStep.of(StepKind.PATTERN, digit, cells));
			return this;
		}
		
		/**
		 * Adds a step that shows one inference of a chain or wing.
		 *
		 * @param digit The digit the link is about, or {@code 0}
		 * @param cells The linked cells
		 * @return This builder
		 */
		public Builder link(int digit, List<PatternCell> cells) {
			this.steps.add(ExplanationStep.of(StepKind.LINK, digit, cells));
			return this;
		}
		
		/**
		 * Adds the reasoning beat between the pattern and its conclusion.
		 *
		 * @param digit The digit the implication is about, or {@code 0}
		 * @param cells The cells the implication rests on
		 * @return This builder
		 */
		public Builder implication(int digit, List<PatternCell> cells) {
			this.steps.add(ExplanationStep.of(StepKind.IMPLICATION, digit, cells));
			return this;
		}
		
		/**
		 * Adds the conclusion, derived from the deduction itself so it can never disagree with it.
		 *
		 * @param deduction The deduction being explained
		 * @return This builder
		 * @throws NullPointerException If the deduction is null
		 */
		public Builder conclusion(Deduction deduction) {
			this.steps.addAll(Explanation.conclusionOnly(deduction).steps());
			return this;
		}
		
		/**
		 * Builds the explanation.
		 *
		 * @return The explanation
		 * @throws IllegalArgumentException If no step was added
		 */
		public Explanation build() {
			return new Explanation(this.technique, this.steps);
		}
	}
}
