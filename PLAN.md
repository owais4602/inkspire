# Inkspire — a top-tier Android drawing app (execution spec)

## Context

The user started from the p5.js Kaleidoscope example and wants a native Android drawing app that surpasses Google Keep's drawing feature: effortless pen/finger inking, resizable canvas, full color control, gradients, and a toggleable kaleidoscope/symmetry mode. **This plan will be executed by a coding model (Sonnet); it is written as a prescriptive spec — versions pinned, tests enforced, traps documented. Follow it literally; deviations must be recorded in the repo's PLAN.md with a one-line rationale.**

Decisions already made with the user:
- **Stack**: Native Kotlin + Jetpack Compose + **Jetpack Ink** (`androidx.ink` 1.0.0 stable — the low-latency stroke engine behind Google Keep's inking; verified on Google Maven).
- **Symmetry**: toggleable mode (off / mirror / 2–12-way kaleidoscope) alongside normal freehand.
- **Canvas**: resizable fixed canvas (image-editor model) with pinch zoom/pan.
- **Testing**: physical phone over USB, finger input (none attached yet — stylus support built in but untested for now). Therefore **JVM unit tests are the primary safety net**; structure code so logic is JVM-testable.

Environment (verified): JDK 21, Android SDK `~/Android/Sdk` (platform 35, build-tools 35.0.0), `adb`, no emulator. `~/dev/graphics` is empty — greenfield.

## Step 0

Create project at `~/dev/graphics/inkspire/` (app id `dev.stupifranc.inkspire`), `git init`, **copy this plan into the repo as `PLAN.md`** and keep its milestone checkboxes updated as work proceeds.

## Pinned versions — do not upgrade during v1

Chosen deliberately conservative and mutually compatible (all stable, all verified to exist on Maven; newer AGP 9.x / Kotlin 2.4 exist but are NOT to be used — they postdate the coding model's reliable knowledge):

| Component | Version |
|---|---|
| Gradle wrapper | 8.14 |
| AGP (`com.android.application`) | 8.13.2 |
| Kotlin + compose-compiler plugin + serialization plugin | 2.1.21 |
| compileSdk / targetSdk / minSdk | 35 / 35 / 26 |
| Compose BOM | 2025.06.01 |
| `androidx.ink:ink-{authoring,brush,strokes,geometry,rendering,storage}` | 1.0.0 |
| `androidx.activity:activity-compose` | 1.10.1 |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.9.0 |
| `androidx.navigation:navigation-compose` | 2.9.0 |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.8.1 |
| JUnit / Truth / kotlinx-coroutines-test | 4.13.2 / 1.4.4 / 1.10.2 |

Use a version catalog (`gradle/libs.versions.toml`). **No Room, no KSP, no Hilt** — persistence is file-based (below); DI is manual constructor injection. Do not add dependencies beyond this list without recording rationale in PLAN.md.

## Architecture

**Ink pipeline (canonical, per developer.android.com Ink API docs):**
- **Wet layer**: `InProgressStrokesView` (`ink-authoring`) hosted via `AndroidView` — receives touch while pointer is down; front-buffered low latency on API 29+, automatic fallback below; motion prediction built in.
- **Dry layer**: custom `View` beneath it rendering finished strokes via `CanvasStrokeRenderer` (`ink-rendering`). On `InProgressStrokesFinishedListener`, strokes hand off wet→dry (call `removeFinishedStrokes` only after the dry layer has drawn them).
- One **document↔screen transform** shared by both: set as `motionEventToWorldTransform` on the wet view; passed as the transform arg to `CanvasStrokeRenderer.draw()`.

**Module layout (single `:app`):**
```
app/src/main/java/dev/stupifranc/inkspire/
├── core/            PURE KOTLIN, no Android/ink imports (JVM-testable):
│   ├── Affine2.kt          minimal 2×3 affine transform (floats)
│   ├── SymmetryEngine.kt   SymmetryConfig -> List<Affine2>
│   ├── HistoryStack.kt     undo/redo command stack (bounded ~100)
│   ├── ResizeAnchor.kt     stroke-offset math for canvas resize
│   └── GradientPen.kt      color interpolation along gesture length
├── model/           Drawing, StrokeEntry, BrushSpec, CanvasSpec, SymmetryConfig
├── ink/             Android+ink glue: DrawingSurface.kt (wet view wrapper),
│                    DryStrokesView.kt, StrokeStore.kt (ink-storage codec), Eraser.kt
├── data/            DrawingRepository: JSON index via kotlinx-serialization
│                    (metadata: id, name, size, bg, timestamps, thumbnail path)
│                    + one binary stroke file per drawing; Thumbnails.kt
├── ui/              gallery/, editor/ (EditorScreen, EditorViewModel, toolbars),
│                    components/ (ColorPicker, BrushPicker, SizeSlider, ResizeDialog)
└── MainActivity.kt  single activity, Compose navigation
```

**Key designs:**
- **Document model**: all stroke coordinates in document space; `StrokeEntry` = ink `Stroke` + stable id + symmetry-group id (copies born from one gesture share a group → undo removes them together).
- **Symmetry**: `SymmetryConfig(sectors, mirror, center)` → rotation matrices about center; with mirror, each sector gets a reflected twin (the p5 example's `rotate` + `scale(1,-1)`, 2N copies). *Live path*: per touch event, `MotionEvent.obtain(event)` + `.transform(matrix)` per copy, driving parallel wet strokes (map pointerId → list of strokeIds). *Fallback* (see traps): wet layer shows primary stroke only; transformed dry copies generated on finish from the `StrokeInputBatch`. Faint sector-guide overlay; draggable center.
- **Canvas/zoom/resize**: one transform maps document→screen; two-finger pinch/pan (one finger always draws), double-tap fits. Resize dialog: presets (1:1, 4:3, 16:9, screen, A4) + custom px + content anchor; anchoring = translating existing stroke coordinates (`ResizeAnchor`).
- **Brushes/eraser**: `StockBrushes` pressure pen, marker, highlighter; per-brush size memory; live size preview. Eraser = whole-stroke via `ink-geometry` hit-testing against an eraser circle, one undoable command. Pixel/partial erase is out of v1.
- **Color/gradients**: HSV wheel + value/alpha sliders + hex field + persisted recents + curated palettes. Gradients (Ink brushes are solid per stroke — scope honestly): (1) canvas background linear/radial gradients — full support; (2) **gradient pen**: long gestures chunked into short consecutive strokes with interpolated colors (`GradientPen`), one undo group; (3) per-vertex gradient strokes = post-v1.
- **Persistence**: `DrawingRepository` — JSON index file + per-drawing binary (each stroke = brush spec + `ink-storage`-encoded input batch). Autosave debounced ~2s + on `onStop`. Thumbnails on save.
- **Export**: render strokes to `Bitmap` at ×1/×2/×4, PNG via `MediaStore` (scoped-storage path; pre-29 compat), share sheet.
- **Input**: finger+stylus draw by default; stylus-only toggle (= palm rejection); pressure→width via pressure-pen brush.

## Testing discipline (non-negotiable)

- **Work order inside every milestone**: (1) write/extend JVM unit tests for the milestone's `core/`+`data/` logic, (2) implement until green, (3) wire UI, (4) run the full gate.
- **Milestone gate** — a milestone is done only when: `./gradlew test assembleDebug` passes clean, PLAN.md checkbox ticked, one git commit per milestone (`m3: symmetry mode — live kaleidoscope + guides`).
- **Test inventory** (in `app/src/test/`): `SymmetryEngineTest` (N-way angles sum to identity, mirror twins reflect, round-trip point mapping), `Affine2Test`, `HistoryStackTest` (undo/redo/bounds/group semantics), `ResizeAnchorTest`, `GradientPenTest` (endpoint colors exact, monotone interpolation), `DrawingRepositoryTest` (JSON round-trip in temp dir, corrupt-file recovery), `EditorViewModelTest` (fake StrokeStore: add/undo/erase flows).
- Instrumented tests: **deferred** (no device attached). Do not write `androidTest` code in v1.

## Known traps — read before coding

1. **`androidx.ink` loads native `.so` libraries.** Code touching `Stroke`/`StrokeInputBatch`/mesh types CANNOT run in JVM unit tests — no Robolectric workaround. Keep ink types strictly out of `core/`; access stroke storage through a `StrokeStore` interface with an in-memory fake for ViewModel tests.
2. **`SymmetryEngine` must be pure Kotlin** — compute transforms as `Affine2` (own float type), convert to `android.graphics.Matrix` only at the view/ink boundary. This is what makes the math testable.
3. **`MotionEvent.transform()` mutates in place** — always `MotionEvent.obtain(event)` a copy first, `recycle()` it after handing off.
4. **Live wet symmetry time-box**: if driving parallel wet strokes fights the `InProgressStrokesView` API after ~2 focused attempts, switch to the documented fallback (dry copies on finish) and note it in PLAN.md. The fallback is still good UX; do not sink hours here.
5. **Wet→dry handoff**: never `removeFinishedStrokes()` before the dry layer has rendered the strokes, or they visibly flicker/vanish for a frame.
6. **API reference**: follow the official Ink API guide (developer.android.com → stylus input → Ink API) for exact 1.0.0 signatures; do not invent method names from memory.

## Milestones (each ends buildable, tested, committed)

- [x] 0. **Scaffold** — Gradle project skeleton, wrapper, version catalog, git init, PLAN.md.
- [x] 1. **Skeleton + it draws** — Gradle project (pinned versions), editor screen, wet/dry pipeline, one brush, undo/redo/clear. Tests: `Affine2Test`, `HistoryStackTest`. *The latency benchmark moment.* `./gradlew test assembleDebug` green (16/16 unit tests). On-device latency check still pending — no phone was attached during this milestone; run the manual checklist once one is.
- [ ] 2. **Brushes & color** — brush picker, size slider, HSV color picker + recents, whole-stroke eraser. Tests: color conversion + `EditorViewModelTest` erase flows.
- [ ] 3. **Symmetry mode** — `SymmetryEngine`, live kaleidoscope (2–12 way, mirror), sector guides, draggable center. Tests: `SymmetryEngineTest`.
- [ ] 4. **Canvas control** — zoom/pan/fit, resize dialog with anchoring, background color/gradient. Tests: `ResizeAnchorTest`.
- [ ] 5. **Documents** — gallery grid, `DrawingRepository` persistence, autosave, thumbnails, rename/delete/duplicate. Tests: `DrawingRepositoryTest`.
- [ ] 6. **Export & polish** — PNG export ×1/×2/×4 + share, gradient pen, stylus-only setting, dark theme, app icon, haptic ticks. Tests: `GradientPenTest`.

## Deviations from original plan

- **Dry layer is a Compose `Canvas` + `drawIntoCanvas`, not a custom `android.view.View`.** Verified against the real 1.0.0 jars: `CanvasStrokeRenderer.draw(android.graphics.Canvas, Stroke, Matrix)` accepts any raw `Canvas`, so a custom View subclass isn't needed — `drawIntoCanvas { it.nativeCanvas }` inside a Compose `Canvas` composable is simpler and avoids View/Compose interop boilerplate for a case that doesn't need it.
- **`model/CanvasSpec.kt` and `model/BrushSpec.kt` are introduced when first used (M4 and M2 respectively), not upfront in M1.** Only `model/StrokeEntry.kt` was needed for M1; adding the others now would be dead code ahead of the milestones that actually consume them.
- Confirmed via jar/source inspection (not memory) before writing Ink integration code: `InProgressStrokesView.startStroke/addToStroke/finishStroke/cancelStroke` signatures, `CanvasStrokeRenderer.create()`, and `Brush.createWithColorIntArgb(family, colorIntArgb, size, epsilon)` param order. No `InProgressStrokes` Compose composable exists in 1.0.0 (a docs summary hallucinated one) — the wet layer is genuinely `InProgressStrokesView` via `AndroidView`.

## Verification

- Per milestone: `./gradlew test assembleDebug` (JDK 21 + SDK 35 already installed; only Gradle/deps download needed).
- On-device (when phone is plugged in, USB debugging on): `adb install -r` the debug APK; manual checklist — fast scribbles (latency/prediction), multi-finger, undo depth ≥50, symmetry with mirror on/off, resize with content anchored, kill-and-restore (autosave), export lands in Photos. Screenshots via `adb exec-out screencap -p` for review.
- Emulator fallback (~1.5 GB image) only if the phone never materializes.

## Out of scope for v1 (deliberate)

Layers, partial/pixel eraser, per-vertex gradient strokes, custom brush textures, SVG export, shape recognition, cloud sync. Architecture (document-space strokes, command stack, `core/` isolation) is chosen so none of these require rework.
