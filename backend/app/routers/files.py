import os
import uuid
from pathlib import Path

from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile, status
from fastapi.responses import FileResponse as FastAPIFileResponse
from sqlalchemy.orm import Session

from app.config import settings
from app.database import get_db
from app.models import Appointment, MedicalFile, Patient, User, UserRole
from app.schemas import FileResponse
from app.utils.deps import get_current_user

router = APIRouter(prefix="/files", tags=["Файлы"])

ALLOWED_EXTENSIONS = {".pdf", ".jpg", ".jpeg", ".png"}
ALLOWED_MIME = {"application/pdf", "image/jpeg", "image/png"}


def _ensure_upload_dir():
    Path(settings.upload_dir).mkdir(parents=True, exist_ok=True)


@router.post("", response_model=FileResponse, status_code=status.HTTP_201_CREATED)
async def upload_file(
    appointment_id: int = Form(...),
    file: UploadFile = File(...),
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    appt = db.get(Appointment, appointment_id)
    if appt is None:
        raise HTTPException(status_code=404, detail="Запись не найдена")

    if user.role == UserRole.PATIENT:
        patient = db.query(Patient).filter(Patient.user_id == user.user_id).first()
        if patient is None or appt.patient_id != patient.patient_id:
            raise HTTPException(status_code=403, detail="Нет доступа")

    ext = Path(file.filename or "").suffix.lower()
    if ext not in ALLOWED_EXTENSIONS:
        raise HTTPException(status_code=400, detail="Допустимы только PDF, JPG, PNG")

    content = await file.read()
    max_bytes = settings.max_file_size_mb * 1024 * 1024
    if len(content) > max_bytes:
        raise HTTPException(status_code=400, detail=f"Размер файла не более {settings.max_file_size_mb} МБ")

    _ensure_upload_dir()
    stored_name = f"{uuid.uuid4().hex}{ext}"
    file_path = os.path.join(settings.upload_dir, stored_name)
    with open(file_path, "wb") as f:
        f.write(content)

    medical_file = MedicalFile(
        appointment_id=appointment_id,
        file_name=file.filename or stored_name,
        file_path=file_path,
        file_type=ext.lstrip("."),
    )
    db.add(medical_file)
    db.commit()
    db.refresh(medical_file)
    return medical_file


@router.get("/{file_id}")
def download_file(
    file_id: int,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    medical_file = db.get(MedicalFile, file_id)
    if medical_file is None:
        raise HTTPException(status_code=404, detail="Файл не найден")

    appt = medical_file.appointment
    if user.role == UserRole.PATIENT:
        patient = db.query(Patient).filter(Patient.user_id == user.user_id).first()
        if patient is None or appt.patient_id != patient.patient_id:
            raise HTTPException(status_code=403, detail="Нет доступа")
    elif user.role == UserRole.DOCTOR:
        if not user.doctor or appt.doctor_id != user.doctor.doctor_id:
            raise HTTPException(status_code=403, detail="Нет доступа")

    if not os.path.exists(medical_file.file_path):
        raise HTTPException(status_code=404, detail="Файл не найден на сервере")

    return FastAPIFileResponse(
        path=medical_file.file_path,
        filename=medical_file.file_name,
        media_type="application/octet-stream",
    )
