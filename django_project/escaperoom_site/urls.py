from django.urls import include, path

urlpatterns = [
    path("escape/", include("escaperoom.urls")),
]
