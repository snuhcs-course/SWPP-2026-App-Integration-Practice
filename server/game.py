"""Game state - the Django layer's job. Stands in for the ORM.

The most important line in the project: the passcode lives HERE and nowhere else.
Not in the system prompt, not in a tool description, not on the phone.

The second most important thing is not a line at all, it is an absence. There is no
function here that takes a code and opens the door. The ONLY way `escaped` becomes
True is `press()`, and the only caller of `press()` is the shape scanner. The camera
is the keypad.

In the real skeleton these become Django models:

    class EscapeSession(models.Model):
        passcode      = models.CharField(max_length=4)
        entered       = models.JSONField(default=list)     # digits shown so far
        pending_image = models.TextField(blank=True)       # base64, one turn only
        escaped       = models.BooleanField(default=False)

    class Turn(models.Model):
        session      = models.ForeignKey(EscapeSession, ...)
        player_text  = models.TextField()
        enigma_text  = models.TextField()
        had_image    = models.BooleanField(default=False)
        tools_called = models.JSONField(default=list)
"""

from __future__ import annotations

import random
import uuid

MAX_TURNS = 25
CODE_LENGTH = 4

# The keypad. Six keys, and you press one by holding it up to the camera.
# This table is NOT a secret - it is printed on the handout. The secret is which
# four keys, in which order.
SHAPES: dict[str, int] = {
    "triangle": 3,
    "square": 4,
    "pentagon": 5,
    "hexagon": 6,
    "heptagon": 7,
    "octagon": 8,
}
DIGITS = sorted(SHAPES.values())                 # 3..8 - the only digits that exist
DIGIT_TO_SHAPE: dict[int, str] = {v: k for k, v in SHAPES.items()}

# One riddle per digit. Three are drawn per session, so no two rooms are the same.
# A hint may narrow the answer; it may never state it.
RIDDLE_POOL = [
    {"digit": 3, "text": "I am how many primary colours a painter mixes from.",
     "hint": "Red, yellow, and one more. Not the ones on a screen."},
    {"digit": 4, "text": "I am how many strings a standard guitar has, less two.",
     "hint": "Six strings. Take two away."},
    {"digit": 5, "text": "I am what you count on one hand.",
     "hint": "Do not forget the thumb."},
    {"digit": 6, "text": "I am the number of walls in one cell of a honeycomb.",
     "hint": "Bees are efficient. Count the walls, not the bees."},
    {"digit": 7, "text": "I am how many colours a rainbow is traditionally said to hold.",
     "hint": "Newton named them. Remember the name Roy G. Biv."},
    {"digit": 8, "text": "I am how many legs carry a spider.",
     "hint": "It is not an insect. Insects have fewer."},
]

_SESSIONS: dict[str, dict] = {}


def new_session() -> dict:
    """Create a session and generate a passcode the model will never see.

    digit 1  free - the Enigma gives it away if asked; the game has to be winnable
    digits 2-4  three riddles, drawn from the pool

    Every digit is between 3 and 8, because every digit has to be pressable: the
    only way to enter one is to hold up a polygon with that many sides.
    """
    riddles = random.sample(RIDDLE_POOL, 3)
    free_digit = random.choice(DIGITS)
    passcode = str(free_digit) + "".join(str(r["digit"]) for r in riddles)

    session = {
        "session_id": str(uuid.uuid4()),
        "passcode": passcode,                 # server-only, forever
        "free_digit": free_digit,             # the Enigma may say this one
        "riddles": [{"id": i, **r} for i, r in enumerate(riddles, start=1)],
        "entered": [],                        # digits pressed so far, via the camera
        "attempts": 0,                        # completed 4-digit entries
        "pending_image": None,                # set by the view, cleared after one turn
        "turns": 0,
        "hints_given": 0,
        "escaped": False,
        "log": [],
    }
    _SESSIONS[session["session_id"]] = session
    return session


def get(session_id: str) -> dict | None:
    return _SESSIONS.get(session_id)


def press(session: dict, digit: int) -> dict:
    """Press one key. The ONLY path to escaped=True in the whole codebase.

    Note the signature: it takes a digit, never a code. Nothing can submit four
    digits at once, so nothing can be talked into submitting the right four.
    """
    session["entered"].append(digit)
    if len(session["entered"]) < CODE_LENGTH:
        return {"opened": False, "reset": False}

    session["attempts"] += 1
    if "".join(str(d) for d in session["entered"]) == session["passcode"]:
        session["escaped"] = True
        return {"opened": True, "reset": False}

    session["entered"] = []                   # a real keypad forgets a wrong entry
    return {"opened": False, "reset": True}


def clear_entry(session: dict) -> None:
    """Wipe what has been pressed. The vision model will miscount eventually."""
    session["entered"] = []


def public_state(session: dict) -> dict:
    """A whitelist, not a blacklist. Note what is missing.

    `entered` is safe to send: the player pressed those keys themselves, in front of
    the camera. `passcode` is not, and serialising the session object would ship it
    to the phone, where anyone with a proxy can read it.
    """
    return {
        "session_id": session["session_id"],
        "turns": session["turns"],
        "turns_left": MAX_TURNS - session["turns"],
        "hints_given": session["hints_given"],
        "entered": list(session["entered"]),
        "digits_remaining": CODE_LENGTH - len(session["entered"]),
        "attempts": session["attempts"],
        "escaped": session["escaped"],
    }


def record_turn(session: dict, player_text: str, enigma_text: str,
                had_image: bool, tools_called: list[str]) -> None:
    session["turns"] += 1
    session["pending_image"] = None          # an image is good for exactly one turn
    session["log"].append({
        "player": player_text,
        "enigma": enigma_text,
        "had_image": had_image,
        "tools": tools_called,
    })
