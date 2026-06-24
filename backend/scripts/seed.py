"""Заполнение БД тестовыми данными для демонстрации ВКР."""

import sys
from datetime import date, datetime, timedelta, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.database import SessionLocal, engine, Base
from app.models import (
    Appointment,
    AppointmentStatus,
    Consultation,
    ConsultationFormat,
    Doctor,
    MedicalFile,
    Message,
    Patient,
    ScheduleSlot,
    SlotStatus,
    User,
    UserRole,
    UserStatus,
)
from app.utils.security import hash_password


def ensure_user(db, login: str, password: str, role: UserRole) -> User:
    user = db.query(User).filter(User.login == login).first()
    if user:
        return user
    user = User(
        login=login,
        password_hash=hash_password(password),
        role=role,
        status=UserStatus.ACTIVE,
    )
    db.add(user)
    db.flush()
    return user


def ensure_patient(
    db,
    login: str,
    password: str,
    full_name: str,
    phone: str,
    email: str,
    birth_date: date | None = None,
    gender: str | None = None,
) -> Patient:
    user = ensure_user(db, login, password, UserRole.PATIENT)
    patient = db.query(Patient).filter(Patient.user_id == user.user_id).first()
    if patient:
        return patient
    patient = Patient(
        user_id=user.user_id,
        full_name=full_name,
        birth_date=birth_date,
        phone=phone,
        email=email,
        gender=gender,
    )
    db.add(patient)
    db.flush()
    return patient


def seed():
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()

    # --- Обычный пользователь (без истории — для демо регистрации/записи) ---
    regular = ensure_patient(
        db,
        login="user@test.ru",
        password="user123",
        full_name="Сидорова Мария Александровна",
        phone="+79161234567",
        email="user@test.ru",
        birth_date=date(1995, 3, 12),
        gender="женский",
    )

    # --- Демо-пациент с готовой консультацией (для скриншотов в дипломе) ---
    demo_patient = ensure_patient(
        db,
        login="patient@test.ru",
        password="patient123",
        full_name="Иванов Иван Иванович",
        phone="+79000000000",
        email="patient@test.ru",
    )

    doctor_user = ensure_user(db, "doctor@test.ru", "doctor123", UserRole.DOCTOR)
    doctor = db.query(Doctor).filter(Doctor.user_id == doctor_user.user_id).first()
    if not doctor:
        doctor = Doctor(
            user_id=doctor_user.user_id,
            full_name="Петров Петр Петрович",
            specialization="терапевт",
            experience=8,
            description="Врач-терапевт с 8-летним стажем. Дистанционные консультации по общим заболеваниям.",
            consultation_format=ConsultationFormat.CHAT,
        )
        db.add(doctor)
        db.flush()

    ensure_user(db, "admin@test.ru", "admin123", UserRole.ADMIN)

    # Второй врач для разнообразия
    doctor2_user = ensure_user(db, "doctor2@test.ru", "doctor123", UserRole.DOCTOR)
    if not db.query(Doctor).filter(Doctor.user_id == doctor2_user.user_id).first():
        db.add(
            Doctor(
                user_id=doctor2_user.user_id,
                full_name="Козлова Анна Сергеевна",
                specialization="педиатр",
                experience=5,
                description="Дистанционные консультации для детей и родителей.",
                consultation_format=ConsultationFormat.CHAT,
            )
        )
        db.flush()

    db.refresh(doctor)

    # Слоты — только если у врача ещё нет расписания
    existing_slots = db.query(ScheduleSlot).filter(ScheduleSlot.doctor_id == doctor.doctor_id).count()
    if existing_slots == 0:
        base = datetime(2026, 6, 15, 9, 0, tzinfo=timezone.utc)
        for day_offset in range(14):
            for hour in (9, 10, 11, 12, 14, 15, 16):
                start = base.replace(hour=hour) + timedelta(days=day_offset)
                end = start + timedelta(minutes=30)
                db.add(
                    ScheduleSlot(
                        doctor_id=doctor.doctor_id,
                        start_time=start,
                        end_time=end,
                        status=SlotStatus.FREE,
                    )
                )
        db.flush()

    # Демо-запись для Иванова — только если ещё нет
    demo_exists = (
        db.query(Appointment)
        .filter(
            Appointment.patient_id == demo_patient.patient_id,
            Appointment.complaint == "повышенная температура и кашель",
        )
        .first()
    )
    if not demo_exists:
        demo_slot = (
            db.query(ScheduleSlot)
            .filter(
                ScheduleSlot.doctor_id == doctor.doctor_id,
                ScheduleSlot.start_time == datetime(2026, 6, 15, 12, 0, tzinfo=timezone.utc),
            )
            .first()
        )
        if demo_slot is None:
            demo_slot = ScheduleSlot(
                doctor_id=doctor.doctor_id,
                start_time=datetime(2026, 6, 15, 12, 0, tzinfo=timezone.utc),
                end_time=datetime(2026, 6, 15, 12, 30, tzinfo=timezone.utc),
                status=SlotStatus.BUSY,
            )
            db.add(demo_slot)
            db.flush()
        else:
            demo_slot.status = SlotStatus.BUSY

        appointment = Appointment(
            patient_id=demo_patient.patient_id,
            doctor_id=doctor.doctor_id,
            slot_id=demo_slot.slot_id,
            complaint="повышенная температура и кашель",
            symptoms="температура 38.2, сухой кашель",
            comment="Симптомы 2 дня",
            status=AppointmentStatus.COMPLETED,
        )
        db.add(appointment)
        db.flush()

        consultation = Consultation(
            appointment_id=appointment.appointment_id,
            recommendation="контроль температуры, повторная консультация через 3 дня",
            result_text="рекомендован очный прием при ухудшении состояния",
            preliminary_assessment="ОРВИ, лёгкое течение",
            needs_in_person=False,
        )
        db.add(consultation)
        db.flush()

        db.add(
            Message(
                consultation_id=consultation.consultation_id,
                sender_id=demo_patient.user_id,
                text="Здравствуйте, доктор. Температура держится второй день.",
            )
        )
        db.add(
            Message(
                consultation_id=consultation.consultation_id,
                sender_id=doctor_user.user_id,
                text="Добрый день. Измеряйте температуру каждые 4 часа, пейте больше жидкости.",
            )
        )

        upload_dir = Path(__file__).resolve().parents[1] / "uploads"
        upload_dir.mkdir(exist_ok=True)
        demo_file_path = upload_dir / "analysis_demo.pdf"
        if not demo_file_path.exists():
            demo_file_path.write_bytes(b"%PDF-1.4 demo analysis file for telemedicine VKR")

        db.add(
            MedicalFile(
                appointment_id=appointment.appointment_id,
                file_name="analysis.pdf",
                file_path=str(demo_file_path),
                file_type="pdf",
            )
        )

    db.commit()
    db.close()

    print("Тестовые данные обновлены.")
    print("")
    print("  Обычный пользователь (Android, без истории):")
    print("    user@test.ru / user123  —  Сидорова Мария Александровна")
    print("")
    print("  Демо-пациент (готовая консультация для диплома):")
    print("    patient@test.ru / patient123  —  Иванов Иван Иванович")
    print("")
    print("  Врач:   doctor@test.ru / doctor123")
    print("  Админ:  admin@test.ru / admin123")


if __name__ == "__main__":
    seed()
