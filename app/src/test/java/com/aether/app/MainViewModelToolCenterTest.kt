package com.aether.app

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.aether.app.data.ApiConfig
import com.aether.app.data.IUserPreferencesRepository

private class FakePrefsRepo : IUserPreferencesRepository {
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
    override suspend fun saveApiConfigs(configs: List<ApiConfig>) { _apiConfigsJson.value = configs.toString() }
    override suspend fun saveHasSeenDangerWarning(value: Boolean) { _hasSeenDangerWarning.value = value }
    override fun deserializeConfigs(json: String): List<ApiConfig> = emptyList()
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainViewModelToolCenterTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakePrefsRepo
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakePrefsRepo()
        viewModel = MainViewModel(repository)
    }

    @After
    fun tearDown() {
        val clearMethod = androidx.lifecycle.ViewModel::class.java.getDeclaredMethod("clear")
        clearMethod.isAccessible = true
        clearMethod.invoke(viewModel)
        Dispatchers.resetMain()
    }

    @Test
    fun `initial toolItems list is non-empty`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.toolItems.isNotEmpty())
            assertTrue(state.toolItems.all { !it.isAuthorized })
            assertNull(state.toolConnectingMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleToolAuthorization on unauthorized tool authorizes it and sets connecting message`() = runTest {
        val targetId = viewModel.uiState.value.toolItems.first().id
        viewModel.uiState.test {
            skipItems(1)
            viewModel.toggleToolAuthorization(targetId)
            dispatcher.scheduler.advanceUntilIdle()
            val state = awaitItem()
            val tool = state.toolItems.find { it.id == targetId }!!
            assertTrue(tool.isAuthorized)
            assertNotNull(state.toolConnectingMessage)
            assertTrue(state.toolConnectingMessage!!.contains(tool.name))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleToolAuthorization on authorized tool revokes authorization`() = runTest {
        val targetId = viewModel.uiState.value.toolItems.first().id
        viewModel.toggleToolAuthorization(targetId)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.test {
            skipItems(1)
            viewModel.toggleToolAuthorization(targetId)
            dispatcher.scheduler.advanceUntilIdle()
            val state = awaitItem()
            val tool = state.toolItems.find { it.id == targetId }!!
            assertFalse(tool.isAuthorized)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearToolConnectingMessage sets message to null`() = runTest {
        val targetId = viewModel.uiState.value.toolItems.first().id
        viewModel.toggleToolAuthorization(targetId)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.uiState.test {
            skipItems(1)
            viewModel.clearToolConnectingMessage()
            dispatcher.scheduler.advanceUntilIdle()
            val state = awaitItem()
            assertNull(state.toolConnectingMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleToolAuthorization with unknown id does not change state`() = runTest {
        viewModel.uiState.test {
            skipItems(1)
            viewModel.toggleToolAuthorization("no_such_tool")
            dispatcher.scheduler.advanceUntilIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
