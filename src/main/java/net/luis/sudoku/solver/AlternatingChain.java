package net.luis.sudoku.solver;

import java.util.*;

/**
 * The shared engine behind every alternating inference chain: the {@link XChain}, the {@link XyChain}, the plain
 * {@link Aic} and the {@link GroupedAic}.
 * <p>
 *     A chain alternates two kinds of link between candidates. A <b>strong</b> link says that if one end is false the
 *     other is true — two candidates that are the only two places a digit can go in a unit, or the two candidates of a
 *     bi-value cell. A <b>weak</b> link says that if one end is true the other is false — two candidates of the same
 *     digit that see each other, or two candidates in the same cell. Starting and ending on a strong link, the chain
 *     proves that at least one of its two ends is true, whatever happens in between.
 * </p>
 * <p>
 *     What that buys depends on how the two ends relate:
 * </p>
 * <ul>
 *     <li>Same digit, different cells — no cell seeing both ends can hold that digit.</li>
 *     <li>Same cell, different digits — the cell holds one of the two, so its other candidates go.</li>
 *     <li>Different cells and digits that see each other — each end's digit goes from the other end's cell.</li>
 * </ul>
 * <p>
 *     The four techniques differ only in which candidates are allowed to be chain nodes and how long the chain may
 *     get: a single digit throughout is an X-Chain, bi-value cells throughout an XY-Chain, anything at all a plain
 *     AIC, and allowing whole groups of candidates in a region-line intersection to act as one node a grouped AIC.
 *     The search is depth-first over a fixed node order, so the chain it finds is the same on every run.
 * </p>
 *
 * @see XChain
 * @see XyChain
 * @see Aic
 * @see GroupedAic
 */
abstract sealed class AlternatingChain implements TechniqueStrategy permits XChain, XyChain, Aic, GroupedAic {
	
	/**
	 * The longest chain the search will build, counted in strong links. Longer chains exist, but they are past what a
	 * player traces by hand and the search cost grows steeply with every additional link.
	 */
	private static final int MAX_STRONG_LINKS = 4;
	
	private final Technique technique;
	private final boolean singleDigit;
	private final boolean biValueOnly;
	private final boolean grouped;
	
	/**
	 * Constructs a chain strategy.
	 *
	 * @param technique The technique to attribute the eliminations to
	 * @param singleDigit True to keep the whole chain on one digit, as an X-Chain does
	 * @param biValueOnly True to allow only candidates of bi-value cells as nodes, as an XY-Chain does
	 * @param grouped True to allow a whole group of candidates in a region-line intersection to act as one node
	 */
	AlternatingChain(Technique technique, boolean singleDigit, boolean biValueOnly, boolean grouped) {
		this.technique = technique;
		this.singleDigit = singleDigit;
		this.biValueOnly = biValueOnly;
		this.grouped = grouped;
	}
	
	@Override
	public final Technique technique() {
		return this.technique;
	}
	
	/**
	 * Builds the link graph for this chain flavour and searches it for the first chain that eliminates something.
	 *
	 * @param grid The working grid; never mutated
	 * @return The first chain elimination, or empty if no chain makes progress
	 */
	@Override
	public final Optional<Deduction> find(CandidateGrid grid) {
		return this.scan(grid, null);
	}
	
	/**
	 * Explains the chain by walking it link by link: assume the first end does not hold its digit, and every strong
	 * link along the way forces the next candidate, until the far end is forced instead. One of the two ends is
	 * therefore true, which is what the conclusion rests on.
	 *
	 * @param grid The working grid; never mutated
	 * @return The eliminations and their explanation, or empty if no chain makes progress
	 */
	@Override
	public final Optional<ExplainedDeduction> findExplained(CandidateGrid grid) {
		Explanation.Builder builder = Explanation.builder(this.technique);
		return this.scan(grid, builder).map(deduction -> new ExplainedDeduction(deduction, builder.conclusion(deduction).build()));
	}
	
