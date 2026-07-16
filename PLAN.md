# Inkspire — a top-tier Android drawing app (working spec)

Native Kotlin + Jetpack Compose + **Jetpack Ink** (`androidx.ink` 1.0.0) drawing app that surpasses Google Keep's drawing feature: low-latency pen/finger inking, resizable fixed canvas with deep zoom, full color control, media brushes (pencil, watercolor, dry-ink, calligraphy, rainbow, neon, airbrush, …), canvas gradients, and a toggleable kaleidoscope/symmetry mode.

**Status (2026-07-16):** Milestones 0–11 are code-complete and committed. The full milestone-by-milestone record, specs, and deviations log were trimmed from this file on 2026-07-16 — they live in git history (`git log -p PLAN.md`). What remains here are the **binding guidelines** every future change must follow, plus the outstanding work.

> **Contributors: read [CONTRIBUTING.md](CONTRIBUTING.md) before touching any code.** It is the enforceable version of the rules below.

## Pinned versions — do not upgrade

Deliberately conservative and mutually compatible; all verified on Google Maven. Newer AGP / Kotlin exist but are NOT to be used.

| Component | Version |
|---|---|
| Gradle wrapper | 8.14 |
| AGP (`com.android.application`) | 8.13.2 |
| Kotlin + compose-compiler plugin + serialization plugin | 2.1.21 |
| compileSdk / targetSdk / minSdk | 35 / 35 / 26 |
| Compose BOM | 2025.06.01 |
| `androidx.ink:ink-{authoring,brush,strokes,geometry,rendering,storage}` | 1.0.0 |
| `androidx.input:input-motionprediction` | (pinned in `libs.versions.toml` — added for Keep-parity motion prediction) |
| `androidx.activity:activity-compose` | 1.10.1 |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.9.0 |
| `androidx.navigation:navigation-compose` | 2.9.0 |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.8.1 |
| JUnit / Truth / kotlinx-coroutines-test | 4.13.2 / 1.4.4 / 1.10.2 |

Version catalog in `gradle/libs.versions.toml`. **No Room, no KSP, no Hilt, no Robolectric, no material-icons-extended.** Persistence is file-based; DI is manual constructor injection; icons are hand-drawn in `ToolIcons` style. Do not add dependencies without recording rationale here.

## Architecture

**Ink pipeline:**
- **Wet layer**: `InProgressStrokesView` (`ink-authoring`) hosted via `AndroidView`; `eagerInit()` at construction + `postInvalidateOnAnimation()` after `finishStroke` (first-stroke handoff fix). Motion prediction is **not** built into the library — the app wires `MotionEventPredictor` itself and passes predicted events as `addToStroke`'s 4th param. `requestUnbufferedDispatch(event)` on pen `ACTION_DOWN`.
- **Dry layer**: Compose `Canvas` + `drawIntoCanvas` rendering finished strokes via `CanvasStrokeRenderer`. On `InProgressStrokesFinishedListener`, strokes hand off wet→dry; never `removeFinishedStrokes()` before the dry layer has rendered them.
- **Renderer transform contract**: whatever matrix is concat'd onto the canvas must ALSO be passed as `strokeToScreenTransform` to `renderer.draw(...)` — the renderer reads scale off it to size anti-aliasing. Never pass identity while the canvas is transformed.
- One shared **document↔screen transform** (`core/Viewport`, MAX_SCALE 32×); brush epsilon `0.0078125f` (= 0.25 ÷ 32, keeps quantization ≤ 1 px at max zoom).

