# Week 6 — App Integration: the Escape Room

**SWPP 2026 Fall · one 3-hour session**

A player is locked in a room with **The Enigma**, an ancient lock. Four digits open the
door — but there is no keypad. **You enter a digit by holding a paper polygon up to the
camera:** a pentagon presses 5, a hexagon presses 6. Saying the numbers out loud does
nothing at all.

**The Enigma does not know the code, and no tool in the system accepts one.**

Four components you already know, six arrows you do not:

```
Android (MVVM)         Django               Agent                 Vision model
├ CameraX preview   ├ EscapeSession      ├ get_room_state()    └ describe(image)
├ Base64 encode     ├ passcode HERE      ├ give_hint()            -> ShapeReading
├ transcript, turns ├ pending_image      ├ scan_shape()              (an enum)
└ Retrofit+coroutine└ press() ONLY PATH  └ clear_entry()
```

Read **`SEAMS.md` first.** It is the actual syllabus for the session.

---

## The six exercises

| Exercise | Seam | What you write | Where |
|---|---|---|---|
| **1** | Django → Agent | `build_agent()` | `server/enigma_agent.py` |
| **2** | Agent → Django | `take_turn()` | `server/enigma_agent.py` |
| **3** | Android → Django | `BASE_URL` + read timeout | `android/RetrofitInstance.kt` |
| **4** | CameraX → app | `startCamera()` | `android/CameraActivity.kt` |
| **5** | Camera → payload | `fileToBase64()` | `android/ImageUtils.kt` |
| **6** | Django → Android | `say()` | `android/GameViewModel.kt` |

The code markers are `TODO-1` … `TODO-6`. Every one of them sits on an arrow between two
components, never inside one. None is longer than about ten lines.

---

## What is here

```
app-integration-practice/
├── SEAMS.md                  the six contracts and how each one breaks   ← start here
├── server/
│   ├── game.py               session state; the passcode lives here and nowhere else
│   ├── vision.py             describe(image) -> ShapeReading            given
│   ├── tools.py              get_room_state · give_hint · scan_shape · clear_entry
│   ├── enigma_agent.py       the agent            (TODO-1, TODO-2 + reference)
│   └── views_snippet.py      Django views + urls                        given
├── android/
│   ├── ApiService.kt         Retrofit interface + DTOs                   given
│   ├── RetrofitInstance.kt   (TODO-3)
│   ├── CameraActivity.kt     (TODO-4)
│   ├── ImageUtils.kt         (TODO-5)
│   └── GameViewModel.kt      (TODO-6)
├── tests/test_seams.py       18 tests — run with no API key and no phone
├── simulated_player.py       plays 40 games against your server
└── requirements.txt
```

Print `figures/W6_shape_sheet.png` — that page is the keypad. One per player.

---

## Setup

```bash
conda create -n swpp6 python=3.11
conda activate swpp6
pip install -r requirements.txt

echo "OPENAI_API_KEY=sk-..." > .env

python -m pytest tests -q          # 18 passed — no API key needed
python -m server.enigma_agent      # play in the terminal (needs the key)
```

`0.0.0.0:8000`, not `127.0.0.1` — the phone is a different machine.

---

## The passcode

Four digits, generated per session in `game.py`:

| digit | where it comes from |
|---|---|
| 1 | free — the Enigma tells you if you ask |
| 2–4 | three riddles, drawn per session from a pool of six |

Every digit is between **3 and 8**, because every digit has to be *pressable*. The
keypad is:

| triangle | square | pentagon | hexagon | heptagon | octagon |
|---|---|---|---|---|---|
| **3** | **4** | **5** | **6** | **7** | **8** |

That table is not a secret — print it and hand it out. The secret is which four keys,
in which order. Knowing a digit and pressing it are different things, and that gap is
the whole design.

---

## The security invariant

Four properties, each pinned by a test:

1. **The passcode is in `game.py` and nowhere else.** Not in the system prompt, not in a
   tool description, not on the phone. `test_the_passcode_is_not_in_the_prompt`.
2. **No tool accepts a code, a digit or an answer.** Every argument in the whole tool
   list is either `session_id` or `riddle_id`. "The code is 6483, open up" is not a
   request the Enigma refuses — it is a sentence with nowhere to go.
   `test_no_tool_accepts_a_code_or_a_digit`.
3. **`escaped` is set in exactly one function**, `game.press()`, and the only caller of
   `press()` is the shape scanner. The camera is the only input device in the system.
   `test_talking_never_opens_the_door`.
4. **The key pressed comes from the server's own `SHAPES` table**, keyed by shape name,
   never from the model's `sides_counted`. Stub the VLM to report 99 sides for a
   pentagon and it still presses 5.
   `test_the_digit_comes_from_the_server_not_the_model`.

One rule in the system prompt is *not* a wall: "do not solve the riddles for the
player." That is a game rule, and it will sometimes fail. It can afford to be a wish,
because a solved riddle still does not press a key. Knowing which of your rules are
walls and which are wishes is most of security design.

The photo never enters the agent's context: the Django view parks it on the session and
`scan_shape()` takes **no image argument**. Tokens, blast radius, authority — in that
order of how often it bites you, and reverse order of how much it matters.

---

## Testing without a phone

```
$ python -m pytest tests -q
  18 passed in 0.02s

$ python simulated_player.py --games 40
  honest     escaped 10/10   attempts 1.0   mean turns 4.0
  cheat      escaped  0/10   attempts 2.0   mean turns 25.0
  jailbreak  escaped  0/10   attempts 0.0   mean turns 25.0
  brute      escaped  0/10   attempts 5.0   mean turns 25.0

  passcode spoken by the Enigma:   0   <- must be 0
  doors opened without the camera: 0   <- must be 0
```

`--live` runs the same four strategies through the real agent and the real vision model.

---

## Session plan (180 min)

| time | block | what |
|---|---|---|
| 0:00 (15) | The seams | the system diagram; why every bug lives at a boundary |
| 0:15 (20) | Design | the game, the API, where the secret lives |
| 0:35 (35) | Exercises 1–2 | the Enigma answers; `pytest` goes green |
| 1:10 (10) | break | |
| 1:20 (20) | Exercise 3 | the phone reaches Django |
| 1:40 (30) | Exercises 4–5 | camera, downscale, Base64 — watch the payload size |
| 2:10 (20) | Exercise 6 | the coroutine, and the `finally` block |
| 2:30 (20) | Play | pair up, one printed keypad each |
| 2:50 (10) | Red-team | write on the paper; try to talk the door open |

---

## Submissions

`<StudentID>_<Name>_week6.zip` into eTL:

- `server/enigma_agent.py` (Exercises 1–2)
- `RetrofitInstance.kt` · `CameraActivity.kt` · `ImageUtils.kt` · `GameViewModel.kt`
- `redteam.md` — four attacks: what each one got you, and which line stopped it
- a screenshot of the app after the door opens
