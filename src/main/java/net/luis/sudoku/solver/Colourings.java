package net.luis.sudoku.solver;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The two-colouring of a strong-link graph, shared by {@link SimpleColouring}, {@link MultiColouring} and the
 * {@link Medusa3d}.
 * <p>
 *     Colouring walks a connected group of strong links and paints alternate candidates in two colours. Because a
 *     strong link means exactly one of its two ends is true, every candidate of one colour is true and every candidate
 *     of the other is false — the colouring just does not say which way round. That is already enough to conclude
 *     something whenever a colour contradicts itself, or whenever an outside candidate is ruled out by both colours at
 *     once.
 * </p>
 * <p>
 *     Clusters are produced in a deterministic order: candidates are visited in ascending cell order, then ascending
 *     digit order, and the first candidate of a cluster is always painted colour {@code 0}.
 * </p>
 *
 * @see SimpleColouring
 * @see Medusa3d
 */
final class Colourings {
	
	private Colourings() {}
	
	/**
	 * Colours the conjugate-pair graph of a single digit.
	 *
	 * @param grid The working grid; never mutated
	 * @param digit The digit to colour
	 * @return One cluster per connected group of two or more candidates
	 */
	static List<Cluster> ofDigit(CandidateGrid grid, int digit) {
		// collect(toList()) rather than Stream.toList(): the latter is Java 16, and Android only gained it in API 34,
		// so on every older device it is a NoSuchMethodError. D8 backports the java.util factory methods this library
		// uses (List.of, List.copyOf, Optional.isEmpty), but it does not backport a default method on a library
		// interface, which is what Stream.toList is.
		return build(grid, ConjugateLinks.of(grid, digit).stream()
			.map(link -> new int[] { link[0], digit, link[1], digit })
			.collect(Collectors.toList()));
	}
	
	/**
	 * Colours the strong-link graph over every digit at once, which additionally links the two candidates of every
	 * bi-value cell.
	 *
	 * @param grid The working grid; never mutated
	 * @return One cluster per connected group of two or more candidates
	 */
	static List<Cluster> ofGrid(CandidateGrid grid) {
		List<int[]> links = new ArrayList<>();
		for (int digit = 1; digit <= grid.n(); digit++) {
			for (int[] link : ConjugateLinks.of(grid, digit)) {
				links.add(new int[] { link[0], digit, link[1], digit });
			}
		}
		
		for (int cell = 0; cell < grid.cellCount(); cell++) {
			if (grid.candidateCount(cell) != 2) {
				continue;
			}
			
			int mask = grid.candidates(cell);
			int first = Integer.numberOfTrailingZeros(mask);
			int second = Integer.numberOfTrailingZeros(mask & (mask - 1));
			links.add(new int[] { cell, first, cell, second });
		}
		return build(grid, links);
	}
	
	/**
	 * Walks the given strong links and paints each connected group in two colours.
	 *
	 * @param links Each entry is {@code cellA, digitA, cellB, digitB} of one strong link
	 */
	private static List<Cluster> build(CandidateGrid grid, List<int[]> links) {
		List<int[]> candidates = new ArrayList<>();
		for (int[] link : links) {
			addCandidate(candidates, link[0], link[1]);
			addCandidate(candidates, link[2], link[3]);
		}
		candidates.sort((left, right) -> left[0] != right[0] ? Integer.compare(left[0], right[0]) : Integer.compare(left[1], right[1]));
		
		int[] colours = new int[candidates.size()];
		java.util.Arrays.fill(colours, -1);
		
		List<Cluster> clusters = new ArrayList<>();
		for (int seed = 0; seed < candidates.size(); seed++) {
			if (colours[seed] != -1) {
				continue;
			}
			
			List<Integer> members = new ArrayList<>();
			colours[seed] = 0;
			members.add(seed);
			for (int cursor = 0; cursor < members.size(); cursor++) {
				int index = members.get(cursor);
				int[] candidate = candidates.get(index);
				for (int[] link : links) {
					int neighbour = neighbourOf(candidates, link, candidate);
					if (neighbour == -1 || colours[neighbour] != -1) {
						continue;
					}
					
					colours[neighbour] = 1 - colours[index];
					members.add(neighbour);
				}
			}
			
			if (members.size() < 2) {
				continue;
			}
			clusters.add(toCluster(candidates, colours, members));
		}
		return clusters;
	}
	
	private static Cluster toCluster(List<int[]> candidates, int[] colours, List<Integer> members) {
		int[] cells = new int[members.size()];
		int[] digits = new int[members.size()];
		int[] painted = new int[members.size()];
		for (int index = 0; index < members.size(); index++) {
			int[] candidate = candidates.get(members.get(index));
			cells[index] = candidate[0];
			digits[index] = candidate[1];
			painted[index] = colours[members.get(index)];
		}
		return new Cluster(cells, digits, painted);
	}
	
	private static int neighbourOf(List<int[]> candidates, int[] link, int[] candidate) {
		if (link[0] == candidate[0] && link[1] == candidate[1]) {
			return indexOf(candidates, link[2], link[3]);
		}
		if (link[2] == candidate[0] && link[3] == candidate[1]) {
			return indexOf(candidates, link[0], link[1]);
		}
		return -1;
	}
	
	private static void addCandidate(List<int[]> candidates, int cell, int digit) {
		if (indexOf(candidates, cell, digit) == -1) {
			candidates.add(new int[] { cell, digit });
		}
	}
	
	private static int indexOf(List<int[]> candidates, int cell, int digit) {
		for (int index = 0; index < candidates.size(); index++) {
			int[] candidate = candidates.get(index);
			if (candidate[0] == cell && candidate[1] == digit) {
				return index;
			}
		}
		return -1;
	}
	
	/**
	 * One connected group of strongly linked candidates, painted in two colours.
	 *
	 * @param cells The cell of each candidate, parallel to the other two arrays
	 * @param digits The digit of each candidate
	 * @param colours The colour of each candidate, {@code 0} or {@code 1}
	 */
	record Cluster(int[] cells, int[] digits, int[] colours) {
		
		/**
		 * Returns whether the given candidate belongs to this cluster.
		 */
		boolean contains(int cell, int digit) {
			return this.indexOf(cell, digit) >= 0;
		}
		
		int indexOf(int cell, int digit) {
			for (int index = 0; index < this.cells.length; index++) {
				if (this.cells[index] == cell && this.digits[index] == digit) {
					return index;
				}
			}
			return -1;
		}
	}
}
