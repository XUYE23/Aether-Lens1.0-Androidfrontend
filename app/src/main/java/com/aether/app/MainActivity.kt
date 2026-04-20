package com.aether.app

import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.aether.app.data.IUserPreferencesRepository
import com.aether.app.data.UserPreferencesRepository
import com.aether.app.navigation.AetherNavGraph
import com.aether.app.navigation.Routes
import com.aether.app.navigation.determinePostSplashRoute
import com.aether.app.ui.theme.AetherTheme
import com.aether.app.ui.theme.Cream50
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    private val repository by lazy { UserPreferencesRepository(applicationContext) }

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AetherTheme {
                val userName by mainViewModel.userName.collectAsState()
                val avatarUriString by mainViewModel.avatarUriString.collectAsState()

                // The logo splash should always play on launch. We only decide where
                // to go AFTER the splash based on the persisted onboarding state.
                var postSplashDestination by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(Unit) {
                    val initialName = repository.userName.first()
                    postSplashDestination = determinePostSplashRoute(initialName)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Cream50)
                ) {
                    postSplashDestination?.let { destination ->
                        AetherNavGraph(
                            startDestination = Routes.SPLASH,
                            postSplashDestination = destination,
                            userName = userName,
                            avatarUriString = avatarUriString,
                            onSaveUserName = mainViewModel::updateUserName,
                            onSaveAvatarUri = { uri: Uri? -> mainViewModel.onAvatarSelected(uri) },
                            onSaveApiKey = { _ ->
                                // API key storage can be wired to DataStore or EncryptedSharedPreferences
                            }
                        )
                    }
                }
            }
        }
    }
}
