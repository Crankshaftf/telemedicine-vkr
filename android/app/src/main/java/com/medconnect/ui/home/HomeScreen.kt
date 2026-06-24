package com.medconnect.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medconnect.MedConnectApp
import com.medconnect.data.model.ProfileResponse
import com.medconnect.ui.components.MedMenuItem
import com.medconnect.ui.components.MedScaffold
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repo = MedConnectApp.instance.repository
    var profile by mutableStateOf<ProfileResponse?>(null)

    init { loadProfile() }

    fun loadProfile() {
        viewModelScope.launch {
            repo.getProfile().onSuccess { profile = it }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.logout()
            onDone()
        }
    }
}

data class HomeMenuItem(val title: String, val subtitle: String, val icon: ImageVector, val route: String)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
) {
    val items = listOf(
        HomeMenuItem("Записаться", "Выбор врача и время", Icons.Default.CalendarMonth, "book"),
        HomeMenuItem("История", "Обращения и заключения", Icons.Default.History, "history"),
        HomeMenuItem("Уведомления", "Записи и результаты", Icons.Default.Notifications, "notifications"),
        HomeMenuItem("Профиль", "Личные данные", Icons.Default.Person, "profile"),
    )

    MedScaffold(
        title = "МедКоннект",
        actions = {
            IconButton(onClick = { viewModel.logout(onLogout) }) {
                Icon(Icons.Default.Logout, contentDescription = "Выйти")
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Добро пожаловать", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            viewModel.profile?.fullName ?: "Пациент",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            items(items) { item ->
                MedMenuItem(
                    title = item.title,
                    subtitle = item.subtitle,
                    icon = item.icon,
                    onClick = {
                        when (item.route) {
                            "book" -> onNavigate("specializations")
                            else -> onNavigate(item.route)
                        }
                    },
                )
            }
        }
    }
}
