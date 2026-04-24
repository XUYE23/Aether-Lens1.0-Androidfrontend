package com.aether.app.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aether.app.DeviceUiState
import com.aether.app.PersonalDeviceUiState
import com.aether.app.data.ApiConfig
import com.aether.app.data.IUserPreferencesRepository
import com.aether.app.data.ScannedDevice
import com.aether.app.ui.screens.AetherWorkspaceScreen
import com.aether.app.ui.screens.DeviceDetailScreen
import com.aether.app.ui.screens.OnboardStep1Name
import com.aether.app.ui.screens.OnboardStep2Voiceprint
import com.aether.app.ui.screens.OnboardStep3CorePortrait
import com.aether.app.ui.screens.OnboardStep4Bluetooth
import com.aether.app.ui.screens.OnboardStep5Api
import com.aether.app.ui.screens.OnboardStep6Permissions
import com.aether.app.ui.screens.PersonalSpaceScreen
import com.aether.app.ui.screens.ProductPhilosophyScreen
import com.aether.app.ui.screens.SplashScreen

object Routes {
    const val SPLASH    = "splash"
    const val ONBOARD_1 = "onboard/1"
    const val ONBOARD_2 = "onboard/2"
    const val ONBOARD_3 = "onboard/3"
    const val ONBOARD_4 = "onboard/4"
    const val ONBOARD_5 = "onboard/5"
    const val ONBOARD_6 = "onboard/6"
    const val WORKSPACE = "workspace"
    const val PERSONAL  = "personal"
    const val PRODUCT_PHILOSOPHY = "product/philosophy"
    const val DEVICE_DETAIL = "device/detail"
    const val DEVICE_SWITCH = "device/switch"
}

fun determinePostSplashRoute(userName: String): String {
    return if (
        userName.isBlank() ||
        userName == IUserPreferencesRepository.DEFAULT_USER_NAME
    ) {
        Routes.ONBOARD_1
    } else {
        Routes.WORKSPACE
    }
}

@Composable
fun AetherNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.SPLASH,
    postSplashDestination: String = Routes.WORKSPACE,
    onSaveUserName: (String) -> Unit = {},
    avatarUriString: String? = null,
    deviceUiState: PersonalDeviceUiState = PersonalDeviceUiState(),
    apiConfigs: List<ApiConfig> = emptyList(),
    activeApiId: String? = null,
    boundDeviceUiState: DeviceUiState = DeviceUiState(),
    onSaveAvatarUri: (Uri?) -> Unit = {},
    onSelectApi: (String) -> Unit = {},
    onAddApi: (ApiConfig) -> Unit = {},
    onUpdateApi: (ApiConfig) -> Unit = {},
    onDeleteApi: (String) -> Unit = {},
    onDisconnectDevice: (String) -> Unit = {},
    onConnectPrimaryDevice: (ScannedDevice) -> Unit = {},
    onStartBluetoothScan: () -> Unit = {},
    onStopBluetoothScan: () -> Unit = {},
    onBluetoothPermissionDenied: () -> Unit = {},
    onSaveApiKey: (String) -> Unit = {},
    userName: String = "",
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(onComplete = {
                navController.navigate(postSplashDestination) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            })
        }

        composable(Routes.ONBOARD_1) {
            OnboardStep1Name(onNext = { name ->
                onSaveUserName(name)
                navController.navigate(Routes.ONBOARD_2)
            })
        }

        composable(Routes.ONBOARD_2) {
            OnboardStep2Voiceprint(
                onNext = { navController.navigate(Routes.ONBOARD_3) },
                onSkip = { navController.navigate(Routes.ONBOARD_3) }
            )
        }

        composable(Routes.ONBOARD_3) {
            OnboardStep3CorePortrait(
                onNext = { navController.navigate(Routes.ONBOARD_4) },
                onSkip = { navController.navigate(Routes.ONBOARD_4) }
            )
        }

        composable(Routes.ONBOARD_4) {
            OnboardStep4Bluetooth(
                onNext = { navController.navigate(Routes.ONBOARD_5) },
                onSkip = { navController.navigate(Routes.ONBOARD_5) }
            )
        }

        composable(Routes.ONBOARD_5) {
            OnboardStep5Api(onNext = { apiKey ->
                onSaveApiKey(apiKey)
                navController.navigate(Routes.ONBOARD_6)
            })
        }

        composable(Routes.ONBOARD_6) {
            OnboardStep6Permissions(onNext = {
                navController.navigate(Routes.WORKSPACE) {
                    popUpTo(Routes.ONBOARD_1) { inclusive = true }
                }
            })
        }

        composable(Routes.WORKSPACE) {
            AetherWorkspaceScreen(
                userName = userName,
                avatarUriString = avatarUriString,
                onNavigateToPersonal = {
                    navController.navigate(Routes.PERSONAL) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.PERSONAL) {
            PersonalSpaceScreen(
                userName = userName,
                avatarUriString = avatarUriString,
                deviceUiState = deviceUiState,
                apiConfigs = apiConfigs,
                activeApiId = activeApiId,
                onSaveUserName = onSaveUserName,
                onSaveAvatarUri = onSaveAvatarUri,
                onSelectApi = onSelectApi,
                onAddApi = onAddApi,
                onUpdateApi = onUpdateApi,
                onDeleteApi = onDeleteApi,
                onOpenDeviceDetail = {
                    navController.navigate(Routes.DEVICE_DETAIL)
                },
                onOpenProductPhilosophy = {
                    navController.navigate(Routes.PRODUCT_PHILOSOPHY)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PRODUCT_PHILOSOPHY) {
            ProductPhilosophyScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.DEVICE_DETAIL) {
            DeviceDetailScreen(
                userName = userName,
                deviceUiState = boundDeviceUiState,
                onBack = { navController.popBackStack() },
                onDisconnectDevice = onDisconnectDevice,
                onOpenSwitchDevice = { navController.navigate(Routes.DEVICE_SWITCH) }
            )
        }

        composable(Routes.DEVICE_SWITCH) {
            DeviceDetailScreen(
                userName = userName,
                deviceUiState = boundDeviceUiState,
                mode = com.aether.app.ui.screens.DeviceDetailMode.Switch,
                onBack = { navController.popBackStack() },
                onDismissSwitch = { navController.popBackStack() },
                onStartBluetoothScan = onStartBluetoothScan,
                onStopBluetoothScan = onStopBluetoothScan,
                onBluetoothPermissionDenied = onBluetoothPermissionDenied,
                onConnectPrimaryDevice = { scannedDevice ->
                    onConnectPrimaryDevice(scannedDevice)
                    navController.popBackStack()
                }
            )
        }
    }
}
