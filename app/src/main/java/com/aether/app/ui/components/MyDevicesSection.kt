package com.aether.app.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.app.DeviceUiState
import com.aether.app.DeviceViewModel
import com.aether.app.data.DeviceState
import com.aether.app.data.ScannedDevice
import com.aether.app.data.WorkStatus
import com.aether.app.ui.theme.GlassBorder
import com.aether.app.ui.theme.NeonPurple
import kotlinx.coroutines.delay

// ══════════════════════════════════════════════════════════════════════════════
//  入口：区域 3 "我的设备"
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDevicesSection(
    viewModel: DeviceViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // 权限申请 launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) viewModel.startBluetoothScan()
        else viewModel.onPermissionDenied()
    }

    fun requestScan() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION)
        }
        permissionLauncher.launch(perms)
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── 顶部标题行 ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "我的设备",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = {
                        viewModel.openScanSheet()
                        requestScan()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "扫描添加设备",
                        tint = NeonPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ── 设备列表区 ────────────────────────────────────────────────────
            if (uiState.boundDevices.isEmpty()) {
                EmptyDeviceState(
                    onAddClick = {
                        viewModel.openScanSheet()
                        requestScan()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp)
                ) {
                    items(
                        items = uiState.boundDevices,
                        key = { it.id }
                    ) { device ->
                        DeviceCard(
                            device = device,
                            onLongClick = { viewModel.cycleWorkStatus(device.id) }
                        )
                    }
                }
            }
        }
    }

    // ── 蓝牙扫描 BottomSheet ───────────────────────────────────────────────
    if (uiState.showScanSheet) {
        BluetoothScanBottomSheet(
            uiState = uiState,
            onDismiss = viewModel::dismissScanSheet,
            onDeviceClick = viewModel::connectDevice
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  空状态：首次进入引导
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun EmptyDeviceState(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Bluetooth,
            contentDescription = null,
            tint = GlassBorder.copy(alpha = 0.2f),
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "尚未绑定任何设备",
            style = MaterialTheme.typography.bodySmall,
            color = GlassBorder.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(onClick = onAddClick) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("添加设备", style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  DeviceCard — 弹簧展开 + ShimmerSweep + 交错内容
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeviceCard(
    device: DeviceState,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ── 高度弹簧动画：80dp(折叠) ↔ 120dp(展开) ────────────────────────────
    val transition = updateTransition(
        targetState = device.isConnected,
        label = "card_expand"
    )
    val cardHeight by transition.animateDp(
        transitionSpec = {
            spring(stiffness = 300f, dampingRatio = 0.75f)
        },
        label = "card_height"
    ) { connected -> if (connected) 120.dp else 80.dp }

    // ── ShimmerSweep：展开瞬间触发一次，不循环 ───────────────────────────
    val shimmerProgress = remember { Animatable(0f) }
    val shimmerValue by shimmerProgress.asState()
    var shimmerTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(device.isConnected) {
        if (device.isConnected && !shimmerTriggered) {
            shimmerTriggered = true
            shimmerProgress.snapTo(0f)
            shimmerProgress.animateTo(1f, tween(durationMillis = 300, easing = LinearOutSlowInEasing))
        }
        if (!device.isConnected) shimmerTriggered = false
    }

    // ── 工作状态行交错延迟 100ms ─────────────────────────────────────────
    var showStatusRow by remember { mutableStateOf(false) }
    LaunchedEffect(device.isConnected) {
        if (device.isConnected) {
            delay(100L)
            showStatusRow = true
        } else {
            showStatusRow = false
        }
    }

    val cardBackground = if (device.isConnected)
        NeonPurple.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val cardBorder = if (device.isConnected)
        NeonPurple.copy(alpha = 0.3f) else Color.Transparent

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(cardBackground)
            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            )
    ) {
        // ── 卡片主内容 ────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 状态圆点 + 设备名
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (device.isConnected) NeonPurple else GlassBorder.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )
                Text(
                    text = device.deviceName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (device.isConnected) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                if (!device.isConnected) {
                    Text(
                        text = "离线",
                        style = MaterialTheme.typography.labelSmall,
                        color = GlassBorder.copy(alpha = 0.4f)
                    )
                }
            }

            // 电量行：连接时立即显示
            AnimatedVisibility(
                visible = device.isConnected,
                enter = fadeIn(tween(200))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BatteryIcon(level = device.batteryLevel)
                    Text(
                        text = "${device.batteryLevel}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (device.batteryLevel > 20) Color(0xFF22C55E) else Color(0xFFEF4444)
                    )
                }
            }

            // 工作状态行：延迟 100ms + slideInVertically
            AnimatedVisibility(
                visible = showStatusRow,
                enter = slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(200)
                ) + fadeIn(tween(200))
            ) {
                WorkStatusChip(status = device.workStatus)
            }
        }

        // ── ShimmerSweepEffect：高光从左向右扫过 ──────────────────────────
        if (shimmerValue > 0f && shimmerValue < 1f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sweepX = size.width * shimmerProgress.value
                val shimmerWidth = size.width * 0.35f
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.45f),
                            Color.Transparent
                        ),
                        startX = sweepX - shimmerWidth / 2,
                        endX = sweepX + shimmerWidth / 2
                    ),
                    size = size
                )
            }
        }
    }
}

