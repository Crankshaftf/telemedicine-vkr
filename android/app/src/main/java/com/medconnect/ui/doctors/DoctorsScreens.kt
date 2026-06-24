package com.medconnect.ui.doctors

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medconnect.MedConnectApp
import com.medconnect.data.model.DoctorResponse
import com.medconnect.data.model.SlotResponse
import com.medconnect.ui.components.*
import com.medconnect.util.formatDateTime
import kotlinx.coroutines.launch

class DoctorsViewModel : ViewModel() {
    private val repo = MedConnectApp.instance.repository
    var specializations by mutableStateOf<List<String>>(emptyList())
    var doctors by mutableStateOf<List<DoctorResponse>>(emptyList())
    var slots by mutableStateOf<List<SlotResponse>>(emptyList())
    var selectedDoctor by mutableStateOf<DoctorResponse?>(null)
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun loadSpecializations() {
        viewModelScope.launch {
            isLoading = true
            repo.getSpecializations().onSuccess { specializations = it; error = null }.onFailure { error = it.message }
            isLoading = false
        }
    }

    fun loadDoctors(specialization: String) {
        viewModelScope.launch {
            isLoading = true
            repo.getDoctors(specialization).onSuccess { doctors = it; error = null }.onFailure { error = it.message }
            isLoading = false
        }
    }

    fun loadDoctor(doctorId: Int) {
        viewModelScope.launch {
            isLoading = true
            repo.getDoctor(doctorId).onSuccess { selectedDoctor = it; error = null }.onFailure { error = it.message }
            isLoading = false
        }
    }

    fun loadSlots(doctorId: Int) {
        viewModelScope.launch {
            isLoading = true
            repo.getSchedule(doctorId).onSuccess { slots = it; error = null }.onFailure { error = it.message }
            isLoading = false
        }
    }
}

@Composable
fun SpecializationsScreen(viewModel: DoctorsViewModel, onSelect: (String) -> Unit, onBack: () -> Unit) {
    LaunchedEffect(Unit) { viewModel.loadSpecializations() }

    MedScaffold(title = "Специализация", onBack = onBack) { padding ->
        when {
            viewModel.isLoading -> LoadingState(Modifier.padding(padding))
            viewModel.specializations.isEmpty() -> EmptyState("Специализации не найдены", Modifier.padding(padding))
            else -> LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { ErrorBanner(viewModel.error) }
                items(viewModel.specializations) { spec ->
                    MedCard(onClick = { onSelect(spec) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(spec, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DoctorsListScreen(
    specialization: String,
    viewModel: DoctorsViewModel,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(specialization) { viewModel.loadDoctors(specialization) }

    MedScaffold(title = "Врачи", onBack = onBack) { padding ->
        when {
            viewModel.isLoading -> LoadingState(Modifier.padding(padding))
            viewModel.doctors.isEmpty() -> EmptyState("Врачи не найдены", Modifier.padding(padding))
            else -> LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        specialization,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    ErrorBanner(viewModel.error)
                }
                items(viewModel.doctors, key = { it.doctorId }) { doctor ->
                    MedCard(onClick = { onSelect(doctor.doctorId) }) {
                        Text(doctor.fullName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${doctor.specialization} · стаж ${doctor.experience} лет",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        doctor.description?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text(
                                if (doctor.consultationFormat == "video") "Видео" else "Чат",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DoctorDetailScreen(
    doctorId: Int,
    viewModel: DoctorsViewModel,
    onBook: () -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(doctorId) { viewModel.loadDoctor(doctorId) }
    val doctor = viewModel.selectedDoctor

    MedScaffold(title = "Карточка врача", onBack = onBack) { padding ->
        if (viewModel.isLoading) {
            LoadingState(Modifier.padding(padding))
        } else if (doctor != null) {
            Column(Modifier.padding(padding).padding(16.dp)) {
                Text(doctor.fullName, style = MaterialTheme.typography.headlineSmall)
                Text(doctor.specialization, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                MedCard {
                    InfoRow("Стаж", "${doctor.experience} лет")
                    doctor.description?.let { InfoRow("О враче", it) }
                    InfoRow("Формат", if (doctor.consultationFormat == "video") "Видеосвязь" else "Текстовый чат")
                }
                Spacer(Modifier.height(24.dp))
                MedButton("Выбрать время", onBook)
            }
        }
    }
}

@Composable
fun SlotsScreen(
    doctorId: Int,
    viewModel: DoctorsViewModel,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(doctorId) { viewModel.loadSlots(doctorId) }

    MedScaffold(title = "Дата и время", onBack = onBack) { padding ->
        when {
            viewModel.isLoading -> LoadingState(Modifier.padding(padding))
            viewModel.slots.isEmpty() -> EmptyState("Нет свободных слотов", Modifier.padding(padding))
            else -> LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { ErrorBanner(viewModel.error) }
                items(viewModel.slots, key = { it.slotId }) { slot ->
                    MedCard(onClick = { onSelect(slot.slotId) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(formatDateTime(slot.startTime), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}
