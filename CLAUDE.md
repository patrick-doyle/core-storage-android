# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew assembleDebug

# Unit tests
./gradlew test

# Instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

KSP generates Dagger code at build time — run `assembleDebug` after adding/changing DI wiring before expecting IDE resolution.

## Architecture

**Stack:** Kotlin, Jetpack Compose, Dagger 2 (KSP), Retrofit + OkHttp, kotlinx.serialization

**Package layout:**
- `com.pdoyle.vcd.app` — app-wide singletons (DI, network, repos, utils). All managed by dagger graph.
- `com.pdoyle.vcd.features.<name>` — one package per screen/feature
- `com.pdoyle.vcd.features.common` — shared compose ui elements
- `com.pdoyle.vcd.features.common.theme` — shared theme for compose

### DI hierarchy

`AppComponent` (`@AppScope`, singleton) owns `NetworkModule` and app-level providers (Retrofit, OkHttp, Json, repos).
each main data groups (`Users`, `etc`) has its own package in the `app` package and its own dagger module.
The module is under the `AppScope` is added to the `AppComponent`

Each feature has a `@Subcomponent` scoped to that screen:
```
AppComponent
  ├── MainScreenComponent  (@MainScreenScope)
  └── DetailScreenComponent (@DetailScreenScope)
```

Feature subcomponents are created in their Activity via a top-level extension function (e.g., `injectMainScreen()` in `DaggerMainScreen.kt`).

### Feature structure

Every feature follows this pattern:

| Class | Role                                                                                                 |
|---|------------------------------------------------------------------------------------------------------|
| `*Activity` | Android entry point; receives injected `View` + `Coordinator`, calls `setContent { view.*Screen() }` |
| `*ScreenView` | Holds all `@Composable` functions, state and view `EventRelay` for the screen                        |
| `*ScreenData` | State holder (MutableState / StateFlow)                                                              |
| `*ScreenCoordinator` | `DefaultLifecycleObserver`; drives business logic using `scope`, `view`, `data`                      |
| `di/*ScreenModule` | Dagger module providing the above four; takes the `Activity` as constructor arg                      |
| `di/Dagger*Screen.kt` | Defines `@Subcomponent` + `@Scope` + the `Activity.inject*()` extension                              |

### Network / data layer

`NetworkModule` provides `OkHttpClient`, `Retrofit`, `Json`, and each `*ApiService`.

API models (e.g., `ApiUser`) are `@Serializable` data classes that map to domain models (e.g., `User`) via a `toUser()` method. Repos (`UserRepo`) own this mapping and expose only domain types.

`Retrofit` has no base URL set yet — add it in `NetworkModule.retrofit()` when wiring a real backend.