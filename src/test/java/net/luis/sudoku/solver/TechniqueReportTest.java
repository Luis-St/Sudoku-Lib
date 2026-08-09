package net.luis.sudoku.solver;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link TechniqueReport}.
 */
class TechniqueReportTest {
	
	private static Map<Technique, Integer> usage(Object... pairs) {
		Map<Technique, Integer> map = new EnumMap<>(Technique.class);
		for (int index = 0; index < pairs.length; index += 2) {
			map.put((Technique) pairs[index], (Integer) pairs[index + 1]);
		}
		return map;
	}
	
	private static TechniqueReport report(boolean solved, boolean stuck, int[] solution, Map<Technique, Integer> usage) {
		return new TechniqueReport(solved, stuck, false, solution, usage);
	}
	
	@Test
	void flags_solvedReport_reportSolvedAndNotStuck() {
		TechniqueReport report = report(true, false, new int[81], usage(Technique.NAKED_SINGLE, 3));
		
		assertAll(
			() -> assertTrue(report.solved()),
			() -> assertFalse(report.stuck())
		);
	}
	
	@Test
	void flags_stuckReport_reportStuckAndNotSolved() {
		TechniqueReport report = report(false, true, new int[81], usage(Technique.NAKED_SINGLE, 3));
		
		assertAll(
			() -> assertFalse(report.solved()),
			() -> assertTrue(report.stuck())
		);
	}
	
	@Test
	void usage_techniqueWithAZeroCount_isTreatedAsAbsent() {
		TechniqueReport report = report(true, false, new int[81], usage(Technique.NAKED_SINGLE, 5, Technique.HIDDEN_SINGLE_REGION, 0));
		
		assertAll(
			() -> assertFalse(report.usage().containsKey(Technique.HIDDEN_SINGLE_REGION)),
			() -> assertEquals(0, report.count(Technique.HIDDEN_SINGLE_REGION)),
			() -> assertEquals(5, report.count(Technique.NAKED_SINGLE))
		);
	}
	
	@Test
	void count_techniqueThatNeverFired_returnsZero() {
		TechniqueReport report = report(true, false, new int[81], usage(Technique.NAKED_SINGLE, 2));
		
		assertEquals(0, report.count(Technique.X_WING));
	}
	
	@Test
	void hardestTechnique_reportWithoutAnyUsage_returnsEmpty() {
		TechniqueReport report = report(false, true, new int[81], usage());
		
		assertTrue(report.hardestTechnique().isEmpty());
	}
	
	@Test
	void hardestTechnique_reportWithSeveralTechniques_returnsTheHighestRank() {
		TechniqueReport report = report(true, false, new int[81], usage(Technique.NAKED_SINGLE, 9, Technique.X_WING, 1, Technique.NAKED_PAIR, 4));
		
		assertEquals(Optional.of(Technique.X_WING), report.hardestTechnique());
	}
	
	@Test
	void totalSteps_reportWithSeveralTechniques_sumsEveryUsageCount() {
		TechniqueReport report = report(true, false, new int[81], usage(Technique.NAKED_SINGLE, 9, Technique.X_WING, 1, Technique.NAKED_PAIR, 4));
		
		assertEquals(14, report.totalSteps());
	}
	
	@Test
	void usage_returnedView_isKeyedInAscendingTechniqueOrder() {
		TechniqueReport report = report(true, false, new int[81], usage(Technique.X_WING, 1, Technique.NAKED_SINGLE, 2, Technique.NAKED_PAIR, 3));
		
		assertIterableEquals(List.of(Technique.NAKED_SINGLE, Technique.NAKED_PAIR, Technique.X_WING), report.usage().keySet());
	}
	
	@Test
	void solution_returnedArray_isADefensiveCopy() {
		int[] source = new int[81];
		source[0] = 7;
		TechniqueReport report = report(true, false, source, usage(Technique.NAKED_SINGLE, 1));
		
		source[0] = 3;
		int[] first = report.solution();
		first[1] = 9;
		int[] second = report.solution();
		
		assertAll(
			() -> assertEquals(7, second[0], "The constructor copies the solution"),
			() -> assertEquals(0, second[1], "Mutating a returned array must not touch the report"),
			() -> assertNotSame(first, second, "Every call returns a fresh array")
		);
	}
}
