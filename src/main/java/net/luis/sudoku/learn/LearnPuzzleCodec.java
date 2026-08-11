package net.luis.sudoku.learn;

import net.luis.sudoku.solver.CellRole;
import net.luis.sudoku.solver.Explanation;
import net.luis.sudoku.solver.ExplanationStep;
import net.luis.sudoku.solver.PatternCell;
import net.luis.sudoku.solver.StepKind;
import net.luis.sudoku.solver.Technique;
import net.luis.sudoku.solver.UnitKind;
import net.luis.sudoku.solver.UnitRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reads and writes {@link LearnPuzzle} as JSON, so the exercises can be generated once on a desktop and shipped with
 * the app.
 * <p>
 *     The format is written by hand rather than through a JSON library on purpose: this class lives in the shared
 *     core, which has no dependencies at all, and adding one so that a build-time task can write a few hundred
 *     objects would put a library into every consumer of the core forever. The writer emits a small, fixed shape and
 *     the reader accepts exactly that shape, which is all either end needs.
 * </p>
 * <p>
 *     Every field is written as a compact string rather than an array of numbers. A board is 81 digits, pencil marks
 *     are 81 comma-separated masks, and both are one line instead of eighty. It keeps the shipped asset small and, more
 *     usefully, keeps it readable when something has to be debugged by eye.
 * </p>
 *
 * @see LearnPuzzle
 */
public final class LearnPuzzleCodec {

	private LearnPuzzleCodec() {}

	/**
	 * Writes a list of exercises as a JSON array.
	 *
	 * @param puzzles The exercises
	 * @return The JSON text
	 * @throws NullPointerException If the list is null or holds a null element
	 */
	public static String writeAll(List<LearnPuzzle> puzzles) {
		Objects.requireNonNull(puzzles, "Puzzles must not be null");

		StringBuilder json = new StringBuilder("[");
		for (int index = 0; index < puzzles.size(); index++) {
			if (index > 0) {
				json.append(",");
			}
			json.append("\n").append(write(puzzles.get(index)));
		}
		return json.append("\n]\n").toString();
	}

	/**
	 * Reads back what {@link #writeAll(List)} wrote.
	 * <p>
	 *     Anything outside the objects is ignored, so a caller can hand over a slice of a larger file rather than
	 *     having to cut the array out of it exactly.
	 * </p>
	 *
	 * @param json The JSON text holding the exercises
	 * @return The exercises, in the order they appear, empty if there are none
	 * @throws NullPointerException If the text is null
	 * @throws IllegalArgumentException If an object in it is not in the written shape
	 */
	public static List<LearnPuzzle> readAll(String json) {
		Objects.requireNonNull(json, "Json must not be null");

		List<LearnPuzzle> puzzles = new ArrayList<>();
		int at = json.indexOf(OBJECT_START);
		while (at >= 0) {
			int next = json.indexOf(OBJECT_START, at + 1);
			puzzles.add(read(next < 0 ? json.substring(at) : json.substring(at, next)));
			at = next;
		}
		return List.copyOf(puzzles);
	}

	/**
	 * How one written exercise opens, which is what separates them inside an array. Every exercise starts with its
	 * technique and holds no nested object before the next one begins, so finding these is enough to split them.
	 */
	private static final String OBJECT_START = "{\"technique\":";

	/**
	 * Writes one exercise as a JSON object.
	 *
	 * @param puzzle The exercise
	 * @return The JSON text
	 * @throws NullPointerException If the exercise is null
	 */
	public static String write(LearnPuzzle puzzle) {
		Objects.requireNonNull(puzzle, "Puzzle must not be null");

		StringBuilder json = new StringBuilder("{");
		json.append("\"technique\":\"").append(puzzle.technique().name()).append("\",");
		json.append("\"board\":\"").append(digits(puzzle.board())).append("\",");
		json.append("\"solution\":\"").append(digits(puzzle.solution())).append("\",");
		json.append("\"pencil\":\"").append(masks(puzzle.pencilMarks())).append("\",");
		json.append("\"targetCell\":").append(puzzle.targetCell()).append(",");
		json.append("\"targetDigit\":").append(puzzle.targetDigit()).append(",");
		json.append("\"steps\":").append(writeSteps(puzzle.explanation()));
		return json.append("}").toString();
	}

	private static String writeSteps(Explanation explanation) {
		StringBuilder json = new StringBuilder("[");
		List<ExplanationStep> steps = explanation.steps();
		for (int index = 0; index < steps.size(); index++) {
			if (index > 0) {
				json.append(",");
			}

			ExplanationStep step = steps.get(index);
			json.append("{\"kind\":\"").append(step.kind().name()).append("\",");
			json.append("\"digit\":").append(step.digit()).append(",");
			json.append("\"cells\":\"").append(writeCells(step.cells())).append("\",");
			json.append("\"units\":\"").append(writeUnits(step.units())).append("\"}");
		}
		return json.append("]").toString();
	}

	/**
	 * Writes the pattern cells as {@code cell:role:mask} triples, separated by commas.
	 */
	private static String writeCells(List<PatternCell> cells) {
		StringBuilder text = new StringBuilder();
		for (PatternCell cell : cells) {
			if (text.length() > 0) {
				text.append(",");
			}
			text.append(cell.cell()).append(":").append(cell.role().name()).append(":").append(cell.digits());
		}
		return text.toString();
	}

