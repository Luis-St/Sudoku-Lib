package net.luis.sudoku.generation;

import net.luis.sudoku.difficulty.Difficulty;
import net.luis.sudoku.grid.*;
import net.luis.sudoku.key.PuzzleKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link GeneratedPuzzle}.
 */
class GeneratedPuzzleTest {
	
	private static final Difficulty RATED = Difficulty.ONE;
	private static final PuzzleKey KEY = PuzzleKey.of(GridSize.FOUR, Variant.CLASSIC, Difficulty.ONE, 7L);
	
	private static int[] solution() {
		return new int[] {
			1, 2, 3, 4,
			3, 4, 1, 2,
			2, 1, 4, 3,
			4, 3, 2, 1
		};
	}
	
	private static int[] givens() {
		return new int[] {
			1, 0, 0, 4,
			0, 4, 1, 0,
			0, 1, 4, 0,
			4, 0, 0, 1
		};
	}
	
	private static Puzzle puzzle() {
		return Puzzle.classicOfGivens(GridSize.FOUR, givens());
	}
	
	@Test
	void constructor_validArguments_keepsEveryComponent() {
		GeneratedPuzzle generated = new GeneratedPuzzle(KEY, puzzle(), solution(), RATED);
		assertAll(
			() -> assertSame(KEY, generated.key()),
			() -> assertEquals(puzzle(), generated.puzzle()),
			() -> assertArrayEquals(solution(), generated.solution()),
			() -> assertSame(RATED, generated.rated())
		);
	}
	
	@Test
	void constructor_mutatingTheSuppliedArray_doesNotAffectTheRecord() {
		int[] supplied = solution();
		GeneratedPuzzle generated = new GeneratedPuzzle(KEY, puzzle(), supplied, RATED);
		supplied[0] = 9;
		assertArrayEquals(solution(), generated.solution());
	}
	
	@Test
	void solution_mutatingTheReturnedArray_doesNotAffectTheRecord() {
		GeneratedPuzzle generated = new GeneratedPuzzle(KEY, puzzle(), solution(), RATED);
		int[] returned = generated.solution();
		returned[0] = 9;
		assertArrayEquals(solution(), generated.solution());
	}
	
	@Test
	void solution_repeatedCalls_returnDistinctArrays() {
		GeneratedPuzzle generated = new GeneratedPuzzle(KEY, puzzle(), solution(), RATED);
		assertNotSame(generated.solution(), generated.solution());
	}
	
	@Test
	void solutionAt_inRange_returnsTheDigit() {
		GeneratedPuzzle generated = new GeneratedPuzzle(KEY, puzzle(), solution(), RATED);
		assertAll(
			() -> assertEquals(1, generated.solutionAt(0)),
			() -> assertEquals(4, generated.solutionAt(3)),
			() -> assertEquals(1, generated.solutionAt(15))
		);
	}
	
	@Test
	void solutionAt_negativeIndex_throwsIndexOutOfBoundsException() {
		GeneratedPuzzle generated = new GeneratedPuzzle(KEY, puzzle(), solution(), RATED);
		assertThrows(IndexOutOfBoundsException.class, () -> generated.solutionAt(-1));
	}
	
	@Test
	void solutionAt_indexAtCellCount_throwsIndexOutOfBoundsException() {
		GeneratedPuzzle generated = new GeneratedPuzzle(KEY, puzzle(), solution(), RATED);
		assertThrows(IndexOutOfBoundsException.class, () -> generated.solutionAt(16));
	}
	
