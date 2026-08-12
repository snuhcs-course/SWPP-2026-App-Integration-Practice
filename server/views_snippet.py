"""Django glue - drop into escaperoom/views.py. Seams 1 and 2.

Three things to notice, because all three are the lesson:

1. `say` accepts an OPTIONAL image. Same endpoint, with or without a photo - the phone
   does not need a second route just because the user pressed the shutter.

2. The server writes the verdict. The agent's reply is data, not authority. `escaped`
   is set in one place - game.press(), reached only through the camera. If the view
   trusted a field from the agent's reply instead, a player could talk the door open.

3. public_state() is a whitelist. Serialising the session object would ship the passcode
   to the phone, where any student with a proxy can read it.
"""

from __future__ import annotations

import json

from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_GET, require_POST

from .services.enigma_agent import take_turn
from .services.game import MAX_TURNS, get, new_session, public_state

MAX_IMAGE_CHARS = 2_000_000        # ~1.5 MB of JPEG. Downscale on the phone, not here.


@csrf_exempt
@require_POST
def create_session(request):
    """POST /escape/sessions/  ->  201 {"session_id", "turns_left", ...}"""
    return JsonResponse(public_state(new_session()), status=201)


@require_GET
def session_state(request, session_id: str):
    """GET /escape/sessions/<session_id>/"""
    s = get(session_id)
    if s is None:
        return JsonResponse({"error": "no such session"}, status=404)
    return JsonResponse(public_state(s))


@csrf_exempt
@require_POST
def say(request, session_id: str):
    """POST /escape/sessions/<session_id>/say/

    body: {"text": "...", "image_base64": "..."}      image_base64 is optional
    """
    s = get(session_id)
    if s is None:
        return JsonResponse({"error": "no such session"}, status=404)
    if s["escaped"]:
        return JsonResponse({"error": "already escaped"}, status=409)
    if s["turns"] >= MAX_TURNS:
        return JsonResponse({"error": "out of turns"}, status=409)

    try:
        payload = json.loads(request.body)
        text = payload["text"]
    except (json.JSONDecodeError, KeyError):
        return JsonResponse({"error": "expected JSON body with 'text'"}, status=400)

    image_base64 = payload.get("image_base64")
    if image_base64 and len(image_base64) > MAX_IMAGE_CHARS:
        # Say why. "413 Payload Too Large" with no body costs a student 40 minutes.
        return JsonResponse(
            {"error": f"image too large ({len(image_base64)} chars). "
                      f"Downscale to 1024px on the device before encoding."},
            status=413,
        )

    result = take_turn(session_id=session_id, player_text=text, image_base64=image_base64)

    return JsonResponse({
        "reply": result["reply"],
        "state": public_state(s),          # the whitelist
        "used_camera": result["had_image"],
    })


# escaperoom/urls.py
#
# urlpatterns = [
#     path("sessions/",                      views.create_session),
#     path("sessions/<str:session_id>/",     views.session_state),
#     path("sessions/<str:session_id>/say/", views.say),
# ]
