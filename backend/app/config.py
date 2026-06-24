from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    database_url: str = "postgresql://telemed:telemed123@localhost:5432/telemedicine"
    secret_key: str = "dev-secret-key-change-in-production"
    access_token_expire_hours: int = 24
    upload_dir: str = "uploads"
    max_file_size_mb: int = 10
    app_name: str = "МедКоннект API"
    app_version: str = "0.1.0"
    admin_panel_dir: str = ""
    download_dir: str = ""
    github_release_url: str = ""
    host: str = "0.0.0.0"
    port: int = 8000


settings = Settings()
