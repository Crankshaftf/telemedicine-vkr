package com.medconnect.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medconnect.MedConnectApp
import com.medconnect.ui.components.*
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repo = MedConnectApp.instance.repository
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun login(login: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            error = null
            repo.login(login.trim(), password)
                .onSuccess { onSuccess() }
                .onFailure { error = it.message ?: "Ошибка входа" }
            isLoading = false
        }
    }

    fun register(
        fullName: String,
        phone: String,
        email: String,
        password: String,
        consent: Boolean,
        onSuccess: () -> Unit,
    ) {
        if (!consent) {
            error = "Необходимо согласие на обработку данных"
            return
        }
        viewModelScope.launch {
            isLoading = true
            error = null
            repo.register(fullName.trim(), phone.trim(), email.trim(), password)
                .onSuccess { onSuccess() }
                .onFailure { error = it.message ?: "Ошибка регистрации" }
            isLoading = false
        }
    }
}

@Composable
fun SplashScreen(onNavigate: (loggedIn: Boolean) -> Unit) {
    val repo = MedConnectApp.instance.repository
    LaunchedEffect(Unit) {
        repo.initToken()
        val loggedIn = repo.validateSession()
        onNavigate(loggedIn)
    }
    Box(Modifier.fillMaxSize()) {
        MedBrandHeader("Телемедицина")
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            CircularProgressIndicator(
                modifier = Modifier.padding(bottom = 64.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun LoginScreen(viewModel: AuthViewModel, onSuccess: () -> Unit, onRegister: () -> Unit) {
    var login by remember { mutableStateOf("user@test.ru") }
    var password by remember { mutableStateOf("user123") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MedBrandHeader("Вход в личный кабинет")
        MedScreenContent {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                login, { login = it },
                label = { Text("Email или телефон") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                password, { password = it },
                label = { Text("Пароль") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(12.dp))
            ErrorBanner(viewModel.error)
            Spacer(Modifier.height(16.dp))
            MedButton("Войти", { viewModel.login(login, password, onSuccess) }, enabled = !viewModel.isLoading)
            TextButton(onClick = onRegister, modifier = Modifier.fillMaxWidth()) {
                Text("Нет аккаунта? Зарегистрироваться")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun RegisterScreen(viewModel: AuthViewModel, onSuccess: () -> Unit, onBack: () -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }

    MedScaffold(title = "Регистрация", onBack = onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(fullName, { fullName = it }, label = { Text("ФИО") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(phone, { phone = it }, label = { Text("Телефон") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                password, { password = it },
                label = { Text("Пароль") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = consent, onCheckedChange = { consent = it })
                Text(
                    "Согласие на обработку персональных данных",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            ErrorBanner(viewModel.error)
            Spacer(Modifier.height(16.dp))
            MedButton(
                "Зарегистрироваться",
                { viewModel.register(fullName, phone, email, password, consent, onSuccess) },
                enabled = !viewModel.isLoading,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