	@Test
	void constructor_nullKey_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> new GeneratedPuzzle(null, puzzle(), solution(), RATED));
	}
	
	@Test
	void constructor_nullPuzzle_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> new GeneratedPuzzle(KEY, null, solution(), RATED));
	}
	
	@Test
	void constructor_nullSolution_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> new GeneratedPuzzle(KEY, puzzle(), null, RATED));
	}
	
	@Test
	void constructor_nullRatedBand_throwsNullPointerException() {
		assertThrows(NullPointerException.class, () -> new GeneratedPuzzle(KEY, puzzle(), solution(), null));
	}
	
	@Test
	void constructor_solutionTooShort_throwsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> new GeneratedPuzzle(KEY, puzzle(), new int[15], RATED));
	}
	
	@Test
	void constructor_solutionTooLong_throwsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> new GeneratedPuzzle(KEY, puzzle(), new int[17], RATED));
	}
	
	@Test
	void equals_sameComponentsFromDistinctButEqualArrays_areEqualAndShareHashCode() {
		int[] first = solution();
		int[] second = solution();
		GeneratedPuzzle a = new GeneratedPuzzle(KEY, puzzle(), first, RATED);
		GeneratedPuzzle b = new GeneratedPuzzle(KEY, puzzle(), second, RATED);
		assertAll(
			() -> assertNotSame(first, second),
			() -> assertEquals(a, b),
			() -> assertEquals(a.hashCode(), b.hashCode())
		);
	}
	
	@Test
	void equals_differingKey_areNotEqual() {
		GeneratedPuzzle base = new GeneratedPuzzle(KEY, puzzle(), solution(), RATED);
		PuzzleKey otherKey = PuzzleKey.of(GridSize.FOUR, Variant.CLASSIC, Difficulty.TWO, 7L);
		assertNotEquals(base, new GeneratedPuzzle(otherKey, puzzle(), solution(), RATED));
	}
	
	@Test
	void equals_differingPuzzle_areNotEqual() {
		GeneratedPuzzle base = new GeneratedPuzzle(KEY, puzzle(), solution(), RATED);
		Puzzle otherPuzzle = Puzzle.classicOfGivens(GridSize.FOUR, new int[16]);
		assertNotEquals(base, new GeneratedPuzzle(KEY, otherPuzzle, solution(), RATED));
	}
	
	@Test
	void equals_differingSolutionContent_areNotEqual() {
		GeneratedPuzzle base = new GeneratedPuzzle(KEY, puzzle(), solution(), RATED);
		int[] otherSolution = solution();
		otherSolution[0] = 2;
		assertNotEquals(base, new GeneratedPuzzle(KEY, puzzle(), otherSolution, RATED));
	}
	
	@Test
	void equals_differingRatedBand_areNotEqual() {
		// The whole point of the component: two records identical in every other way describe different products,
		// because one of them is a puzzle the generator missed its target with.
		GeneratedPuzzle base = new GeneratedPuzzle(KEY, puzzle(), solution(), RATED);
		assertNotEquals(base, new GeneratedPuzzle(KEY, puzzle(), solution(), Difficulty.SEVEN));
	}
	
	@Test
	void equals_nullForeignTypeAndSelf() {
		GeneratedPuzzle generated = new GeneratedPuzzle(KEY, puzzle(), solution(), RATED);
		assertAll(
			() -> assertNotEquals(null, generated),
			() -> assertNotEquals("generated", generated),
			() -> assertEquals(generated, generated)
		);
	}
	
	@Test
	void hashCode_differingSolutionContent_usuallyDiffers() {
		GeneratedPuzzle base = new GeneratedPuzzle(KEY, puzzle(), solution(), RATED);
		int[] otherSolution = solution();
		otherSolution[0] = 2;
		GeneratedPuzzle other = new GeneratedPuzzle(KEY, puzzle(), otherSolution, RATED);
		assertNotEquals(base.hashCode(), other.hashCode());
	}
	
	@Test
	void toString_validRecord_mentionsEveryComponent() {
		String text = new GeneratedPuzzle(KEY, puzzle(), solution(), RATED).toString();
		assertAll(
			() -> assertTrue(text.contains(KEY.toString()), text),
			() -> assertTrue(text.contains(puzzle().toString()), text),
			() -> assertTrue(text.contains("1, 2, 3, 4"), text)
		);
	}
}
