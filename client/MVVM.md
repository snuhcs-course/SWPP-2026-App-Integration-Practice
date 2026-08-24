# EscapeRoom Android MVVM

The Android client keeps UI work, turn orchestration, and HTTP access in separate
layers. The camera is a second Activity, but it does not call the server itself.

## Model and data layer

- `SessionState.kt` mirrors the safe fields returned by `server/game.py::public_state`.
  It intentionally has no passcode or riddle-answer field.
- `ApiService.kt` defines the two HTTP operations used by the app: create a session and
  send a turn with an optional `image_base64` value.
- `RetrofitInstance.kt` configures the server address and HTTP client.
- `GameRepository.kt` is the only class that calls the Retrofit service. It exposes
  `createSession()` and `say()` as suspend functions.

## ViewModel

`GameViewModel.kt` owns the screen state and the coroutine boundary. It exposes
read-only `LiveData` for:

- the current `SessionState`;
- transcript lines;
- whether the Enigma is thinking; and
- a persistent connection error message.

The ViewModel creates the session on startup. Exercise 6 completes `say()`: it adds the
player's line, calls the repository, adds the reply, publishes the new session state,
and clears the thinking flag even when the request fails.

## Views

### MainActivity

`MainActivity.kt` observes the ViewModel and renders the transcript, lock status,
loading indicator, and errors. Text input and encoded camera results are both forwarded
to `GameViewModel.say()`. The Activity never calls Retrofit.

### CameraActivity

`CameraActivity.kt` owns CameraX preview and capture because those APIs are tied to an
Activity lifecycle. After the player accepts a photo, it uses `ImageUtils.fileToBase64`
and returns the encoded string with `setResult()`. It does not upload the photo.

## Turn flow

1. `MainActivity` forwards text, optionally with the camera result, to `GameViewModel`.
2. `GameViewModel` launches a lifecycle-aware coroutine and calls `GameRepository`.
3. `GameRepository` sends a `SayRequest` through `ApiService`.
4. Django returns a reply plus a whitelisted `SessionState`.
5. `GameViewModel` updates its observable state.
6. `MainActivity` and `TranscriptAdapter` redraw from those observations.

This boundary keeps camera lifecycle code in the camera screen, asynchronous UI state
in the ViewModel, and transport details in the repository/network layer.
