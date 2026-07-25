# Sudoku-Lib — Implementation Plan (shared-core)

Source of truth: `../sudoku-feature-spec.md` §2–4, §11 (determinism), §14 (build order).
Current state: empty Gradle skeleton (`java-library`, Java 25 toolchain, JSpecify, JUnit 5, publishes to `maven.luis-st.net`). No production code beyond `package-info.java`.

This module has **no Android and no Javalin dependency**, ever. It is consumed by both other projects at an exact version. Every phase below ends with a JUnit suite proving determinism, since that's the one property both consumers depend on absolutely.

## Package layout

```
net.luis.sudoku
├── grid/          GridSize, Variant, Region, RegionPartition, Cell, Puzzle
├── key/           PuzzleKey, KeyDerivation (fold64/SHA-256)
├── rng/           DeterministicRandom (java.util.Random wrapper, no Splittable/ThreadLocalRandom)
├── generation/    RegionGenerator (classic + chaos), SolutionFiller, HoleDigger, PuzzleGenerator
├── solver/        BacktrackingSolver (solve/count), TechniqueSolver, Technique, SolveStep
├── difficulty/    Difficulty (enum incl. LISA), DifficultyBands, DifficultyRater
├── hint/          HintEngine
├── sharecode/     ShareCodeCodec (Base32)
└── version/       GenVersion
```

## Phase L1 — Domain model
- `grid.GridSize`: enum {FOUR(4), SIX(6), NINE(9), TWELVE(12), SIXTEEN(16)} carrying `n`, box dims for classic (2×2, 3×2, 3×3, 4×3, 4×4).
- `grid.Variant`: enum {CLASSIC, CHAOS}; validate CHAOS excluded for FOUR at construction time.
- `grid.Region`: immutable set of `n` cell indices (use `int[]`, not `Set<Integer>`, to avoid autoboxing and hash-order pitfalls per §3.2).
- `grid.RegionPartition`: `List<Region>` plus lookup `int regionOf(int cellIndex)`. Built via `LinkedHashMap`/arrays only — **no `HashMap`/`HashSet` anywhere in this package tree**.
- `grid.ClassicRegionPartition`: computed directly from `GridSize` box dimensions.
- `grid.Cell`: three layers — `given` (boolean+digit), `penValue` (int, 0 = empty), `pencilMarks` (bitset over 1..n). Provide immutable snapshot + mutation methods returning new state or in-place mutators — decide once, keep consistent (recommend mutable `Cell` since undo (§5.7) stores full previous state externally, not via cell immutability).
- `grid.Puzzle`: `GridSize`, `Variant`, `RegionPartition`, `Cell[]` (row-major), row/column/region accessors, conflict detection (row/col/region duplicate check).
- Unit tests: partition covers every cell exactly once for every classic size; row/col/region accessors correct on a hand-built 4×4.

**Exit criteria:** a `Puzzle` can be constructed for all 5 classic sizes and queried for conflicts, with 100% deterministic iteration order.

## Phase L2 — Backtracking solver + uniqueness counting
- `solver.BacktrackingSolver`: operates on the generalized `RegionPartition`, not hardcoded 3×3 boxes.
- Two entry points: `Optional<int[]> solve(Puzzle)` and `int countSolutions(Puzzle, int cap)` that aborts once `cap` (2) is reached.
- Cell/candidate ordering must be deterministic (row-major scan + ascending digit try order) — no shortcuts that depend on iteration order of a hash structure.
- Tests: known 9×9 puzzles with 1 and 2+ solutions; a full solved grid solves to itself; count caps correctly at 2 without enumerating further.

**Exit criteria:** solver is variant/size-agnostic and its output is byte-identical across repeated runs on the same input.

