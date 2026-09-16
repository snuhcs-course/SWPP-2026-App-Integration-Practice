# Week 3 — App Integration: the Escape Room

**SWPP 2026 Fall**

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
| **1** | Android → Django | `BASE_URL` + read timeout | `data/network/RetrofitInstance.kt` |
| **2** | Django → Agent | `build_agent()` | `server/enigma_agent.py` |
| **3** | Agent → Django | `take_turn()` | `server/enigma_agent.py` |
| **4** | CameraX → app | `startCamera()` | `ui/camera/CameraActivity.kt` |
| **5** | Camera → payload | `fileToBase64()` | `util/ImageUtils.kt` |
| **6** | Django → Android | `say()` | `ui/main/GameViewModel.kt` |

Exercise 1 comes first on purpose: it is one line, and it is the line that most often
eats an afternoon. Do it while Android Studio is still syncing, run the server with
`ESCAPE_FAKE_MODEL=1`, and you have a live pipe to debug everything else against.

The code markers are `TODO-1` … `TODO-6`. Every one of them sits on an arrow between two
components, never inside one.

---

## What is here

```
app-integration-practice/
├── SEAMS.md                  the six contracts and how each one breaks   ← start here
├── server/
│   ├── game.py               session state; the passcode lives here and nowhere else
│   ├── vision.py             describe(image) -> ShapeReading            given
│   ├── tools.py              get_room_state · give_hint · scan_shape · clear_entry
│   ├── enigma_agent.py       the agent                     (TODO-2, TODO-3)
│   └── offline.py            no-model stand-in for ESCAPE_FAKE_MODEL=1   given
├── django_project/           a real, runnable Django project            given
│   ├── manage.py             puts the repo root on sys.path
│   ├── escaperoom_site/      settings · urls · wsgi
│   └── escaperoom/           views.py · urls.py  — imports from server/
├── client/                   the Android Studio project — open THIS folder
│   └── app/src/main/java/com/example/escaperoom/
│       ├── data/network/ApiService.kt        Retrofit interface + DTOs   given
│       ├── data/network/RetrofitInstance.kt  (TODO-1)
│       ├── data/repository/GameRepository.kt                             given
│       ├── ui/main/MainActivity.kt · TranscriptAdapter.kt                given
│       ├── ui/main/GameViewModel.kt          (TODO-6)
│       ├── ui/camera/CameraActivity.kt       (TODO-4)
│       └── util/ImageUtils.kt                (TODO-5)
├── tests/
│   ├── test_seams.py         18 tests — tools, game rules, the security invariant
│   ├── test_agent_wiring.py   5 tests — runs your agent against a FAKE model
│   └── test_offline.py        2 tests — API-key-free phone path
├── maintainer_tests/
│   └── test_handout_distribution.py  6 checks — handout leakage and hygiene
├── simulated_player.py       plays 40 games against your server
└── requirements.txt
```

Print `figures/W6_shape_sheet.png` — that page is the keypad. One per player.
`figures/shapes/` has the same six shapes as single PNGs, for testing without a camera.

---

## The Android project

Open **`client/`** in Android Studio and let it sync. That is the whole setup.

The project is already configured with AGP 8.12.0, Kotlin 2.0.21, Gradle 8.13,
compileSdk 36, CameraX 1.4.2, Retrofit 2.9.0, the OkHttp logging interceptor,
lifecycle/livedata-ktx, and RecyclerView. The manifest also sets
`usesCleartextTraffic="true"`, so there is no `network_security_config.xml` to add.

Edit the files in `client/` directly. There is no second copy to keep in sync.

---

## Setup

```bash
conda create -n swpp3 python=3.11
conda activate swpp3
pip install -r requirements.txt

cp .env.example .env        # then put your key in it — .env is gitignored

python -m pytest tests -q          # 25 tests — no API key needed
python -m server.enigma_agent      # play in the terminal (needs the key)

cd django_project
python manage.py runserver 0.0.0.0:8000                    # needs the key
ESCAPE_FAKE_MODEL=1 python manage.py runserver 0.0.0.0:8000   # does not
```

`0.0.0.0:8000`, not `127.0.0.1` — the phone is a different machine.

**The key lives on the server and nowhere else.** `.env` is read by `manage.py`, by
`python -m server.enigma_agent` and by `simulated_player.py --live`; a real environment
variable still overrides the file for one run. It never goes into the Android app: an
APK can be decompiled, and a key shipped to a phone is a key you have published.

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

These properties describe the real agent and vision path. `ESCAPE_FAKE_MODEL=1` is an
explicitly privileged development stub: it uses server state to make camera requests
succeed and must never be used as evidence of shape recognition or security.

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
$ python -m pytest tests -q          # before you start
  5 failed, 20 passed                # the 5 are test_agent_wiring.py

$ python -m pytest tests -q          # after Exercises 2 and 3
  25 passed

