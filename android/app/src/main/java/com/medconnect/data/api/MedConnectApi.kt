package com.medconnect.data.api

import com.medconnect.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface MedConnectApi {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): TokenResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): TokenResponse

    @POST("auth/logout")
    suspend fun logout()

    @GET("profile")
    suspend fun getProfile(): ProfileResponse

    @PUT("profile")
    suspend fun updateProfile(@Body body: ProfileUpdateRequest): ProfileResponse

    @GET("specializations")
    suspend fun getSpecializations(): List<String>

    @GET("doctors")
    suspend fun getDoctors(@Query("specialization") specialization: String? = null): List<DoctorResponse>

    @GET("doctors/{id}")
    suspend fun getDoctor(@Path("id") doctorId: Int): DoctorResponse

    @GET("schedule/{doctorId}")
    suspend fun getSchedule(@Path("doctorId") doctorId: Int): List<SlotResponse>

    @POST("appointments")
    suspend fun createAppointment(@Body body: AppointmentCreateRequest): AppointmentResponse

    @GET("appointments/history")
    suspend fun getHistory(): List<AppointmentResponse>

    @GET("appointments/{id}")
    suspend fun getAppointment(@Path("id") appointmentId: Int): AppointmentDetailResponse

    @PUT("appointments/{id}/cancel")
    suspend fun cancelAppointment(@Path("id") appointmentId: Int): AppointmentResponse

    @Multipart
    @POST("files")
    suspend fun uploadFile(
        @Part("appointment_id") appointmentId: RequestBody,
        @Part file: MultipartBody.Part,
    ): FileResponse

    @GET("consultations/{appointmentId}")
    suspend fun getConsultation(@Path("appointmentId") appointmentId: Int): ConsultationResponse

    @POST("consultations/{id}/message")
    suspend fun sendMessage(
        @Path("id") consultationId: Int,
        @Body body: MessageCreateRequest,
    ): MessageResponse

    @GET("notifications")
    suspend fun getNotifications(): List<NotificationResponse>

    @PUT("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") notificationId: Int): NotificationResponse
}
