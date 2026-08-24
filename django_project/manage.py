#!/usr/bin/env python
"""Standard Django entry point, with one extra line.

The game logic lives one directory up in `server/`, and the Django app imports it
rather than owning a copy. That is the shape of the whole session: Django is glue.
"""
import os
import pathlib
import sys

REPO_ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO_ROOT))          # so `import server.game` works

from server.env import load_env             # noqa: E402  (needs sys.path first)


def main():
    # Read .env before anything constructs a model client.
    load_env()
    os.environ.setdefault("DJANGO_SETTINGS_MODULE", "escaperoom_site.settings")
    from django.core.management import execute_from_command_line
    execute_from_command_line(sys.argv)


if __name__ == "__main__":
    main()