// ── 工作状态 Chip ──────────────────────────────────────────────────────────

@Composable
fun WorkStatusChip(status: WorkStatus) {
    val (label, color) = when (status) {
        WorkStatus.WORKING    -> "工作中" to Color(0xFF22C55E)
        WorkStatus.ORGANIZING -> "整理中" to Color(0xFFF59E0B)
        WorkStatus.SLEEPING   -> "休眠"   to GlassBorder.copy(alpha = 0.45f)
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  BatteryIcon — Canvas 绘制，含涨潮动画
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun BatteryIcon(
    level: Int,              // 0–100
    modifier: Modifier = Modifier,
    width: Dp = 28.dp,
    height: Dp = 16.dp
) {
    // 涨潮动画：800ms EaseInOut，level 变化时平滑过渡
    val animatedLevel by animateFloatAsState(
        targetValue = level / 100f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "battery_level"
    )

    val fillColor = if (level > 20) Color(0xFF22C55E) else Color(0xFFEF4444)
    val outlineColor = GlassBorder.copy(alpha = 0.5f)

    Canvas(modifier = modifier.size(width, height)) {
        val cornerRadius = 3.dp.toPx()
        val terminalWidth = 4.dp.toPx()
        val terminalHeight = 8.dp.toPx()
        val bodyWidth = size.width - terminalWidth
        val strokeWidth = 1.5.dp.toPx()
        val padding = strokeWidth

        // ── 电池外框 ──────────────────────────────────────────────────────
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(0f, 0f),
            size = Size(bodyWidth, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
            style = Stroke(width = strokeWidth)
        )

        // ── 电极凸起（右侧） ───────────────────────────────────────────────
        val terminalLeft = bodyWidth
        val terminalTop = (size.height - terminalHeight) / 2f
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(terminalLeft, terminalTop),
            size = Size(terminalWidth, terminalHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            style = Stroke(width = strokeWidth)
        )

        // ── 填充区（内部，按 animatedLevel 比例） ──────────────────────────
        val innerLeft = padding * 2
        val innerTop = padding * 2
        val innerMaxWidth = bodyWidth - padding * 4
        val innerHeight = size.height - padding * 4

        if (animatedLevel > 0f) {
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(innerLeft, innerTop),
                size = Size(innerMaxWidth * animatedLevel, innerHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  BluetoothScanBottomSheet — Aether 风格扫描弹窗
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScanBottomSheet(
    uiState: DeviceUiState,
    onDismiss: () -> Unit,
    onDeviceClick: (ScannedDevice) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // ── 顶部脉冲波纹区 ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                PulseRippleAnimation()
                Icon(
                    imageVector = Icons.Filled.Bluetooth,
                    contentDescription = "蓝牙扫描",
                    tint = NeonPurple,
                    modifier = Modifier.size(28.dp)
                )
            }

            // ── 扫描状态标题 ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (uiState.isScanning) "扫描周边设备…" else "扫描完成",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (uiState.isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = NeonPurple
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── 扫描结果列表 ───────────────────────────────────────────────
            if (uiState.scannedDevices.isEmpty() && !uiState.isScanning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "未发现设备",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassBorder.copy(alpha = 0.4f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp, vertical = 4.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = uiState.scannedDevices,
                        key = { it.macAddress }
                    ) { device ->
                        ScannedDeviceItem(
                            device = device,
                            isConnecting = uiState.connectingMac == device.macAddress,
                            onClick = { onDeviceClick(device) }
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  PulseRippleAnimation — 三圈无限紫色脉冲（错相 400ms）
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun PulseRippleAnimation() {
    val ripple1 = remember { Animatable(0f) }
    val ripple2 = remember { Animatable(0f) }
    val ripple3 = remember { Animatable(0f) }

    val ripples = listOf(ripple1, ripple2, ripple3)

    ripples.forEachIndexed { index, anim ->
        LaunchedEffect(Unit) {
            delay(index * 400L)
            while (true) {
                anim.snapTo(0f)
                anim.animateTo(1f, tween(1200, easing = LinearOutSlowInEasing))
            }
        }
    }

    val ripple1Value by ripple1.asState()
    val ripple2Value by ripple2.asState()
    val ripple3Value by ripple3.asState()

    Canvas(modifier = Modifier.size(90.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f

        listOf(ripple1Value, ripple2Value, ripple3Value).forEach { progress ->
            drawCircle(
                color = NeonPurple.copy(alpha = (1f - progress) * 0.35f),
                radius = maxRadius * (0.25f + 0.75f * progress),
                center = center
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ScannedDeviceItem — 扫描结果列表项
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScannedDeviceItem(
    device: ScannedDevice,
    isConnecting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = null),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LetterAvatar(
                letter = device.name.firstOrNull()?.uppercaseChar() ?: 'B',
                modifier = Modifier.size(38.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = device.macAddress,
                    style = MaterialTheme.typography.labelSmall,
                    color = GlassBorder.copy(alpha = 0.45f)
                )
            }

            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = NeonPurple
                )
            } else {
                SignalIcon(rssi = device.rssi)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  SignalIcon — Canvas 3格动态信号强度
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun SignalIcon(
    rssi: Int,
    modifier: Modifier = Modifier
) {
    val activeCount = when {
        rssi > -60  -> 3
        rssi > -75  -> 2
        rssi > -90  -> 1
        else        -> 0
    }

    Canvas(modifier = modifier.size(18.dp, 14.dp)) {
        val barCount = 3
        val barSpacing = 2.dp.toPx()
        val barWidth = (size.width - barSpacing * (barCount - 1)) / barCount
        val maxBarHeight = size.height

        for (i in 0 until barCount) {
            val barHeight = maxBarHeight * ((i + 1).toFloat() / barCount)
            val left = i * (barWidth + barSpacing)
            val top = maxBarHeight - barHeight
            val isActive = i < activeCount

            drawRoundRect(
                color = if (isActive) NeonPurple else GlassBorder.copy(alpha = 0.2f),
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  LetterAvatar — 字母头像（本地备用，与 PersonalNexusScreen 保持一致）
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun LetterAvatar(
    letter: Char,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(NeonPurple.copy(alpha = 0.15f))
            .border(1.5.dp, NeonPurple.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = NeonPurple
        )
    }
}
