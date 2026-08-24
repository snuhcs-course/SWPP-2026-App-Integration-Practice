"""A stand-in for the Enigma that calls no model at all.

    ESCAPE_FAKE_MODEL=1 python manage.py runserver 0.0.0.0:8000

Why this exists: Exercises 1, 4, 5 and 6 are all on the phone, and none of them
should be blocked on having an API key. With this switch the whole client path works end to
end - camera, downscale, Base64, POST, reply, LiveData, and the door actually opens.

Read this before you use it, because it CHEATS in a way the real system refuses to.
It cannot inspect the photo, so every request with an attached image presses the next
correct key by reading the server-only passcode and current entry position. The image
is only treated as a boolean signal that the camera path ran.

This is a development stand-in, not shape recognition and not security verification.
It trusts the request and privileged server state so students can exercise CameraX,
Base64, HTTP and MVVM without an API key. The real `scan_shape` derives a key from the
photo and never reads the passcode.

Turn it off before you demo. `figures/shapes/` plus a real key is the honest path.
"""

from __future__ import annotations

from .game import CODE_LENGTH, get, press, record_turn

_BANTER = [
    "Speak, mortal. The door is patient; I am not.",
    "Four keys. You have found none of them by talking.",
    "Numbers spoken aloud are just noise. Show me a shape.",
]


def take_turn(session_id: str, player_text: str,
              image_base64: str | None = None) -> dict:
    """Same signature and same return shape as enigma_agent.take_turn."""
    session = get(session_id)
    if session is None:
        return {"error": "no such session"}

    session["pending_image"] = image_base64
    tools_called: list[str] = []

    if image_base64:
        tools_called.append("scan_shape")
        position = len(session["entered"])
        digit = int(session["passcode"][position])
        result = press(session, digit)
        entered = "".join(str(d) for d in session["entered"])
        if result["opened"]:
            reply = f"[FAKE MODEL] Key {digit}. The lock clicks open. You are out."
        elif result["reset"]:
            reply = (f"[FAKE MODEL] Key {digit}. Four keys, and the door does not "
                     "move. The lock forgets. Begin again.")
        else:
            remaining = CODE_LENGTH - len(session["entered"])
            reply = (f"[FAKE MODEL] Key {digit} pressed. So far: {entered}. "
                     f"{remaining} to go.")
    else:
        tools_called.append("get_room_state")
        free = session["free_digit"]
        riddles = " / ".join(f"{r['id']}: {r['text']}" for r in session["riddles"])
        reply = (f"[FAKE MODEL] {_BANTER[session['turns'] % len(_BANTER)]} "
                 f"The first key is {free}. Then: {riddles}")

    record_turn(session, player_text, reply, bool(image_base64), tools_called)
    return {"reply": reply, "tools_called": tools_called, "had_image": bool(image_base64)}
