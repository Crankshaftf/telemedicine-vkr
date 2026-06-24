package com.medconnect.ui.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medconnect.MedConnectApp
import com.medconnect.data.model.NotificationResponse
import com.medconnect.ui.components.*
import com.medconnect.util.formatDateTime
import kotlinx.coroutines.launch

class NotificationsViewModel : ViewModel() {
    private val repo = MedConnectApp.instance.repository
    var notifications by mutableStateOf<List<NotificationResponse>>(emptyList())
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun load() {
        viewModelScope.launch {
            isLoading = true
            repo.getNotifications().onSuccess { notifications = it; error = null }.onFailure { error = it.message }
            isLoading = false
        }
    }

    fun markRead(id: Int) {
        viewModelScope.launch {
            repo.markNotificationRead(id).onSuccess {
                notifications = notifications.map { n ->
                    if (n.notificationId == id) n.copy(status = "read") else n
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(viewModel: NotificationsViewModel, onBack: () -> Unit) {
    LaunchedEffect(Unit) { viewModel.load() }

    MedScaffold(title = "Уведомления", onBack = onBack) { padding ->
        when {
            viewModel.isLoading -> LoadingState(Modifier.padding(padding))
            viewModel.notifications.isEmpty() -> EmptyState("Уведомлений нет", Modifier.padding(padding))
            else -> LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { ErrorBanner(viewModel.error) }
                items(viewModel.notifications, key = { it.notificationId }) { n ->
                    val isUnread = n.status == "unread"
                    ElevatedCard(
                        onClick = { if (isUnread) viewModel.markRead(n.notificationId) },
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (isUnread) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            if (isUnread) {
                                Text("Новое", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(4.dp))
                            }
                            Text(n.text, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(4.dp))
                            Text(formatDateTime(n.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
