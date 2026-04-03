from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """应用配置"""
    
    # 应用配置
    app_name: str = "BlueTutor AI 教学助手"
    debug: bool = True
    
    # API 配置
    api_prefix: str = "/api/v1"
    
    # OpenAI 配置
    openai_api_key: str = ""
    openai_base_url: str = "https://api.openai.com/v1"
    openai_model: str = "gpt-4o-mini"
    
    class Config:
        env_file = ".env"
        case_sensitive = False


settings = Settings()
