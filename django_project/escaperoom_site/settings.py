"""Deliberately small. Everything here is either required by Django or by the lab."""

import pathlib

BASE_DIR = pathlib.Path(__file__).resolve().parent.parent

# Fine for a classroom. Never for anything reachable from the internet.
SECRET_KEY = "swpp-2026-week6-not-a-secret"
DEBUG = True

# The phone is a different machine, so localhost is not enough.
ALLOWED_HOSTS = ["*"]

INSTALLED_APPS = ["escaperoom"]

MIDDLEWARE = ["django.middleware.common.CommonMiddleware"]

ROOT_URLCONF = "escaperoom_site.urls"
WSGI_APPLICATION = "escaperoom_site.wsgi.application"

# No models yet: sessions live in memory in server/game.py, and game.py's docstring
# shows the EscapeSession / Turn models you would write to persist them.
DATABASES = {}

# A base64 image is a big POST body. Django's default cap is 2.5 MB, and the view
# rejects anything over 2,000,000 characters with a message before that bites.
DATA_UPLOAD_MAX_MEMORY_SIZE = 8 * 1024 * 1024

USE_TZ = True
DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"
