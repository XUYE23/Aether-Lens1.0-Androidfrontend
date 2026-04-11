package com.aether.app

import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import com.aether.app.data.UserPreferencesRepository

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainViewModelDangerModeTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: UserPreferencesRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = UserPreferencesRepository(ApplicationProvider.getApplicationContext())
        viewModel = MainViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
