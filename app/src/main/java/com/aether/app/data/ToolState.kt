package com.aether.app.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class ToolState(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val iconTint: Color,
    val isAuthorized: Boolean
)

// TODO: 替换 MainViewModel.toggleToolAuthorization() 中的模拟逻辑，
//       通过此接口接入 OAuth2.0 或各平台 SDK 授权流程。
interface AuthRepository {
    suspend fun authorize(toolId: String): Result<Unit>
    suspend fun revoke(toolId: String): Result<Unit>
}
