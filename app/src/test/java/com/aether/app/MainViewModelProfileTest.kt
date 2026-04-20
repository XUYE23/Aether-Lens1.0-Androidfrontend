package com.aether.app

import android.net.Uri
import com.aether.app.data.ApiConfig
import com.aether.app.data.IUserPreferencesRepository
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class FakeProfilePrefsRepo : IUserPreferencesRepository {
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
class MainViewModelProfileTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeProfilePrefsRepo
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeProfilePrefsRepo()
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
    fun `updateUserName trims whitespace before persisting`() = runTest {
        viewModel.updateUserName("  Aurora  ")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Aurora", repository.userName.first())
    }

    @Test
    fun `updateUserName ignores blank values`() = runTest {
        viewModel.updateUserName("   ")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(IUserPreferencesRepository.DEFAULT_USER_NAME, repository.userName.first())
    }

    @Test
    fun `onAvatarSelected stores avatar uri string`() = runTest {
        val uri = Uri.parse("content://media/external/images/media/42")

        viewModel.onAvatarSelected(uri)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(uri.toString(), repository.avatarUriString.first())
    }

    @Test
    fun `onAvatarSelected ignores null uri`() = runTest {
        viewModel.onAvatarSelected(null)
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(repository.avatarUriString.first())
    }
}