## Phase L3 — Deterministic RNG, PuzzleKey, seed derivation
- `rng.DeterministicRandom`: thin wrapper over `java.util.Random` (document why — algorithm is Javadoc-specified, portable across JVMs). Forbid `SplittableRandom`/`ThreadLocalRandom` via a checkstyle/manual review note in this file.
- `key.PuzzleKey`: record `(int genVersion, GridSize size, Variant variant, Difficulty difficulty, long seed)`.
- `key.KeyDerivation.fold64(byte[] sha256Digest)`: XOR-fold 256-bit digest to 64 bits, per §3.1.
- `key.KeyDerivation.deriveState(PuzzleKey)`: `fold64(SHA256(genVersion‖size‖variant‖difficulty‖seed))` → seeds `DeterministicRandom`.
- Tests: same key twice → identical derived state; changing any one field → different state (no accidental collisions in the concatenation encoding — pick a delimiter/length-prefixed scheme so `(1,9)` vs `(19)` can't collide).

**Exit criteria:** `PuzzleKey → seed state` is a pure, tested function.

## Phase L4 — Classic generator (fill, dig, verify)
- `generation.SolutionFiller`: seeded randomized backtracking to produce one complete valid grid for a given `RegionPartition` + `DeterministicRandom`.
- `generation.HoleDigger`: seeded random cell removal order; after each removal, call `BacktrackingSolver.countSolutions(..., cap=2)` and restore the cell if it yields a second solution (§3.3 step 4).
- `generation.PuzzleGenerator` (classic path only for now): orchestrates key → seed → partition → fill → dig, bounded attempt loop with deterministic fallback (§3.2 "bounded, deterministic loops").
- Tests: for 9×9 classic at a fixed key, generation is byte-identical across 100 repeated runs and across a second JVM process (fork a test JVM or at minimum assert no use of identity hashcodes/hash-ordered collections anywhere in the call path — grep test, not just unit test).

**Exit criteria:** classic puzzles at all 5 sizes generate, always have a unique solution, and are fully deterministic.

## Phase L5 — Technique (human) solver
- `solver.Technique`: enum in escalating order — NAKED_SINGLE, HIDDEN_SINGLE, NAKED_PAIR, NAKED_TRIPLE, HIDDEN_PAIR, HIDDEN_TRIPLE, POINTING_PAIR, BOX_LINE_REDUCTION, X_WING, SWORDFISH, XY_WING (and beyond, §4.2).
- `solver.SolveStep`: record `(int cellIndex, int digit, Technique)`.
- `solver.TechniqueSolver`: applies techniques in order, never guesses; returns either the next deducible step or "stuck" (falls back to backtracking-only difficulty = effectively unrated/reject in generation).
- Tests: one fixture puzzle solvable purely by naked/hidden singles; one requiring pointing pairs; one requiring X-Wing — assert the solver reaches for the *lowest* sufficient technique, never a higher one when a lower one would do (this is what makes rating meaningful).

**Exit criteria:** given any partially-filled valid grid, the technique solver reports the correct next step and the technique required.

## Phase L6 — Difficulty rating + generate-and-rate loop
- `difficulty.Difficulty`: enum {ONE, TWO, THREE, FOUR, FIVE, LISA} — `LISA` is a first-class member, not a magic value.
- `difficulty.DifficultyBands`: per-`GridSize` thresholds (technique-frequency based, §4.3) — a `Map<GridSize, Bands>` config, tunable without touching the generator.
- `difficulty.DifficultyRater`: runs `TechniqueSolver` to full solve, records which techniques were required and how often, maps to a `Difficulty` per the size's band.
- Update `generation.PuzzleGenerator`: after digging, rate the result; if outside target band, discard and retry deterministically within the attempt bound; on exhaustion emit the closest-rated candidate (§3.3 step 6).
- Tests: generation at each of the 6 difficulty tiers (5 sizes × up to 6 tiers, skip LISA-invalid combos none — LISA is valid at all sizes per §4.3) produces a puzzle whose rated difficulty matches the requested tier, or the documented closest-candidate fallback.

**Exit criteria:** `PuzzleKey → rated Puzzle` is complete for the classic variant across all sizes and all six tiers.

## Phase L7 — Chaos (jigsaw) region generation
- `generation.RegionGenerator` (chaos path): seeded contiguous region-growing producing `n` regions of `n` cells each.
- Acceptance checks, in order: reject degenerate partitions (any region exactly a full row/column), then confirm fillability via `BacktrackingSolver.solve` (not every contiguous partition admits a completion). Bounded deterministic retry.
- Wire into `PuzzleGenerator` for `Variant.CHAOS`, sizes 6×6–16×16 only (constructor-level rejection for 4×4 already exists from L1).
- Tests: chaos partitions at 6/9/12/16 always pass both acceptance checks before being handed to the filler; determinism holds under the same rules as L4.

**Exit criteria:** full generation pipeline (classic + chaos) complete for all valid (size, variant, difficulty) combinations.

## Phase L8 — Hint engine
- `hint.HintEngine`: two-stage API mirroring §4.4 — `HintCandidate peek(Puzzle)` (cell + technique, doesn't consume) and `HintResult consume(Puzzle, HintCandidate)` (returns the digit to fill). Built directly on `TechniqueSolver`, capped externally by callers at 5 (cap is a per-game session concern, not a lib concern — keep the lib stateless).

**Exit criteria:** hint peek/consume round-trips correctly against `TechniqueSolver` output from L5.

## Phase L9 — Share codes
- `sharecode.ShareCodeCodec`: pack `PuzzleKey` into a fixed-width binary layout, encode/decode Base32 (RFC 4648, no padding, uppercase). Round-trip test: `decode(encode(key)) == key` for edge values (max seed, max enum ordinals) plus a golden fixed-string test so the wire format itself is pinned before any client depends on it.

**Exit criteria:** share codes are stable, documented byte layout, round-trip tested.

## Phase L10 — Cross-platform determinism hardening + release
- Dedicated `determinism` test source set: assert no `HashMap`/`HashSet` iteration anywhere reachable from `PuzzleGenerator`/`TechniqueSolver`/`DifficultyRater` (a simple reflective or code-review checklist is fine — this is a correctness-critical invariant, not a nice-to-have).
- Golden-fixture regression tests: pin a handful of `(genVersion=1, size, variant, difficulty, seed)` → exact expected grid, so any future refactor that accidentally changes output is caught immediately.
- `version.GenVersion` constant = `1`; document in Javadoc that it must bump whenever generator/region builder/hole-digging order/rater changes (§3.5).
- Confirm `build.gradle.kts` publishing block works end-to-end (tag → `VERSION` env → publish to `maven.luis-st.net/libraries/`) before Server/Android take a dependency on it.

**Exit criteria:** first tagged release (e.g. `1.0.0`) published and consumable as an exact-version dependency by both other projects.

---

### Sequencing note
L1→L6 (classic pipeline) unblocks Server phase 1 and Android phases 1–2 (single-player offline). L7 (chaos) and L8/L9 (hints/share codes) can proceed in parallel with early Android UI work once L6 lands, since they don't change the `PuzzleKey` shape. Any change after L10's first release that touches generation semantics requires a `genVersion` bump and a new published version — never a silent patch.
