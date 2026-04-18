from __future__ import annotations

import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from .api.preview_api import preview_router
from .api.shared_api import shared_router

logger = logging.getLogger(__name__)

guide_router = None
guide_import_error: ModuleNotFoundError | None = None

try:
    from .api.guide_api import guide_router
except ModuleNotFoundError as exc:
    guide_import_error = exc


app = FastAPI(
    title="BlueTutor Backend",
    version="0.1.0",
    description="BlueTutor backend service for preview, guide, mistakes, and shared modules.",
)
main = app

app.include_router(preview_router)
app.include_router(shared_router)

if guide_router is not None:
    app.include_router(guide_router)
elif guide_import_error is not None:
    logger.warning(
        "Guide routes were not registered because dependency %s is missing.",
        guide_import_error.name,
    )

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Allow all origins during development.
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/")
def root() -> dict[str, str]:
    return {
        "name": "BlueTutor Backend",
        "status": "ok",
    }


@app.get("/health")
def health() -> dict[str, str]:
    return {
        "status": "ok",
    }


__all__ = ["app", "main"]