	private Optional<Deduction> scan(CandidateGrid grid, Explanation.Builder explanation) {
		List<int[]> nodes = this.buildNodes(grid);
		int[] digits = new int[nodes.size()];
		for (int index = 0; index < nodes.size(); index++) {
			digits[index] = nodes.get(index)[0];
		}
		
		List<List<Integer>> strong = this.buildStrong(grid, nodes);
		List<List<Integer>> weak = this.buildWeak(grid, nodes);
		
		boolean[] onChain = new boolean[nodes.size()];
		int[] chain = new int[2 * MAX_STRONG_LINKS];
		for (int start = 0; start < nodes.size(); start++) {
			onChain[start] = true;
			chain[0] = start;
			Optional<Deduction> found = this.extend(grid, nodes, digits, strong, weak, onChain, chain, 1, start, explanation);
			onChain[start] = false;
			if (found.isPresent()) {
				return found;
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Extends the chain by one strong link, tests the resulting ends, and recurses through a weak link.
	 *
	 * @param length How many nodes the chain currently holds
	 * @param start The node the chain began at, which is one of the two ends
	 */
	private Optional<Deduction> extend(CandidateGrid grid, List<int[]> nodes, int[] digits, List<List<Integer>> strong, List<List<Integer>> weak, boolean[] onChain, int[] chain, int length, int start, Explanation.Builder explanation) {
		int last = chain[length - 1];
		for (int next : strong.get(last)) {
			if (onChain[next]) {
				continue;
			}
			
			onChain[next] = true;
			chain[length] = next;
			
			Optional<Deduction> found = this.conclude(grid, nodes, digits, start, next, chain, length, explanation);
			if (found.isPresent()) {
				onChain[next] = false;
				return found;
			}
			
			if (length + 1 < 2 * MAX_STRONG_LINKS) {
				for (int bridge : weak.get(next)) {
					if (onChain[bridge]) {
						continue;
					}
					
					onChain[bridge] = true;
					chain[length + 1] = bridge;
					Optional<Deduction> deeper = this.extend(grid, nodes, digits, strong, weak, onChain, chain, length + 2, start, explanation);
					onChain[bridge] = false;
					if (deeper.isPresent()) {
						onChain[next] = false;
						return deeper;
					}
				}
			}
			onChain[next] = false;
		}
		return Optional.empty();
	}
	
	/**
	 * Applies the three end-relation rules to a finished chain.
	 */
	private Optional<Deduction> conclude(CandidateGrid grid, List<int[]> nodes, int[] digits, int start, int end, int[] chain, int length, Explanation.Builder explanation) {
		int[] first = nodes.get(start);
		int[] second = nodes.get(end);
		EliminationBuilder builder = new EliminationBuilder();
		
		if (digits[start] == digits[end]) {
			int digit = digits[start];
			for (int cell = 0; cell < grid.cellCount(); cell++) {
				if (this.holds(first, cell) || this.holds(second, cell)) {
					continue;
				}
				if (this.seenByAll(grid, first, cell) && this.seenByAll(grid, second, cell)) {
					builder.add(grid, cell, digit);
				}
			}
		} else if (first.length == 2 && second.length == 2 && first[1] == second[1]) {
			builder.addAll(grid, first[1], ~((1 << digits[start]) | (1 << digits[end])));
		} else if (first.length == 2 && second.length == 2 && grid.peers(first[1], second[1])) {
			builder.add(grid, first[1], digits[end]);
			builder.add(grid, second[1], digits[start]);
		}
		
		builder.sortByCell();
		Optional<Deduction> deduction = builder.build(this.technique);
		// Only a chain that removes something is the deduction being returned, so only that one is worth explaining:
		// any earlier one was walked and abandoned.
		if (deduction.isPresent() && explanation != null) {
			this.explain(nodes, digits, chain, length, explanation);
		}
		return deduction;
	}
	
	/**
	 * Records the chain: the digit if the whole chain is about one, then each strong link in turn with its two ends
	 * shown as assumed false and assumed true, and finally the pair of chain ends one of which must be true.
	 *
	 * @param nodes The node list
	 * @param digits The digit of each node
	 * @param chain The node indices of the chain, alternating along its links
	 * @param length The index of the chain's last node
	 * @param explanation The explanation to record into
	 */
	private void explain(List<int[]> nodes, int[] digits, int[] chain, int length, Explanation.Builder explanation) {
		if (this.singleDigit) {
			explanation.focusDigit(digits[chain[0]]);
		}
		
		// The chain alternates from its first node: assumed false, forced true across a strong link, forced false
		// across the weak bridge that follows, and so on to the far end.
		for (int index = 0; index + 1 <= length; index += 2) {
			int from = chain[index];
			int to = chain[index + 1];
			List<PatternCell> cells = new ArrayList<>();
			this.addNode(cells, nodes.get(from), digits[from], CellRole.LINK_OFF);
			this.addNode(cells, nodes.get(to), digits[to], CellRole.LINK_ON);
			explanation.link(digits[from] == digits[to] ? digits[from] : 0, cells);
		}
		
		int first = chain[0];
		int last = chain[length];
		List<PatternCell> ends = new ArrayList<>();
		this.addNode(ends, nodes.get(first), digits[first], CellRole.LINK_ON);
		this.addNode(ends, nodes.get(last), digits[last], CellRole.LINK_ON);
		explanation.implication(digits[first] == digits[last] ? digits[first] : 0, ends);
	}
	
	/**
	 * Adds one node's cells in the given role. A grouped node covers several cells that act as one candidate, so all
	 * of them are shown together.
	 */
	private void addNode(List<PatternCell> cells, int[] node, int digit, CellRole role) {
		for (int index = 1; index < node.length; index++) {
			cells.add(PatternCell.of(node[index], role, digit));
		}
	}
	
	/**
	 * Builds the node list: every candidate that this flavour admits, plus the region-line groups for a grouped chain.
	 * A node is stored as its digit followed by its ascending cell indices.
	 */
	private List<int[]> buildNodes(CandidateGrid grid) {
		List<int[]> nodes = new ArrayList<>();
		for (int cell = 0; cell < grid.cellCount(); cell++) {
			if (!grid.isEmpty(cell) || (this.biValueOnly && grid.candidateCount(cell) != 2)) {
				continue;
			}
			
			for (int digit = 1; digit <= grid.n(); digit++) {
				if (grid.hasCandidate(cell, digit)) {
					nodes.add(new int[] { digit, cell });
				}
			}
		}
		
		if (this.grouped) {
			this.addGroups(grid, nodes);
		}
		return nodes;
	}
	
	/**
	 * Adds a node for every set of two or more candidates of one digit sharing both a region and a line, which is the
	 * only shape that behaves as a single candidate for chaining purposes.
	 */
	private void addGroups(CandidateGrid grid, List<int[]> nodes) {
		int n = grid.n();
		for (int digit = 1; digit <= n; digit++) {
			for (int region = 0; region < grid.partition().regionCount(); region++) {
				for (int line = 0; line < n; line++) {
					this.addGroup(grid, nodes, digit, region, grid.rowCells(line));
					this.addGroup(grid, nodes, digit, region, grid.columnCells(line));
				}
			}
		}
	}
	
	private void addGroup(CandidateGrid grid, List<int[]> nodes, int digit, int region, int[] lineCells) {
		int[] members = new int[lineCells.length];
		int count = 0;
		for (int cell : lineCells) {
			if (grid.regionOf(cell) == region && grid.hasCandidate(cell, digit)) {
				members[count++] = cell;
			}
		}
		
		if (count < 2) {
			return;
		}
		
		int[] node = new int[count + 1];
		node[0] = digit;
		System.arraycopy(members, 0, node, 1, count);
		nodes.add(node);
	}
	
	/**
	 * Builds the strong-link adjacency: within a bi-value cell, and within a unit where the digit's candidates split
	 * exactly into the two nodes.
	 */
	private List<List<Integer>> buildStrong(CandidateGrid grid, List<int[]> nodes) {
		List<List<Integer>> links = this.emptyAdjacency(nodes.size());
		for (int i = 0; i < nodes.size(); i++) {
			for (int j = i + 1; j < nodes.size(); j++) {
				if (this.isStrong(grid, nodes.get(i), nodes.get(j))) {
					links.get(i).add(j);
					links.get(j).add(i);
				}
			}
		}
		return links;
	}
	
	private boolean isStrong(CandidateGrid grid, int[] first, int[] second) {
		if (first[0] != second[0]) {
			// Two candidates of one bi-value cell: exactly one of them is the answer.
			return !this.singleDigit && first.length == 2 && second.length == 2 && first[1] == second[1] && grid.candidateCount(first[1]) == 2;
		}
		if (this.overlaps(first, second)) {
			return false;
		}
		
		for (int[] unit : grid.allUnits()) {
			if (!this.within(unit, first) || !this.within(unit, second)) {
				continue;
			}
			
			int total = 0;
			for (int cell : unit) {
				if (grid.hasCandidate(cell, first[0])) {
					total++;
				}
			}
			
			if (total == first.length - 1 + second.length - 1) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Builds the weak-link adjacency: same cell, or same digit with every cell of one node seeing every cell of the
	 * other through a shared unit.
	 */
	private List<List<Integer>> buildWeak(CandidateGrid grid, List<int[]> nodes) {
		List<List<Integer>> links = this.emptyAdjacency(nodes.size());
		for (int i = 0; i < nodes.size(); i++) {
			for (int j = i + 1; j < nodes.size(); j++) {
				if (this.isWeak(grid, nodes.get(i), nodes.get(j))) {
					links.get(i).add(j);
					links.get(j).add(i);
				}
			}
		}
		return links;
	}
	
	private boolean isWeak(CandidateGrid grid, int[] first, int[] second) {
		if (first[0] != second[0]) {
			return !this.singleDigit && first.length == 2 && second.length == 2 && first[1] == second[1];
		}
		if (this.overlaps(first, second)) {
			return false;
		}
		
		for (int[] unit : grid.allUnits()) {
			if (this.within(unit, first) && this.within(unit, second)) {
				return true;
			}
		}
		return false;
	}
	
	private List<List<Integer>> emptyAdjacency(int size) {
		List<List<Integer>> links = new ArrayList<>(size);
		for (int index = 0; index < size; index++) {
			links.add(new ArrayList<>());
		}
		return links;
	}
	
	private boolean within(int[] unit, int[] node) {
		for (int index = 1; index < node.length; index++) {
			if (!this.holds(unit, node[index], 0)) {
				return false;
			}
		}
		return true;
	}
	
	private boolean overlaps(int[] first, int[] second) {
		for (int i = 1; i < first.length; i++) {
			for (int j = 1; j < second.length; j++) {
				if (first[i] == second[j]) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean holds(int[] node, int cell) {
		return this.holds(node, cell, 1);
	}
	
	private boolean holds(int[] array, int cell, int from) {
		for (int index = from; index < array.length; index++) {
			if (array[index] == cell) {
				return true;
			}
		}
		return false;
	}
	
	private boolean seenByAll(CandidateGrid grid, int[] node, int cell) {
		for (int index = 1; index < node.length; index++) {
			if (!grid.peers(cell, node[index])) {
				return false;
			}
		}
		return true;
	}
}
