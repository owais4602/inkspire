# Contributing to Inkspire

Inkspire is a native Android drawing app built on `androidx.ink`. This document is the enforceable checklist behind the rules summarized in `PLAN.md`. **Read `PLAN.md` first** — it has the architecture, the pinned versions, and the settled design decisions. This file is about *how to submit changes safely*.

If any instruction here conflicts with a comment in code, this file and `PLAN.md` win — code comments can go stale, this file is the source of truth for process.

## 0. Before you write any code

- Read `PLAN.md` in full, especially **Known traps**, **Working agreements**, and the **Key design decisions** list. Several features (infinite canvas, live parallel wet symmetry, chunked gradient pen, blur/smudge tools) were built, tested, and **explicitly rejected by the user**. Re-proposing them wastes review time — check the deviations log / design-decisions list before re-implementing anything that sounds familiar.
- Open an issue or draft PR describing the change *before* a large implementation. For anything touching `ink/` or brush recipes, state which real API you intend to call and how you verified it exists (see §3).

## 1. Environment & pinned versions

Do not upgrade Gradle, AGP, Kotlin, Compose BOM, or any `androidx.ink` artifact from what's pinned in `gradle/libs.versions.toml`. These were chosen deliberately and conservatively; newer versions may postdate what's been verified against this project.

- No Room, no KSP, no Hilt, no Robolectric, no `material-icons-extended`.
- No new Gradle dependency without a one-line rationale recorded in `PLAN.md`'s deviations log, added in the same PR.
- No bundled image/font assets. Textures are procedurally generated pure-Kotlin pixel arrays (`core/TextureGen.kt`).

A CI check greps `gradle/libs.versions.toml` and `app/build.gradle.kts` against `origin/main`; an unexplained diff there fails review unless the deviations log was updated in the same PR.

## 2. Architecture rules (enforced, not suggested)

- **`core/` and `model/` must never import `android.*` or `androidx.ink.*`.** These packages exist specifically so their logic is JVM-testable. If you think a piece of `ink/` logic needs to move there for testability, it usually means the ink-specific part needs to shrink around a pure kernel, not that the rule should bend.
- **`@OptIn(ExperimentalInkCustomBrushApi::class)` is confined to files under `ink/`.** If a `ui/` or `data/` file seems to need it, route the operation through an `ink/`-layer abstraction instead.
- **Persistence layout is frozen.** `StrokeStore`'s binary format (`id | groupId | familyName | color | size | epsilon | batch bytes`) must not be reordered, resized, or have fields removed. New brush types add new enum name *strings* through the existing field only. Any parser for a brush/family name must degrade gracefully (unknown → a safe default), never throw.
- **One process-wide `TextureBitmapStore`**, shared by the wet view and every `CanvasStrokeRenderer.create()` call site. Do not construct a brush, `BrushFamily`, or texture per-frame or per-stroke.
- **The renderer transform contract**: any matrix concatenated onto a canvas must also be passed as the `strokeToScreenTransform` argument to `CanvasStrokeRenderer.draw(...)`. Passing identity while the canvas is transformed silently breaks anti-aliasing at zoom — this has been a real bug before, watch for regressions.
- **No app-side stroke smoothing** (moving averages, Bézier refitting, point decimation). `androidx.ink` smooths internally; double-smoothing reintroduces the exact lag/wobble this project has already fixed once.
- **Never delay `ACTION_DOWN` stroke start** for any reason — this is the single highest-priority latency rule in the app.

## 3. Verifying `androidx.ink` APIs — mandatory, not optional

This library's public surface has repeatedly been misdescribed by fetched documentation summaries (a nonexistent `InProgressStrokes` composable, a nonexistent `PartitionedMesh.intersects`, hidden nullable return types). Because of this history:

- **Never write code against a remembered or fetched-doc API signature for `androidx.ink`.** Before calling any method or constructor you haven't already used elsewhere in this codebase, verify it exists by inspecting the actual pinned jar:
  ```
  javap -p -classpath ~/.gradle/caches/modules-2/files-2.1/androidx.ink/<artifact>/<version>/<hash>/<artifact>-<version>.jar <FullyQualifiedClassName>
  ```
