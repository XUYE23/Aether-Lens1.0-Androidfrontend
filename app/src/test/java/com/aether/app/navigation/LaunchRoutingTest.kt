package com.aether.app.navigation

import com.aether.app.data.IUserPreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchRoutingTest {

    @Test
    fun `post splash route goes to onboarding when name is blank`() {
        assertEquals(Routes.ONBOARD_1, determinePostSplashRoute(""))
    }

    @Test
    fun `post splash route goes to onboarding when name is default placeholder`() {
        assertEquals(
            Routes.ONBOARD_1,
            determinePostSplashRoute(IUserPreferencesRepository.DEFAULT_USER_NAME)
        )
    }

    @Test
    fun `post splash route goes to workspace when name is customized`() {
        assertEquals(Routes.WORKSPACE, determinePostSplashRoute("Aurora"))
    }
}
