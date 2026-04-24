package com.aether.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.aether.app.R
import com.aether.app.DeviceWorkingStatus
import com.aether.app.PersonalDeviceUiState
import com.aether.app.data.ApiConfig

data class PortraitCard(
    val category: String,
    val text: String,
    val tail: String? = null,
    val accent: Boolean = false,
)

data class PortraitGroup(val label: String, val cards: List<PortraitCard>)

private data class PortraitTagOption(
    val label: String,
    val prompt: String,
    val placeholder: String,
    val supporting: String,
)

private data class PortraitSelection(
    val groupLabel: String,
    val card: PortraitCard,
)

private val Ink900 = Color(0xFF1A1614)
private val Ink700 = Color(0xFF3D342E)
private val Ink500 = Color(0xFF6B5F57)
private val Ink300 = Color(0xFFA89B90)
private val Ink200 = Color(0xFFC9BEB4)

private val Cream50 = Color(0xFFFAF6F0)
private val Cream100 = Color(0xFFF3ECE2)
private val Cream300 = Color(0xFFD9CBB6)

private val DawnDusk = Color(0xFF2E2438)
private val DawnEmber = Color(0xFFB85C3C)
private val DawnPeach = Color(0xFFE8A57A)
private val DawnHaze = Color(0xFFF2D6B3)

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val FontDisplay = FontFamily(
    Font(googleFont = GoogleFont("Fraunces"), fontProvider = fontProvider, weight = FontWeight.Light),
    Font(googleFont = GoogleFont("Fraunces"), fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Fraunces"), fontProvider = fontProvider, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(googleFont = GoogleFont("Fraunces"), fontProvider = fontProvider, weight = FontWeight.Light, style = FontStyle.Italic),
    Font(googleFont = GoogleFont("Noto Serif SC"), fontProvider = fontProvider, weight = FontWeight.Light),
    Font(googleFont = GoogleFont("Noto Serif SC"), fontProvider = fontProvider, weight = FontWeight.Normal),
)

private val FontBody = FontFamily(
    Font(googleFont = GoogleFont("Inter"), fontProvider = fontProvider, weight = FontWeight.Light),
    Font(googleFont = GoogleFont("Inter"), fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Inter"), fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Inter"), fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Noto Sans SC"), fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Noto Sans SC"), fontProvider = fontProvider, weight = FontWeight.Medium),
)

private val FontMono = FontFamily(
    Font(googleFont = GoogleFont("JetBrains Mono"), fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("JetBrains Mono"), fontProvider = fontProvider, weight = FontWeight.Medium),
)

private val FontCnDisplay = FontFamily(
    Font(googleFont = GoogleFont("Noto Serif SC"), fontProvider = fontProvider, weight = FontWeight.Light),
    Font(googleFont = GoogleFont("Noto Serif SC"), fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Noto Serif SC"), fontProvider = fontProvider, weight = FontWeight.Medium),
)

private val FontCnBody = FontFamily(
    Font(googleFont = GoogleFont("Noto Sans SC"), fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Noto Sans SC"), fontProvider = fontProvider, weight = FontWeight.Medium),
)

@Composable
private fun AetherToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val trackColor = if (checked) DawnEmber else Ink200
    val thumbOffset = if (checked) 22.dp else 2.dp

    Box(
        modifier = modifier
            .size(width = 46.dp, height = 26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(trackColor.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbOffset)
                .size(22.dp)
                .clip(CircleShape)
                .background(Cream50)
        )
    }
}

@Composable
private fun GlassesMark(
    modifier: Modifier = Modifier,
    color: Color? = null,
    strokeWidth: Float = 3.5f,
) {
    Canvas(modifier = modifier) {
        val sx = size.width / 200f
        val sy = size.height / 110f
        val path = Path().apply {
            moveTo(6 * sx, 40 * sy)
            cubicTo(16 * sx, 32 * sy, 26 * sx, 30 * sy, 36 * sx, 36 * sy)
            cubicTo(46 * sx, 42 * sy, 52 * sx, 50 * sy, 56 * sx, 62 * sy)
            cubicTo(60 * sx, 78 * sy, 52 * sx, 90 * sy, 40 * sx, 90 * sy)
            cubicTo(26 * sx, 90 * sy, 18 * sx, 80 * sy, 20 * sx, 66 * sy)
            cubicTo(22 * sx, 52 * sy, 36 * sx, 44 * sy, 52 * sx, 48 * sy)
            cubicTo(70 * sx, 52 * sy, 82 * sx, 58 * sy, 100 * sx, 58 * sy)
            cubicTo(118 * sx, 58 * sy, 130 * sx, 52 * sy, 148 * sx, 48 * sy)
            cubicTo(164 * sx, 44 * sy, 178 * sx, 52 * sy, 180 * sx, 66 * sy)
            cubicTo(182 * sx, 80 * sy, 174 * sx, 90 * sy, 160 * sx, 90 * sy)
            cubicTo(148 * sx, 90 * sy, 140 * sx, 78 * sy, 144 * sx, 62 * sy)
            cubicTo(148 * sx, 50 * sy, 154 * sx, 42 * sy, 164 * sx, 36 * sy)
            cubicTo(174 * sx, 30 * sy, 184 * sx, 32 * sy, 194 * sx, 40 * sy)
        }
        val stroke = Stroke(
            width = strokeWidth * minOf(sx, sy),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        if (color != null) {
            drawPath(path = path, color = color, style = stroke)
        } else {
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(DawnDusk, DawnEmber, DawnPeach),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                ),
                style = stroke
            )
        }
    }
}

