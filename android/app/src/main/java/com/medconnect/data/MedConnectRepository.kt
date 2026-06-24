package com.medconnect.data

import com.medconnect.BuildConfig
import com.medconnect.data.api.MedConnectApi
import com.medconnect.data.local.TokenStore
import com.medconnect.data.model.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

class MedConnectRepository(
    private val tokenStore: TokenStore,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private var cachedToken: String? = null

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        cachedToken?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        chain.proceed(requestBuilder.build())
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val api: MedConnectApi = retrofit.create(MedConnectApi::class.java)

    suspend fun initToken() {
        cachedToken = tokenStore.tokenFlow.first()
    }

    val isLoggedIn: Boolean get() = !cachedToken.isNullOrBlank()

    /** Проверяет сохранённый токен; при ошибке очищает сессию. */
    suspend fun validateSession(): Boolean {
        if (cachedToken.isNullOrBlank()) return false
        val ok = getProfile().isSuccess
        if (!ok) clearSession()
        return ok
    }

    suspend fun clearSession() {
        cachedToken = null
        tokenStore.clearToken()
    }

    private suspend fun <T> apiCall(block: suspend () -> T): Result<T> =
        runCatching { block() }.mapError()

    private fun <T> Result<T>.mapError(): Result<T> =
        exceptionOrNull()?.let { Result.failure(Exception(it.toUserMessage(), it)) } ?: this

    suspend fun register(fullName: String, phone: String, email: String, password: String): Result<Unit> =
        apiCall {
            clearSession()
            val response = api.register(RegisterRequest(fullName, phone, email, password, consent = true))
            saveToken(response.accessToken)
        }

    suspend fun login(login: String, password: String): Result<Unit> = apiCall {
        clearSession()
        val response = api.login(LoginRequest(login, password))
        saveToken(response.accessToken)
    }

    suspend fun logout() {
        runCatching { api.logout() }
        clearSession()
    }

    suspend fun getProfile(): Result<ProfileResponse> = apiCall { api.getProfile() }

    suspend fun updateProfile(body: ProfileUpdateRequest): Result<ProfileResponse> =
        apiCall { api.updateProfile(body) }

    suspend fun getSpecializations(): Result<List<String>> = apiCall { api.getSpecializations() }

    suspend fun getDoctors(specialization: String? = null): Result<List<DoctorResponse>> =
        apiCall { api.getDoctors(specialization) }

    suspend fun getDoctor(doctorId: Int): Result<DoctorResponse> = apiCall { api.getDoctor(doctorId) }

    suspend fun getSchedule(doctorId: Int): Result<List<SlotResponse>> =
        apiCall { api.getSchedule(doctorId) }

    suspend fun createAppointment(
        doctorId: Int,
        slotId: Int,
        complaint: String,
        symptoms: String?,
        comment: String?,
    ): Result<AppointmentResponse> = apiCall {
        api.createAppointment(
            AppointmentCreateRequest(doctorId, slotId, complaint, symptoms, comment)
        )
    }

    suspend fun getHistory(): Result<List<AppointmentResponse>> = apiCall { api.getHistory() }

    suspend fun getAppointmentDetail(appointmentId: Int): Result<AppointmentDetailResponse> =
        apiCall { api.getAppointment(appointmentId) }

    suspend fun cancelAppointment(appointmentId: Int): Result<AppointmentResponse> =
        apiCall { api.cancelAppointment(appointmentId) }

    suspend fun uploadFile(appointmentId: Int, file: File, mimeType: String): Result<FileResponse> =
        apiCall {
            val appointmentBody = appointmentId.toString().toRequestBody("text/plain".toMediaType())
            val fileBody = file.asRequestBody(mimeType.toMediaType())
            val part = MultipartBody.Part.createFormData("file", file.name, fileBody)
            api.uploadFile(appointmentBody, part)
        }

    suspend fun getConsultation(appointmentId: Int): Result<ConsultationResponse> =
        apiCall { api.getConsultation(appointmentId) }

    suspend fun sendMessage(consultationId: Int, text: String): Result<MessageResponse> =
        apiCall { api.sendMessage(consultationId, MessageCreateRequest(text)) }

    suspend fun getNotifications(): Result<List<NotificationResponse>> =
        apiCall { api.getNotifications() }

    suspend fun markNotificationRead(id: Int): Result<NotificationResponse> =
        apiCall { api.markNotificationRead(id) }

    private suspend fun saveToken(token: String) {
        cachedToken = token
        tokenStore.saveToken(token)
    }
}
