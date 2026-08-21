"""Does the agent you built in Exercises 1 and 2 actually run?

The other tests never construct an agent. These do - against a FAKE model, so they
need no API key, cost nothing, and cannot flake.

    pytest tests/test_agent_wiring.py -q

What a fake model proves:
  - create_agent accepted your arguments and produced a runnable graph
  - the tools were bound and are callable by name
  - a tool call round-trips: model -> tool -> model -> final answer
  - take_turn parks the photo, extracts the trajectory, and records the turn

What it cannot prove: that a real model chooses to call the right tool. That needs
a key. See README, "Running it for real".
"""

from __future__ import annotations

import pathlib
import sys

import pytest
from langchain_core.language_models.fake_chat_models import GenericFakeChatModel
from langchain_core.messages import AIMessage

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))

from server import enigma_agent, game, tools, vision            # noqa: E402


class ScriptedModel(GenericFakeChatModel):
    """A model that says exactly what we tell it to, in order."""

    def bind_tools(self, tools, **kwargs):        # noqa: A002 - matches the base API
        return self                              # the script already knows what to call


def _script(*messages):
    return ScriptedModel(messages=iter(messages))


def _call(name, args, id_):
    return AIMessage(content="", tool_calls=[{"name": name, "args": args, "id": id_}])


@pytest.fixture
def wired(monkeypatch):
    """Build the real agent, but with a scripted model instead of ChatOpenAI."""
    def install(*messages):
        monkeypatch.setattr(enigma_agent, "ChatOpenAI",
                            lambda **_kw: _script(*messages))
    return install


@pytest.fixture
def session():
    return game.new_session()


def test_build_agent_returns_something_runnable(wired):
    wired(AIMessage(content="Speak, mortal."))
    agent = enigma_agent.build_agent()
    assert hasattr(agent, "invoke"), "build_agent() must return a runnable agent"

    out = agent.invoke({"messages": [("user", "hello")]})
    assert out["messages"][-1].content == "Speak, mortal."


def test_a_turn_with_no_photo_records_the_trajectory(wired, session):
    sid = session["session_id"]
    wired(_call("get_room_state", {"session_id": sid}, "c1"),
          AIMessage(content="Three riddles stand between you and daylight."))

    out = enigma_agent.take_turn(sid, "where do i start?")

    assert out["tools_called"] == ["get_room_state"]
    assert out["had_image"] is False
    assert session["turns"] == 1
    assert session["log"][-1]["tools"] == ["get_room_state"]


def test_a_photo_reaches_the_tool_but_never_the_prompt(wired, monkeypatch, session):
    """The seam-5 claim, checked end to end instead of asserted in a comment."""
    sid = session["session_id"]
    monkeypatch.setattr(tools, "describe",
                        lambda _img: vision.ShapeReading(shape="pentagon",
                                                         sides_counted=5))
    wired(_call("scan_shape", {"session_id": sid}, "c1"),
          AIMessage(content="The lock clicks. Five."))

    out = enigma_agent.take_turn(sid, "i am holding it up", image_base64="PHOTOBYTES")

    assert out["tools_called"] == ["scan_shape"]
    assert session["entered"] == [5]               # the tool really ran
    assert "PHOTOBYTES" not in str(session["log"][-1])   # and the bytes went nowhere
    assert session["pending_image"] is None        # cleared: good for one turn only


def test_four_scans_open_the_door(wired, monkeypatch, session):
    sid = session["session_id"]
    for digit in session["passcode"]:
        monkeypatch.setattr(tools, "describe",
                            lambda _img, d=int(digit): vision.ShapeReading(
                                shape=game.DIGIT_TO_SHAPE[d], sides_counted=d))
        wired(_call("scan_shape", {"session_id": sid}, "c1"),
              AIMessage(content="Click."))
        enigma_agent.take_turn(sid, "here", image_base64="PHOTOBYTES")

    assert session["escaped"] is True


def test_an_unknown_session_fails_before_the_model_is_called(wired):
    wired(AIMessage(content="should never be reached"))
    assert enigma_agent.take_turn("nope", "hello") == {"error": "no such session"}
