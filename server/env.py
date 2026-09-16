"""Load the API key from .env, once, at the edge of the program.

Configuration is read where the process starts - manage.py, the terminal player,
the simulator - and never inside library code. Import a module and it should not
quietly reach into your filesystem.

The key itself is only ever needed by the SERVER. `langchain_google_genai.ChatGoogleGenerativeAI`
reads the GOOGLE_API_KEY environment variable when it is constructed, so all this
has to do is put the value there before the first construction.

    GOOGLE_API_KEY=...

goes in a file called `.env` at the root of this repo. `.gitignore` already lists
it. An existing environment variable always wins, so `GOOGLE_API_KEY=... python ...`
still overrides the file for one run.
"""

from __future__ import annotations

import pathlib

REPO_ROOT = pathlib.Path(__file__).resolve().parents[1]
ENV_FILE = REPO_ROOT / ".env"


def load_env() -> bool:
    """Return True if a .env file was found and loaded."""
    try:
        from dotenv import load_dotenv
    except ImportError:          # pip install -r requirements.txt
        return False
    # override=False: a real environment variable beats the file.
    return load_dotenv(ENV_FILE, override=False)
