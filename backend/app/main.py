from __future__ import annotations

from fastapi import FastAPI

from .api.preview_api import preview_router


app = FastAPI(
	title="BlueTutor Backend",
	version="0.1.0",
	description="BlueTutor backend service for preview, guide, mistakes, and shared modules.",
)

app.include_router(preview_router)


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


__all__ = ["app"]
