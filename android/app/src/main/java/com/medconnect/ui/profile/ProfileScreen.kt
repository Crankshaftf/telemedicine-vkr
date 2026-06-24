package com.medconnect.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medconnect.MedConnectApp
import com.medconnect.data.model.ProfileResponse
import com.medconnect.data.model.ProfileUpdateRequest
import com.medconnect.ui.components.*
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val repo = MedConnectApp.instance.repository
    var profile by mutableStateOf<ProfileResponse?>(null)
    var isLoading by mutableStateOf(true)
    var isSaving by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var saved by mutableStateOf(false)

    init { load() }

    fun load() {
        viewModelScope.launch {
            isLoading = true
            repo.getProfile()
                .onSuccess { profile = it; error = null }
                .onFailure { error = it.message }
            isLoading = false
        }
    }

    fun save(
        fullName: String,
        phone: String,
        email: String,
        birthDate: String,
        gender: String,
        address: String,
        notifyAppointments: Boolean,
        notifyResults: Boolean,
    ) {
        viewModelScope.launch {
            isSaving = true
            saved = false
            repo.updateProfile(
                ProfileUpdateRequest(
                    fullName = fullName.ifBlank { null },
                    phone = phone.ifBlank { null },
                    email = email.ifBlank { null },
                    birthDate = birthDate.ifBlank { null },
                    gender = gender.ifBlank { null },
                    address = address.ifBlank { null },
                    notifyAppointments = notifyAppointments,
                    notifyResults = notifyResults,
                )
            ).onSuccess { profile = it; saved = true; error = null }
                .onFailure { error = it.message; saved = false }
            isSaving = false
        }
    }
}

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, onBack: () -> Unit) {
    val p = viewModel.profile
    var fullName by remember(p) { mutableStateOf(p?.fullName ?: "") }
    var phone by remember(p) { mutableStateOf(p?.phone ?: "") }
    var email by remember(p) { mutableStateOf(p?.email ?: "") }
    var birthDate by remember(p) { mutableStateOf(p?.birthDate ?: "") }
    var gender by remember(p) { mutableStateOf(p?.gender ?: "") }
    var address by remember(p) { mutableStateOf(p?.address ?: "") }
    var notifyAppointments by remember(p) { mutableStateOf(p?.notifyAppointments ?: true) }
    var notifyResults by remember(p) { mutableStateOf(p?.notifyResults ?: true) }

    MedScaffold(title = "Профиль", onBack = onBack) { padding ->
        when {
            viewModel.isLoading -> LoadingState(Modifier.padding(padding))
            else -> Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                MedCard {
                    SectionTitle("Личные данные")
                    OutlinedTextField(fullName, { fullName = it }, label = { Text("ФИО") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(phone, { phone = it }, label = { Text("Телефон") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(birthDate, { birthDate = it }, label = { Text("Дата рождения") }, placeholder = { Text("ГГГГ-ММ-ДД") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(gender, { gender = it }, label = { Text("Пол") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(address, { address = it }, label = { Text("Адрес") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                }
                Spacer(Modifier.height(12.dp))
                MedCard {
                    SectionTitle("Уведомления")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("О записях")
                        Switch(notifyAppointments, { notifyAppointments = it })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("О результатах")
                        Switch(notifyResults, { notifyResults = it })
                    }
                }
                Spacer(Modifier.height(16.dp))
                ErrorBanner(viewModel.error)
                if (viewModel.saved) {
                    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                        Text("Сохранено", modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
                Spacer(Modifier.height(16.dp))
                MedButton(
                    "Сохранить",
                    {
                        viewModel.save(fullName, phone, email, birthDate, gender, address, notifyAppointments, notifyResults)
                    },
                    enabled = !viewModel.isSaving,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
