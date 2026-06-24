from datetime import datetime, timezone
from pathlib import Path

from fastapi import APIRouter, HTTPException
from fastapi.responses import FileResponse

from app.config import settings

router = APIRouter(tags=["download"])

APK_FILENAME = "MedConnect.apk"


def _project_root() -> Path:
    return Path(__file__).resolve().parents[3]


def _github_release_url() -> str | None:
    if settings.github_release_url.strip():
        return settings.github_release_url.strip()
    url_file = _project_root() / "github-release.url"
    if not url_file.is_file():
        return None
    for line in url_file.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#"):
            return line
    return None


def _download_candidates() -> list[Path]:
    here = Path(__file__).resolve()
    backend_root = here.parents[2]
    project_root = here.parents[3]
    roots = [
        backend_root / "downloads",
        project_root / "download-web",
        project_root,
    ]
    if settings.download_dir:
        roots.insert(0, Path(settings.download_dir))
    seen: set[Path] = set()
    out: list[Path] = []
    for root in roots:
        if root in seen:
            continue
        seen.add(root)
        out.append(root / APK_FILENAME)
    return out


def _find_apk() -> Path | None:
    for path in _download_candidates():
        if path.is_file():
            return path
    return None


@router.get("/download/info")
def download_info():
    github_url = _github_release_url()
    apk = _find_apk()
    base = {
        "filename": APK_FILENAME,
        "version": settings.app_version,
        "github_url": github_url,
    }
    if apk is None:
        return {
            **base,
            "available": bool(github_url),
            "source": "github" if github_url else "none",
        }
    stat = apk.stat()
    updated = datetime.fromtimestamp(stat.st_mtime, tz=timezone.utc).strftime("%d.%m.%Y %H:%M")
    return {
        **base,
        "available": True,
        "source": "github" if github_url else "local",
        "size_bytes": stat.st_size,
        "size_mb": round(stat.st_size / (1024 * 1024), 1),
        "updated_at": updated,
        "url": github_url or f"/download/{APK_FILENAME}",
    }


@router.get(f"/download/{APK_FILENAME}")
def download_apk():
    apk = _find_apk()
    if apk is None:
        raise HTTPException(status_code=404, detail="APK not found. Run build-apk.ps1 on the server PC.")
    return FileResponse(
        path=apk,
        filename=APK_FILENAME,
        media_type="application/vnd.android.package-archive",
    )
