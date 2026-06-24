from fastapi import APIRouter, Depends
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.database import get_db
from app.models import Appointment, AppointmentStatus, Doctor, Report, UserRole
from app.schemas import ReportResponse
from app.utils.deps import require_roles

router = APIRouter(prefix="/reports", tags=["Отчёты"])


@router.get("", response_model=list[ReportResponse])
def get_reports(
    db: Session = Depends(get_db),
    _user=Depends(require_roles(UserRole.MANAGER, UserRole.ADMIN)),
):
    reports = db.query(Report).order_by(Report.report_id.desc()).all()
    if reports:
        return reports

    total = db.query(func.count(Appointment.appointment_id)).scalar() or 0
    completed = (
        db.query(func.count(Appointment.appointment_id))
        .filter(Appointment.status == AppointmentStatus.COMPLETED)
        .scalar()
        or 0
    )
    cancelled = (
        db.query(func.count(Appointment.appointment_id))
        .filter(Appointment.status == AppointmentStatus.CANCELLED)
        .scalar()
        or 0
    )

    doctor_stats = []
    for doctor in db.query(Doctor).all():
        count = db.query(func.count(Appointment.appointment_id)).filter(Appointment.doctor_id == doctor.doctor_id).scalar()
        doctor_stats.append(f"{doctor.full_name}: {count} записей")

    return [
        ReportResponse(
            report_id=0,
            period="все время",
            consultation_count=completed,
            no_show_count=cancelled,
            doctor_load="; ".join(doctor_stats) if doctor_stats else None,
        )
    ]