$ python simulated_player.py --games 40
  honest     escaped 10/10   attempts 1.0   mean turns 4.0
  cheat      escaped  0/10   attempts 2.0   mean turns 25.0
  jailbreak  escaped  0/10   attempts 0.0   mean turns 25.0
  brute      escaped  0/10   attempts 5.0   mean turns 25.0

  passcode spoken by the Enigma:   0   <- must be 0
  doors opened without the camera: 0   <- must be 0
```

`--live` runs the same four strategies through the real agent and the real vision model.

In the handout, 5 of the 25 tests are RED until Exercises 2 and 3 are done. That is the
signal: `test_agent_wiring.py` going green means your agent actually runs.

`maintainer_tests/` is for checking an untouched handout before release, not for student
completion. Maintainers run it without creating cache artifacts:

```bash
PYTHONDONTWRITEBYTECODE=1 python -m pytest -p no:cacheprovider maintainer_tests -q
```

---

## Running it for real

There are four levels, and each one needs more than the last. Do them in order — a
failure at level 1 is ten seconds to diagnose, the same failure at level 4 is an hour.

### Level 1 — no API key, no phone   *(seconds)*

```bash
python -m pytest tests -q          # 25 tests
python simulated_player.py --games 40
```

`test_agent_wiring.py` builds your real agent and runs it against a **fake model** that
is scripted to call a tool. It proves `create_agent` accepted your arguments, the tools
were bound, a tool call round-trips, the photo reaches `scan_shape` and never reaches
the transcript, and `record_turn` fired. What it cannot prove is that a *real* model
chooses the right tool — that is level 2.

### Level 2 — API key, still no phone   *(a minute)*

```bash
python -m server.enigma_agent          # reads .env
```

You get a prompt. Talk to it, then hand it a picture:

```
you > what is the first digit?
you > give me a riddle
you > photo figures/shapes/pentagon.png
```

`figures/shapes/` has one clean PNG per key — triangle through octagon — so you can
press keys without a camera. The line under each reply shows the trajectory and the
digits entered so far:

```
enigma > The lock shifts. Five, and three keys remain.
          [tools ['scan_shape']  entered [5]]
```

Then run the adversarial pass through the real model and the real VLM:

```bash
python simulated_player.py --games 12 --live
```

Both bottom lines must read **0**.

### Level 3 — Django, still no phone   *(five minutes)*

Two terminals. **Terminal 1**, the server — note the `cd`:

```bash
cd django_project
python manage.py runserver 0.0.0.0:8000
```

No API key? Start it as `ESCAPE_FAKE_MODEL=1 python manage.py runserver 0.0.0.0:8000`
and the Enigma is replaced by a scripted stand-in that calls no model. The whole
client path then works, door included. Read `server/offline.py` first: because the stub
cannot inspect photos, any request with an attached image presses the next correct key
using the server-only passcode and current entry position. It trusts the camera flag and
privileged server state. This is useful for UI integration, but it is neither shape
recognition nor a security test. Turn fake mode off for the real camera boundary.

**Terminal 2**, from the repo root — that is where `figures/` lives:

```bash
SID=$(curl -sX POST http://localhost:8000/escape/sessions/ \
      | python -c 'import json,sys;print(json.load(sys.stdin)["session_id"])')

curl -sX POST http://localhost:8000/escape/sessions/$SID/say/ \
     -H 'Content-Type: application/json' \
     -d '{"text":"what is the first digit?"}'

curl -sX POST http://localhost:8000/escape/sessions/$SID/say/ \
     -H 'Content-Type: application/json' \
     -d "{\"text\":\"I am holding it up to the camera.\",\"image_base64\":\"$(base64 -i figures/shapes/pentagon.png | tr -d '\n')\"}"
```

The second call is the whole system minus the phone. Repeat that camera request four
times in fake mode; the fourth opens the door. A working first reply looks like this:

```json
{"reply": "[FAKE MODEL] Key 6 pressed. So far: 6. 3 to go.",
 "state": {"entered": [6], "digits_remaining": 3, "escaped": false, "...": "..."},
 "used_camera": true}
```

Check three things: `used_camera` is true, `entered` grew, and there is no `passcode`
anywhere in the body.

### Level 4 — the phone   *(Android Studio)*

`0.0.0.0`, not `127.0.0.1` — the emulator is a different machine, and so is your phone.

| symptom | look at |
|---|---|
| app cannot reach the server | `BASE_URL` is your LAN IP; both devices on one Wi-Fi |
| `CLEARTEXT communication not permitted` in Logcat | `usesCleartextTraffic` in the manifest |
| request seems to hang, then fails | read timeout is 60s, not the default 10s |
| server answers 413 | the downscale in `fileToBase64()` — log `sizeKb(b64)`, expect ~180 |
| server answers 400, body looks fine | `Base64.NO_WRAP` |
| spinner never stops | `_thinking.value = false` is not in a `finally` |

Emulator camera: **Camera › Back → VirtualScene**, then point it at a shape on your
screen and use the in-app shutter.

### What still cannot be checked from a terminal

`./gradlew :app:compileDebugKotlin` catches Kotlin compilation errors, but no automated
test in this repo exercises CameraX, a real device network, or the visible UI flow.
Exercises 1 and 4–6 therefore still need to be verified by running the app and using the
table above.
