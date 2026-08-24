"""Regression tests for the API-key-free Android development stub."""

from __future__ import annotations

from server import game, offline


CAMERA_TEXT = "I am holding it up to the camera."


def test_four_ordinary_camera_submissions_open_the_door():
    session = game.new_session()

    results = [
        offline.take_turn(session["session_id"], CAMERA_TEXT, image_base64="opaque-photo")
        for _ in range(game.CODE_LENGTH)
    ]

    assert session["escaped"] is True
    assert session["turns"] == game.CODE_LENGTH
    assert all(result["had_image"] for result in results)
    assert all(result["tools_called"] == ["scan_shape"] for result in results)


def test_shape_words_without_a_photo_cannot_press_a_key():
    session = game.new_session()

    offline.take_turn(
        session["session_id"],
        "pentagon hexagon heptagon octagon",
        image_base64=None,
    )

    assert session["entered"] == []
    assert session["escaped"] is False

