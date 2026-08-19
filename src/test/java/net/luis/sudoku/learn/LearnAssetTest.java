package net.luis.sudoku.learn;

import net.luis.sudoku.solver.Technique;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link LearnAsset}.
 * <p>
 *     The file is written on a desktop and parsed on a phone, so the round trip is the whole contract: a writer and a
 *     reader that disagree ship an asset nobody notices is broken until the learn area refuses to open.
 * </p>
 */
class LearnAssetTest {
	
	private static final LearnPuzzleGenerator.Budget BUDGET = new LearnPuzzleGenerator.Budget(400, 60_000L);
	
	private static LearnAsset asset(Technique technique, int examples, int exercises) {
		LearnPuzzle puzzle = LearnPuzzleGenerator.generate(technique, 1L, BUDGET).orElseThrow();
		List<LearnPuzzle> exampleList = new ArrayList<>();
		List<LearnPuzzle> exerciseList = new ArrayList<>();
		for (int index = 0; index < examples; index++) {
			exampleList.add(puzzle);
		}
		for (int index = 0; index < exercises; index++) {
			exerciseList.add(puzzle);
		}
		return new LearnAsset(technique, exampleList, exerciseList);
	}
	
	@Test
	void writesAndReadsBackTheSameContent() {
		LearnAsset written = asset(Technique.NAKED_SINGLE, LearnContent.EXAMPLES_PER_TECHNIQUE, LearnContent.EXERCISES_PER_TECHNIQUE);
		
		LearnAsset read = LearnAsset.read(LearnAsset.write(written));
		
		assertEquals(Technique.NAKED_SINGLE, read.technique());
		assertEquals(written.examples(), read.examples());
		assertEquals(written.exercises(), read.exercises());
		assertTrue(read.isComplete());
	}
	
	@Test
	void keepsTheTwoListsApart() {
		LearnAsset read = LearnAsset.read(LearnAsset.write(asset(Technique.NAKED_SINGLE, 2, 3)));
		
		assertEquals(2, read.examples().size());
		assertEquals(3, read.exercises().size());
		assertFalse(read.isComplete());
	}
	
	@Test
	void readsAnAssetWithNoExamples() {
		LearnAsset read = LearnAsset.read(LearnAsset.write(asset(Technique.NAKED_SINGLE, 0, LearnContent.EXERCISES_PER_TECHNIQUE)));
		
		assertTrue(read.examples().isEmpty());
		assertEquals(LearnContent.EXERCISES_PER_TECHNIQUE, read.exercises().size());
		assertEquals(Technique.NAKED_SINGLE, read.technique());
	}
	
	@Test
	void rejectsTextThatIsNotAnAsset() {
		assertThrows(IllegalArgumentException.class, () -> LearnAsset.read("{}"));
		assertThrows(IllegalArgumentException.class, () -> LearnAsset.read("{\"exercises\":[],\"examples\":[]}"));
		assertThrows(NullPointerException.class, () -> LearnAsset.read(null));
	}
	
	@Test
	void rejectsAnAssetHoldingNoPuzzles() {
		assertThrows(IllegalArgumentException.class, () -> LearnAsset.read("{\"examples\":[],\"exercises\":[]}"));
	}
	
	@Test
	void exerciseAddressesTheTrainingByLevelAndSubLevel() {
		LearnAsset read = asset(Technique.NAKED_SINGLE, 0, LearnContent.EXERCISES_PER_TECHNIQUE);
		
		assertSame(read.exercises().get(0), read.exercise(1, 0));
		assertSame(read.exercises().get(LearnContent.SUB_LEVELS), read.exercise(2, 0));
		assertSame(read.exercises().get(LearnContent.EXERCISES_PER_TECHNIQUE - 1), read.exercise(LearnContent.LEVELS, LearnContent.SUB_LEVELS - 1));
	}
	
	@Test
	void exerciseRejectsAPlaceOutsideTheTraining() {
		LearnAsset read = asset(Technique.NAKED_SINGLE, 0, LearnContent.EXERCISES_PER_TECHNIQUE);
		
		assertThrows(IllegalArgumentException.class, () -> read.exercise(0, 0));
		assertThrows(IllegalArgumentException.class, () -> read.exercise(LearnContent.LEVELS + 1, 0));
		assertThrows(IllegalArgumentException.class, () -> read.exercise(1, -1));
		assertThrows(IllegalArgumentException.class, () -> read.exercise(1, LearnContent.SUB_LEVELS));
	}
	
	@Test
	void fileNameIsTheTechniqueInLowerCase() {
		assertEquals("naked_single.json", LearnAsset.fileNameOf(Technique.NAKED_SINGLE));
		assertEquals("als_xz.json", LearnAsset.fileNameOf(Technique.ALS_XZ));
		assertThrows(NullPointerException.class, () -> LearnAsset.fileNameOf(null));
	}
	
	@Test
	void rejectsNullComponents() {
		assertThrows(NullPointerException.class, () -> new LearnAsset(null, List.of(), List.of()));
		assertThrows(NullPointerException.class, () -> new LearnAsset(Technique.NAKED_SINGLE, null, List.of()));
		assertThrows(NullPointerException.class, () -> new LearnAsset(Technique.NAKED_SINGLE, List.of(), null));
	}
	
	@Test
	void readAllIgnoresWhatIsAroundTheObjects() {
		LearnPuzzle puzzle = LearnPuzzleGenerator.generate(Technique.NAKED_SINGLE, 1L, BUDGET).orElseThrow();
		
		List<LearnPuzzle> read = LearnPuzzleCodec.readAll("\"examples\":" + LearnPuzzleCodec.writeAll(List.of(puzzle, puzzle)) + ",");
		
		assertEquals(List.of(puzzle, puzzle), read);
		assertTrue(LearnPuzzleCodec.readAll("[]").isEmpty());
	}
}
