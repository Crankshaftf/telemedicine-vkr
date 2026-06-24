package com.medconnect.ui.appointment

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medconnect.MedConnectApp
import com.medconnect.data.model.AppointmentResponse
import com.medconnect.ui.components.*
import kotlinx.coroutines.launch
import java.io.File

class AppointmentViewModel : ViewModel() {
    private val repo = MedConnectApp.instance.repository
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var createdAppointment by mutableStateOf<AppointmentResponse?>(null)
    var uploadedFileName by mutableStateOf<String?>(null)

    fun create(
        doctorId: Int,
        slotId: Int,
        complaint: String,
        symptoms: String,
        comment: String,
        onSuccess: (Int) -> Unit,
    ) {
        viewModelScope.launch {
            isLoading = true
            error = null
            repo.createAppointment(doctorId, slotId, complaint, symptoms.ifBlank { null }, comment.ifBlank { null })
                .onSuccess {
                    createdAppointment = it
                    onSuccess(it.appointmentId)
                }
                .onFailure { error = it.message ?: "Ошибка создания записи" }
            isLoading = false
        }
    }

    fun uploadFile(appointmentId: Int, file: File, mimeType: String) {
        viewModelScope.launch {
            isLoading = true
            repo.uploadFile(appointmentId, file, mimeType)
                .onSuccess { uploadedFileName = it.fileName; error = null }
                .onFailure { error = it.message }
            isLoading = false
        }
    }
}

@Composable
fun CreateAppointmentScreen(
    doctorId: Int,
    slotId: Int,
    viewModel: AppointmentViewModel,
    onCreated: (Int) -> Unit,
    onBack: () -> Unit,
) {
    var complaint by remember { mutableStateOf("") }
    var symptoms by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var appointmentId by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val id = appointmentId ?: return@let
            val input = context.contentResolver.openInputStream(it) ?: return@let
            val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}")
            tempFile.outputStream().use { out -> input.copyTo(out) }
            val mime = context.contentResolver.getType(it) ?: "application/octet-stream"
            viewModel.uploadFile(id, tempFile, mime)
        }
    }

    MedScaffold(title = "Новая заявка", onBack = onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            MedCard {
                SectionTitle("Жалобы и симптомы")
                OutlinedTextField(
                    complaint, { complaint = it },
                    label = { Text("Жалобы *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = MaterialTheme.shapes.medium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    symptoms, { symptoms = it },
                    label = { Text("Симптомы") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = MaterialTheme.shapes.medium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    comment, { comment = it },
                    label = { Text("Комментарий") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
            }
            Spacer(Modifier.height(16.dp))
            ErrorBanner(viewModel.error)

            if (appointmentId == null) {
                Spacer(Modifier.height(8.dp))
                MedButton(
                    "Создать заявку",
                    {
                        viewModel.create(doctorId, slotId, complaint, symptoms, comment) { id ->
                            appointmentId = id
                        }
                    },
                    enabled = complaint.length >= 3 && !viewModel.isLoading,
                )
            } else {
                Spacer(Modifier.height(12.dp))
                MedCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Заявка №$appointmentId создана", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { filePicker.launch("application/pdf") },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Прикрепить файл")
                    }
                    viewModel.uploadedFileName?.let {
                        Spacer(Modifier.height(8.dp))
                        Text("Загружен: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(16.dp))
                MedButton("Подтверждение записи", { onCreated(appointmentId!!) })
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun ConfirmationScreen(appointmentId: Int, onHome: () -> Unit, onBack: () -> Unit) {
    MedScaffold(title = "Готово", onBack = onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Text("Запись подтверждена!", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("Номер заявки: $appointmentId", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Вы получите уведомление перед консультацией.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            MedButton("На главную", onHome)
        }
    }
}
