package at.mcbabo.authenticator.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.toRoute
import at.mcbabo.authenticator.ui.screen.AddOTPScreen
import at.mcbabo.authenticator.ui.screen.AuthScreen
import at.mcbabo.authenticator.ui.screen.CameraScreen
import at.mcbabo.authenticator.ui.screen.EditOTPScreen
import at.mcbabo.authenticator.ui.screen.FirstLaunchScreen
import at.mcbabo.authenticator.ui.screen.ImportAccountsScreen
import at.mcbabo.authenticator.ui.screen.SettingsScreen
import at.mcbabo.authenticator.ui.screen.ViewOTPScreen
import at.mcbabo.authenticator.ui.viewmodel.PreferenceViewModel
import kotlinx.serialization.Serializable

const val DEBUG = false

@Composable
fun Navigation(
    navController: NavHostController,
    viewModel: PreferenceViewModel = hiltViewModel()
) {
    val isFirstLaunch by viewModel.firstLaunch.collectAsState(false)
    val isLockEnabled by viewModel.lockEnabled.collectAsState(false)

    val startDestination = when {
        isFirstLaunch -> ScreenFirstLaunch
        else -> if (isLockEnabled) ScreenAuth else ScreenOtpList
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        animatedComposable<ScreenFirstLaunch> {
            FirstLaunchScreen {
                viewModel.setFirstLaunch(false)
                navController.navigate(ScreenOtpList) {
                    popUpTo(ScreenFirstLaunch) { inclusive = true }
                }
            }
        }

        animatedComposable<ScreenAuth> {
            AuthScreen {
                navController.navigate(ScreenOtpList) {
                    popUpTo(ScreenAuth) { inclusive = true }
                }
            }
        }

        animatedComposable<ScreenOtpList> {
            ViewOTPScreen(
                onAddQRCode = {
                    navController.navigate(ScreenCamera)
                },
                onAddManual = {
                    navController.navigate(ScreenAddOtpAccount(null))
                },
                onAccountClick = {
                    navController.navigate(ScreenOtpEdit(it))
                },
                onNavigateSettings = {
                    navController.navigate(ScreenSettings)
                }
            )
        }

        animatedComposable<ScreenOtpEdit> {
            val args = it.toRoute<ScreenOtpEdit>()
            EditOTPScreen(
                accountId = args.accountId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        animatedComposable<ScreenAddOtpAccount> {
            val args = it.toRoute<ScreenAddOtpAccount>()
            AddOTPScreen(args.qrCode, { navController.popBackStack() })
        }

        animatedComposable<ScreenCamera> {
            CameraScreen(
                onAddAccount = { data ->
                    navController.navigate(ScreenAddOtpAccount(data)) {
                        popUpTo(ScreenCamera) { inclusive = true }
                    }
                },
                onImportAccounts = { data ->
                    navController.navigate(ScreenImportAccounts(data)) {
                        popUpTo(ScreenCamera) { inclusive = true }
                    }
                }
            ) {
                navController.popBackStack()
            }
        }

        animatedComposable<ScreenImportAccounts> {
            val args = it.toRoute<ScreenImportAccounts>()
            ImportAccountsScreen(args.qrCode, { navController.popBackStack() })
        }

        animatedComposable<ScreenSettings> {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}

@Serializable
object ScreenFirstLaunch

@Serializable
object ScreenAuth

@Serializable
object ScreenOtpList

@Serializable
data class ScreenOtpEdit(val accountId: Long)

@Serializable
data class ScreenAddOtpAccount(val qrCode: String? = null)

@Serializable
object ScreenCamera

@Serializable
data class ScreenImportAccounts(val qrCode: String)

@Serializable
object ScreenSettings