# MVVM Architecture in SnapDo

This document explains how the SnapDo Android application utilizes the Model-View-ViewModel (MVVM) architectural pattern. The app is composed of three main screens: `MainActivity`, `AddTaskActivity`, and `CameraActivity`.

## Core Principles of MVVM

The MVVM pattern separates the application's logic into three interconnected components:

-   **Model**: Represents the data and business logic of the application. It's responsible for fetching and managing data from sources like a remote server or a local database.
-   **View**: The UI layer of the application (Activities, Fragments). It observes the ViewModel for data changes and displays them to the user. It also notifies the ViewModel about user interactions.
-   **ViewModel**: Acts as a bridge between the Model and the View. It holds the UI-related data, is responsible for the presentation logic, and is independent of the View's lifecycle.

---

## 1. Main Screen (`MainActivity`)

This is the main screen of the app, which displays a list of "To-Do" items.

-   **View**: `MainActivity.kt` and its layout `activity_main.xml`.
    -   It observes the `todos` LiveData from the `MainViewModel`.
    -   When the LiveData is updated, the `TodoAdapter` is notified to refresh the `RecyclerView`.
    -   User interactions (like clicking the FAB to add a task or clicking a list item) are forwarded to either launch a new Activity or are handled by the ViewModel.

-   **ViewModel**: `MainViewModel.kt`
    -   It holds the list of todos in a `MutableLiveData<List<Todo>>` to make it observable by the View.
    -   It communicates with the `TodoRepository` (Model) to fetch the list of todos.
    -   It exposes methods like `refreshTodos()` and `updateVerdict()` that the View can call in response to user actions or other events.
    -   The ViewModel is lifecycle-aware and survives configuration changes (like screen rotations), preventing data from being re-fetched unnecessarily.

-   **Model**:
    -   `Todo.kt`: A data class that defines the structure of a To-Do item.
    -   `TodoRepository.kt`: Manages the data operations. It abstracts the data source from the rest of the app. In this case, it fetches data from the remote server using `Retrofit`.
    -   `ApiService.kt` & `RetrofitInstance.kt`: These handle the actual network communication with the backend API.

**Flow:**
1.  `MainActivity` is created and gets an instance of `MainViewModel`.
2.  `MainViewModel`'s `init` block calls `fetchTodos()`.
3.  `fetchTodos()` uses a coroutine to ask `TodoRepository` for the list of todos.
4.  `TodoRepository` makes a network call via `RetrofitInstance.api`.
5.  Once the data is retrieved, `MainViewModel` updates the `_todos` LiveData.
6.  `MainActivity`, which is observing `todos`, receives the new list and updates the `TodoAdapter` to display it.

---

## 2. Add Task Screen (`AddTaskActivity`)

This screen allows the user to create a new To-Do item.

-   **View**: `AddTaskActivity.kt` and `activity_add_task.xml`.
    -   It collects user input from `EditText` fields.
    -   On button clicks (`btnSubmit`, `btnGenerateEvidence`), it calls methods in the `TodoRepository` directly.
    -   **Note**: This Activity currently bypasses a ViewModel for some of its logic (like adding a task), interacting directly with the `TodoRepository`. While it has a `AddTaskViewModel` for generating evidence, the task creation logic could also be moved into the ViewModel to better adhere to the MVVM pattern.

-   **ViewModel**: `AddTaskViewModel.kt`
    -   It contains the logic for generating an "evidence" string based on the title and description.
    -   It exposes the generated evidence via LiveData (`_evidence`).
    -   The `AddTaskActivity` can observe this LiveData to update the UI.

-   **Model**: `TodoRepository.kt` is used to make the network calls for adding a new task and generating evidence.

**Flow (for adding a task):**
1.  The user fills in the details in `AddTaskActivity`.
2.  The user clicks the "Submit" button.
3.  The `onClickListener` for the button directly calls `repository.addTodo()`.
4.  After the task is successfully added, it sets the result to `RESULT_OK` and finishes, notifying `MainActivity` to refresh its list.

---

## 3. Camera Screen (`CameraActivity`)

This screen is used for capturing an image as evidence for a To-Do item.

-   **View**: `CameraActivity.kt` and `activity_camera.xml`.
    -   This Activity manages the camera preview, captures an image, and handles the UI state changes (e.g., showing the captured image, retake/submit buttons).
    -   It directly uses `RetrofitInstance.api` to send the captured image for verification.

-   **ViewModel**: This screen does **not** have a dedicated ViewModel.
    -   All the logic, including the network call, is handled directly within the `CameraActivity`.
    -   For a stricter MVVM approach, a `CameraViewModel` could be created to handle the image upload and verification logic, separating it from the Activity's responsibility of managing the camera and UI.

-   **Model**: The network layer (`RetrofitInstance` and `ApiService`) is used directly by the Activity to perform the verification request.

**Flow:**
1.  `CameraActivity` is launched from `MainActivity` with a `task_id`.
2.  The user captures a photo.
3.  The user clicks "Submit".
4.  The Activity encodes the image to Base64 and, using a coroutine, calls `RetrofitInstance.api.verifyTask()`.
5.  Upon receiving the response, it sends a broadcast intent (`VERIFY_RESULT`) with the verdict.
6.  `MainActivity` listens for this broadcast, and its `BroadcastReceiver` updates the corresponding task's verdict via the `MainViewModel`.

This approach of using a broadcast receiver decouples the `CameraActivity` from `MainActivity`, but the core logic remains inside the View component.
