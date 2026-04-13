package com.aether.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.aether.app.data.ApiConfig
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.aether.app.DeviceViewModel
import com.aether.app.MainViewModel
import com.aether.app.PersonalSpaceUiState
import com.aether.app.ui.components.DangerModeCard
import com.aether.app.ui.components.DangerWarningDialog
import com.aether.app.ui.components.MyDevicesSection
import com.aether.app.ui.components.ToolCenterSection
import com.aether.app.ui.theme.AetherTheme
import com.aether.app.ui.theme.GlassBorder
import com.aether.app.ui.theme.NeonPurple
import kotlin.math.roundToInt

// ══════════════════════════════════════════════════════════════════════════════
//  子页面导航状态
// ══════════════════════════════════════════════════════════════════════════════

sealed class PersonalSubScreen {
    object Main         : PersonalSubScreen()
    object ApiSelection : PersonalSubScreen()
    object ApiConfig    : PersonalSubScreen()
}

// ══════════════════════════════════════════════════════════════════════════════
//  有状态入口 Screen（接收 Activity 级 MainViewModel）
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun PersonalNexusScreen(viewModel: MainViewModel, deviceViewModel: DeviceViewModel) {
    val uiState         by viewModel.uiState.collectAsState()
    val userName        by viewModel.userName.collectAsState()
    val avatarUriString by viewModel.avatarUriString.collectAsState()
    val avatarUri = remember(avatarUriString) { avatarUriString?.let { Uri.parse(it) } }
    val context = LocalContext.current

    var subScreen by remember { mutableStateOf<PersonalSubScreen>(PersonalSubScreen.Main) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.onAvatarSelected(uri)
        }
    )

    when (subScreen) {
        is PersonalSubScreen.Main -> PersonalNexusContent(
            uiState           = uiState,
            userName          = userName,
            avatarUri         = avatarUri,
            onAvatarClick     = { photoPickerLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
            onToggleCard      = viewModel::toggleCard,
            onOpenDialog      = viewModel::openDialog,
            onDismissDialog   = viewModel::dismissDialog,
            onInputChange     = viewModel::onDialogInputChange,
            onSubmit          = viewModel::submitTrait,
            onOpenEditName    = viewModel::openEditNameDialog,
            onDismissEditName = viewModel::dismissEditNameDialog,
            onEditNameChange  = viewModel::onEditNameInputChange,
            onSaveName        = viewModel::saveUserName,
            deviceViewModel   = deviceViewModel,
            onNavigateToApiSelection = { subScreen = PersonalSubScreen.ApiSelection },
            onToggleDangerMode   = viewModel::toggleDangerMode,
            onConfirmDangerMode  = viewModel::confirmDangerMode,
            onDismissDangerWarning = viewModel::dismissDangerMode,
            onToggleToolAuthorization    = viewModel::toggleToolAuthorization,
            onClearToolConnectingMessage = viewModel::clearToolConnectingMessage,
        )
        is PersonalSubScreen.ApiSelection -> ApiSelectionScreen(
            configs     = uiState.apiConfigs,
            activeApiId = uiState.activeApiId,
            onSelect    = { id -> id?.let { viewModel.selectApi(it) }; subScreen = PersonalSubScreen.Main },
            onAddClick  = { subScreen = PersonalSubScreen.ApiConfig },
            onBack      = { subScreen = PersonalSubScreen.Main }
        )
        is PersonalSubScreen.ApiConfig -> ApiConfigScreen(
            onSave = { config -> viewModel.addApiConfig(config); subScreen = PersonalSubScreen.ApiSelection },
            onBack = { subScreen = PersonalSubScreen.ApiSelection }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  无状态内容组件（便于 Preview 和测试）
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun PersonalNexusContent(
    uiState: PersonalSpaceUiState,
    userName: String,
    avatarUri: Uri?,
    onAvatarClick: () -> Unit,
    onToggleCard: () -> Unit,
    onOpenDialog: () -> Unit,
    onDismissDialog: () -> Unit,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onOpenEditName: () -> Unit,
    onDismissEditName: () -> Unit,
    onEditNameChange: (String) -> Unit,
    onSaveName: () -> Unit,
    deviceViewModel: DeviceViewModel,
    onNavigateToApiSelection: () -> Unit,
    onToggleDangerMode: () -> Unit,
    onConfirmDangerMode: () -> Unit,
    onDismissDangerWarning: () -> Unit,
    onToggleToolAuthorization: (String) -> Unit,
    onClearToolConnectingMessage: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(uiState.toolConnectingMessage) {
        uiState.toolConnectingMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            onClearToolConnectingMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionLabel("个人信息")
            ProfileExpandableCard(
                userName     = userName,
                avatarUri    = avatarUri,
                traits       = uiState.profileTraits,
                isExpanded   = uiState.isCardExpanded,
                onToggle     = onToggleCard,
                onAvatarClick = onAvatarClick,
                onAddClick   = onOpenDialog,
                onEditNameClick = onOpenEditName
            )

            SectionLabel("目前API选择")
            ApiConfigCard(
                activeApi = uiState.apiConfigs.find { it.id == uiState.activeApiId },
                onClick   = onNavigateToApiSelection,
                modifier  = Modifier.align(Alignment.CenterHorizontally)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MyDevicesSection(
                    viewModel = deviceViewModel,
                    modifier = Modifier
                        .weight(1f)
                        .height(240.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DangerModeCard(
                        isActive = uiState.isDangerModeActive,
                        onToggle = onToggleDangerMode
                    )
                    SectionLabel("可用工具")
                    ToolCenterSection(
                        toolItems = uiState.toolItems,
                        onToggle  = onToggleToolAuthorization
                    )
                }
            }
        }

        if (uiState.isDialogVisible) {
            AddTraitDialog(
                input        = uiState.dialogInput,
                isSubmitting = uiState.isSubmitting,
                isError      = uiState.inputError,
                shakeSignal  = uiState.shakeSignal,
                onInputChange = onInputChange,
                onDismiss    = onDismissDialog,
                onConfirm    = onSubmit
            )
        }

        if (uiState.showEditNameDialog) {
            EditNameDialog(
                currentInput  = uiState.editNameInput,
                isError       = uiState.editNameError,
                onInputChange = onEditNameChange,
                onDismiss     = onDismissEditName,
                onSave        = onSaveName
            )
        }

        if (uiState.showDangerWarningDialog) {
            DangerWarningDialog(
                onConfirm = onConfirmDangerMode,
                onDismiss = onDismissDangerWarning
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  区域 2：API 配置卡片
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun ApiConfigCard(
    activeApi: ApiConfig?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            if (activeApi != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LetterAvatar(letter = activeApi.providerName.first().uppercaseChar())
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = activeApi.providerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = activeApi.requestUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 1
                        )
                    }
                }
            } else {
                Text(
                    text = "未配置 API，点击设置",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GlassBorder.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun LetterAvatar(letter: Char, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(NeonPurple.copy(alpha = 0.15f))
            .border(1.5.dp, NeonPurple.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = NeonPurple
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  区域 1：可折叠个人信息卡片
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun ProfileExpandableCard(
    userName: String,
    avatarUri: Uri?,
    traits: List<String>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onAvatarClick: () -> Unit,
    onAddClick: () -> Unit,
    onEditNameClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "arrow_rotation"
    )

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // ── 概要行：头像 + 姓名(+编辑图标) + 展开箭头 ───────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AvatarView(
                        avatarUri = avatarUri,
                        userName  = userName,
                        onClick   = onAvatarClick
                    )

                    // 姓名 + 编辑图标（独立 clickable，不冒泡到外层 onToggle）
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable(onClick = onEditNameClick)
                    ) {
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "修改昵称",
                            tint = GlassBorder.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "折叠" else "展开",
                    tint = GlassBorder.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(arrowRotation)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioLowBouncy
                    )
                ) + fadeIn(),
                exit = shrinkVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                ) + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "个性化画像",
                        style = MaterialTheme.typography.labelMedium,
                        color = GlassBorder.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        traits.forEach { trait -> TraitItem(text = trait) }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        FilledTonalIconButton(
                            onClick = onAddClick,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "添加个性化信息",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  头像组件：相册 URI / 默认占位 双态
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AvatarView(
    avatarUri: Uri?,
    userName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUri != null) {
            AsyncImage(
                model = avatarUri,
                contentDescription = "${userName}的头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NeonPurple.copy(alpha = 0.12f))
                    .border(1.5.dp, NeonPurple.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "默认头像",
                    tint = NeonPurple,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = "更换头像",
                tint = NeonPurple,
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  单条个性化画像条目
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun TraitItem(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 0.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  修改昵称弹窗
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun EditNameDialog(
    currentInput: String,
    isError: Boolean,
    onInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var fieldValue by remember(currentInput) {
        mutableStateOf(
            TextFieldValue(
                text = currentInput,
                selection = TextRange(currentInput.length)
            )
        )
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "修改昵称",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = fieldValue,
                    onValueChange = { newValue ->
                        fieldValue = newValue
                        onInputChange(newValue.text)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    label = { Text("昵称") },
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("昵称不能为空", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple,
                        errorBorderColor = MaterialTheme.colorScheme.error,
                        errorCursorColor = MaterialTheme.colorScheme.error
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = GlassBorder.copy(alpha = 0.7f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onSave) {
                        Text("保存", color = NeonPurple, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  添加个性化信息弹窗
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AddTraitDialog(
    input: String,
    isSubmitting: Boolean,
    isError: Boolean,
    shakeSignal: Int,
    onInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var shakeOffset by remember { mutableStateOf(0f) }
    val animatedShakeOffset by animateFloatAsState(
        targetValue = shakeOffset,
        animationSpec = keyframes {
            durationMillis = 400
            0f     at 0
            (-18f) at 50
            18f    at 100
            (-14f) at 150
            14f    at 200
            (-8f)  at 260
            8f     at 310
            0f     at 400
        },
        label = "shake_offset",
        finishedListener = { shakeOffset = 0f }
    )

    LaunchedEffect(shakeSignal) { if (shakeSignal > 0) shakeOffset = 1f }

    Dialog(onDismissRequest = { if (!isSubmitting) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "你希望 Aether 记住什么？",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = input,
                    onValueChange = { if (!isSubmitting) onInputChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(animatedShakeOffset.roundToInt(), 0) },
                    placeholder = {
                        Text(
                            text = "例如：我是一个计算机专业的大学生",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    },
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("内容太短，请至少输入 4 个字符", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = false,
                    minLines = 3,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple,
                        errorBorderColor = MaterialTheme.colorScheme.error,
                        errorCursorColor = MaterialTheme.colorScheme.error
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = NeonPurple
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                        Text("取消", color = GlassBorder.copy(alpha = 0.7f))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = onConfirm,
                        enabled = !isSubmitting && input.isNotBlank()
                    ) {
                        Text(
                            text = "添加",
                            color = if (!isSubmitting && input.isNotBlank()) NeonPurple
                                    else GlassBorder.copy(alpha = 0.35f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  区域 2–5 通用占位容器
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun SectionPlaceholder(
    label: String,
    minHeight: Int,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(minHeight.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = GlassBorder.copy(alpha = 0.35f),
                letterSpacing = 1.sp
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  @Preview
// ══════════════════════════════════════════════════════════════════════════════

@Preview(
    name = "区域 1 · 展开状态",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 360
)
@Composable
private fun PreviewProfileCardExpanded() {
    AetherTheme {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileExpandableCard(
                userName     = "江南少",
                avatarUri    = null,
                traits       = listOf(
                    "Agent Tech 极客",
                    "三角洲行动老兵",
                    "偏好简洁直接的沟通风格",
                    "✦ 我是一个计算机专业的大学生"
                ),
                isExpanded   = true,
                onToggle     = {},
                onAvatarClick = {},
                onAddClick   = {},
                onEditNameClick = {}
            )
        }
    }
}