	/**
	 * Writes the units as {@code kind:index} pairs, separated by commas.
	 */
	private static String writeUnits(List<UnitRef> units) {
		StringBuilder text = new StringBuilder();
		for (UnitRef unit : units) {
			if (text.length() > 0) {
				text.append(",");
			}
			text.append(unit.kind().name()).append(":").append(unit.index());
		}
		return text.toString();
	}

	private static String digits(int[] values) {
		StringBuilder text = new StringBuilder(values.length);
		for (int value : values) {
			text.append((char) ('0' + value));
		}
		return text.toString();
	}

	private static String masks(int[] values) {
		StringBuilder text = new StringBuilder();
		for (int value : values) {
			if (text.length() > 0) {
				text.append(",");
			}
			text.append(value);
		}
		return text.toString();
	}

	/**
	 * Reads back what {@link #write(LearnPuzzle)} wrote.
	 * <p>
	 *     The parser is deliberately strict and minimal: it understands the shape this class writes and nothing else.
	 *     It exists so the export task can prove it wrote something readable, which is the only check that catches a
	 *     format change before the asset reaches a device.
	 * </p>
	 *
	 * @param json The JSON text of one exercise
	 * @return The exercise
	 * @throws NullPointerException If the text is null
	 * @throws IllegalArgumentException If the text is not in the written shape
	 */
	public static LearnPuzzle read(String json) {
		Objects.requireNonNull(json, "Json must not be null");

		Technique technique = Technique.valueOf(stringField(json, "technique"));
		int[] board = readDigits(stringField(json, "board"));
		int[] solution = readDigits(stringField(json, "solution"));
		int[] pencil = readMasks(stringField(json, "pencil"));
		int targetCell = intField(json, "targetCell");
		int targetDigit = intField(json, "targetDigit");

		int stepsAt = json.indexOf("\"steps\":[");
		if (stepsAt < 0) {
			throw new IllegalArgumentException("No steps in " + json);
		}
		Explanation explanation = readSteps(technique, json.substring(stepsAt + "\"steps\":".length()));
		return new LearnPuzzle(technique, board, solution, pencil, targetCell, targetDigit, explanation);
	}

	private static Explanation readSteps(Technique technique, String json) {
		List<ExplanationStep> steps = new ArrayList<>();
		int at = 0;
		while (true) {
			int open = json.indexOf("{\"kind\":", at);
			if (open < 0) {
				break;
			}

			int close = json.indexOf("}", open);
			String step = json.substring(open, close + 1);
			StepKind kind = StepKind.valueOf(stringField(step, "kind"));
			int digit = intField(step, "digit");
			steps.add(new ExplanationStep(kind, digit, readCells(stringField(step, "cells")), readUnits(stringField(step, "units"))));
			at = close + 1;
		}

		if (steps.isEmpty()) {
			throw new IllegalArgumentException("No steps in " + json);
		}
		return new Explanation(technique, steps);
	}

	private static List<PatternCell> readCells(String text) {
		List<PatternCell> cells = new ArrayList<>();
		if (text.isEmpty()) {
			return cells;
		}

		for (String entry : text.split(",")) {
			String[] parts = entry.split(":");
			if (parts.length != 3) {
				throw new IllegalArgumentException("Malformed pattern cell " + entry);
			}
			cells.add(new PatternCell(Integer.parseInt(parts[0]), CellRole.valueOf(parts[1]), Integer.parseInt(parts[2])));
		}
		return cells;
	}

	private static List<UnitRef> readUnits(String text) {
		List<UnitRef> units = new ArrayList<>();
		if (text.isEmpty()) {
			return units;
		}

		for (String entry : text.split(",")) {
			String[] parts = entry.split(":");
			if (parts.length != 2) {
				throw new IllegalArgumentException("Malformed unit " + entry);
			}
			units.add(new UnitRef(UnitKind.valueOf(parts[0]), Integer.parseInt(parts[1])));
		}
		return units;
	}

	private static int[] readDigits(String text) {
		int[] values = new int[text.length()];
		for (int index = 0; index < values.length; index++) {
			values[index] = text.charAt(index) - '0';
		}
		return values;
	}

	private static int[] readMasks(String text) {
		String[] parts = text.split(",");
		int[] values = new int[parts.length];
		for (int index = 0; index < parts.length; index++) {
			values[index] = Integer.parseInt(parts[index]);
		}
		return values;
	}

	private static String stringField(String json, String name) {
		String key = "\"" + name + "\":\"";
		int at = json.indexOf(key);
		if (at < 0) {
			throw new IllegalArgumentException("No field " + name + " in " + json);
		}

		int start = at + key.length();
		return json.substring(start, json.indexOf('"', start));
	}

	private static int intField(String json, String name) {
		String key = "\"" + name + "\":";
		int at = json.indexOf(key);
		if (at < 0) {
			throw new IllegalArgumentException("No field " + name + " in " + json);
		}

		int start = at + key.length();
		int end = start;
		while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
			end++;
		}
		return Integer.parseInt(json.substring(start, end));
	}
}
