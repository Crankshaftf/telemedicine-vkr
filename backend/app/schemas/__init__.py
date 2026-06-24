from datetime import datetime
from typing import Optional

from pydantic import BaseModel, EmailStr, Field


# --- Auth ---

class RegisterRequest(BaseModel):
    full_name: str = Field(min_length=2, max_length=255)
    phone: str = Field(min_length=10, max_length=20)
    email: EmailStr
    password: str = Field(min_length=6, max_length=128)
    consent: bool = Field(description="Согласие на обработку персональных данных")


class LoginRequest(BaseModel):
    login: str = Field(description="Email или телефон")
    password: str


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"


class UserResponse(BaseModel):
    user_id: int
    login: str
    role: str
    status: str

    model_config = {"from_attributes": True}


# --- Profile ---

class ProfileResponse(BaseModel):
    user_id: int
    full_name: str
    birth_date: Optional[str] = None
    phone: str
    email: str
    gender: Optional[str] = None
    address: Optional[str] = None
    notify_appointments: bool = True
    notify_results: bool = True
    role: str


class ProfileUpdateRequest(BaseModel):
    full_name: Optional[str] = None
    birth_date: Optional[str] = None
    phone: Optional[str] = None
    email: Optional[EmailStr] = None
    gender: Optional[str] = None
    address: Optional[str] = None
    notify_appointments: Optional[bool] = None
    notify_results: Optional[bool] = None


# --- Doctors ---

class DoctorResponse(BaseModel):
    doctor_id: int
    full_name: str
    specialization: str
    experience: int
    description: Optional[str] = None
    consultation_format: str

    model_config = {"from_attributes": True}


class SlotResponse(BaseModel):
    slot_id: int
    doctor_id: int
    start_time: datetime
    end_time: datetime
    status: str

    model_config = {"from_attributes": True}


# --- Appointments ---

class AppointmentCreateRequest(BaseModel):
    doctor_id: int
    slot_id: int
    complaint: str = Field(min_length=3)
    symptoms: Optional[str] = None
    comment: Optional[str] = None


class AppointmentResponse(BaseModel):
    appointment_id: int
    doctor_id: int
    doctor_name: str
    specialization: str
    slot_id: int
    start_time: datetime
    end_time: datetime
    complaint: str
    symptoms: Optional[str] = None
    comment: Optional[str] = None
    status: str
    created_at: datetime
    has_consultation: bool = False


class AppointmentDetailResponse(AppointmentResponse):
    files: list["FileResponse"] = []
    consultation: Optional["ConsultationResponse"] = None


class StaffAppointmentResponse(AppointmentResponse):
    patient_name: str
    patient_phone: str


class SlotCreateRequest(BaseModel):
    doctor_id: int
    start_time: datetime
    end_time: datetime


class SlotStatusUpdateRequest(BaseModel):
    status: str = Field(description="free, busy, blocked")


class AppointmentStatusUpdateRequest(BaseModel):
    status: str = Field(description="created, confirmed, rescheduled, cancelled, completed")

class FileResponse(BaseModel):
    file_id: int
    file_name: str
    file_type: str
    uploaded_at: datetime

    model_config = {"from_attributes": True}


# --- Consultations ---

class MessageResponse(BaseModel):
    message_id: int
    sender_id: int
    sender_name: str
    text: str
    created_at: datetime

    model_config = {"from_attributes": True}


class MessageCreateRequest(BaseModel):
    text: str = Field(min_length=1, max_length=5000)


class ConsultationResultRequest(BaseModel):
    recommendation: str
    result_text: str
    preliminary_assessment: Optional[str] = None
    needs_in_person: bool = False


class ConsultationResponse(BaseModel):
    consultation_id: int
    appointment_id: int
    recommendation: Optional[str] = None
    result_text: Optional[str] = None
    preliminary_assessment: Optional[str] = None
    needs_in_person: bool = False
    created_at: datetime
    messages: list[MessageResponse] = []


# --- Notifications ---

class NotificationResponse(BaseModel):
    notification_id: int
    type: str
    text: str
    status: str
    created_at: datetime

    model_config = {"from_attributes": True}


# --- Reports ---

class ReportResponse(BaseModel):
    report_id: int
    period: str
    consultation_count: int
    no_show_count: int
    doctor_load: Optional[str] = None

    model_config = {"from_attributes": True}


class Message(BaseModel):
    message: str
