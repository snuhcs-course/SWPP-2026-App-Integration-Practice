from django.urls import path

from . import views

urlpatterns = [
    path("sessions/", views.create_session),
    path("sessions/<str:session_id>/", views.session_state),
    path("sessions/<str:session_id>/say/", views.say),
]