@Composable
fun PersonalSpaceScreen(
    userName: String,
    avatarUriString: String? = null,
    deviceUiState: PersonalDeviceUiState = PersonalDeviceUiState(),
    apiConfigs: List<ApiConfig> = emptyList(),
    activeApiId: String? = null,
    daysWithAether: Int = 28,
    portraitGroups: List<PortraitGroup> = defaultPortraitGroups(),
    onSaveUserName: (String) -> Unit = {},
    onSaveAvatarUri: (Uri?) -> Unit = {},
    onSelectApi: (String) -> Unit = {},
    onAddApi: (ApiConfig) -> Unit = {},
    onUpdateApi: (ApiConfig) -> Unit = {},
    onDeleteApi: (String) -> Unit = {},
    onOpenDeviceDetail: () -> Unit = {},
    onOpenProductPhilosophy: () -> Unit = {},
    onBack: () -> Unit = {},
    onAddCard: (group: String) -> Unit = {},
    onEditCard: (PortraitCard) -> Unit = {},
) {
    val context = LocalContext.current
    val avatarUri = remember(avatarUriString) { avatarUriString?.let(Uri::parse) }
    var portraitState by remember(portraitGroups) { mutableStateOf(portraitGroups) }
    var isProfileDialogVisible by remember { mutableStateOf(false) }
    var isAddPortraitDialogVisible by remember { mutableStateOf(false) }
    var isPortraitActionDialogVisible by remember { mutableStateOf(false) }
    var isDeletePortraitDialogVisible by remember { mutableStateOf(false) }
    var editingPortrait by remember { mutableStateOf<PortraitSelection?>(null) }
    var selectedPortrait by remember { mutableStateOf<PortraitSelection?>(null) }
    var selectedPortraitLabel by remember { mutableStateOf<String?>(null) }
    var portraitDraftText by remember { mutableStateOf("") }
    var portraitDraftError by remember { mutableStateOf(false) }
    var nameDraft by remember(userName) { mutableStateOf(userName) }
    var isApiListExpanded by remember { mutableStateOf(false) }
    var apiDialogMode by remember { mutableStateOf<ApiDialogMode?>(null) }
    var editingApiConfig by remember { mutableStateOf<ApiConfig?>(null) }
    var apiNameDraft by remember { mutableStateOf("") }
    var apiKeyDraft by remember { mutableStateOf("") }
    var apiUrlDraft by remember { mutableStateOf("") }
    var apiDraftError by remember { mutableStateOf(false) }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            onSaveAvatarUri(uri)
        }
    )

    val permStates = remember {
        listOf(
            mutableStateOf(true),
            mutableStateOf(true),
            mutableStateOf(true),
            mutableStateOf(false),
            mutableStateOf(false),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream50)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Cream50,
                                Cream100.copy(alpha = 0.26f),
                                Cream50
                            ),
                            startY = 0f,
                            endY = 2400f
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                DawnHaze.copy(alpha = 0.24f),
                                DawnPeach.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(540f, 260f),
                            radius = 980f
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                DawnPeach.copy(alpha = 0.06f),
                                Cream100.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            startY = 220f,
                            endY = 980f
                        )
                    )
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                PersonalHeaderSection(
                    userName = userName,
                    avatarUri = avatarUri,
                    daysWithAether = daysWithAether,
                    onEditProfile = {
                        nameDraft = userName
                        isProfileDialogVisible = true
                    },
                    onBack = onBack
                )

                Spacer(modifier = Modifier.height(18.dp))

                PortraitSection(
                    portraitGroups = portraitState,
                    onAddCard = { preferredLabel ->
                        editingPortrait = null
                        selectedPortraitLabel = resolvePreferredPortraitLabel(
                            preferredLabel = preferredLabel,
                            portraitGroups = portraitState
                        )
                        portraitDraftText = ""
                        portraitDraftError = false
                        isAddPortraitDialogVisible = true
                    },
                    onEditCard = { groupLabel, card ->
                        selectedPortrait = PortraitSelection(groupLabel, card)
                        isPortraitActionDialogVisible = true
                    }
                )

                Spacer(modifier = Modifier.height(56.dp))

                DeviceSection(
                    userName = userName,
                    deviceUiState = deviceUiState,
                    onClick = onOpenDeviceDetail
                )

                Spacer(modifier = Modifier.height(40.dp))

                ApiModelSection(
                    apiConfigs = apiConfigs,
                    activeApiId = activeApiId,
                    isExpanded = isApiListExpanded,
                    onToggleExpanded = { isApiListExpanded = !isApiListExpanded },
                    onSelectApi = onSelectApi,
                    onEditApi = { config ->
                        editingApiConfig = config
                        apiDialogMode = ApiDialogMode.EDIT
                        apiNameDraft = config.providerName
                        apiKeyDraft = config.apiKey
                        apiUrlDraft = config.requestUrl
                        apiDraftError = false
                    },
                    onDeleteApi = onDeleteApi,
                    onAddApi = {
                        editingApiConfig = null
                        apiDialogMode = ApiDialogMode.ADD
                        apiNameDraft = ""
                        apiKeyDraft = ""
                        apiUrlDraft = ""
                        apiDraftError = false
                    }
                )

                Spacer(modifier = Modifier.height(40.dp))

                PermissionsSection(permStates = permStates)

                Spacer(modifier = Modifier.height(40.dp))

                AboutSection(onOpenProductPhilosophy = onOpenProductPhilosophy)

                ClosingQuoteSection()

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    if (isProfileDialogVisible) {
        EditProfileDialog(
            userName = nameDraft,
            avatarUri = avatarUri,
            onNameChange = { nameDraft = it },
            onChooseAvatar = { avatarPickerLauncher.launch(arrayOf("image/*")) },
            onDismiss = {
                nameDraft = userName
                isProfileDialogVisible = false
            },
            onSave = {
                onSaveUserName(nameDraft)
                nameDraft = nameDraft.trim().ifBlank { userName }
                isProfileDialogVisible = false
            }
        )
    }

    if (isAddPortraitDialogVisible) {
        AddPortraitDialog(
            options = portraitTagOptions(portraitState),
            selectedLabel = selectedPortraitLabel,
            draftText = portraitDraftText,
            isError = portraitDraftError,
            title = if (editingPortrait == null) "新增一段关于你的记忆" else "编辑这段画像内容",
            description = if (editingPortrait == null) {
                "先选择标签，再写下对应内容。Aether 会把它放进正确的画像分组里。"
            } else {
                "修改这段画像的标签或内容，让 Aether 记得更准确。"
            },
            confirmLabel = if (editingPortrait == null) "保存到画像" else "保存修改",
            onSelectLabel = {
                selectedPortraitLabel = it
                portraitDraftError = false
            },
            onDraftChange = {
                portraitDraftText = it
                portraitDraftError = false
            },
            onDismiss = {
                isAddPortraitDialogVisible = false
                portraitDraftText = ""
                portraitDraftError = false
            },
            onConfirm = {
                val label = selectedPortraitLabel
                val content = portraitDraftText.trim()
                if (label == null || content.isEmpty()) {
                    portraitDraftError = true
                } else {
                    portraitState = if (editingPortrait == null) {
                        addPortraitCardToGroups(
                            portraitGroups = portraitState,
                            targetLabel = label,
                            content = content
                        )
                    } else {
                        updatePortraitCardInGroups(
                            portraitGroups = portraitState,
                            targetLabel = label,
                            originalCard = editingPortrait!!.card,
                            originalGroupLabel = editingPortrait!!.groupLabel,
                            content = content
                        )
                    }
                    editingPortrait = null
                    isAddPortraitDialogVisible = false
                    portraitDraftText = ""
                    portraitDraftError = false
                    onAddCard(label)
                }
            }
        )
    }

    if (isPortraitActionDialogVisible && selectedPortrait != null) {
        PortraitActionDialog(
            selection = selectedPortrait!!,
            onDismiss = { isPortraitActionDialogVisible = false },
            onEdit = {
                val selection = selectedPortrait ?: return@PortraitActionDialog
                editingPortrait = selection
                selectedPortraitLabel = selection.groupLabel
                portraitDraftText = selection.card.text
                portraitDraftError = false
                isPortraitActionDialogVisible = false
                isAddPortraitDialogVisible = true
            },
            onDelete = {
                isPortraitActionDialogVisible = false
                isDeletePortraitDialogVisible = true
            }
        )
    }

    if (isDeletePortraitDialogVisible && selectedPortrait != null) {
        DeletePortraitDialog(
            selection = selectedPortrait!!,
            onDismiss = { isDeletePortraitDialogVisible = false },
            onConfirm = {
                val selection = selectedPortrait ?: return@DeletePortraitDialog
                portraitState = deletePortraitCardFromGroups(
                    portraitGroups = portraitState,
                    targetLabel = selection.groupLabel,
                    targetCard = selection.card
                )
                isDeletePortraitDialogVisible = false
                selectedPortrait = null
            }
        )
    }

    if (apiDialogMode != null) {
        ApiConfigDialog(
            mode = apiDialogMode!!,
            name = apiNameDraft,
            apiKey = apiKeyDraft,
            requestUrl = apiUrlDraft,
            isError = apiDraftError,
            onNameChange = {
                apiNameDraft = it
                apiDraftError = false
            },
            onApiKeyChange = {
                apiKeyDraft = it
                apiDraftError = false
            },
            onRequestUrlChange = {
                apiUrlDraft = it
                apiDraftError = false
            },
            onDismiss = {
                apiDialogMode = null
                editingApiConfig = null
                apiDraftError = false
            },
            onConfirm = {
                val providerName = apiNameDraft.trim()
                val apiKey = apiKeyDraft.trim()
                val requestUrl = apiUrlDraft.trim()
                if (providerName.isEmpty() || apiKey.isEmpty() || requestUrl.isEmpty()) {
                    apiDraftError = true
                } else {
                    val existing = editingApiConfig
                    if (apiDialogMode == ApiDialogMode.EDIT && existing != null) {
                        onUpdateApi(
                            existing.copy(
                                providerName = providerName,
                                apiKey = apiKey,
                                requestUrl = requestUrl
                            )
                        )
                    } else {
                        onAddApi(
                            ApiConfig(
                                providerName = providerName,
                                apiKey = apiKey,
                                requestUrl = requestUrl
                            )
                        )
                    }
                    apiDialogMode = null
                    editingApiConfig = null
                    apiDraftError = false
                    isApiListExpanded = true
                }
            }
        )
    }
}

