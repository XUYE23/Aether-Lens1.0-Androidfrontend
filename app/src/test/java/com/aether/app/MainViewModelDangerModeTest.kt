package com.aether.app

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.aether.app.data.ApiConfig
import com.aether.app.data.IUserPreferencesRepository

/**
 * 纯内存版 Repository，用于单元测试。
 * 所有数据由 MutableStateFlow 承载，不涉及任何文件 IO，
 * 彻底规避 Windows 上 DataStore File.renameTo() 文件锁问题。
 */
private class FakeUserPreferencesRepository : IUserPreferencesRepository {
    private val _userName = MutableStateFlow(IUserPreferencesRepository.DEFAULT_USER_NAME)
    override val userName: Flow<String> = _userName.asStateFlow()

    private val _avatarUriString = MutableStateFlow<String?>(null)
    override val avatarUriString: Flow<String?> = _avatarUriString.asStateFlow()

    private val _activeApiId = MutableStateFlow<String?>(null)
    override val activeApiId: Flow<String?> = _activeApiId.asStateFlow()

    private val _apiConfigsJson = MutableStateFlow<String?>(null)
    override val apiConfigsJson: Flow<String?> = _apiConfigsJson.asStateFlow()

    private val _hasSeenDangerWarning = MutableStateFlow(false)
    override val hasSeenDangerWarning: Flow<Boolean> = _hasSeenDangerWarning.asStateFlow()

    override suspend fun saveUserName(name: String) { _userName.value = name }
    override suspend fun saveAvatarUri(uriString: String) { _avatarUriString.value = uriString }
    override suspend fun saveActiveApiId(id: String) { _activeApiId.value = id }
    override suspend fun saveApiConfigs(configs: List<ApiConfig>) {
        _apiConfigsJson.value = configs.toString()
    }
    override suspend fun saveHasSeenDangerWarning(value: Boolean) {
        _hasSeenDangerWarning.value = value
    }
    override fun deserializeConfigs(json: String): List<ApiConfig> = emptyList()
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainViewModelDangerModeTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeUserPreferencesRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeUserPreferencesRepository()
        viewModel = MainViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        // TODO: Replace with lifecycle-runtime-testing TestViewModelStoreOwner when available.
        // We call ViewModel.clear() via reflection to stop the Eagerly-started StateFlow
        // collector. This is fragile — revisit if lifecycle library is upgraded past 2.7.0.
        val clearMethod = androidx.lifecycle.ViewModel::class.java.getDeclaredMethod("clear")
        clearMethod.isAccessible = true
        clearMethod.invoke(viewModel)
    }

    @Test
    fun `toggleDangerMode when hasSeenWarning is false shows dialog`() = runTest {
        viewModel.uiState.test {
            val initial = awaitItem()
            assertFalse(initial.isDangerModeActive)
            assertFalse(initial.showDangerWarningDialog)

            viewModel.toggleDangerMode()
            dispatcher.scheduler.advanceUntilIdle()

            val afterToggle = awaitItem()
            assertFalse(afterToggle.isDangerModeActive)
            assertTrue(afterToggle.showDangerWarningDialog)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `confirmDangerMode activates mode and dismisses dialog`() = runTest {
        viewModel.toggleDangerMode()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            skipItems(1)
            viewModel.confirmDangerMode()
            dispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertTrue(state.isDangerModeActive)
            assertFalse(state.showDangerWarningDialog)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleDangerMode when hasSeenWarning is true activates directly`() = runTest {
        viewModel.toggleDangerMode()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.confirmDangerMode()
        dispatcher.scheduler.advanceUntilIdle()
        // FakeRepository 使用 MutableStateFlow，写入是同步的，advanceUntilIdle 足够
        assertTrue(viewModel.hasSeenDangerWarning.first())
        viewModel.dismissDangerMode()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            skipItems(1)
            viewModel.toggleDangerMode()
            dispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertTrue(state.isDangerModeActive)
            assertFalse(state.showDangerWarningDialog)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dismissDangerMode deactivates mode`() = runTest {
        viewModel.toggleDangerMode()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.confirmDangerMode()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            skipItems(1)
            viewModel.dismissDangerMode()
            dispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertFalse(state.isDangerModeActive)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
