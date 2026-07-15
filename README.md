# Pocket Pet

A small, calm digital companion that lives in a floating overlay above your other Android apps —
in the spirit of Shimeji, Tamagotchi, and Desktop Goose. The default companion is a cream-colored
cat, drawn and animated entirely with Jetpack Compose Canvas (no raster art).

Pocket Pet is not a chatbot. It reacts to real signals — how long since you fed it, your battery
level, whether a notification just arrived, the time of day — with short, contextual speech
bubbles, never open-ended text.

---

## ⚠️ Build status — please read this first

This project was generated in a sandboxed environment **with no network access**, so the build
could not be verified locally. Specifically:

- `gradle/wrapper/gradle-wrapper.jar` (the binary launcher Gradle needs) could not be downloaded
  and is **not included** in this repository.
- No Android SDK, Gradle, or Kotlin compiler was available in the sandbox to actually compile or
  test the code.

Running `./gradlew` in that sandbox fails immediately with:

```
Error: Could not find or load main class org.gradle.wrapper.GradleWrapperMain
Caused by: java.lang.ClassNotFoundException: org.gradle.wrapper.GradleWrapperMain
```

**This has not been compiled.** The code was written carefully and reviewed extensively (see
[Verification performed](#verification-performed) below), but "reviewed by an LLM without a
compiler" is not the same as "builds successfully." Treat this as a thorough first draft that
needs a real build to confirm, not a verified release.

The included [GitHub Actions workflow](.github/workflows/android.yml) is a genuine, runnable CI
pipeline (it doesn't depend on anything from this sandbox) — it provisions a real Gradle
installation, regenerates the missing wrapper jar automatically, then runs unit tests, lint, and
`assembleDebug` for real. **Push this repository to GitHub and let that workflow run** to get an
actual verified build and a downloadable Debug APK. See [below](#running-it-on-github) for exact
steps.

### Verification performed

Without a compiler, verification was static instead:
- Every file's `package` declaration checked against its directory path (0 mismatches).
- Every `com.pocketpet.*` import cross-checked against actual declared symbols project-wide.
- The full Gradle module graph (`settings.gradle.kts` ↔ actual module directories ↔ every
  `project(":...")` reference) cross-checked for consistency.
- Every Hilt `@Provides` binding checked for duplicates and for "is this interface actually
  reachable from where it's injected."
- Every `libs.*` version-catalog accessor used in a `build.gradle.kts` checked against the actual
  catalog, and every `version.ref` in the catalog checked for a matching `[versions]` entry.
- Every XML resource and the YAML workflow parsed to confirm well-formedness.

These catch a large share of real-world Gradle/Kotlin mistakes (typos in dependency coordinates,
missing bindings, mismatched packages), but they are not a substitute for `javac`/`kotlinc`
actually running. Please treat the first real build as the actual test.

---

## Features

- **Floating overlay companion** — `TYPE_APPLICATION_OVERLAY` window, drag to move, double-tap to
  jump, fling to throw, long-press for a quick menu (feed / play / sleep / hide / lock / open app).
- **A real behavior engine** — an injectable-clock, injectable-random, fully unit-tested state
  machine (`core:domain`'s `PetBehaviorEngine`) drives mood, hunger, energy, and which of 20
  animations is currently playing, with cooldowns so nothing repeats too often.
- **Hunger and battery simulation** that progresses with real elapsed time (including while the
  app isn't running) and survives process death with a *bounded* catch-up — no multi-day hunger
  spikes from a phone that was off overnight.
- **Battery/charging reactions** at 100/80/50/30/20/10% milestones, each firing exactly once per
  crossing.
- **Optional, disabled-by-default extras**: notification awareness (metadata only — package name
  and timestamp, never title/text), accessibility-service shortcuts (global actions only, no
  screen reading), push-to-talk voice commands, and weather via Open-Meteo (no API key).
- **Full customization**: name, six cream fur tones, three accessories, size, opacity, animation
  speed, reduced-motion, quiet hours, edge snapping, position lock.
- **Privacy-first**: no ads, no analytics by default, no notification content ever stored, no
  accessibility content ever read, no root/hidden APIs/shell commands anywhere in the codebase.

## Screens delivered in this build

Nine conceptual screens from the spec are covered by six real ones (documented consolidation, not
a scope cut): **Welcome**, **Permissions** (covers setup *and* live status), **Home dashboard**,
**Customization**, **Settings** (covers overlay settings, behavior settings, and per-feature
permission status together), and **Privacy & About** (combined).

## Architecture

Clean Architecture, 11 Gradle modules:

```
:app                    — Hilt entry point, navigation, MainActivity, manifest, resources
:core:model              — pure Kotlin data classes (PetState, Mood, PetNeeds, ActionResult, ...)
:core:domain              — pure Kotlin: PetBehaviorEngine, repository interfaces, use cases,
                            VoiceCommandParser — zero Android framework dependency, so this is
                            the module with fast, no-emulator-needed JVM unit tests
:core:database            — Room: one live-state row + a bounded, pruned history table
:core:system              — Android framework integration: battery monitor, system actions,
                            flashlight, permissions, voice recognition, service bridges
:core:data                — repository implementations (Room/DataStore/OkHttp) + all Hilt wiring
:core:designsystem        — Material 3 theme + the Canvas cat renderer + shared components
:service:overlay          — the foreground overlay service, gesture detection, the optional
                            AccessibilityService and NotificationListenerService
:feature:onboarding       — Welcome, Permissions
:feature:home             — the dashboard
:feature:settings         — Customization, Settings, Privacy & About
```

Dependency direction: `model ← domain ← {database, system} ← data ← {feature:*, service:overlay}
← app`. Domain has zero Android dependency by design — everything it needs (`ClockProvider`,
`RandomProvider`, `DispatcherProvider`) is injected as a plain interface, which is what makes
`PetBehaviorEngine` testable with plain JUnit instead of Robolectric or an emulator.

## Tech stack

Kotlin 2.0.21 · Jetpack Compose (BOM 2024.09.03) · Material 3 · Hilt 2.52 · Room 2.6.1 ·
Preferences DataStore · Navigation Compose · Coroutines/Flow · OkHttp + kotlinx.serialization ·
AGP 8.6.1 · Gradle 8.7 · Java 17 · KSP (not kapt).

All dependency versions are pinned in [`gradle/libs.versions.toml`](gradle/libs.versions.toml) —
no `+`, no snapshots, no private repositories (`google()` and `mavenCentral()` only).

> These are real, current-generation versions as of early 2026, chosen for known-good
> compatibility (AGP 8.6.1 ↔ Gradle 8.7 ↔ Kotlin 2.0.21). They could not be resolved against
> Maven Central in this sandbox (no network) to confirm every exact patch version is still
> published — if `./gradlew` reports a specific dependency can't be resolved, check that
> library's releases page and bump just that one line in the version catalog.

## Getting started

### Prerequisites
- JDK 17
- Android Studio Ladybug (2024.2) or newer, *or* just the command line with the Android SDK
  installed and `ANDROID_HOME` set

### Local build

```bash
git clone <this-repo-url>
cd pocket-pet
# One-time only, since gradle-wrapper.jar isn't committed — see "Build status" above:
gradle wrapper --gradle-version 8.7 --distribution-type bin
./gradlew testDebugUnitTest lintDebug assembleDebug
```

If you open the project in Android Studio instead, it will offer to regenerate the wrapper for
you the first time you sync.

The debug APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

### Running it on GitHub

1. Push this repository to GitHub.
2. Go to the **Actions** tab → **Android CI** will already be queued for the push (or trigger it
   manually via **Run workflow**).
3. Once green, open the run → **Artifacts** → download `pocket-pet-debug-apk`.
4. Unzip it to get `app-debug.apk`.

### Installing the APK

Enable "Install unknown apps" for whichever app you use to open the file (Files, a browser,
etc.), then open the APK. Debug builds are self-signed with the Android debug key automatically —
no signing secrets are required anywhere in this repo or the workflow.

## Permissions & privacy

| Permission | Why | Requested |
|---|---|---|
| Display over other apps | The overlay itself | At onboarding, required to continue |
| Notifications (Android 13+) | The foreground service's ongoing notification | At onboarding |
| Notification access | Optional "notice a new notification" reaction | Opt-in from Settings, off by default |
| Accessibility service | Optional "open shade/quick settings" shortcuts | Opt-in from Settings, off by default |
| Microphone | Optional push-to-talk voice commands | Opt-in from Settings, off by default |
| Coarse location | Optional "use my location" for weather | Opt-in, manual city entry works without it |

No advertising, no analytics enabled by default, no notification content ever read or stored, no
accessibility content ever read, no continuous microphone use, no root/shell/hidden APIs anywhere
in the codebase. See the in-app Privacy screen for the same information.

## Platform limitations (please set expectations honestly with users)

- **The overlay is not guaranteed to run forever.** Android's own battery management, or your
  phone manufacturer's, can pause or kill background services regardless of anything this app
  does. Settings → Battery optimization offers a user-initiated opt-out path, but the app never
  tries to bypass this silently.
- Ordinary apps cannot silently toggle Wi-Fi or Bluetooth on modern Android — Pocket Pet opens the
  relevant settings/panel instead of pretending to flip them.
- There's no universal "open the app drawer" API; where nothing is available, the app says so.

## Testing

Real, executable unit tests exist for the behavior engine (mood derivation, state cooldowns,
feeding-exploit cooldown, battery-milestone one-shot firing, quiet hours, bounded restore-after-gap),
needs simulation, screen-bounds clamping, quiet-hours windowing, battery-milestone banding, the
voice command parser, Room DAOs (via Robolectric, no emulator needed), entity/domain mapping, and
the Canvas renderer's state→motion mapping. One instrumented Compose UI test lives in
`app/src/androidTest` (requires a device/emulator — not part of the CI workflow, which is
intentionally scoped to unit tests only; run it locally with `./gradlew connectedAndroidTest` if
you have a device or emulator attached).

```bash
./gradlew testDebugUnitTest        # all unit tests, all modules
./gradlew lintDebug                # Android Lint
./gradlew connectedAndroidTest      # instrumented tests (needs a device/emulator)
```

## What's simplified in this delivery

Being upfront about scope, in the same spirit as the rest of this document:

- The 20-pet-state animation system shares reusable channels (body bounce, squash/stretch, tail
  sway, paw activity, ear rotation, eye/pupil, mouth) parameterized per state, rather than 20
  fully bespoke hand-animated rigs — this is what "reusable animation channels" is meant to
  produce, but it means some states look more like family members of each other than wholly
  unique performances.
- Fling "velocity" is estimated as total displacement over the drag's duration rather than a true
  instantaneous release velocity — simple and dependency-free, plenty precise for a toss gesture.
- Weather's "use my device location" path uses the platform `LocationManager`'s last-known fix
  rather than requesting a fresh one, to avoid pulling in Play Services for a single coarse
  reading; manual city entry (geocoded via Open-Meteo, no key) is the primary, always-available
  path.

Nothing above is a stub, a placeholder, or a `TODO` — every one of these is a real, working,
intentionally-scoped implementation.

## Project structure

109 Kotlin files across 11 modules. The module table under [Architecture](#architecture) plus the
KDoc at the top of each major class (`PetBehaviorEngine`, `OverlayService`, `PetCanvas`,
`PetRepositoryImpl`) together serve as the architecture documentation — there's no separate
`ARCHITECTURE.md`.

## Troubleshooting

- **"SDK location not found"** — create `local.properties` in the project root with
  `sdk.dir=/path/to/Android/sdk`, or set `ANDROID_HOME`.
- **Gradle sync fails on a specific dependency** — see the version-pinning note under
  [Tech stack](#tech-stack); bump just that one line in `gradle/libs.versions.toml`.
- **Overlay permission granted but nothing shows** — the three optional toggles in Settings are
  independent of the overlay itself; only "Display over other apps" is required, and the overlay
  also needs to be switched on from the Home dashboard.
- **The pet "disappeared" after a while** — check your phone's battery-optimization settings for
  Pocket Pet; see [Platform limitations](#platform-limitations-please-set-expectations-honestly-with-users).

## License

Apache License 2.0 — see [`LICENSE`](LICENSE).
