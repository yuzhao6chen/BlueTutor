from __future__ import annotations

from fastapi import FastAPI

from .api.preview_api import preview_router
from .api.guide_api import guide_router


app = FastAPI(
	title="BlueTutor Backend",
	version="0.1.0",
	description="BlueTutor backend service for preview, guide, mistakes, and shared modules.",
)

app.include_router(preview_router)
app.include_router(guide_router)

from fastapi.middleware.cors import CORSMiddleware

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 开发阶段允许所有来源
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


__all__ = ["app"]
