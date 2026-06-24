package com.medconnect.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.medconnect.ui.appointment.AppointmentViewModel
import com.medconnect.ui.appointment.ConfirmationScreen
import com.medconnect.ui.appointment.CreateAppointmentScreen
import com.medconnect.ui.auth.AuthViewModel
import com.medconnect.ui.auth.LoginScreen
import com.medconnect.ui.auth.RegisterScreen
import com.medconnect.ui.auth.SplashScreen
import com.medconnect.ui.consultation.ConsultationScreen
import com.medconnect.ui.consultation.ConsultationViewModel
import com.medconnect.ui.doctors.*
import com.medconnect.ui.history.AppointmentDetailScreen
import com.medconnect.ui.history.HistoryScreen
import com.medconnect.ui.history.HistoryViewModel
import com.medconnect.ui.home.HomeScreen
import com.medconnect.ui.home.HomeViewModel
import com.medconnect.ui.notifications.NotificationsScreen
import com.medconnect.ui.notifications.NotificationsViewModel
import com.medconnect.ui.profile.ProfileScreen
import com.medconnect.ui.profile.ProfileViewModel

@Composable
fun MedConnectNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen { loggedIn ->
                navController.navigate(if (loggedIn) Routes.HOME else Routes.LOGIN) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }
        }

        composable(Routes.LOGIN) {
            val vm: AuthViewModel = viewModel()
            LoginScreen(
                viewModel = vm,
                onSuccess = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onRegister = { navController.navigate(Routes.REGISTER) },
            )
        }

        composable(Routes.REGISTER) {
            val vm: AuthViewModel = viewModel()
            RegisterScreen(
                viewModel = vm,
                onSuccess = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel()
            HomeScreen(
                viewModel = vm,
                onNavigate = { route ->
                    when (route) {
                        "specializations" -> navController.navigate(Routes.SPECIALIZATIONS)
                        "history" -> navController.navigate(Routes.HISTORY)
                        "notifications" -> navController.navigate(Routes.NOTIFICATIONS)
                        "profile" -> navController.navigate(Routes.PROFILE)
                    }
                },
                onLogout = {
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                },
            )
        }

        composable(Routes.PROFILE) {
            val vm: ProfileViewModel = viewModel()
            ProfileScreen(vm) { navController.popBackStack() }
        }

        composable(Routes.SPECIALIZATIONS) {
            val vm: DoctorsViewModel = viewModel()
            SpecializationsScreen(
                viewModel = vm,
                onSelect = { spec -> navController.navigate(Routes.doctors(spec)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            Routes.DOCTORS,
            arguments = listOf(navArgument("specialization") { type = NavType.StringType }),
        ) { entry ->
            val spec = entry.arguments?.getString("specialization") ?: return@composable
            val vm: DoctorsViewModel = viewModel()
            DoctorsListScreen(
                specialization = spec,
                viewModel = vm,
                onSelect = { id -> navController.navigate(Routes.slots(id)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            Routes.SLOTS,
            arguments = listOf(navArgument("doctorId") { type = NavType.IntType }),
        ) { entry ->
            val doctorId = entry.arguments?.getInt("doctorId") ?: return@composable
            val vm: DoctorsViewModel = viewModel()
            SlotsScreen(
                doctorId = doctorId,
                viewModel = vm,
                onSelect = { slotId -> navController.navigate(Routes.createAppointment(doctorId, slotId)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            Routes.CREATE_APPOINTMENT,
            arguments = listOf(
                navArgument("doctorId") { type = NavType.IntType },
                navArgument("slotId") { type = NavType.IntType },
            ),
        ) { entry ->
            val doctorId = entry.arguments?.getInt("doctorId") ?: return@composable
            val slotId = entry.arguments?.getInt("slotId") ?: return@composable
            val vm: AppointmentViewModel = viewModel()
            CreateAppointmentScreen(
                doctorId = doctorId,
                slotId = slotId,
                viewModel = vm,
                onCreated = { id -> navController.navigate(Routes.confirmation(id)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            Routes.CONFIRMATION,
            arguments = listOf(navArgument("appointmentId") { type = NavType.IntType }),
        ) { entry ->
            val id = entry.arguments?.getInt("appointmentId") ?: return@composable
            ConfirmationScreen(
                appointmentId = id,
                onHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.HISTORY) {
            val vm: HistoryViewModel = viewModel()
            HistoryScreen(
                viewModel = vm,
                onOpenDetail = { id -> navController.navigate(Routes.appointmentDetail(id)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            Routes.APPOINTMENT_DETAIL,
            arguments = listOf(navArgument("appointmentId") { type = NavType.IntType }),
        ) { entry ->
            val id = entry.arguments?.getInt("appointmentId") ?: return@composable
            val vm: HistoryViewModel = viewModel()
            AppointmentDetailScreen(
                appointmentId = id,
                viewModel = vm,
                onConsultation = { apptId -> navController.navigate(Routes.consultation(apptId)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            Routes.CONSULTATION,
            arguments = listOf(navArgument("appointmentId") { type = NavType.IntType }),
        ) { entry ->
            val id = entry.arguments?.getInt("appointmentId") ?: return@composable
            val vm: ConsultationViewModel = viewModel()
            ConsultationScreen(
                appointmentId = id,
                viewModel = vm,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.NOTIFICATIONS) {
            val vm: NotificationsViewModel = viewModel()
            NotificationsScreen(vm) { navController.popBackStack() }
        }
    }
}
