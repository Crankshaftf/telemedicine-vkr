package com.medconnect.ui.navigation

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val PROFILE = "profile"
    const val SPECIALIZATIONS = "specializations"
    const val DOCTORS = "doctors/{specialization}"
    const val DOCTOR_DETAIL = "doctor/{doctorId}"
    const val SLOTS = "slots/{doctorId}"
    const val CREATE_APPOINTMENT = "create/{doctorId}/{slotId}"
    const val CONFIRMATION = "confirmation/{appointmentId}"
    const val HISTORY = "history"
    const val APPOINTMENT_DETAIL = "appointment/{appointmentId}"
    const val CONSULTATION = "consultation/{appointmentId}"
    const val NOTIFICATIONS = "notifications"

    fun doctors(specialization: String) = "doctors/$specialization"
    fun doctorDetail(doctorId: Int) = "doctor/$doctorId"
    fun slots(doctorId: Int) = "slots/$doctorId"
    fun createAppointment(doctorId: Int, slotId: Int) = "create/$doctorId/$slotId"
    fun confirmation(appointmentId: Int) = "confirmation/$appointmentId"
    fun appointmentDetail(appointmentId: Int) = "appointment/$appointmentId"
    fun consultation(appointmentId: Int) = "consultation/$appointmentId"
}
