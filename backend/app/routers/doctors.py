from datetime import date

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from app.database import get_db
from app.models import Doctor, ScheduleSlot, SlotStatus
from app.schemas import DoctorResponse, SlotResponse

router = APIRouter(tags=["Врачи и расписание"])


@router.get("/specializations", response_model=list[str])
def get_specializations(db: Session = Depends(get_db)):
    rows = db.query(Doctor.specialization).distinct().order_by(Doctor.specialization).all()
    return [r[0] for r in rows]


@router.get("/doctors", response_model=list[DoctorResponse])
def get_doctors(
    specialization: str | None = Query(None, description="Фильтр по специализации"),
    db: Session = Depends(get_db),
):
    query = db.query(Doctor)
    if specialization:
        query = query.filter(Doctor.specialization.ilike(f"%{specialization}%"))
    return query.order_by(Doctor.full_name).all()


@router.get("/doctors/{doctor_id}", response_model=DoctorResponse)
def get_doctor(doctor_id: int, db: Session = Depends(get_db)):
    doctor = db.get(Doctor, doctor_id)
    if doctor is None:
        raise HTTPException(status_code=404, detail="Врач не найден")
    return doctor


@router.get("/schedule/{doctor_id}", response_model=list[SlotResponse])
def get_doctor_schedule(
    doctor_id: int,
    date_from: date | None = Query(None),
    date_to: date | None = Query(None),
    db: Session = Depends(get_db),
):
    from datetime import datetime, time, timezone
    doctor = db.get(Doctor, doctor_id)
    if doctor is None:
        raise HTTPException(status_code=404, detail="Врач не найден")

    now_utc = datetime.now(timezone.utc)
    query = db.query(ScheduleSlot).filter(
        ScheduleSlot.doctor_id == doctor_id,
        ScheduleSlot.status == SlotStatus.FREE,
        ScheduleSlot.start_time > now_utc,
    )
    if date_from:
        query = query.filter(ScheduleSlot.start_time >= datetime.combine(date_from, time.min, tzinfo=timezone.utc))
    if date_to:
        query = query.filter(ScheduleSlot.start_time <= datetime.combine(date_to, time.max, tzinfo=timezone.utc))

    return query.order_by(ScheduleSlot.start_time).all()
