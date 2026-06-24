from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session, joinedload

from app.database import get_db
from app.models import (
    Appointment,
    AppointmentStatus,
    Consultation,
    Message,
    User,
    UserRole,
)
from app.schemas import ConsultationResponse, ConsultationResultRequest, MessageCreateRequest, MessageResponse
from app.services.notifications import create_notification
from app.utils.deps import get_current_user, require_roles

router = APIRouter(prefix="/consultations", tags=["Консультации"])


def _sender_name(user: User) -> str:
    if user.patient:
        return user.patient.full_name
    if user.doctor:
        return user.doctor.full_name
    return user.login


def _to_consultation_response(consultation: Consultation) -> ConsultationResponse:
    return ConsultationResponse(
        consultation_id=consultation.consultation_id,
        appointment_id=consultation.appointment_id,
        recommendation=consultation.recommendation,
        result_text=consultation.result_text,
        preliminary_assessment=consultation.preliminary_assessment,
        needs_in_person=consultation.needs_in_person,
        created_at=consultation.created_at,
        messages=[
            MessageResponse(
                message_id=m.message_id,
                sender_id=m.sender_id,
                sender_name=_sender_name(m.sender),
                text=m.text,
                created_at=m.created_at,
            )
            for m in consultation.messages
        ],
    )


def _get_consultation_for_user(appointment_id: int, user: User, db: Session) -> Consultation:
    consultation = (
        db.query(Consultation)
        .options(joinedload(Consultation.messages).joinedload(Message.sender), joinedload(Consultation.appointment))
        .join(Appointment)
        .filter(Consultation.appointment_id == appointment_id)
        .first()
    )
    if consultation is None:
        raise HTTPException(status_code=404, detail="Консультация не найдена")

    appt = consultation.appointment
    if user.role == UserRole.PATIENT:
        if not user.patient or appt.patient_id != user.patient.patient_id:
            raise HTTPException(status_code=403, detail="Нет доступа")
    elif user.role == UserRole.DOCTOR:
        if not user.doctor or appt.doctor_id != user.doctor.doctor_id:
            raise HTTPException(status_code=403, detail="Нет доступа")
    else:
        raise HTTPException(status_code=403, detail="Нет доступа")

    return consultation


@router.get("/{appointment_id}", response_model=ConsultationResponse)
def get_consultation(
    appointment_id: int,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    consultation = _get_consultation_for_user(appointment_id, user, db)
    return _to_consultation_response(consultation)


@router.post("/{consultation_id}/message", response_model=MessageResponse, status_code=status.HTTP_201_CREATED)
def send_message(
    consultation_id: int,
    data: MessageCreateRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    consultation = (
        db.query(Consultation)
        .options(joinedload(Consultation.appointment))
        .filter(Consultation.consultation_id == consultation_id)
        .first()
    )
    if consultation is None:
        raise HTTPException(status_code=404, detail="Консультация не найдена")

    appt = consultation.appointment
    if user.role == UserRole.PATIENT:
        if not user.patient or appt.patient_id != user.patient.patient_id:
            raise HTTPException(status_code=403, detail="Нет доступа")
    elif user.role == UserRole.DOCTOR:
        if not user.doctor or appt.doctor_id != user.doctor.doctor_id:
            raise HTTPException(status_code=403, detail="Нет доступа")
    else:
        raise HTTPException(status_code=403, detail="Нет доступа")

    message = Message(consultation_id=consultation_id, sender_id=user.user_id, text=data.text)
    db.add(message)
    db.commit()
    db.refresh(message)

    return MessageResponse(
        message_id=message.message_id,
        sender_id=message.sender_id,
        sender_name=_sender_name(user),
        text=message.text,
        created_at=message.created_at,
    )


@router.post("/{consultation_id}/result", response_model=ConsultationResponse)
def save_consultation_result(
    consultation_id: int,
    data: ConsultationResultRequest,
    user: User = Depends(require_roles(UserRole.DOCTOR)),
    db: Session = Depends(get_db),
):
    consultation = (
        db.query(Consultation)
        .options(joinedload(Consultation.messages).joinedload(Message.sender), joinedload(Consultation.appointment))
        .filter(Consultation.consultation_id == consultation_id)
        .first()
    )
    if consultation is None:
        raise HTTPException(status_code=404, detail="Консультация не найдена")

    appt = consultation.appointment
    if not user.doctor or appt.doctor_id != user.doctor.doctor_id:
        raise HTTPException(status_code=403, detail="Нет доступа")

    consultation.recommendation = data.recommendation
    consultation.result_text = data.result_text
    consultation.preliminary_assessment = data.preliminary_assessment
    consultation.needs_in_person = data.needs_in_person
    appt.status = AppointmentStatus.COMPLETED

    create_notification(
        db,
        appt.patient.user_id,
        "consultation_result",
        "Готово заключение врача. Откройте историю обращений.",
    )
    db.commit()
    db.refresh(consultation)
    return _to_consultation_response(consultation)
