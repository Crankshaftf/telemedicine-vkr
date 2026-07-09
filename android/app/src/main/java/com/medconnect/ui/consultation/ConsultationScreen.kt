package com.medconnect.ui.consultation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medconnect.MedConnectApp
import com.medconnect.data.model.ConsultationResponse
import com.medconnect.ui.components.*
import com.medconnect.util.formatDateTime
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class ConsultationViewModel : ViewModel() {
    private val repo = MedConnectApp.instance.repository
    var consultation by mutableStateOf<ConsultationResponse?>(null)
    var currentUserId by mutableStateOf<Int?>(null)
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun load(appointmentId: Int) {
        viewModelScope.launch {
            isLoading = true
            error = null
            val profileJob = async { repo.getProfile() }
            val consultationJob = async { repo.getConsultation(appointmentId) }
            profileJob.await().onSuccess { currentUserId = it.userId }
            consultationJob.await()
                .onSuccess { consultation = it; error = null }
                .onFailure { error = it.message }
            isLoading = false
        }
    }

    fun sendMessage(text: String) {
        val c = consultation ?: return
        viewModelScope.launch {
            repo.sendMessage(c.consultationId, text)
                .onSuccess { msg ->
                    consultation = c.copy(messages = c.messages + msg)
                    error = null
                }
                .onFailure { error = it.message }
        }
    }
}

@Composable
fun ConsultationScreen(
    appointmentId: Int,
    viewModel: ConsultationViewModel,
    onBack: () -> Unit,
) {
    LaunchedEffect(appointmentId) { viewModel.load(appointmentId) }
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val messages = viewModel.consultation?.messages ?: emptyList()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Box(Modifier.fillMaxSize().imePadding()) {
    MedScaffold(
        title = "Консультация",
        onBack = onBack,
        bottomBar = {
            ChatInputBar(
                value = messageText,
                onValueChange = { messageText = it },
                onSend = {
                    viewModel.sendMessage(messageText.trim())
                    messageText = ""
                },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when {
                viewModel.isLoading -> LoadingState()
                viewModel.consultation == null && !viewModel.error.isNullOrBlank() -> {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    ) {
                        ErrorBanner(viewModel.error)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.load(appointmentId) }) {
                            Text("Повторить")
                        }
                    }
                }
                messages.isEmpty() -> EmptyState("Начните диалог с врачом")
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(messages, key = { it.messageId }) { msg ->
                        ChatBubble(
                            text = msg.text,
                            senderName = msg.senderName,
                            time = formatDateTime(msg.createdAt),
                            isOwnMessage = msg.senderId == viewModel.currentUserId,
                        )
                    }
                }
            }
            if (!viewModel.error.isNullOrBlank() && viewModel.consultation != null) {
                Box(
                    Modifier
                        .align(androidx.compose.ui.Alignment.TopCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                ) {
                    ErrorBanner(viewModel.error)
                }
            }
        }
    }
    } // Box imePadding
}