**Module layout (single `:app`, package `dev.stupifranc.inkspire`):**
```
core/    PURE KOTLIN, no Android/ink imports (JVM-testable): Affine2, SymmetryEngine,
         HistoryStack, ResizeAnchor, Viewport, ContentBounds, PinchSteps, ReorderTarget,
         TextureGen, EntryCollection, ColorConversions, clampToRect …
model/   StrokeEntry, BrushSpec (BrushFamilyChoice), CanvasSpec, SymmetryConfig,
         DrawingMeta, GalleryPrefs — serializable, no ink types
ink/     Android+ink glue (the ONLY place @OptIn(ExperimentalInkCustomBrushApi) is allowed):
         DrawingSurface, StrokeStore, BrushCatalog, InkTextures, CanvasExporter,
         SymmetryStrokes, StrokeBounds, Eraser
data/    DrawingRepository (JSON index + per-drawing binary), RecentColorsStore,
         GalleryPrefsStore
ui/      gallery/, editor/, components/ (ToolDock, HistoryPill, ColorPicker, Minimap …)
MainActivity.kt  single activity, Compose navigation (gallery ↔ editor)
```

**Key design decisions (settled — do not revisit without user sign-off):**
- **Fixed, manually resizable page** (image-editor model), deep pinch zoom/pan. The infinite/auto-growing canvas was built, hand-tested by the user, **rejected and fully reverted** — never resurrect `CanvasGrowth` or remove the `CanvasSpec.contains` drawing gate.
- **Drawing is bounds-gated to the page; erasing is deliberately NOT** (cleanup must reach strokes stranded off-page after a shrink).
- **Symmetry uses the dry-copy fallback**, not parallel wet strokes: primary stroke stays fully live; on finish its `StrokeInputBatch` is replayed through each `SymmetryEngine` transform (`ink/SymmetryStrokes.kt`), all sharing one undo group. Symmetry replicas are not bounds-gated. `InProgressStrokesView` supports only one active stroke per pointer — verified in bytecode; don't retry live parallel wet symmetry.
- **Persistence**: our own `BrushFamilyChoice` enum (never `ink-storage`'s `@Experimental` generic `BrushFamily` codec). `StrokeStore` binary layout `id | groupId | familyName | color | size | epsilon | batch bytes` **must not change** — new brushes only add enum name strings; `parseBrushFamilyChoice` maps legacy/unknown names to `PEN`, never throws.
- **Brush identity is user-verified on-device (Keep parity)**: `PEN → StockBrushes.marker()` (speed-sensitive), `MARKER → pressurePen()`, `HIGHLIGHTER → highlighter()`. Do not remap or "tune" these.
- **No app-side input smoothing/filtering** (moving averages, Bézier fitting, decimation) — the library smooths internally; double-smoothing causes the exact wobble it pretends to fix. **Never delay `ACTION_DOWN` stroke start.**
- **`Tool.NONE` is the initial tool** (deliberate UX, not a bug fix — double-tap-fit lives there).
- **GradientPen (chunked-stroke gradients) was deleted at the user's request** — along-stroke color = `BrushBehavior` targets only, inside one real stroke. **Blur/smudge/soft-focus/region effects were explicitly dropped by the user** — do not build or propose them.
- One process-wide `TextureBitmapStore` shared by the wet view and all three `CanvasStrokeRenderer.create()` sites (no wet→dry texture pop); every `BrushFamily` built once and cached (`toBrushFamilyChoice` matches by identity against cache). Texture budget: each ≤ 256×256 ARGB_8888, total ≤ 2 MB, generated procedurally (`core/TextureGen`), no bundled image assets.
- Export/thumbnails render through a hardware path (`RenderNode`+`HardwareRenderer`+`ImageReader`) on API 29+, software fallback below (particle brushes drop on software canvases); thumbnails supersample 2× then downscale.

## Testing discipline (non-negotiable)

- **Work order for every change**: (1) write/extend JVM unit tests for the `core/`+`data/`+`model/` logic FIRST, (2) implement until green, (3) wire UI, (4) run the gate.
- **Gate**: `./gradlew test assembleDebug` passes clean before any commit.
- **Trap #1 (memorize):** `androidx.ink` types (`Stroke`, `Brush`, `BrushFamily`, `BrushTip`, `BrushPaint`, meshes, textures…) are native-backed and **cannot be constructed in JVM unit tests** — no Robolectric workaround. Keep ink types strictly out of `core/`/`model/`; extract every testable kernel to pure Kotlin. Do not fake coverage by touching ink types in tests, and do not invent tests for native-backed glue — record it as on-device-verified instead.
- No `androidTest/` code in v1 (no device farm; on-device checks are manual with the user).

## Known traps — read before coding

1. **Never invent `androidx.ink` API names and never trust fetched doc summaries.** Verify against the real jars in `~/.gradle/caches/modules-2/files-2.1/androidx.ink/` with `javap` — doc summaries have hallucinated APIs for this library repeatedly (`PartitionedMesh.intersects`, an `InProgressStrokes` composable). `javap` doesn't show Kotlin nullability or param names — use **named arguments** and let the compiler arbitrate (`PartitionedMesh.computeBoundingBox()` returns a *nullable* `Box`; eraser hit-testing is `computeCoverageIsGreaterThan`, not `intersects`).
2. **`MotionEvent.transform()` mutates in place** — always `MotionEvent.obtain(event)` a copy first, `recycle()` after.
3. **Wet→dry handoff**: never `removeFinishedStrokes()` before the dry layer has drawn the strokes (visible flicker).
4. **`detectTransformGestures` eats one-finger scroll** over lazy grids — the gallery uses a hand-rolled `PointerEventPass.Initial` detector that only consumes with 2+ pointers down. Don't "simplify" it back.
5. **Never assume a grid column count** (gallery is `Adaptive`); hit-test against `visibleItemsInfo` by key, and remember the header is a grid item.
6. **`EntryCollection.transformAll` must remap history-command snapshots too** (`TransformableCommand`) — undo/redo after a resize corrupts positions otherwise. Regression-tested; keep it that way.

## Working agreements (standing)

- JVM tests before implementation for `core/`+`data/` logic.
- One commit per milestone/feature, message stating what was verified on-device vs. inference.
- Verify ink APIs against the cached jars, not memory or fetched docs.
- Record every deviation from an agreed spec in this file with a one-line rationale.
- Never claim parity with / equivalence to another app ("same as Google Keep") without a completed side-by-side on-device pass.
- UI feel is accepted only by the user's hands on the phone — "builds and looks right in code review" has been wrong repeatedly (museum-wall gallery, infinite canvas, threshold reorder).

## Outstanding work

1. ~~**The consolidated on-device acceptance pass**~~ — **DONE (2026-07-16): the user has completed extensive on-device testing** covering the accumulated M5/M7/M8/M9a/M10a/M10b/M11 debt. Any issues found from here are new bug reports, not standing debt.
2. **Known rough edges (accepted for now):**
   - Wet strokes aren't clipped to the page (candidate: `InProgressStrokesView.maskPath` — verify semantics against the jar first).
   - Paper-treatment constants (`WORKSPACE_COLOR` etc., top of `ink/DrawingSurface.kt`) are first-guess and hardcoded light-only — must become theme-aware.
   - Double-tap in `Tool.NONE`/`Tool.PAN` can leave two stray undoable dots; the fix (delaying stroke start) costs latency — decide with the user, never silently.
   - Brush recipe numerics (watercolor speed range, airbrush opacity) are starting values — tune on-device with the user, one at a time, and record final values here.
   - Thumbnails only regenerate on save — pre-M11 airbrush drawings stay thumbnail-less until next edited.

## Verification

- Per change: `./gradlew test assembleDebug` (JDK 21 + SDK 35 installed).
- On-device (USB debugging): `adb install -r` the debug APK; manual checklist — fast scribbles (latency/prediction), multi-finger, undo depth ≥50, symmetry with mirror on/off, resize with content anchored, kill-and-restore (autosave), export lands in Photos. Screenshots via `adb exec-out screencap -p`.
- On-device tests must be **single-source input** — adb-driven with hands off, or user-driven alone; interleaving both on one touch stream has produced phantom "bugs" twice.

## Out of scope for v1 (deliberate)

Layers, partial/pixel eraser, per-vertex gradient strokes, SVG export, shape recognition, cloud sync, blur/smudge (user-dropped). Architecture (document-space strokes, command stack, `core/` isolation) is chosen so none of these require rework.

## Deviations log

(Older entries trimmed 2026-07-16 — see git history. New deviations go here, one line each with rationale.)
