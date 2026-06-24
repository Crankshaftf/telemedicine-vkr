package com.medconnect.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
)

@Serializable
data class RegisterRequest(
    @SerialName("full_name") val fullName: String,
    val phone: String,
    val email: String,
    val password: String,
    val consent: Boolean = true,
)

@Serializable
data class LoginRequest(
    val login: String,
    val password: String,
)

@Serializable
data class ProfileResponse(
    @SerialName("user_id") val userId: Int,
    @SerialName("full_name") val fullName: String,
    @SerialName("birth_date") val birthDate: String? = null,
    val phone: String,
    val email: String,
    val gender: String? = null,
    val address: String? = null,
    @SerialName("notify_appointments") val notifyAppointments: Boolean = true,
    @SerialName("notify_results") val notifyResults: Boolean = true,
    val role: String,
)

@Serializable
data class ProfileUpdateRequest(
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("birth_date") val birthDate: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val gender: String? = null,
    val address: String? = null,
    @SerialName("notify_appointments") val notifyAppointments: Boolean? = null,
    @SerialName("notify_results") val notifyResults: Boolean? = null,
)

@Serializable
data class DoctorResponse(
    @SerialName("doctor_id") val doctorId: Int,
    @SerialName("full_name") val fullName: String,
    val specialization: String,
    val experience: Int,
    val description: String? = null,
    @SerialName("consultation_format") val consultationFormat: String,
)

@Serializable
data class SlotResponse(
    @SerialName("slot_id") val slotId: Int,
    @SerialName("doctor_id") val doctorId: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val status: String,
)

@Serializable
data class AppointmentCreateRequest(
    @SerialName("doctor_id") val doctorId: Int,
    @SerialName("slot_id") val slotId: Int,
    val complaint: String,
    val symptoms: String? = null,
    val comment: String? = null,
)

@Serializable
data class AppointmentResponse(
    @SerialName("appointment_id") val appointmentId: Int,
    @SerialName("doctor_id") val doctorId: Int,
    @SerialName("doctor_name") val doctorName: String,
    val specialization: String,
    @SerialName("slot_id") val slotId: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val complaint: String,
    val symptoms: String? = null,
    val comment: String? = null,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("has_consultation") val hasConsultation: Boolean = false,
)

@Serializable
data class FileResponse(
    @SerialName("file_id") val fileId: Int,
    @SerialName("file_name") val fileName: String,
    @SerialName("file_type") val fileType: String,
    @SerialName("uploaded_at") val uploadedAt: String,
)

@Serializable
data class MessageResponse(
    @SerialName("message_id") val messageId: Int,
    @SerialName("sender_id") val senderId: Int,
    @SerialName("sender_name") val senderName: String,
    val text: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class MessageCreateRequest(val text: String)

@Serializable
data class ConsultationResponse(
    @SerialName("consultation_id") val consultationId: Int,
    @SerialName("appointment_id") val appointmentId: Int,
    val recommendation: String? = null,
    @SerialName("result_text") val resultText: String? = null,
    @SerialName("preliminary_assessment") val preliminaryAssessment: String? = null,
    @SerialName("needs_in_person") val needsInPerson: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    val messages: List<MessageResponse> = emptyList(),
)

@Serializable
data class AppointmentDetailResponse(
    @SerialName("appointment_id") val appointmentId: Int,
    @SerialName("doctor_id") val doctorId: Int,
    @SerialName("doctor_name") val doctorName: String,
    val specialization: String,
    @SerialName("slot_id") val slotId: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val complaint: String,
    val symptoms: String? = null,
    val comment: String? = null,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("has_consultation") val hasConsultation: Boolean = false,
    val files: List<FileResponse> = emptyList(),
    val consultation: ConsultationResponse? = null,
)

@Serializable
data class NotificationResponse(
    @SerialName("notification_id") val notificationId: Int,
    val type: String,
    val text: String,
    val status: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class ApiMessage(val message: String)

@Serializable
data class ApiErrorDetail(val detail: String)
