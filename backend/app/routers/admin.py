from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session, joinedload

from app.database import get_db
from app.models import (
    Appointment,
    AppointmentStatus,
    Doctor,
    ScheduleSlot,
    SlotStatus,
    UserRole,
)
from app.schemas import (
    AppointmentStatusUpdateRequest,
    SlotCreateRequest,
    SlotResponse,
    SlotStatusUpdateRequest,
    StaffAppointmentResponse,
)
from app.utils.deps import require_roles

router = APIRouter(prefix="/admin", tags=["Администрирование"])


def _to_staff_appointment(appt: Appointment) -> StaffAppointmentResponse:
    return StaffAppointmentResponse(
        appointment_id=appt.appointment_id,
        doctor_id=appt.doctor_id,
        doctor_name=appt.doctor.full_name,
        specialization=appt.doctor.specialization,
        slot_id=appt.slot_id,
        start_time=appt.slot.start_time,
        end_time=appt.slot.end_time,
        complaint=appt.complaint,
        symptoms=appt.symptoms,
        comment=appt.comment,
        status=appt.status.value,
        created_at=appt.created_at,
        has_consultation=appt.consultation is not None,
        patient_name=appt.patient.full_name,
        patient_phone=appt.patient.phone,
    )


@router.get("/appointments", response_model=list[StaffAppointmentResponse])
def list_all_appointments(
    db: Session = Depends(get_db),
    _user=Depends(require_roles(UserRole.ADMIN, UserRole.MANAGER)),
):
    appts = (
        db.query(Appointment)
        .options(
            joinedload(Appointment.doctor),
            joinedload(Appointment.slot),
            joinedload(Appointment.patient),
            joinedload(Appointment.consultation),
        )
        .order_by(Appointment.created_at.desc())
        .all()
    )
    return [_to_staff_appointment(a) for a in appts]


@router.put("/appointments/{appointment_id}/status", response_model=StaffAppointmentResponse)
def update_appointment_status(
    appointment_id: int,
    data: AppointmentStatusUpdateRequest,
    db: Session = Depends(get_db),
    _user=Depends(require_roles(UserRole.ADMIN)),
):
    try:
        new_status = AppointmentStatus(data.status)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail="Недопустимый статус") from exc

    appt = (
        db.query(Appointment)
        .options(
            joinedload(Appointment.doctor),
            joinedload(Appointment.slot),
            joinedload(Appointment.patient),
            joinedload(Appointment.consultation),
        )
        .filter(Appointment.appointment_id == appointment_id)
        .with_for_update()
        .first()
    )
    if appt is None:
        raise HTTPException(status_code=404, detail="Запись не найдена")

    appt.status = new_status
    if new_status == AppointmentStatus.CANCELLED:
        appt.slot.status = SlotStatus.FREE
    elif new_status in (AppointmentStatus.CONFIRMED, AppointmentStatus.CREATED):
        appt.slot.status = SlotStatus.BUSY

    db.commit()
    db.refresh(appt)
    return _to_staff_appointment(appt)


@router.get("/schedule/{doctor_id}", response_model=list[SlotResponse])
def list_doctor_slots_admin(
    doctor_id: int,
    db: Session = Depends(get_db),
    _user=Depends(require_roles(UserRole.ADMIN)),
):
    doctor = db.get(Doctor, doctor_id)
    if doctor is None:
        raise HTTPException(status_code=404, detail="Врач не найден")
    slots = (
        db.query(ScheduleSlot)
        .filter(ScheduleSlot.doctor_id == doctor_id)
        .order_by(ScheduleSlot.start_time)
        .all()
    )
    return slots


@router.post("/slots", response_model=SlotResponse, status_code=status.HTTP_201_CREATED)
def create_slot(
    data: SlotCreateRequest,
    db: Session = Depends(get_db),
    _user=Depends(require_roles(UserRole.ADMIN)),
):
    doctor = db.get(Doctor, data.doctor_id)
    if doctor is None:
        raise HTTPException(status_code=404, detail="Врач не найден")
    if data.end_time <= data.start_time:
        raise HTTPException(status_code=400, detail="Время окончания должно быть позже начала")

    slot = ScheduleSlot(
        doctor_id=data.doctor_id,
        start_time=data.start_time,
        end_time=data.end_time,
        status=SlotStatus.FREE,
    )
    db.add(slot)
    db.commit()
    db.refresh(slot)
    return slot


@router.put("/slots/{slot_id}", response_model=SlotResponse)
def update_slot_status(
    slot_id: int,
    data: SlotStatusUpdateRequest,
    db: Session = Depends(get_db),
    _user=Depends(require_roles(UserRole.ADMIN)),
):
    try:
        new_status = SlotStatus(data.status)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail="Недопустимый статус слота") from exc

    slot = db.get(ScheduleSlot, slot_id)
    if slot is None:
        raise HTTPException(status_code=404, detail="Слот не найден")
    if slot.status == SlotStatus.BUSY and new_status == SlotStatus.FREE:
        raise HTTPException(status_code=400, detail="Нельзя освободить занятый слот с активной записью")

    slot.status = new_status
    db.commit()
    db.refresh(slot)
    return slot
