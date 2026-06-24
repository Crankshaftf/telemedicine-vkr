from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from app.config import settings
from app.database import Base, engine
from app.routers import admin, appointments, auth, consultations, doctors, download, files, notifications, reports


@asynccontextmanager
async def lifespan(app: FastAPI):
    Base.metadata.create_all(bind=engine)
    Path(settings.upload_dir).mkdir(parents=True, exist_ok=True)
    yield


app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    description="REST API телемедицинской платформы «МедКоннект» для ВКР",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router)
app.include_router(auth.profile_router)
app.include_router(doctors.router)
app.include_router(appointments.router)
app.include_router(files.router)
app.include_router(consultations.router)
app.include_router(notifications.router)
app.include_router(reports.router)
app.include_router(admin.router)
app.include_router(download.router)

_panel_candidates = []
if settings.admin_panel_dir:
    _panel_candidates.append(Path(settings.admin_panel_dir))
_panel_candidates.extend([
    Path(__file__).resolve().parents[2] / "admin-web",
])
_panel_dir = next((p for p in _panel_candidates if p.exists()), None)
if _panel_dir is not None:
    app.mount("/panel", StaticFiles(directory=str(_panel_dir), html=True), name="panel")

_download_candidates = []
if settings.download_dir:
    _download_candidates.append(Path(settings.download_dir))
_download_candidates.extend([
    Path(__file__).resolve().parents[1] / "downloads",
    Path(__file__).resolve().parents[2] / "download-web",
])
_download_dir = next((p for p in _download_candidates if p.exists() and (p / "index.html").exists()), None)
if _download_dir is not None:
    app.mount("/download", StaticFiles(directory=str(_download_dir), html=True), name="download")


@app.get("/health")
def health():
    return {"status": "ok", "app": settings.app_name, "version": settings.app_version}
