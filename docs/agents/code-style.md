# Code style

## Build and test

No hook or CI gate. Run these before handoff and paste the result in the PR:

```sh
./gradlew :app:compileDebugKotlin
./gradlew :app:lintDebug
./gradlew :app:testDebugUnitTest
```

- JDK 21. Compile/target SDK 37, min SDK 30.
- Single test: `./gradlew :app:testDebugUnitTest --tests "com.lostf1sh.pixelplayeross.SomeTest.someMethod"`.
- Universal debug APK: `./gradlew :app:assembleDebug -Ppixelplayer.enableAbiSplits=false`.

## Layout

- One app module, package `com.lostf1sh.pixelplayeross`: `data/` (Room, repositories, music sources, Media3 service), `presentation/` (screens, ViewModels), `ui/` (theme, shared Compose), `di/` (Hilt), `utils/`.
- Music sources: local MediaStore, Navidrome/Subsonic (`data/navidrome`), Jellyfin (`data/jellyfin`). A feature that touches songs works for all three, or says why not.

## Shape

- Names come from `GLOSSARY.md`. Use the term, never a word it lists under _Avoid_.
- A variable of a `-Model` type drops the suffix: `val song: SongModel`, not `songModel`. `ViewModel` is not a `-Model` type here.
- Deep modules: a lot of behaviour behind a small interface. Prefer small pure functions and plain data between modules.
- No inline string literals in logic: preference keys, URLs, API paths, intent actions and SQL are named constants.

## Compose and performance

- Slice `PlayerUiState`, never collect the whole thing. It updates on every position tick. Map to a small slice + `distinctUntilChanged()`. Reference: `PlayerUiSheetSliceV2` in `UnifiedPlayerSheetV2`.
- `ImmutableList<T>` over `List<T>` in `@Composable` params. `.toImmutableList()` is `O(n)`: never on every recomposition. New widely used domain models go in `app/compose_stability.conf`.
- Navigate with `navController.navigateSafely(...)`, never `navController.navigate(...)`.
- `@Singleton` is already the dominant scope. Prefer `@ViewModelScoped` for single-consumer state.

## Data

- Room schema changes need a migration; schemas export to `app/schemas`. Never edit a shipped migration. Destructive migration only in debug.
- Log with Timber, never `android.util.Log`.

## Copy

- User-facing text lives in `res/values/strings*.xml`, read with `stringResource(...)`. No hardcoded copy in Composables.
- UI text: short labels, no helper sentences, and passes `/deslop-text`.

## Comments

Applies to new and changed code. Existing `//` comments stay until their code changes.

- KDoc form only: `/** … */`. `//` survives only for suppressions and pragmas.
- One line. Two at most, when a second earns its place.
- Only what the code cannot say: an outside fact (a Subsonic API quirk), a magic value, a rejected alternative, a failure that happened, a constraint no type enforces.
- Test each one: name the edit its absence would let through, one a reviewer would reject and the tests would not catch. If you cannot, delete it.
- No `@param`, `@return` or `@sample`. No section banners. Test files carry no comments.
- A pointer to an issue or ADR is never derivable. Keep the last one in the code it governs.

## Tests

- JUnit Jupiter (`org.junit.jupiter.api.Test`), not JUnit 4.
- Spec logic, not wiring. Logic is code a plausible edit can break without a type error: parsing, sorting, queue math, migrations, sync. Hilt modules and delegating ViewModels get no test.
- Assert on outside behaviour, never on internal calls.
- Make a test double do the hardest thing the real one does. A double shaped to fit the code hides the bug.
- Fixtures use made-up songs, artists and servers.

## Dependencies

A new runtime dependency needs a reason in the PR: what it does, why the platform and current code cannot, and what it costs to carry. FOSS only; F-Droid must still build the app.
