package com.aether.app

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aether.app.data.ApiConfig
import com.aether.app.data.IUserPreferencesRepository
import com.aether.app.data.ToolState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ══════════════════════════════════════════════════════════════════════════════
//  UI State（瞬态交互状态，不持久化）
//  持久化数据（userName / avatarUri）由 Repository 的 StateFlow 单独提供
// ══════════════════════════════════════════════════════════════════════════════

private fun defaultToolList() = listOf(
    ToolState(id = "feishu",   name = "飞书", icon = Icons.AutoMirrored.Filled.Send, iconTint = Color(0xFF2B5BFF), isAuthorized = false),
    ToolState(id = "calendar", name = "日历", icon = Icons.Default.DateRange,         iconTint = Color(0xFF4CAF50), isAuthorized = false),
    ToolState(id = "email",    name = "邮件", icon = Icons.Default.Email,             iconTint = Color(0xFF9C27B0), isAuthorized = false),
)

data class PersonalSpaceUiState(
    val profileTraits: List<String> = listOf(
        "Agent Tech 极客",
        "三角洲行动老兵",
        "偏好简洁直接的沟通风格"
    ),
    val isCardExpanded: Boolean = false,
    // ── 添加个性化信息弹窗 ────────────────────────────────────────────────────
    val isDialogVisible: Boolean = false,
    val dialogInput: String = "",
    val isSubmitting: Boolean = false,
    val inputError: Boolean = false,
    val shakeSignal: Int = 0,
    // ── 修改昵称弹窗 ─────────────────────────────────────────────────────────
    val showEditNameDialog: Boolean = false,
    val editNameInput: String = "",
    val editNameError: Boolean = false,
    // ── API 配置 ─────────────────────────────────────────────────────────────
    val apiConfigs: List<ApiConfig> = listOf(
        ApiConfig(
            id = "mock-openai",
            providerName = "OpenAI",
            apiKey = "sk-mock-openai",
            requestUrl = "https://api.openai.com/v1/chat/completions"
        ),
        ApiConfig(
            id = "mock-deepseek",
            providerName = "DeepSeek",
            apiKey = "sk-mock-deepseek",
            requestUrl = "https://api.deepseek.com/v1/chat/completions",
            remark = "性价比首选"
        ),
        ApiConfig(
            id = "mock-moonshot",
            providerName = "Moonshot",
            apiKey = "sk-mock-moonshot",
            requestUrl = "https://api.moonshot.cn/v1/chat/completions",
            websiteUrl = "https://moonshot.cn"
        )
    ),
    val activeApiId: String? = null,
    // ── 危险模式 ──────────────────────────────────────────────────────────────────
    val isDangerModeActive: Boolean = false,
    val showDangerWarningDialog: Boolean = false,
    // ── 工具中心 ──────────────────────────────────────────────────────────────────
    val toolItems: List<ToolState> = defaultToolList(),
    val toolConnectingMessage: String? = null,
)

// ══════════════════════════════════════════════════════════════════════════════
//  MainViewModel — Activity 作用域，DataStore 为单一数据源
// ══════════════════════════════════════════════════════════════════════════════

