"""A stand-in for the Enigma that calls no model at all.

    ESCAPE_FAKE_MODEL=1 python manage.py runserver 0.0.0.0:8000

Why this exists: Exercises 3 to 6 are all on the phone, and none of them should be
blocked on having an API key. With this switch the whole client path works end to
end - camera, downscale, Base64, POST, reply, LiveData, and the door actually opens.

Read this before you use it, because it CHEATS in a way the real system refuses to:

    it decides which key you pressed by reading the shape name out of your TEXT.

The real `scan_shape` reads the photo and never trusts a word the player says. This
stub does the opposite, because it has no eyes. That is the difference between a
development stand-in and a security boundary, and it is worth sitting with for a
moment: the stub is convenient precisely because it trusts the client.

Turn it off before you demo. `figures/shapes/` plus a real key is the honest path.
"""

from __future__ import annotations

from .game import CODE_LENGTH, SHAPES, get, press, record_turn

_BANTER = [
    "Speak, mortal. The door is patient; I am not.",
    "Four keys. You have found none of them by talking.",
    "Numbers spoken aloud are just noise. Show me a shape.",
]


def _shape_in(text: str) -> str | None:
    lowered = text.lower()
    return next((name for name in SHAPES if name in lowered), None)


def take_turn(session_id: str, player_text: str,
              image_base64: str | None = None) -> dict:
    """Same signature and same return shape as enigma_agent.take_turn."""
    session = get(session_id)
    if session is None:
        return {"error": "no such session"}

    session["pending_image"] = image_base64
    tools_called: list[str] = []

    if image_base64:
        shape = _shape_in(player_text)
        tools_called.append("scan_shape")
        if shape is None:
            reply = ("[FAKE MODEL] I see something, but I cannot name it. Say which "
                     "shape you are holding - this stub reads your words, not your photo.")
        else:
            result = press(session, SHAPES[shape])
            entered = "".join(str(d) for d in session["entered"])
            if result["opened"]:
                reply = f"[FAKE MODEL] {SHAPES[shape]}. The lock clicks open. You are out."
            elif result["reset"]:
                reply = (f"[FAKE MODEL] {SHAPES[shape]}. Four keys, and the door does "
                         f"not move. The lock forgets. Begin again.")
            else:
                remaining = CODE_LENGTH - len(session["entered"])
                reply = (f"[FAKE MODEL] {SHAPES[shape]} - key {SHAPES[shape]} pressed. "
                         f"So far: {entered}. {remaining} to go.")
    else:
        tools_called.append("get_room_state")
        free = session["free_digit"]
        riddles = " / ".join(f"{r['id']}: {r['text']}" for r in session["riddles"])
        reply = (f"[FAKE MODEL] {_BANTER[session['turns'] % len(_BANTER)]} "
                 f"The first key is {free}. Then: {riddles}")

    record_turn(session, player_text, reply, bool(image_base64), tools_called)
    return {"reply": reply, "tools_called": tools_called, "had_image": bool(image_base64)}