- **`javap` does not show Kotlin nullability or parameter names.** Use named arguments for every constructor/factory call with more than two parameters, and treat any unguarded access on a platform type returned from `androidx.ink` as suspect until you've confirmed nullability by attempting to compile an unguarded access and seeing what the Kotlin compiler says.
- A reviewer who cannot see the verification step (a `javap` transcript, or a reference to an existing verified call site) in the PR description should ask for it before approving any new `ink/` API usage.

## 4. Testing discipline

- **Order of work for any change touching `core/`, `model/`, or `data/`**: (1) write or extend the JVM unit test first, (2) implement until green, (3) wire the UI, (4) run the full gate.
- **Gate, required before every commit and every PR**: `./gradlew test assembleDebug` must pass clean.
- **Trap #1**: `androidx.ink` types (`Stroke`, `Brush`, `BrushFamily`, `BrushTip`, `BrushPaint`, mesh/texture types) are native-backed and cannot be constructed in a JVM unit test — there is no Robolectric workaround in this project (Robolectric itself is banned, see §1). Do not attempt to fake coverage by touching these types in `src/test/`. If logic can't be extracted to a pure-Kotlin kernel, it is legitimately untestable in CI — say so explicitly in the PR ("not JVM-testable, on-device verified" or "not JVM-testable, needs on-device verification") rather than skipping the topic.
- No `androidTest/` sources — this project has no device farm; on-device verification is manual (§6).

## 5. Commit & PR hygiene

- **One coherent change per PR.** Do not bundle an unrelated UI redesign with a bug fix (this repo has paid for that mistake before — see the "infinite canvas + gallery redesign mixed in one diff" incident in git history).
- Commit messages state **what was verified on-device vs. what is inference** whenever the change touches touch/render/ink glue. E.g. "wet-stroke lag improvement verified on [device]; prediction-overshoot behavior is inference, not yet compared side-by-side."
- Any deviation from an agreed design or spec — including this document — gets a one-line rationale entry appended to `PLAN.md`'s deviations log **in the same PR**, not after the fact.
- Never claim parity with another product ("matches Google Keep", "identical to X") in a commit message, code comment, or PR description unless a completed side-by-side on-device comparison backs it up.
- Rebase or update your branch against `main` before requesting review; do not merge with unresolved conflicts.

## 6. On-device verification

Because this project has no CI-attached emulator or device farm, UI/feel changes are accepted only after a human runs them on a real phone.

- **Never mix automated (adb-driven) and manual (hand) input on the same touch stream during a test pass.** This project has produced two rounds of phantom "bugs" from interleaved automated+manual taps racing on the same input. Either script it, or drive it by hand — never both in the same session.
- For anything touching pan/zoom/symmetry/brush rendering, run the relevant checklist section referenced from `PLAN.md`'s "Outstanding work" / verification section before marking a PR ready for merge.
- If you cannot access a physical device, say so explicitly in the PR and mark the on-device checklist items as pending — do not mark them done from build success or emulator behavior alone.

## 7. Quality gates a reviewer should check before approving

- [ ] `./gradlew test assembleDebug` is green (CI or pasted local output).
- [ ] No diff to `gradle/libs.versions.toml` / dependency blocks in `app/build.gradle.kts` without a matching deviations-log entry.
- [ ] No `ExperimentalInkCustomBrushApi` opt-in outside `ink/`: `grep -rn "ExperimentalInkCustomBrushApi" app/src/main/java --include="*.kt" | grep -v "/ink/"` returns empty.
- [ ] No `android.*` / `androidx.ink.*` import inside `core/` (`model/` is exempt — e.g. `StrokeEntry` legitimately wraps an ink `Stroke`).
- [ ] New `core/`/`model/`/`data/` logic has a test added in the same PR, written before the implementation per commit history (not required to prove chronologically, but the PR should read that way).
- [ ] Any new/changed `androidx.ink` API call cites how it was verified (§3).
- [ ] PR description states what's on-device-verified vs. inferred, for anything touching touch/render/ink glue.
- [ ] Persisted file formats (`StrokeStore`, `DrawingRepository`'s JSON index) are backward compatible — old files still decode; forward-compat (unknown enum names, missing new fields) degrades safely instead of throwing.
- [ ] No blur/smudge/soft-focus/region-effect brush or filter, no chunked gradient pen, no auto-growing canvas — these are settled rejections, not open design questions.

## 8. Reporting problems

Open an issue with: device model + Android version, exact repro steps, whether the touch input was automated or manual (never both in one session — see §6), and the commit SHA. For rendering/latency issues, a short screen recording is worth far more than a text description.