class MainViewModel(
    private val repository: IUserPreferencesRepository
) : ViewModel() {

    // ── 持久化状态（来自 DataStore，转为 StateFlow 供 UI 收集） ───────────────

    /** 当前用户昵称，工作台和个人空间共享此同一数据源 */
    val userName: StateFlow<String> = repository.userName.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = IUserPreferencesRepository.DEFAULT_USER_NAME
    )

    /** 当前头像 URI 字符串（null 表示未设置） */
    val avatarUriString: StateFlow<String?> = repository.avatarUriString.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    /** 是否已看过危险模式警告（持久化）；初始值 false 为安全默认 */
    val hasSeenDangerWarning: StateFlow<Boolean> = repository.hasSeenDangerWarning.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    // ── 瞬态 UI 状态 ─────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow(PersonalSpaceUiState())
    val uiState: StateFlow<PersonalSpaceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val json      = repository.apiConfigsJson.first()
            val savedId   = repository.activeApiId.first()
            val configs   = if (json != null) repository.deserializeConfigs(json)
                            else _uiState.value.apiConfigs
            _uiState.update { it.copy(apiConfigs = configs, activeApiId = savedId) }
        }
    }

    // ── 头像选择 ─────────────────────────────────────────────────────────────

    /**
     * 接收已申请过 persistable 权限的 URI，持久化存储。
     * takePersistableUriPermission 由调用方（Composable）负责，
     * 此处只写入字符串。
     */
    fun onAvatarSelected(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            repository.saveAvatarUri(uri.toString())
        }
    }

    // ── 折叠卡片 ─────────────────────────────────────────────────────────────

    fun toggleCard() {
        _uiState.update { it.copy(isCardExpanded = !it.isCardExpanded) }
    }

    // ── 添加个性化信息 ────────────────────────────────────────────────────────

    fun openDialog() {
        _uiState.update {
            it.copy(isDialogVisible = true, dialogInput = "", inputError = false)
        }
    }

    fun dismissDialog() {
        _uiState.update {
            it.copy(
                isDialogVisible = false,
                dialogInput = "",
                inputError = false,
                isSubmitting = false
            )
        }
    }

    fun onDialogInputChange(text: String) {
        _uiState.update { it.copy(dialogInput = text, inputError = false) }
    }

    /**
     * 模拟提交：delay 1000ms；长度 > 3 合法，否则触发 shake + 清空。
     */
    fun submitTrait() {
        val input = _uiState.value.dialogInput
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, inputError = false) }
            delay(1_000L)

            if (input.length > 3) {
                _uiState.update { state ->
                    state.copy(
                        profileTraits = state.profileTraits + polishTrait(input),
                        isDialogVisible = false,
                        dialogInput = "",
                        inputError = false,
                        isSubmitting = false
                    )
                }
            } else {
                _uiState.update { state ->
                    state.copy(
                        inputError = true,
                        isSubmitting = false,
                        dialogInput = "",
                        shakeSignal = state.shakeSignal + 1
                    )
                }
            }
        }
    }

    private fun polishTrait(raw: String): String = "✦ $raw"

    // ── 修改昵称 ─────────────────────────────────────────────────────────────

    fun openEditNameDialog() {
        _uiState.update {
            it.copy(
                showEditNameDialog = true,
                editNameInput = userName.value,
                editNameError = false
            )
        }
    }

    fun dismissEditNameDialog() {
        _uiState.update {
            it.copy(showEditNameDialog = false, editNameInput = "", editNameError = false)
        }
    }

    fun onEditNameInputChange(text: String) {
        _uiState.update { it.copy(editNameInput = text, editNameError = false) }
    }

    // ── API 配置管理 ──────────────────────────────────────────────────────────

    fun selectApi(id: String) {
        _uiState.update { it.copy(activeApiId = id) }
        viewModelScope.launch { repository.saveActiveApiId(id) }
    }

    fun addApiConfig(config: ApiConfig) {
        val updated = _uiState.value.apiConfigs + config
        _uiState.update { it.copy(apiConfigs = updated) }
        viewModelScope.launch { repository.saveApiConfigs(updated) }
    }

    fun saveUserName() {
        val trimmed = _uiState.value.editNameInput.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(editNameError = true) }
            return
        }
        viewModelScope.launch {
            repository.saveUserName(trimmed)           // 写入 DataStore（持久化）
        }
        _uiState.update {
            it.copy(showEditNameDialog = false, editNameInput = "", editNameError = false)
        }
    }

    // ── 危险模式 ──────────────────────────────────────────────────────────────────

    /**
     * Switch 点击入口：
     *   - 若已激活 → 关闭（同 dismissDangerMode）
     *   - 若未看过警告 → 显示弹窗
     *   - 若已看过警告 → 直接激活
     */
    fun toggleDangerMode() {
        if (_uiState.value.isDangerModeActive) {
            dismissDangerMode()
            return
        }
        if (hasSeenDangerWarning.value) {
            _uiState.update { it.copy(isDangerModeActive = true) }
        } else {
            _uiState.update { it.copy(showDangerWarningDialog = true) }
        }
    }

    /** 用户在警告弹窗中点击"确认并开启" */
    fun confirmDangerMode() {
        // DataStore write is async — in the rare case the process is killed before
        // this coroutine completes, hasSeenDangerWarning won't be persisted and the
        // user will see the warning dialog once more on next launch. Accepted tradeoff.
        viewModelScope.launch {
            repository.saveHasSeenDangerWarning(true)
        }
        _uiState.update {
            it.copy(isDangerModeActive = true, showDangerWarningDialog = false)
        }
    }

    /** 关闭危险模式（Switch OFF 或弹窗取消） */
    fun dismissDangerMode() {
        _uiState.update {
            it.copy(isDangerModeActive = false, showDangerWarningDialog = false)
        }
    }

    // ── 工具中心 ──────────────────────────────────────────────────────────────────

    fun toggleToolAuthorization(id: String) {
        val tool = _uiState.value.toolItems.find { it.id == id } ?: return
        val message = if (!tool.isAuthorized) "正在联通 ${tool.name}..." else "${tool.name} 已断开"
        _uiState.update { state ->
            state.copy(
                toolItems = state.toolItems.map {
                    if (it.id == id) it.copy(isAuthorized = !it.isAuthorized) else it
                },
                toolConnectingMessage = message
            )
        }
    }

    fun clearToolConnectingMessage() {
        _uiState.update { it.copy(toolConnectingMessage = null) }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ViewModelFactory — 注入 Repository 依赖
// ══════════════════════════════════════════════════════════════════════════════

class MainViewModelFactory(
    private val repository: IUserPreferencesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == MainViewModel::class.java) {
            "Unknown ViewModel class: $modelClass"
        }
        return MainViewModel(repository) as T
    }
}