// ── Shared sub-components ─────────────────────────────────────────────

@Composable
private fun PersonalHeaderSection(
    userName: String,
    avatarUri: Uri?,
    daysWithAether: Int,
    onEditProfile: () -> Unit,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(532.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            DawnHaze.copy(alpha = 0.42f),
                            DawnPeach.copy(alpha = 0.22f),
                            Cream50.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(540f, 210f),
                        radius = 980f
                    )
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 166.dp)
                .size(width = 280.dp, height = 236.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            DawnPeach.copy(alpha = 0.18f),
                            DawnHaze.copy(alpha = 0.11f),
                            Color.Transparent
                        ),
                        radius = 460f
                    ),
                    shape = RoundedCornerShape(120.dp)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 62.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AetherBackButton(onClick = onBack)
                Text(
                    text = "YOUR SPACE",
                    style = TextStyle(
                        fontFamily = FontMono, fontSize = 10.sp,
                        letterSpacing = 0.18.sp, color = Ink500
                    )
                )
                Spacer(modifier = Modifier.width(22.dp))
            }

            Spacer(modifier = Modifier.height(48.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(44.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Cream50.copy(alpha = 0.26f),
                                Cream100.copy(alpha = 0.38f),
                                Cream50.copy(alpha = 0.18f)
                            )
                        )
                    )
                    .padding(top = 18.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    DawnHaze.copy(alpha = 0.34f),
                                    DawnPeach.copy(alpha = 0.2f),
                                    Color.Transparent
                                ),
                                radius = 180f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(102.dp)
                            .clip(CircleShape)
                            .background(Cream50.copy(alpha = 0.72f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarUri != null) {
                            AsyncImage(
                                model = avatarUri,
                                contentDescription = "$userName avatar",
                                modifier = Modifier
                                    .size(94.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(94.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(DawnHaze, DawnEmber),
                                            start = Offset(0f, 0f),
                                            end = Offset(94f, 94f)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userName.firstOrNull()?.uppercase() ?: "A",
                                    style = TextStyle(
                                        fontFamily = FontDisplay, fontSize = 40.sp,
                                        fontWeight = FontWeight.Light, color = Cream50,
                                        letterSpacing = (-1.2).sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = userName,
                    style = TextStyle(
                        fontFamily = FontDisplay, fontSize = 36.sp,
                        fontWeight = FontWeight.Light, letterSpacing = (-0.72).sp, color = Ink900
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row {
                    Text(
                        text = "与 Aether 相伴 ",
                        style = TextStyle(
                            fontFamily = FontCnBody, fontStyle = FontStyle.Italic,
                            fontSize = 13.sp, color = Ink500
                        )
                    )
                    Text(
                        text = "$daysWithAether",
                        style = TextStyle(
                            fontFamily = FontMono, fontSize = 13.sp, color = Ink700
                        )
                    )
                    Text(
                        text = " 天",
                        style = TextStyle(
                            fontFamily = FontCnBody, fontStyle = FontStyle.Italic,
                            fontSize = 13.sp, color = Ink500
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Cream100.copy(alpha = 0.92f),
                                    Cream50.copy(alpha = 0.96f)
                                )
                            )
                        )
                        .clickable(onClick = onEditProfile)
                        .padding(horizontal = 18.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EDIT PROFILE",
                        style = TextStyle(
                            fontFamily = FontMono,
                            fontSize = 10.sp,
                            letterSpacing = 0.16.sp,
                            color = Ink700
                        )
                    )
                    Text(
                        text = "›",
                        style = TextStyle(
                            fontFamily = FontMono,
                            fontSize = 12.sp,
                            color = DawnEmber
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AetherBackButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Cream100.copy(alpha = 0.72f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(width = 14.dp, height = 10.dp)) {
            val stroke = 1.7.dp.toPx()
            drawLine(
                color = Ink700,
                start = Offset(size.width, size.height / 2f),
                end = Offset(3.dp.toPx(), size.height / 2f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Ink700,
                start = Offset(3.dp.toPx(), size.height / 2f),
                end = Offset(8.dp.toPx(), 1.2.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Ink700,
                start = Offset(3.dp.toPx(), size.height / 2f),
                end = Offset(8.dp.toPx(), size.height - 1.2.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
        Text(
            text = "BACK",
            style = TextStyle(
                fontFamily = FontMono,
                fontSize = 10.sp,
                letterSpacing = 0.14.sp,
                color = Ink500
            )
        )
    }
}

@Composable
private fun PortraitSection(
    portraitGroups: List<PortraitGroup>,
    onAddCard: (String) -> Unit,
    onEditCard: (String, PortraitCard) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val totalCount = portraitTotalCount(portraitGroups)

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        if (isExpanded) {
            PortraitManagerCard(
                portraitGroups = portraitGroups,
                totalCount = totalCount,
                onCollapse = { isExpanded = false },
                onAddCard = onAddCard,
                onEditCard = onEditCard
            )
        } else {
            PortraitSummaryCard(
                portraitGroups = portraitGroups,
                totalCount = totalCount,
                onExpand = { isExpanded = true },
                onAddCard = { onAddCard("") }
            )
        }
    }
}

@Composable
private fun PortraitManagerCard(
    portraitGroups: List<PortraitGroup>,
    totalCount: Int,
    onCollapse: () -> Unit,
    onAddCard: (String) -> Unit,
    onEditCard: (String, PortraitCard) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Cream100.copy(alpha = 0.9f))
            .border(1.dp, Ink200.copy(alpha = 0.65f), RoundedCornerShape(28.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "CORE PORTRAIT",
                    style = TextStyle(
                        fontFamily = FontMono,
                        fontSize = 10.sp,
                        letterSpacing = 0.18.sp,
                        color = Ink500
                    )
                )
                Text(
                    text = "· 管理画像",
                    style = TextStyle(
                        fontFamily = FontCnBody,
                        fontSize = 11.sp,
                        color = Ink500
                    )
                )
            }
            Text(
                text = "${totalCount} 项",
                style = TextStyle(
                    fontFamily = FontMono,
                    fontSize = 11.sp,
                    color = Ink500
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "关于你的记忆，仍可继续整理。",
            style = TextStyle(
                fontFamily = FontDisplay,
                fontStyle = FontStyle.Italic,
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                lineHeight = 30.sp,
                color = Ink700
            )
        )

        Spacer(modifier = Modifier.height(18.dp))

        portraitGroups.forEachIndexed { index, group ->
            PortraitGroupSection(
                group = group,
                onAdd = { onAddCard(group.label) },
                onEdit = { card -> onEditCard(group.label, card) }
            )
            if (index != portraitGroups.lastIndex) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Ink200.copy(alpha = 0.7f))
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.clickable(onClick = onCollapse),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "收起管理",
                    style = TextStyle(
                        fontFamily = FontCnBody,
                        fontSize = 12.sp,
                        color = DawnEmber
                    )
                )
                Text(
                    text = "←",
                    style = TextStyle(
                        fontFamily = FontMono,
                        fontSize = 12.sp,
                        color = DawnEmber
                    )
                )
            }

            Row(
                modifier = Modifier.clickable { onAddCard("") },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "+",
                    style = TextStyle(
                        fontFamily = FontMono,
                        fontSize = 14.sp,
                        color = Ink500
                    )
                )
                Text(
                    text = "新增",
                    style = TextStyle(
                        fontFamily = FontCnBody,
                        fontSize = 12.sp,
                        color = Ink700
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PortraitSummaryCard(
    portraitGroups: List<PortraitGroup>,
    totalCount: Int,
    onExpand: () -> Unit,
    onAddCard: () -> Unit,
) {
    val previewItems = portraitPreviewItems(portraitGroups, limit = 7)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Cream100.copy(alpha = 0.88f))
            .border(1.dp, Ink200.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "CORE PORTRAIT",
                    style = TextStyle(
                        fontFamily = FontMono,
                        fontSize = 10.sp,
                        letterSpacing = 0.18.sp,
                        color = Ink500
                    )
                )
                Text(
                    text = "· 我的画像",
                    style = TextStyle(
                        fontFamily = FontCnBody,
                        fontSize = 11.sp,
                        color = Ink500
                    )
                )
            }
            Text(
                text = "${totalCount} 项",
                style = TextStyle(
                    fontFamily = FontMono,
                    fontSize = 11.sp,
                    color = Ink500
                )
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Aether 记得的关于你。",
            style = TextStyle(
                fontFamily = FontDisplay,
                fontStyle = FontStyle.Italic,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                lineHeight = 34.sp,
                color = Ink700
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            previewItems.forEach { item ->
                PortraitPreviewPill(text = item)
            }
            if (totalCount > previewItems.size) {
                PortraitPreviewPill(text = "+${totalCount - previewItems.size}", accent = true)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Ink200.copy(alpha = 0.7f))
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.clickable(onClick = onExpand),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "展开管理",
                    style = TextStyle(
                        fontFamily = FontCnBody,
                        fontSize = 12.sp,
                        color = DawnEmber
                    )
                )
                Text(
                    text = "→",
                    style = TextStyle(
                        fontFamily = FontMono,
                        fontSize = 12.sp,
                        color = DawnEmber
                    )
                )
            }

            Row(
                modifier = Modifier.clickable(onClick = onAddCard),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "+",
                    style = TextStyle(
                        fontFamily = FontMono,
                        fontSize = 14.sp,
                        color = Ink500
                    )
                )
                Text(
                    text = "新增",
                    style = TextStyle(
                        fontFamily = FontCnBody,
                        fontSize = 12.sp,
                        color = Ink700
                    )
                )
            }
        }
    }
}

@Composable
private fun PortraitPreviewPill(
    text: String,
    accent: Boolean = false,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (accent) Cream50 else Cream50.copy(alpha = 0.92f))
            .border(
                1.dp,
                if (accent) Ink300.copy(alpha = 0.7f) else Ink200.copy(alpha = 0.7f),
                RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontFamily = if (accent) FontMono else FontCnBody,
                fontSize = 12.sp,
                color = Ink700
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddPortraitDialog(
    options: List<PortraitTagOption>,
    selectedLabel: String?,
    draftText: String,
    isError: Boolean,
    title: String,
    description: String,
    confirmLabel: String,
    onSelectLabel: (String) -> Unit,
    onDraftChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val selectedOption = options.firstOrNull { it.label == selectedLabel }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Cream100)
                .border(1.dp, Ink200.copy(alpha = 0.8f), RoundedCornerShape(28.dp))
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Text(
                text = "CORE PORTRAIT",
                style = TextStyle(
                    fontFamily = FontMono,
                    fontSize = 10.sp,
                    letterSpacing = 0.18.sp,
                    color = Ink500
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = TextStyle(
                    fontFamily = FontCnDisplay,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = Ink900
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = TextStyle(
                    fontFamily = FontCnBody,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Ink500
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { option ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (option.label == selectedLabel) {
                                    DawnPeach.copy(alpha = 0.24f)
                                } else {
                                    Cream50.copy(alpha = 0.9f)
                                }
                            )
                            .border(
                                1.dp,
                                if (option.label == selectedLabel) DawnEmber.copy(alpha = 0.55f)
                                else Ink200.copy(alpha = 0.75f),
                                RoundedCornerShape(999.dp)
                            )
                            .clickable { onSelectLabel(option.label) }
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = option.label,
                            style = TextStyle(
                                fontFamily = FontCnBody,
                                fontSize = 12.sp,
                                color = if (option.label == selectedLabel) Ink900 else Ink700
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = draftText,
                onValueChange = onDraftChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                label = {
                    Text(
                        text = selectedOption?.prompt ?: "输入内容",
                        style = TextStyle(
                            fontFamily = FontCnBody,
                            fontSize = 12.sp,
                            color = Ink500
                        )
                    )
                },
                placeholder = {
                    Text(
                        text = selectedOption?.placeholder ?: "例如：喜欢清晨跑步，节奏会让我安静下来",
                        style = TextStyle(
                            fontFamily = FontCnBody,
                            fontSize = 12.sp,
                            color = Ink300
                        )
                    )
                },
                isError = isError,
                textStyle = TextStyle(
                    fontFamily = FontCnBody,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = Ink900
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Cream50.copy(alpha = 0.95f),
                    unfocusedContainerColor = Cream50.copy(alpha = 0.9f),
                    disabledContainerColor = Cream50.copy(alpha = 0.9f),
                    focusedBorderColor = DawnEmber.copy(alpha = 0.75f),
                    unfocusedBorderColor = Ink200.copy(alpha = 0.8f),
                    errorBorderColor = DawnEmber,
                    cursorColor = DawnEmber,
                    focusedLabelColor = DawnEmber,
                    unfocusedLabelColor = Ink500
                ),
                shape = RoundedCornerShape(20.dp),
                supportingText = {
                    Text(
                        text = if (isError) {
                            "请选择标签并填写内容"
                        } else {
                            selectedOption?.supporting ?: "这条内容会作为新的画像条目加入。"
                        },
                        style = TextStyle(
                            fontFamily = FontCnBody,
                            fontSize = 11.sp,
                            color = if (isError) DawnEmber else Ink500
                        )
                    )
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Cream50.copy(alpha = 0.9f))
                ) {
                    Text(
                        text = "取消",
                        style = TextStyle(
                            fontFamily = FontCnBody,
                            fontSize = 14.sp,
                            color = Ink700
                        )
                    )
                }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DawnEmber,
                        contentColor = Cream50
                    )
                ) {
                    Text(
                        text = confirmLabel,
                        style = TextStyle(
                            fontFamily = FontCnBody,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PortraitActionDialog(
    selection: PortraitSelection,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Cream100)
                .border(1.dp, Ink200.copy(alpha = 0.8f), RoundedCornerShape(28.dp))
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Text(
                text = "CORE PORTRAIT",
                style = TextStyle(
                    fontFamily = FontMono,
                    fontSize = 10.sp,
                    letterSpacing = 0.18.sp,
                    color = Ink500
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "管理这段画像",
                style = TextStyle(
                    fontFamily = FontCnDisplay,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = Ink900
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "“${selection.card.text}”",
                style = TextStyle(
                    fontFamily = FontDisplay,
                    fontStyle = FontStyle.Italic,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Light,
                    color = Ink700
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            PortraitActionRow(
                label = "编辑内容",
                detail = "修改标签或文字内容",
                onClick = onEdit
            )

            Spacer(modifier = Modifier.height(10.dp))

            PortraitActionRow(
                label = "删除内容",
                detail = "从画像中移除这条记忆",
                accent = true,
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun PortraitActionRow(
    label: String,
    detail: String,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Cream50.copy(alpha = 0.9f))
            .border(
                1.dp,
                if (accent) DawnEmber.copy(alpha = 0.4f) else Ink200.copy(alpha = 0.8f),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = TextStyle(
                    fontFamily = FontCnBody,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (accent) DawnEmber else Ink900
                )
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = detail,
                style = TextStyle(
                    fontFamily = FontCnBody,
                    fontSize = 11.sp,
                    color = Ink500
                )
            )
        }
        Text(
            text = "›",
            style = TextStyle(
                fontFamily = FontMono,
                fontSize = 14.sp,
                color = if (accent) DawnEmber else Ink300
            )
        )
    }
}

@Composable
private fun DeletePortraitDialog(
    selection: PortraitSelection,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Cream100)
                .border(1.dp, Ink200.copy(alpha = 0.8f), RoundedCornerShape(28.dp))
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Text(
                text = "CORE PORTRAIT",
                style = TextStyle(
                    fontFamily = FontMono,
                    fontSize = 10.sp,
                    letterSpacing = 0.18.sp,
                    color = Ink500
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "确认删除这段画像？",
                style = TextStyle(
                    fontFamily = FontCnDisplay,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = Ink900
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "删除后，这条关于你的记忆会从“${selection.groupLabel}”中移除。",
                style = TextStyle(
                    fontFamily = FontCnBody,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Ink500
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "“${selection.card.text}”",
                style = TextStyle(
                    fontFamily = FontDisplay,
                    fontStyle = FontStyle.Italic,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Light,
                    color = Ink700
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Cream50.copy(alpha = 0.9f))
                ) {
                    Text(
                        text = "取消",
                        style = TextStyle(
                            fontFamily = FontCnBody,
                            fontSize = 14.sp,
                            color = Ink700
                        )
                    )
                }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DawnEmber,
                        contentColor = Cream50
                    )
                ) {
                    Text(
                        text = "确认删除",
                        style = TextStyle(
                            fontFamily = FontCnBody,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceSection(
    userName: String,
    deviceUiState: PersonalDeviceUiState,
    onClick: () -> Unit,
) {
    val statusColor = deviceStatusColor(deviceUiState.workingStatus)

    SectionHeader(label = "Device")
    Box(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Cream100)
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GlassesMark(
                modifier = Modifier
                    .width(52.dp)
                    .aspectRatio(200f / 110f),
                color = null
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Aether · $userName",
                    style = TextStyle(
                        fontFamily = FontDisplay, fontSize = 17.sp,
                        fontWeight = FontWeight.Normal, color = Ink900
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Cream300)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.72f)
                                .height(6.dp)
                                .background(DawnEmber)
                        )
                    }
                    Text(
                        text = "${deviceUiState.batteryLevel}%",
                        style = TextStyle(
                            fontFamily = FontMono, fontSize = 10.sp,
                            letterSpacing = 0.08.sp, color = Ink500
                        )
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        text = deviceStatusLabel(deviceUiState.workingStatus),
                        style = TextStyle(
                            fontFamily = FontMono, fontSize = 9.sp,
                            letterSpacing = 0.14.sp, color = statusColor
                        )
                    )
                }
            }
            Text(text = "›", style = TextStyle(fontSize = 18.sp, color = Ink300))
        }
    }
}

private fun deviceStatusLabel(status: DeviceWorkingStatus): String {
    return when (status) {
        DeviceWorkingStatus.WORKING -> "Working"
        DeviceWorkingStatus.SLEEPING -> "Sleeping"
        DeviceWorkingStatus.CONPRESSING -> "Conpressing"
    }
}

private fun deviceStatusColor(status: DeviceWorkingStatus): Color {
    return when (status) {
        DeviceWorkingStatus.WORKING -> DawnEmber
        DeviceWorkingStatus.SLEEPING -> Ink300
        DeviceWorkingStatus.CONPRESSING -> DawnDusk
    }
}

@Composable
private fun ApiModelSection(
    apiConfigs: List<ApiConfig>,
    activeApiId: String?,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onSelectApi: (String) -> Unit,
    onEditApi: (ApiConfig) -> Unit,
    onDeleteApi: (String) -> Unit,
    onAddApi: () -> Unit,
) {
    val activeConfig = apiConfigs.firstOrNull { it.id == activeApiId } ?: apiConfigs.firstOrNull()

    SectionHeader(label = "API & Model")
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Cream100)
    ) {
        ApiCurrentConfigCard(activeConfig = activeConfig)
        DividerLine()
        ApiExpandRow(
            isExpanded = isExpanded,
            count = apiConfigs.size,
            onClick = onToggleExpanded
        )
        if (isExpanded) {
            DividerLine()
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Cream50.copy(alpha = 0.72f))
                    .border(1.dp, Ink900.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "SAVED CONFIGS",
                    style = TextStyle(
                        fontFamily = FontMono,
                        fontSize = 9.sp,
                        letterSpacing = 0.18.sp,
                        color = Ink300
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (apiConfigs.isEmpty()) {
                    Text(
                        text = "还没有保存 API 配置。新增一组后，就可以在这里切换当前模型。",
                        style = TextStyle(
                            fontFamily = FontCnBody,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = Ink500
                        )
                    )
                } else {
                    apiConfigs.forEachIndexed { index, config ->
                        if (index > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Ink900.copy(alpha = 0.05f))
                            )
                        }
                        ApiConfigRow(
                            config = config,
                            isActive = config.id == activeConfig?.id,
                            onSelect = { onSelectApi(config.id) },
                            onEdit = { onEditApi(config) },
                            onDelete = { onDeleteApi(config.id) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onAddApi,
                    modifier = Modifier.align(Alignment.Start),
                    colors = ButtonDefaults.textButtonColors(contentColor = DawnEmber)
                ) {
                    Text(
                        text = "+ 新增配置",
                        style = TextStyle(
                            fontFamily = FontCnBody,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = DawnEmber
                        )
                    )
                }
            }
        }
    }
}

private enum class ApiDialogMode {
    ADD,
    EDIT,
}

@Composable
private fun ApiCurrentConfigCard(activeConfig: ApiConfig?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = activeConfig?.providerName ?: "当前未设置模型",
            style = TextStyle(
                fontFamily = FontCnBody,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Ink900
            )
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = activeConfig?.requestUrl?.ifBlank { "未填写 URL" } ?: "未填写 URL",
            style = TextStyle(
                fontFamily = FontMono,
                fontSize = 10.sp,
                letterSpacing = 0.04.sp,
                color = Ink500
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = activeConfig?.apiKey?.let(::maskApiKey) ?: "未填写 API Key",
            style = TextStyle(
                fontFamily = FontMono,
                fontSize = 10.sp,
                letterSpacing = 0.04.sp,
                color = Ink300
            )
        )
    }
}

@Composable
private fun ApiExpandRow(
    isExpanded: Boolean,
    count: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "切换模型",
                style = TextStyle(
                    fontFamily = FontCnBody,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Ink900
                )
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = if (count == 0) "点击展开并新增你的第一组 API 信息" else "已保存 $count 组 API 信息，点击展开管理",
                style = TextStyle(
                    fontFamily = FontCnBody,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    color = Ink500
                )
            )
        }
        Text(
            text = if (isExpanded) "∧" else "∨",
            style = TextStyle(
                fontFamily = FontMono,
                fontSize = 15.sp,
                color = DawnEmber
            )
        )
    }
}

@Composable
private fun ApiConfigRow(
    config: ApiConfig,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isActive) DawnHaze.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onSelect)
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(if (isActive) DawnEmber else Ink200)
            )
            Text(
                text = config.providerName,
                style = TextStyle(
                    fontFamily = FontCnBody,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Ink900
                ),
                modifier = Modifier.weight(1f)
            )
            if (isActive) {
                Text(
                    text = "当前",
                    style = TextStyle(
                        fontFamily = FontMono,
                        fontSize = 9.sp,
                        letterSpacing = 0.12.sp,
                        color = DawnEmber
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = config.requestUrl,
            style = TextStyle(
                fontFamily = FontMono,
                fontSize = 10.sp,
                color = Ink500,
                lineHeight = 15.sp
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = maskApiKey(config.apiKey),
            style = TextStyle(
                fontFamily = FontMono,
                fontSize = 10.sp,
                color = Ink300
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(
                onClick = onEdit,
                colors = ButtonDefaults.textButtonColors(contentColor = Ink700),
            ) {
                Text(
                    text = "编辑",
                    style = TextStyle(
                        fontFamily = FontCnBody,
                        fontSize = 12.sp,
                        color = Ink700
                    )
                )
            }
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = DawnEmber),
            ) {
                Text(
                    text = "删除",
                    style = TextStyle(
                        fontFamily = FontCnBody,
                        fontSize = 12.sp,
                        color = DawnEmber
                    )
                )
            }
        }
    }
}

@Composable
private fun ApiConfigDialog(
    mode: ApiDialogMode,
    name: String,
    apiKey: String,
    requestUrl: String,
    isError: Boolean,
    onNameChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onRequestUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Cream50,
                            Cream100.copy(alpha = 0.96f)
                        )
                    )
                )
                .border(1.dp, Ink900.copy(alpha = 0.06f), RoundedCornerShape(30.dp))
                .padding(horizontal = 22.dp, vertical = 24.dp)
        ) {
            Column {
                Text(
                    text = if (mode == ApiDialogMode.ADD) "NEW API MEMORY" else "REFINE API MEMORY",
                    style = TextStyle(
                        fontFamily = FontMono,
                        fontSize = 10.sp,
                        letterSpacing = 0.18.sp,
                        color = Ink300
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (mode == ApiDialogMode.ADD) "新增一组 API 配置" else "编辑这组 API 配置",
                    style = TextStyle(
                        fontFamily = FontCnDisplay,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Normal,
                        color = Ink900
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "请输入名称、API Key 与请求 URL。保存后，你可以在当前页面直接切换使用。",
                    style = TextStyle(
                        fontFamily = FontCnBody,
                        fontSize = 12.sp,
                        lineHeight = 19.sp,
                        color = Ink500
                    )
                )
                Spacer(modifier = Modifier.height(18.dp))
                ApiField(
                    label = "名称",
                    value = name,
                    placeholder = "例如 OpenAI / DeepSeek / 内部代理",
                    isError = isError && name.isBlank(),
                    onValueChange = onNameChange
                )
                Spacer(modifier = Modifier.height(12.dp))
                ApiField(
                    label = "API Key",
                    value = apiKey,
                    placeholder = "输入完整 API Key",
                    isError = isError && apiKey.isBlank(),
                    onValueChange = onApiKeyChange
                )
                Spacer(modifier = Modifier.height(12.dp))
                ApiField(
                    label = "URL",
                    value = requestUrl,
                    placeholder = "https://...",
                    isError = isError && requestUrl.isBlank(),
                    onValueChange = onRequestUrlChange
                )
                if (isError) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "三个字段都需要填写完整。",
                        style = TextStyle(
                            fontFamily = FontCnBody,
                            fontSize = 11.sp,
                            color = DawnEmber
                        )
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Ink900,
                        contentColor = Cream50
                    )
                ) {
                    Text(
                        text = if (mode == ApiDialogMode.ADD) "保存配置" else "保存修改",
                        style = TextStyle(
                            fontFamily = FontCnBody,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = Ink500)
                ) {
                    Text(
                        text = "再想想",
                        style = TextStyle(
                            fontFamily = FontCnBody,
                            fontSize = 13.sp,
                            color = Ink500
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ApiField(
    label: String,
    value: String,
    placeholder: String,
    isError: Boolean,
    onValueChange: (String) -> Unit,
) {
    Column {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = FontMono,
                fontSize = 9.sp,
                letterSpacing = 0.14.sp,
                color = Ink500
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            isError = isError,
            placeholder = {
                Text(
                    text = placeholder,
                    style = TextStyle(
                        fontFamily = FontCnBody,
                        fontSize = 12.sp,
                        color = Ink300
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Cream50.copy(alpha = 0.96f),
                unfocusedContainerColor = Cream50.copy(alpha = 0.9f),
                focusedBorderColor = DawnPeach.copy(alpha = 0.8f),
                unfocusedBorderColor = Ink900.copy(alpha = 0.08f),
                errorBorderColor = DawnEmber,
                focusedTextColor = Ink900,
                unfocusedTextColor = Ink900,
                cursorColor = DawnEmber
            ),
            textStyle = TextStyle(
                fontFamily = FontCnBody,
                fontSize = 13.sp,
                color = Ink900
            )
        )
    }
}

private fun maskApiKey(key: String): String {
    if (key.length <= 8) return "••••${key.takeLast(2)}"
    return "${key.take(4)}••••${key.takeLast(4)}"
}

@Composable
private fun PermissionsSection(permStates: List<androidx.compose.runtime.MutableState<Boolean>>) {
    val permLabels = listOf("麦克风", "蓝牙", "通知", "日历", "位置")

    SectionHeader(label = "Permissions")
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Cream100)
    ) {
        permLabels.forEachIndexed { i, label ->
            if (i > 0) DividerLine()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = TextStyle(
                        fontFamily = FontCnBody, fontSize = 14.sp,
                        fontWeight = FontWeight.Medium, color = Ink900
                    ),
                    modifier = Modifier.weight(1f)
                )
                AetherToggle(
                    checked = permStates[i].value,
                    onCheckedChange = { permStates[i].value = it }
                )
            }
        }
    }
}

@Composable
private fun AboutSection(
    onOpenProductPhilosophy: () -> Unit,
) {
    SectionHeader(label = "About")
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Cream100)
    ) {
        PSRow(
            title = "版本",
            detailText = "1.0.0 · build 284"
        )
        DividerLine()
        PSRow(
            title = "产品哲学",
            sub = "为何 Aether 这样说话",
            showArrow = true,
            onClick = onOpenProductPhilosophy
        )
        DividerLine()
        PSRow(title = "隐私政策", showArrow = true)
        DividerLine()
        PSRow(title = "退出登录", titleColor = DawnEmber, last = true)
    }
}

@Composable
private fun ClosingQuoteSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 44.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "\"The best interface is the one\nthat dims its own light\nwhen you don't need it.\"",
            style = TextStyle(
                fontFamily = FontDisplay, fontStyle = FontStyle.Italic,
                fontSize = 16.sp, fontWeight = FontWeight.Light,
                lineHeight = 24.sp, color = Ink300
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label.uppercase(),
        style = TextStyle(
            fontFamily = FontMono, fontSize = 10.sp,
            letterSpacing = 0.18.sp, color = Ink500
        ),
        modifier = Modifier.padding(start = 24.dp, bottom = 12.dp)
    )
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .height(1.dp)
            .fillMaxWidth()
            .background(Ink900.copy(alpha = 0.06f))
    )
}

@Composable
private fun PSRow(
    title: String,
    sub: String? = null,
    detailText: String? = null,
    showArrow: Boolean = false,
    last: Boolean = false,
    titleColor: Color = Ink900,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = FontCnBody, fontSize = 14.sp,
                    fontWeight = FontWeight.Medium, color = titleColor
                )
            )
            if (sub != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = sub,
                    style = TextStyle(
                        fontFamily = FontCnBody, fontSize = 11.sp,
                        lineHeight = 17.sp, color = Ink500
                    )
                )
            }
        }
        if (detailText != null) {
            Text(
                text = detailText,
                style = TextStyle(
                    fontFamily = FontMono, fontSize = 12.sp, color = Ink500
                )
            )
        }
        if (showArrow) {
            Text(text = "›", style = TextStyle(fontSize = 18.sp, color = Ink300))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PortraitGroupSection(
    group: PortraitGroup,
    onAdd: () -> Unit,
    onEdit: (PortraitCard) -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = group.label,
                style = TextStyle(
                    fontFamily = FontCnDisplay, fontSize = 16.sp,
                    fontWeight = FontWeight.Normal, color = Ink900
                )
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(Ink200.copy(alpha = 0.8f))
            )
            Text(
                text = if (expanded) "∧" else "∨",
                style = TextStyle(
                    fontFamily = FontMono,
                    fontSize = 11.sp,
                    color = Ink500
                )
            )
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                group.cards.forEach { card ->
                    PSPortraitCard(card = card, onClick = { onEdit(card) })
                }
                Box(
                    modifier = Modifier
                        .size(width = 88.dp, height = 64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Cream50.copy(alpha = 0.68f))
                        .border(
                            width = 1.dp,
                            color = Ink200.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable(onClick = onAdd),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "+",
                            style = TextStyle(
                                fontFamily = FontMono,
                                fontSize = 18.sp,
                                color = Ink300
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "新增",
                            style = TextStyle(
                                fontFamily = FontCnBody,
                                fontSize = 10.sp,
                                color = Ink500
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PSPortraitCard(card: PortraitCard, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Cream50.copy(alpha = 0.82f))
            .border(
                width = 1.dp,
                color = if (card.accent) DawnEmber.copy(alpha = 0.7f) else Ink200.copy(alpha = 0.7f),
                shape = RoundedCornerShape(18.dp)
            )
            .combinedClickable(
                onClick = {},
                onLongClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Column {
            Text(
                text = card.category.uppercase(),
                style = TextStyle(
                    fontFamily = FontMono, fontSize = 8.sp,
                    letterSpacing = 0.14.sp, color = DawnEmber
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = card.text,
                style = TextStyle(
                    fontFamily = FontDisplay, fontSize = 15.sp,
                    fontWeight = FontWeight.Normal, lineHeight = 19.sp, color = Ink900
                )
            )
            if (card.tail != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = card.tail,
                    style = TextStyle(
                        fontFamily = FontCnBody, fontSize = 10.sp, color = Ink500
                    )
                )
            }
        }
    }
}

@Composable
private fun EditProfileDialog(
    userName: String,
    avatarUri: Uri?,
    onNameChange: (String) -> Unit,
    onChooseAvatar: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val trimmedName = userName.trim()
    val isSaveEnabled = trimmedName.isNotEmpty()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Cream100)
                .border(1.dp, Ink200, RoundedCornerShape(28.dp))
                .padding(horizontal = 22.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "YOUR SPACE",
                style = TextStyle(
                    fontFamily = FontMono,
                    fontSize = 10.sp,
                    letterSpacing = 0.18.sp,
                    color = Ink500
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "编辑个人名片",
                style = TextStyle(
                    fontFamily = FontCnDisplay,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = Ink900
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "昵称与头像都会写入本地持久存储，不依赖缓存；头像选择后会立即更新。",
                style = TextStyle(
                    fontFamily = FontCnBody,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Ink500
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(DawnHaze, DawnEmber),
                            start = Offset(0f, 0f),
                            end = Offset(104f, 104f)
                        )
                    )
                    .clickable(onClick = onChooseAvatar),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUri != null) {
                    AsyncImage(
                        model = avatarUri,
                        contentDescription = "$userName avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = userName.firstOrNull()?.uppercase() ?: "A",
                        style = TextStyle(
                            fontFamily = FontDisplay,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Light,
                            color = Cream50
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onChooseAvatar) {
                Text(
                    text = "从相册选择头像",
                    style = TextStyle(
                        fontFamily = FontCnBody,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = DawnEmber
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = userName,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = {
                    Text(
                        text = "昵称",
                        style = TextStyle(
                            fontFamily = FontCnBody,
                            fontSize = 12.sp,
                            color = Ink500
                        )
                    )
                },
                textStyle = TextStyle(
                    fontFamily = FontDisplay,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Light,
                    color = Ink900
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Cream50,
                    unfocusedContainerColor = Cream50,
                    disabledContainerColor = Cream50,
                    focusedBorderColor = DawnEmber,
                    unfocusedBorderColor = Ink200,
                    focusedLabelColor = DawnEmber,
                    unfocusedLabelColor = Ink500,
                    cursorColor = DawnEmber
                ),
                shape = RoundedCornerShape(18.dp),
                supportingText = {
                    Text(
                        text = "这里是你在 Aether 里的称呼，不会改动画像中的真实姓名内容。",
                        style = TextStyle(
                            fontFamily = FontCnBody,
                            fontSize = 11.sp,
                            color = Ink500
                        )
                    )
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Cream50)
                ) {
                    Text(
                        text = "取消",
                        style = TextStyle(
                            fontFamily = FontCnBody,
                            fontSize = 14.sp,
                            color = Ink700
                        )
                    )
                }

                Button(
                    onClick = onSave,
                    enabled = isSaveEnabled,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DawnEmber,
                        contentColor = Cream50,
                        disabledContainerColor = Ink200,
                        disabledContentColor = Ink500
                    )
                ) {
                    Text(
                        text = "保存",
                        style = TextStyle(
                            fontFamily = FontCnBody,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

// ── Default data ──────────────────────────────────────────────────────

private fun defaultPortraitGroups() = listOf(
    PortraitGroup(
        label = "我是谁",
        cards = listOf(
            PortraitCard("Me", "Léa · 28 岁", "独立记者 · 她"),
            PortraitCard("Me", "住在巴黎", "2025 · 3月起"),
        )
    ),
    PortraitGroup(
        label = "我的人",
        cards = listOf(
            PortraitCard("Family", "爸爸在成都", "每月一次电话", accent = true),
            PortraitCard("Friend", "Mira — 周四读书会", "最亲近的朋友"),
        )
    ),
    PortraitGroup(
        label = "我在意的",
        cards = listOf(
            PortraitCard("Value", "把话说清楚，比说快更重要"),
            PortraitCard("Value", "答应的事要做到"),
        )
    ),
    PortraitGroup(
        label = "我喜欢的",
        cards = listOf(
            PortraitCard("Like", "清晨第一杯燕麦拿铁"),
            PortraitCard("Like", "坂本龙一 · Satie"),
        )
    ),
    PortraitGroup(
        label = "我的目标",
        cards = listOf(
            PortraitCard("Near", "这周完成专访稿"),
            PortraitCard("Long", "三十岁前出一本书"),
        )
    ),
)

internal fun portraitPreviewItems(
    portraitGroups: List<PortraitGroup>,
    limit: Int,
): List<String> {
    return portraitGroups
        .flatMap { group -> group.cards }
        .map { card -> card.text }
        .take(limit)
}

internal fun portraitTotalCount(portraitGroups: List<PortraitGroup>): Int {
    return portraitGroups.sumOf { it.cards.size }
}

internal fun addPortraitCardToGroups(
    portraitGroups: List<PortraitGroup>,
    targetLabel: String,
    content: String,
): List<PortraitGroup> {
    return portraitGroups.map { group ->
        if (group.label == targetLabel) {
            group.copy(
                cards = group.cards + PortraitCard(
                    category = portraitCategoryForGroup(targetLabel),
                    text = content,
                    tail = "刚刚添加"
                )
            )
        } else {
            group
        }
    }
}

internal fun updatePortraitCardInGroups(
    portraitGroups: List<PortraitGroup>,
    targetLabel: String,
    originalCard: PortraitCard,
    originalGroupLabel: String,
    content: String,
): List<PortraitGroup> {
    val cleaned = deletePortraitCardFromGroups(
        portraitGroups = portraitGroups,
        targetLabel = originalGroupLabel,
        targetCard = originalCard
    )
    return addPortraitCardToGroups(
        portraitGroups = cleaned,
        targetLabel = targetLabel,
        content = content
    ).map { group ->
        if (group.label == targetLabel) {
            group.copy(
                cards = group.cards.dropLast(1) + group.cards.last().copy(tail = "已更新")
            )
        } else {
            group
        }
    }
}

internal fun deletePortraitCardFromGroups(
    portraitGroups: List<PortraitGroup>,
    targetLabel: String,
    targetCard: PortraitCard,
): List<PortraitGroup> {
    return portraitGroups.map { group ->
        if (group.label == targetLabel) {
            var removed = false
            group.copy(
                cards = group.cards.filter {
                    if (!removed && it == targetCard) {
                        removed = true
                        false
                    } else {
                        true
                    }
                }
            )
        } else {
            group
        }
    }
}

internal fun resolvePreferredPortraitLabel(
    preferredLabel: String,
    portraitGroups: List<PortraitGroup>,
): String? {
    return preferredLabel
        .takeIf { label -> label.isNotBlank() && portraitGroups.any { it.label == label } }
        ?: portraitGroups.firstOrNull()?.label
}

private fun portraitTagOptions(portraitGroups: List<PortraitGroup>): List<PortraitTagOption> {
    return portraitGroups.map { group ->
        when (group.label) {
            "我是谁" -> PortraitTagOption(
                label = group.label,
                prompt = "写下你是谁",
                placeholder = "例如：跑步王 / 独立设计师 / 喜欢清晨出门的人",
                supporting = "适合填写身份、角色、个人自我描述。"
            )
            "我的人" -> PortraitTagOption(
                label = group.label,
                prompt = "写下重要的人",
                placeholder = "例如：Mira 是我每周都会见面的朋友",
                supporting = "适合补充家人、朋友、同事和重要关系。"
            )
            "我在意的" -> PortraitTagOption(
                label = group.label,
                prompt = "写下你在意的事",
                placeholder = "例如：把话说清楚，比说快更重要",
                supporting = "适合价值观、原则、判断标准。"
            )
            "我喜欢的" -> PortraitTagOption(
                label = group.label,
                prompt = "写下你喜欢的东西",
                placeholder = "例如：清晨散步、燕麦拿铁、坂本龙一",
                supporting = "适合记录偏好、习惯和让你开心的事。"
            )
            "我的目标" -> PortraitTagOption(
                label = group.label,
                prompt = "写下你的目标",
                placeholder = "例如：这周完成专访稿 / 三十岁前出一本书",
                supporting = "适合近期目标和长期愿望。"
            )
            else -> PortraitTagOption(
                label = group.label,
                prompt = "写下新的画像内容",
                placeholder = "例如：这是一条新的个人画像内容",
                supporting = "保存后会加入对应分组。"
            )
        }
    }
}

private fun portraitCategoryForGroup(label: String): String {
    return when (label) {
        "我是谁" -> "ME"
        "我的人" -> "BOND"
        "我在意的" -> "VALUE"
        "我喜欢的" -> "LIKE"
        "我的目标" -> "GOAL"
        else -> "NOTE"
    }
}
