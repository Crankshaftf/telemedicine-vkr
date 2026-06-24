from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session, joinedload

from app.database import get_db
from app.models import (
    Appointment,
    AppointmentStatus,
    Consultation,
    Doctor,
    Patient,
    ScheduleSlot,
    SlotStatus,
    User,
    UserRole,
)
from app.schemas import (
    AppointmentCreateRequest,
    AppointmentDetailResponse,
    AppointmentResponse,
    ConsultationResponse,
    FileResponse,
    MessageResponse,
    StaffAppointmentResponse,
)
from app.services.notifications import create_notification
from app.utils.deps import get_current_user, require_roles

router = APIRouter(prefix="/appointments", tags=["Записи"])


def _to_appointment_response(appt: Appointment) -> AppointmentResponse:
    return AppointmentResponse(
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
    )


def _to_detail(appt: Appointment) -> AppointmentDetailResponse:
    base = _to_appointment_response(appt)
    consultation = None
    if appt.consultation:
        c = appt.consultation
        consultation = ConsultationResponse(
            consultation_id=c.consultation_id,
            appointment_id=c.appointment_id,
            recommendation=c.recommendation,
            result_text=c.result_text,
            preliminary_assessment=c.preliminary_assessment,
            needs_in_person=c.needs_in_person,
            created_at=c.created_at,
            messages=[
                MessageResponse(
                    message_id=m.message_id,
                    sender_id=m.sender_id,
                    sender_name=_sender_name(m.sender),
                    text=m.text,
                    created_at=m.created_at,
                )
                for m in c.messages
            ],
        )
    return AppointmentDetailResponse(
        **base.model_dump(),
        files=[FileResponse.model_validate(f) for f in appt.files],
        consultation=consultation,
    )


def _sender_name(user: User) -> str:
    if user.patient:
        return user.patient.full_name
    if user.doctor:
        return user.doctor.full_name
    return user.login


@router.post("", response_model=AppointmentResponse, status_code=status.HTTP_201_CREATED)
def create_appointment(
    data: AppointmentCreateRequest,
    user: User = Depends(require_roles(UserRole.PATIENT)),
    db: Session = Depends(get_db),
):
    patient = db.query(Patient).filter(Patient.user_id == user.user_id).first()
    if patient is None:
        raise HTTPException(status_code=400, detail="Профиль пациента не найден")

    doctor = db.get(Doctor, data.doctor_id)
    if doctor is None:
        raise HTTPException(status_code=404, detail="Врач не найден")

    slot = (
        db.query(ScheduleSlot)
        .filter(ScheduleSlot.slot_id == data.slot_id, ScheduleSlot.doctor_id == data.doctor_id)
        .with_for_update()
        .first()
    )
    if slot is None:
        raise HTTPException(status_code=404, detail="Слот не найден")
    if slot.status != SlotStatus.FREE:
        raise HTTPException(status_code=409, detail="Слот уже занят")

    appointment = Appointment(
        patient_id=patient.patient_id,
        doctor_id=data.doctor_id,
        slot_id=data.slot_id,
        complaint=data.complaint,
        symptoms=data.symptoms,
        comment=data.comment,
        status=AppointmentStatus.CONFIRMED,
    )
    slot.status = SlotStatus.BUSY
    db.add(appointment)
    db.flush()

    consultation = Consultation(appointment_id=appointment.appointment_id)
    db.add(consultation)

    create_notification(
        db,
        user.user_id,
        "appointment_confirmed",
        f"Запись подтверждена: {doctor.full_name}, {slot.start_time.strftime('%d.%m.%Y %H:%M')}",
    )
    create_notification(
        db,
        doctor.user_id,
        "new_appointment",
        f"Новая запись от {patient.full_name}: {slot.start_time.strftime('%d.%m.%Y %H:%M')}",
    )
    db.commit()
    db.refresh(appointment)

    appt = (
        db.query(Appointment)
        .options(joinedload(Appointment.doctor), joinedload(Appointment.slot), joinedload(Appointment.consultation))
        .filter(Appointment.appointment_id == appointment.appointment_id)
        .first()
    )
    return _to_appointment_response(appt)


