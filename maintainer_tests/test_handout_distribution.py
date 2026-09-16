"""Regression tests for the student handout as a distributable artifact.

These tests validate the untouched handout. They intentionally live outside `tests/`
because students are expected to fill TODO-1 through TODO-6 after distribution.
"""

from __future__ import annotations

import ast
import pathlib


ROOT = pathlib.Path(__file__).resolve().parents[1]


def _text(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def test_agent_exercises_are_declared_but_have_no_reference_answer():
    source = _text("server/enigma_agent.py")
    tree = ast.parse(source)
    functions = {
        node.name: node
        for node in tree.body
        if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef))
    }

    assert "_reference_take_turn" not in functions
    for name, marker in (("build_agent", "TODO-2"), ("take_turn", "TODO-3")):
        function = functions[name]
        function_source = ast.get_source_segment(source, function) or ""
        assert marker in function_source
        executable_body = function.body[1:] if (
            function.body and isinstance(function.body[0], ast.Expr)
            and isinstance(function.body[0].value, ast.Constant)
            and isinstance(function.body[0].value.value, str)
        ) else function.body
        assert len(executable_body) == 1
        assert isinstance(executable_body[0], ast.Raise)


def test_todo_comments_state_contracts_without_copyable_answer_sequences():
    paths = [
        "server/enigma_agent.py",
        "client/app/src/main/java/com/example/escaperoom/ui/camera/CameraActivity.kt",
        "client/app/src/main/java/com/example/escaperoom/util/ImageUtils.kt",
        "client/app/src/main/java/com/example/escaperoom/ui/main/GameViewModel.kt",
    ]
    text = "\n".join(_text(path) for path in paths)
    answer_fragments = [
        "middleware=[ModelCallLimitMiddleware(thread_limit=6",
        # TODO-4, TODO-5 and TODO-6 are deliberate exceptions: the commented-out
        # answers ship in CameraActivity.kt, ImageUtils.kt and GameViewModel.kt as
        # guided walkthroughs instead of from-scratch exercises. See their TODO
        # comments for the tone. TODO-2/3 (enigma_agent.py) stay real exercises.
        "provider.unbindAll(), then bindToLifecycle",
    ]

    for fragment in answer_fragments:
        assert fragment not in text


def test_runnable_entry_points_use_the_student_take_turn():
    agent_source = _text("server/enigma_agent.py")
    player_source = _text("simulated_player.py")

    assert "r = take_turn(s[\"session_id\"], line, img)" in agent_source
    assert "from server.enigma_agent import take_turn" in player_source
    assert "_reference_take_turn" not in agent_source + player_source


def test_timeout_remains_an_unfinished_student_exercise():
    source = _text(
        "client/app/src/main/java/com/example/escaperoom/data/network/RetrofitInstance.kt"
    )

    assert "TODO-1" in source
    assert ".readTimeout(" not in source


def test_exercise_numbering_is_consistent_across_student_facing_text():
    readme = _text("README.md")
    seams = _text("SEAMS.md")
    strings = _text("client/app/src/main/res/values/strings.xml")
    views = _text("django_project/escaperoom/views.py")
    canonical_rows = [
        "| **1** | Android → Django | `BASE_URL` + read timeout",
        "| **2** | Django → Agent | `build_agent()`",
        "| **3** | Agent → Django | `take_turn()`",
    ]

    for row in canonical_rows:
        assert row in readme
        assert row in seams
    assert "`TODO-1` … `TODO-6`" in seams
    assert "That is Exercise 1." in strings
    assert "check BASE_URL (Exercise 1)" in strings
    assert "client exercises (1, 4-6)" in views


def test_exercise_guidance_matches_the_current_implementations():
    readme = _text("README.md")
    seams = _text("SEAMS.md")
    retrofit = _text(
        "client/app/src/main/java/com/example/escaperoom/data/network/RetrofitInstance.kt"
    )
    view_model = _text(
        "client/app/src/main/java/com/example/escaperoom/ui/main/GameViewModel.kt"
    )

    assert "None is longer than about ten lines." not in readme
    assert "kills a 5-second VLM turn" not in seams
    assert "a VLM turn takes 2–5s" not in seams
    assert "a turn takes 2 to 5 seconds" not in view_model
    assert "multiple model, tool, and vision round trips" in seams
    assert "multiple model, tool, and vision round trips" in retrofit
    assert "one or more model calls" in view_model
    assert "| 2:10 (20) | Play |" in readme
    assert "| 2:30 (20) | Red-team |" in readme
    assert "| 2:50 (10) | Wrap |" in readme
    assert "| 2:30 (20) | Play |" not in readme
    assert "| 2:50 (10) | Red-team |" not in readme


def test_tracked_handout_text_has_no_stale_source_project_claims():
    paths = [
        "README.md",
        "SEAMS.md",
        "client/MVVM.md",
        "client/app/src/main/java/com/example/escaperoom/data/repository/GameRepository.kt",
        "client/app/src/main/java/com/example/escaperoom/ui/camera/CameraActivity.kt",
        "client/app/src/main/java/com/example/escaperoom/ui/main/GameViewModel.kt",
        "client/app/src/main/java/com/example/escaperoom/ui/main/MainActivity.kt",
        "client/app/src/main/java/com/example/escaperoom/ui/main/TranscriptAdapter.kt",
        "client/app/src/main/res/values/colors.xml",
        "client/app/src/main/res/values/themes.xml",
    ]
    text = "\n".join(_text(path) for path in paths)

    assert "SnapDo" not in text
    assert "DESIGN.md" not in text
    assert "gallery" not in text.lower()


def test_python_cache_artifacts_are_absent_and_ignored():
    ignore = _text(".gitignore")
    cache_artifacts = [
        path
        for root in (ROOT / "server", ROOT / "tests")
        for path in root.rglob("*")
        if path.is_file() and ("__pycache__" in path.parts or path.suffix == ".pyc")
    ]

    assert cache_artifacts == []
    assert "__pycache__/" in ignore
    assert "*.py[cod]" in ignore
