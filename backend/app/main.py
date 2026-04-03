"""
BlueTutor - AI 引导式元认知教学助手

面向小学三年级至初中阶段的智能学习系统，通过引导式提问帮助学生主动构建知识体系。
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.shared.config import settings
from app.api import preview_api, guide_api, mistakes_api, shared_api


def create_app() -> FastAPI:
    """创建并配置 FastAPI 应用"""
    
    app = FastAPI(
        title=settings.app_name,
        description="AI 引导式元认知教学助手 - 预习感知 → 可视化讲题 → 错题闭环加练",
        version="1.0.0",
        debug=settings.debug,
    )
    
    # 配置 CORS
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )
    
    # 注册路由
    app.include_router(preview_api.router, prefix=settings.api_prefix, tags=["预习模块"])
    app.include_router(guide_api.router, prefix=settings.api_prefix, tags=["讲题模块"])
    app.include_router(mistakes_api.router, prefix=settings.api_prefix, tags=["错题模块"])
    app.include_router(shared_api.router, prefix=settings.api_prefix, tags=["共享模块"])
    
    @app.get("/")
    async def root():
        return {
            "name": settings.app_name,
            "version": "1.0.0",
            "description": "AI 引导式元认知教学助手"
        }
    
    @app.get("/health")
    async def health_check():
        return {"status": "healthy"}
    
    return app


app = create_app()


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