@router.get("/doctor/list", response_model=list[StaffAppointmentResponse])
def get_doctor_appointments(
    user: User = Depends(require_roles(UserRole.DOCTOR)),
    db: Session = Depends(get_db),
):
    if not user.doctor:
        return []
    appts = (
        db.query(Appointment)
        .options(
            joinedload(Appointment.doctor),
            joinedload(Appointment.slot),
            joinedload(Appointment.patient),
            joinedload(Appointment.consultation),
        )
        .filter(Appointment.doctor_id == user.doctor.doctor_id)
        .order_by(Appointment.created_at.desc())
        .all()
    )
    return [
        StaffAppointmentResponse(
            appointment_id=a.appointment_id,
            doctor_id=a.doctor_id,
            doctor_name=a.doctor.full_name,
            specialization=a.doctor.specialization,
            slot_id=a.slot_id,
            start_time=a.slot.start_time,
            end_time=a.slot.end_time,
            complaint=a.complaint,
            symptoms=a.symptoms,
            comment=a.comment,
            status=a.status.value,
            created_at=a.created_at,
            has_consultation=a.consultation is not None,
            patient_name=a.patient.full_name,
            patient_phone=a.patient.phone,
        )
        for a in appts
    ]


@router.get("/history", response_model=list[AppointmentResponse])
def get_history(user: User = Depends(require_roles(UserRole.PATIENT)), db: Session = Depends(get_db)):
    patient = db.query(Patient).filter(Patient.user_id == user.user_id).first()
    if patient is None:
        return []

    appts = (
        db.query(Appointment)
        .options(joinedload(Appointment.doctor), joinedload(Appointment.slot), joinedload(Appointment.consultation))
        .filter(Appointment.patient_id == patient.patient_id)
        .order_by(Appointment.created_at.desc())
        .all()
    )
    return [_to_appointment_response(a) for a in appts]


@router.get("/{appointment_id}", response_model=AppointmentDetailResponse)
def get_appointment(
    appointment_id: int,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    appt = (
        db.query(Appointment)
        .options(
            joinedload(Appointment.doctor),
            joinedload(Appointment.slot),
            joinedload(Appointment.files),
            joinedload(Appointment.consultation).joinedload(Consultation.messages),
        )
        .filter(Appointment.appointment_id == appointment_id)
        .first()
    )
    if appt is None:
        raise HTTPException(status_code=404, detail="Запись не найдена")

    if user.role == UserRole.PATIENT:
        if not user.patient or appt.patient_id != user.patient.patient_id:
            raise HTTPException(status_code=403, detail="Нет доступа")
    elif user.role == UserRole.DOCTOR:
        if not user.doctor or appt.doctor_id != user.doctor.doctor_id:
            raise HTTPException(status_code=403, detail="Нет доступа")
    elif user.role not in (UserRole.ADMIN, UserRole.MANAGER):
        raise HTTPException(status_code=403, detail="Нет доступа")

    return _to_detail(appt)


@router.put("/{appointment_id}/cancel", response_model=AppointmentResponse)
def cancel_appointment(
    appointment_id: int,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    appt = (
        db.query(Appointment)
        .options(joinedload(Appointment.doctor), joinedload(Appointment.slot), joinedload(Appointment.consultation))
        .filter(Appointment.appointment_id == appointment_id)
        .with_for_update()
        .first()
    )
    if appt is None:
        raise HTTPException(status_code=404, detail="Запись не найдена")

    if user.role == UserRole.PATIENT:
        if not user.patient or appt.patient_id != user.patient.patient_id:
            raise HTTPException(status_code=403, detail="Нет доступа")
    elif user.role not in (UserRole.DOCTOR, UserRole.ADMIN):
        raise HTTPException(status_code=403, detail="Нет доступа")

    if appt.status in (AppointmentStatus.CANCELLED, AppointmentStatus.COMPLETED):
        raise HTTPException(status_code=400, detail="Запись уже завершена или отменена")

    appt.status = AppointmentStatus.CANCELLED
    appt.slot.status = SlotStatus.FREE

    create_notification(
        db,
        appt.patient.user_id,
        "appointment_cancelled",
        f"Консультация отменена: {appt.slot.start_time.strftime('%d.%m.%Y %H:%M')}",
    )
    db.commit()
    db.refresh(appt)
    return _to_appointment_response(appt)
