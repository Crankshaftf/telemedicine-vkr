package com.medconnect.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medconnect.MedConnectApp
import com.medconnect.data.model.AppointmentDetailResponse
import com.medconnect.data.model.AppointmentResponse
import com.medconnect.ui.components.*
import com.medconnect.util.formatDateTime
import com.medconnect.util.statusLabel
import kotlinx.coroutines.launch

class HistoryViewModel : ViewModel() {
    private val repo = MedConnectApp.instance.repository
    var appointments by mutableStateOf<List<AppointmentResponse>>(emptyList())
    var detail by mutableStateOf<AppointmentDetailResponse?>(null)
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun loadHistory() {
        viewModelScope.launch {
            isLoading = true
            repo.getHistory().onSuccess { appointments = it; error = null }.onFailure { error = it.message }
            isLoading = false
        }
    }

    fun loadDetail(appointmentId: Int) {
        viewModelScope.launch {
            isLoading = true
            repo.getAppointmentDetail(appointmentId).onSuccess { detail = it; error = null }.onFailure { error = it.message }
            isLoading = false
        }
    }
}

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onOpenDetail: (Int) -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.loadHistory() }

    MedScaffold(title = "История обращений", onBack = onBack) { padding ->
        when {
            viewModel.isLoading -> LoadingState(Modifier.padding(padding))
            viewModel.appointments.isEmpty() -> EmptyState("Обращений пока нет", Modifier.padding(padding))
            else -> LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { ErrorBanner(viewModel.error) }
                items(viewModel.appointments, key = { it.appointmentId }) { appt ->
                    MedCard(onClick = { onOpenDetail(appt.appointmentId) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(appt.doctorName, style = MaterialTheme.typography.titleMedium)
                                Text(appt.specialization, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                Text(formatDateTime(appt.startTime), style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(8.dp))
                                StatusChip(appt.status)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppointmentDetailScreen(
    appointmentId: Int,
    viewModel: HistoryViewModel,
    onConsultation: (Int) -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(appointmentId) { viewModel.loadDetail(appointmentId) }
    val appt = viewModel.detail

    MedScaffold(
        title = "Обращение",
        onBack = onBack,
        bottomBar = {
            if (appt != null && appt.status != "cancelled" && appt.status != "completed") {
                Surface(tonalElevation = 3.dp) {
                    MedButton(
                        text = "Перейти в консультацию",
                        onClick = { onConsultation(appt.appointmentId) },
                        modifier = Modifier.padding(16.dp).navigationBarsPadding(),
                    )
                }
            }
        },
    ) { padding ->
        when {
            viewModel.isLoading -> LoadingState(Modifier.padding(padding))
            appt == null -> EmptyState("Данные не найдены", Modifier.padding(padding))
            else -> Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(appt.doctorName, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    StatusChip(appt.status)
                }
                Text(appt.specialization, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))

                MedCard {
                    SectionTitle("Информация")
                    InfoRow("Дата", formatDateTime(appt.startTime))
                    InfoRow("Жалобы", appt.complaint)
                    appt.symptoms?.let { InfoRow("Симптомы", it) }
                    appt.comment?.let { InfoRow("Комментарий", it) }
                }

                if (appt.files.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    MedCard {
                        SectionTitle("Файлы")
                        appt.files.forEach { Text("• ${it.fileName}", style = MaterialTheme.typography.bodyMedium) }
                    }
                }

                appt.consultation?.let { c ->
                    if (c.resultText != null || c.recommendation != null) {
                        Spacer(Modifier.height(12.dp))
                        MedCard {
                            SectionTitle("Заключение врача")
                            c.preliminaryAssessment?.let { InfoRow("Оценка", it) }
                            c.resultText?.let { InfoRow("Результат", it) }
                            c.recommendation?.let { InfoRow("Рекомендации", it) }
                            if (c.needsInPerson) {
                                Spacer(Modifier.height(8.dp))
                                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.small) {
                                    Text(
                                        "Рекомендован очный приём",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}
