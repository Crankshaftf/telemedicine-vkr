"""Auto-seed: создаёт тестовых пользователей и свежие слоты при каждом старте сервера."""

from datetime import date, datetime, timedelta, timezone

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


def _ensure_user(db, login: str, password: str, role: UserRole) -> User:
    user = db.query(User).filter(User.login == login).first()
    if user:
        return user
    user = User(login=login, password_hash=hash_password(password), role=role, status=UserStatus.ACTIVE)
    db.add(user)
    db.flush()
    return user


def _ensure_patient(db, login, password, full_name, phone, email, birth_date=None, gender=None) -> Patient:
    user = _ensure_user(db, login, password, UserRole.PATIENT)
    patient = db.query(Patient).filter(Patient.user_id == user.user_id).first()
    if patient:
        return patient
    patient = Patient(user_id=user.user_id, full_name=full_name, phone=phone, email=email,
                      birth_date=birth_date, gender=gender)
    db.add(patient)
    db.flush()
    return patient


def run_seed():
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()
    try:
        # Тестовый пациент для демо
        _ensure_patient(db, "user@test.ru", "user123", "Сидорова Мария Александровна",
                        "+79161234567", "user@test.ru", birth_date=date(1995, 3, 12), gender="женский")

        # Демо-пациент с готовой консультацией
        demo_patient = _ensure_patient(db, "patient@test.ru", "patient123",
                                       "Иванов Иван Иванович", "+79000000000", "patient@test.ru")

        # Врач
        doctor_user = _ensure_user(db, "doctor@test.ru", "doctor123", UserRole.DOCTOR)
        doctor = db.query(Doctor).filter(Doctor.user_id == doctor_user.user_id).first()
        if not doctor:
            doctor = Doctor(user_id=doctor_user.user_id, full_name="Петров Петр Петрович",
                            specialization="терапевт", experience=8,
                            description="Врач-терапевт с 8-летним стажем. Дистанционные консультации по общим заболеваниям.",
                            consultation_format=ConsultationFormat.CHAT)
            db.add(doctor)
            db.flush()

        # Второй врач
        d2_user = _ensure_user(db, "doctor2@test.ru", "doctor123", UserRole.DOCTOR)
        if not db.query(Doctor).filter(Doctor.user_id == d2_user.user_id).first():
            db.add(Doctor(user_id=d2_user.user_id, full_name="Козлова Анна Сергеевна",
                          specialization="педиатр", experience=5,
                          description="Дистанционные консультации для детей и родителей.",
                          consultation_format=ConsultationFormat.CHAT))
            db.flush()

        # Администратор
        _ensure_user(db, "admin@test.ru", "admin123", UserRole.ADMIN)

        db.refresh(doctor)

        # Слоты: пересоздать если нет будущих свободных
        now_utc = datetime.now(timezone.utc)
        future_free = (db.query(ScheduleSlot)
                       .filter(ScheduleSlot.doctor_id == doctor.doctor_id,
                               ScheduleSlot.status == SlotStatus.FREE,
                               ScheduleSlot.start_time > now_utc)
                       .count())

        if future_free == 0:
            base_day = now_utc.date()
            if now_utc.hour >= 8:
                base_day += timedelta(days=1)
            base = datetime(base_day.year, base_day.month, base_day.day, 9, 0, tzinfo=timezone.utc)
            for day_offset in range(14):
                day = base + timedelta(days=day_offset)
                if day.weekday() >= 5:
                    continue
                for hour in (9, 10, 11, 12, 14, 15, 16):
                    start = day.replace(hour=hour)
                    end = start + timedelta(minutes=30)
                    exists = db.query(ScheduleSlot).filter(
                        ScheduleSlot.doctor_id == doctor.doctor_id,
                        ScheduleSlot.start_time == start).first()
                    if not exists:
                        db.add(ScheduleSlot(doctor_id=doctor.doctor_id,
                                            start_time=start, end_time=end, status=SlotStatus.FREE))
            db.flush()

        # Демо-запись для Иванова (если нет)
        demo_exists = db.query(Appointment).filter(
            Appointment.patient_id == demo_patient.patient_id,
            Appointment.complaint == "повышенная температура и кашель").first()

        if not demo_exists:
            demo_slot = db.query(ScheduleSlot).filter(
                ScheduleSlot.doctor_id == doctor.doctor_id,
                ScheduleSlot.start_time == datetime(2026, 6, 15, 12, 0, tzinfo=timezone.utc)).first()
            if demo_slot is None:
                demo_slot = ScheduleSlot(doctor_id=doctor.doctor_id,
                                         start_time=datetime(2026, 6, 15, 12, 0, tzinfo=timezone.utc),
                                         end_time=datetime(2026, 6, 15, 12, 30, tzinfo=timezone.utc),
                                         status=SlotStatus.BUSY)
                db.add(demo_slot)
                db.flush()
            else:
                demo_slot.status = SlotStatus.BUSY

            appointment = Appointment(patient_id=demo_patient.patient_id, doctor_id=doctor.doctor_id,
                                      slot_id=demo_slot.slot_id,
                                      complaint="повышенная температура и кашель",
                                      symptoms="температура 38.2, сухой кашель",
                                      comment="Симптомы 2 дня",
                                      status=AppointmentStatus.COMPLETED)
            db.add(appointment)
            db.flush()

            consultation = Consultation(appointment_id=appointment.appointment_id,
                                        recommendation="контроль температуры, повторная консультация через 3 дня",
                                        result_text="рекомендован очный приём при ухудшении состояния",
                                        preliminary_assessment="ОРВИ, лёгкое течение",
                                        needs_in_person=False)
            db.add(consultation)
            db.flush()

            db.add(Message(consultation_id=consultation.consultation_id,
                           sender_id=demo_patient.user_id,
                           text="Здравствуйте, доктор. Температура держится второй день."))
            db.add(Message(consultation_id=consultation.consultation_id,
                           sender_id=doctor_user.user_id,
                           text="Добрый день. Измеряйте температуру каждые 4 часа, пейте больше жидкости."))

        db.commit()
        print("Startup seed: OK")
    except Exception as e:
        db.rollback()
        print(f"Startup seed warning: {e}")
    finally:
        db.close()
