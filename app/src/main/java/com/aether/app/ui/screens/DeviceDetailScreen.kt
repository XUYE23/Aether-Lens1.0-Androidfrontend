package com.aether.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.app.DeviceUiState
import com.aether.app.data.DeviceState
import com.aether.app.data.ScannedDevice
import com.aether.app.ui.components.GlassesMark
import com.aether.app.ui.theme.Cream100
import com.aether.app.ui.theme.Cream200
import com.aether.app.ui.theme.Cream50
import com.aether.app.ui.theme.DawnDusk
import com.aether.app.ui.theme.DawnEmber
import com.aether.app.ui.theme.DawnHaze
import com.aether.app.ui.theme.DawnPeach
import com.aether.app.ui.theme.FontCnBody
import com.aether.app.ui.theme.FontCnDisplay
import com.aether.app.ui.theme.FontDisplay
import com.aether.app.ui.theme.FontMono
import com.aether.app.ui.theme.Ink200
import com.aether.app.ui.theme.Ink300
import com.aether.app.ui.theme.Ink500
import com.aether.app.ui.theme.Ink700
import com.aether.app.ui.theme.Ink900

enum class DeviceDetailMode {
    Detail,
    Switch,
}

private data class WeeklyWearBar(
    val label: String,
    val hours: Float,
    val isToday: Boolean = false,
)

@Composable
fun DeviceDetailScreen(
    userName: String,
    deviceUiState: DeviceUiState,
    mode: DeviceDetailMode = DeviceDetailMode.Detail,
    onBack: () -> Unit,
    onDisconnectDevice: (String) -> Unit = {},
    onOpenSwitchDevice: () -> Unit = {},
    onDismissSwitch: () -> Unit = {},
    onStartBluetoothScan: () -> Unit = {},
    onStopBluetoothScan: () -> Unit = {},
    onBluetoothPermissionDenied: () -> Unit = {},
    onConnectPrimaryDevice: (ScannedDevice) -> Unit = {},
) {
    val currentDevice = remember(deviceUiState.boundDevices) {
        deviceUiState.boundDevices.firstOrNull { it.isConnected }
            ?: deviceUiState.boundDevices.firstOrNull()
    }
    var showDisconnectSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream50)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            DawnHaze.copy(alpha = 0.44f),
                            DawnPeach.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(500f, 220f),
                        radius = 820f
                    )
                )
        )

        when (mode) {
            DeviceDetailMode.Detail -> {
                DeviceDetailContent(
                    userName = userName,
                    device = currentDevice,
                    onBack = onBack,
                    onOpenSwitchDevice = onOpenSwitchDevice,
                    onDisconnect = { showDisconnectSheet = true }
                )
            }

            DeviceDetailMode.Switch -> {
                SwitchDeviceContent(
                    userName = userName,
                    currentDevice = currentDevice,
                    scannedDevices = deviceUiState.scannedDevices.ifEmpty {
                        fallbackScannedDevices(currentDevice)
                    },
                    connectingMac = deviceUiState.connectingMac,
                    permissionDenied = deviceUiState.permissionDenied,
                    isScanning = deviceUiState.isScanning,
                    onBack = onBack,
                    onDismiss = onDismissSwitch,
                    onStartBluetoothScan = onStartBluetoothScan,
                    onStopBluetoothScan = onStopBluetoothScan,
                    onBluetoothPermissionDenied = onBluetoothPermissionDenied,
                    onConnectPrimaryDevice = onConnectPrimaryDevice
                )
            }
        }

        if (showDisconnectSheet && currentDevice != null) {
            DisconnectSheet(
                userName = userName,
                device = currentDevice,
                onDismiss = { showDisconnectSheet = false },
                onConfirm = {
                    onDisconnectDevice(currentDevice.id)
                    showDisconnectSheet = false
                    onBack()
                }
            )
        }
    }
}

@Composable
private fun DeviceDetailContent(
    userName: String,
    device: DeviceState?,
    onBack: () -> Unit,
    onOpenSwitchDevice: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val wearBars = remember { defaultWeeklyWearBars() }
    val todayHours = wearBars.last().hours
    val currentName = deviceDisplayName(device, userName)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 54.dp, bottom = 24.dp)
    ) {
        DeviceTopBar(
            title = "DEVICE",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(34.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                DawnPeach.copy(alpha = 0.28f),
                                DawnHaze.copy(alpha = 0.22f),
                                Color.Transparent
                            ),
                            radius = 320f
                        ),
                        shape = CircleShape
                    )
            )
            GlassesMark(
                modifier = Modifier
                    .width(168.dp)
                    .aspectRatio(200f / 110f),
                color = null,
                strokeWidth = 3.2f
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = currentName,
            style = TextStyle(
                fontFamily = FontDisplay,
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = (-0.56).sp,
                color = Ink900
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (device?.isConnected != false) DawnEmber else Ink300)
            )
            Text(
                text = buildString {
                    append(if (device?.isConnected != false) "CONNECTED" else "PAIRED")
                    append(" · FW 1.4.2")
                },
                style = TextStyle(
                    fontFamily = FontMono,
                    fontSize = 10.sp,
                    letterSpacing = 0.18.sp,
                    color = DawnEmber
                )
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        DeviceMetricCard(
            label = "BATTERY",
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${device?.batteryLevel ?: 72}%",
                style = TextStyle(
                    fontFamily = FontDisplay,
                    fontSize = 22.sp,
                    color = Ink900
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            BatteryBar(progress = ((device?.batteryLevel ?: 72) / 100f).coerceIn(0f, 1f))
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "约剩 5 小时",
                style = TextStyle(
                    fontFamily = FontCnBody,
                    fontSize = 11.sp,
                    color = Ink500
                )
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "今日佩戴时长",
                    style = TextStyle(
                        fontFamily = FontCnBody,
                        fontSize = 11.sp,
                        color = Ink500
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatWearDuration(todayHours),
                    style = TextStyle(
                        fontFamily = FontCnDisplay,
                        fontSize = 26.sp,
                        color = Ink900
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "比昨日少了 18 分钟。今天更安静。",
                    style = TextStyle(
                        fontFamily = FontCnBody,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = Ink300
                    )
                )
            }

            Text(
                text = "本周 35.3h",
                style = TextStyle(
                    fontFamily = FontMono,
                    fontSize = 10.sp,
                    color = Ink300
                )
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        WeeklyWearChart(
            bars = wearBars,
            modifier = Modifier
                .fillMaxWidth()
                .height(228.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailActionButton(
                label = "更换设备",
                filled = false,
                modifier = Modifier.weight(1f),
                onClick = onOpenSwitchDevice
            )
            DetailActionButton(
                label = "断开连接",
                filled = true,
                modifier = Modifier.weight(1f),
                onClick = onDisconnect
            )
        }
    }
}

@Composable
private fun SwitchDeviceContent(
    userName: String,
    currentDevice: DeviceState?,
    scannedDevices: List<ScannedDevice>,
    connectingMac: String?,
    permissionDenied: Boolean,
    isScanning: Boolean,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onStartBluetoothScan: () -> Unit,
    onStopBluetoothScan: () -> Unit,
    onBluetoothPermissionDenied: () -> Unit,
    onConnectPrimaryDevice: (ScannedDevice) -> Unit,
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            onStartBluetoothScan()
        } else {
            onBluetoothPermissionDenied()
        }
    }

    val requestScanPermissions = remember(permissionLauncher) {
        {
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
            permissionLauncher.launch(permissions)
        }
    }

    LaunchedEffect(Unit) {
        requestScanPermissions()
    }

    DisposableEffect(Unit) {
        onDispose { onStopBluetoothScan() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .padding(top = 54.dp, bottom = 24.dp)
    ) {
        DeviceTopBar(
            title = "SWITCH DEVICE",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "连接另一副 Aether",
            style = TextStyle(
                fontFamily = FontCnDisplay,
                fontSize = 22.sp,
                color = Ink900
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Your memory comes with you.",
            style = TextStyle(
                fontFamily = FontDisplay,
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Light,
                color = Ink300
            )
        )

        Spacer(modifier = Modifier.height(26.dp))

        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(168.dp)
                .clip(CircleShape)
                .border(1.dp, Cream200.copy(alpha = 0.72f), CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            DawnPeach.copy(alpha = 0.18f),
                            DawnHaze.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        radius = 290f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            GlassesMark(
                modifier = Modifier
                    .width(142.dp)
                    .aspectRatio(200f / 110f),
                color = null,
                strokeWidth = 3.1f
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "CURRENTLY PAIRED",
            style = TextStyle(
                fontFamily = FontMono,
                fontSize = 10.sp,
                letterSpacing = 0.22.sp,
                color = Ink500
            )
        )
        Spacer(modifier = Modifier.height(10.dp))

        DeviceListCard(
            title = deviceDisplayName(currentDevice, userName),
            subtitle = "AE-001 · 当前",
            leadingAccent = DawnPeach,
            trailingBars = 2,
            enabled = false,
            onClick = {}
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(DawnEmber)
            )
            Text(
                text = "NEARBY · ${scannedDevices.size} FOUND",
                style = TextStyle(
                    fontFamily = FontMono,
                    fontSize = 10.sp,
                    letterSpacing = 0.18.sp,
                    color = Ink500
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (permissionDenied) {
            Text(
                text = "需要蓝牙权限后，才能发现附近设备。",
                style = TextStyle(
                    fontFamily = FontCnBody,
                    fontSize = 11.sp,
                    color = Ink500
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
        } else if (isScanning) {
            Text(
                text = "正在扫描附近设备…",
                style = TextStyle(
                    fontFamily = FontCnBody,
                    fontSize = 11.sp,
                    color = Ink500
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
        ) {
            scannedDevices.forEachIndexed { index, scanned ->
                DeviceListCard(
                    title = scanned.name,
                    subtitle = "AE-${(index + 7).toString().padStart(3, '0')} · ${signalLabel(scanned.rssi)}",
                    leadingAccent = Cream100,
                    trailingBars = signalBars(scanned.rssi),
                    enabled = connectingMac != scanned.macAddress,
                    onClick = { onConnectPrimaryDevice(scanned) }
                )
                if (index != scannedDevices.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        DetailActionButton(
            label = if (permissionDenied) "重新授权" else "稍后再说",
            filled = false,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (permissionDenied) {
                    requestScanPermissions()
                } else {
                    onDismiss()
                }
            }
        )
    }
}

@Composable
private fun DisconnectSheet(
    userName: String,
    device: DeviceState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF8D8882).copy(alpha = 0.68f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Cream50)
                .clickable(enabled = false) {}
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Ink200)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "DISCONNECT",
                style = TextStyle(
                    fontFamily = FontMono,
                    fontSize = 10.sp,
                    letterSpacing = 0.24.sp,
                    color = Ink500
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "要和 ${device.deviceName}\n暂时分开吗？",
                style = TextStyle(
                    fontFamily = FontCnDisplay,
                    fontSize = 22.sp,
                    lineHeight = 30.sp,
                    color = Ink900
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "断开后它会保留所有记忆。随时可以再连回来。",
                style = TextStyle(
                    fontFamily = FontCnBody,
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    color = Ink500
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Cream100)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassesMark(
                    modifier = Modifier
                        .width(42.dp)
                        .aspectRatio(200f / 110f),
                    color = null,
                    strokeWidth = 3f
                )
                Column {
                    Text(
                        text = deviceDisplayName(device, userName),
                        style = TextStyle(
                            fontFamily = FontDisplay,
                            fontSize = 16.sp,
                            color = Ink900
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "已陪伴 28 天",
                        style = TextStyle(
                            fontFamily = FontCnBody,
                            fontSize = 11.sp,
                            color = Ink500
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            DetailActionButton(
                label = "确认断开",
                filled = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = onConfirm
            )

            Spacer(modifier = Modifier.height(12.dp))

            DetailActionButton(
                label = "再想想",
                filled = false,
                modifier = Modifier.fillMaxWidth(),
                onClick = onDismiss
            )
        }
    }
}

@Composable
private fun DeviceTopBar(
    title: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularIconButton(
            text = "‹",
            onClick = onBack
        )
        Text(
            text = title,
            style = TextStyle(
                fontFamily = FontMono,
                fontSize = 11.sp,
                letterSpacing = 0.28.sp,
                color = Ink500
            )
        )
        Spacer(modifier = Modifier.width(34.dp))
    }
}

@Composable
private fun CircularIconButton(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .border(1.dp, Ink200.copy(alpha = 0.72f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontFamily = FontMono,
                fontSize = 16.sp,
                color = Ink700
            )
        )
    }
}

@Composable
private fun DeviceMetricCard(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Cream100.copy(alpha = 0.92f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        content = {
            Text(
                text = label,
                style = TextStyle(
                    fontFamily = FontMono,
                    fontSize = 9.sp,
                    letterSpacing = 0.18.sp,
                    color = Ink500
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    )
}

@Composable
private fun BatteryBar(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Ink200.copy(alpha = 0.6f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(DawnEmber, DawnPeach)
                    )
                )
        )
    }
}

@Composable
private fun WeeklyWearChart(
    bars: List<WeeklyWearBar>,
    modifier: Modifier = Modifier,
) {
    val chartHeight = 168.dp
    val labelWidth = 76.dp
    val barWidth = 28.dp
    val maxHours = remember(bars) { chartMaxHours(bars) }
    val guideValues = remember(maxHours) {
        listOf(8f, 4f, 0f).filter { it <= maxHours }
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = labelWidth)
                    .height(chartHeight)
            ) {
                guideValues.forEach { guideValue ->
                    val fraction = if (maxHours == 0f) 0f else (guideValue / maxHours).coerceIn(0f, 1f)
                    val y = size.height - (size.height * fraction)
                    drawLine(
                        color = Ink200.copy(alpha = if (guideValue == 0f) 0.72f else 0.48f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = if (guideValue == 0f) 2f else 1f
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp),
                horizontalAlignment = Alignment.End
            ) {
                guideValues.forEachIndexed { index, guideValue ->
                    Text(
                        text = formatGuideHours(guideValue),
                        style = chartGuideTextStyle()
                    )
                    if (index != guideValues.lastIndex) {
                        val nextGuideValue = guideValues[index + 1]
                        val currentY = if (maxHours == 0f) 0f else (guideValue / maxHours) * chartHeight.value
                        val nextY = if (maxHours == 0f) 0f else (nextGuideValue / maxHours) * chartHeight.value
                        Spacer(modifier = Modifier.height((currentY - nextY - 14f).coerceAtLeast(0f).dp))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = labelWidth)
                    .align(Alignment.BottomStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                bars.forEach { bar ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = "${bar.hours}h",
                            style = TextStyle(
                                fontFamily = FontMono,
                                fontSize = 10.sp,
                                color = if (bar.isToday) DawnEmber else Ink500
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val barHeight = if (maxHours == 0f) 0f else (bar.hours / maxHours).coerceIn(0f, 1f) * chartHeight.value
                        Box(
                            modifier = Modifier
                                .width(barWidth)
                                .height(barHeight.dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (bar.isToday) DawnEmber.copy(alpha = 0.92f)
                                    else Ink700.copy(alpha = 0.74f)
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = labelWidth),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            bars.forEach { bar ->
                Box(
                    modifier = Modifier.width(barWidth),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = bar.label,
                        style = TextStyle(
                            fontFamily = FontCnBody,
                            fontSize = 11.sp,
                            color = if (bar.isToday) DawnEmber else Ink500
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun chartGuideTextStyle(): TextStyle {
    return TextStyle(
        fontFamily = FontMono,
        fontSize = 10.sp,
        color = Ink300
    )
}

private fun chartMaxHours(bars: List<WeeklyWearBar>): Float {
    return maxOf(8.5f, bars.maxOfOrNull { it.hours } ?: 0f)
}

private fun formatGuideHours(hours: Float): String {
    if (hours == 0f) return "0"
    val wholeHours = hours.toInt()
    return "${wholeHours}小时0分钟"
}

@Composable
private fun DetailActionButton(
    label: String,
    filled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (filled) Ink900 else Color.Transparent)
            .border(
                width = if (filled) 0.dp else 1.dp,
                color = Ink200.copy(alpha = 0.8f),
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = FontCnBody,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (filled) DawnPeach else Ink700
            )
        )
    }
}

@Composable
private fun DeviceListCard(
    title: String,
    subtitle: String,
    leadingAccent: Color,
    trailingBars: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Cream100.copy(alpha = if (enabled) 0.84f else 0.72f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Cream50),
            contentAlignment = Alignment.Center
        ) {
            PlayGlyph(color = if (enabled) DawnEmber else leadingAccent)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = FontDisplay,
                    fontSize = 16.sp,
                    color = Ink900
                )
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = TextStyle(
                    fontFamily = FontMono,
                    fontSize = 10.sp,
                    letterSpacing = 0.14.sp,
                    color = Ink300
                )
            )
        }

        SignalBars(
            bars = trailingBars,
            color = Ink500
        )
    }
}

@Composable
private fun PlayGlyph(color: Color) {
    Canvas(modifier = Modifier.size(10.dp)) {
        drawLine(
            color = color,
            start = Offset(size.width * 0.25f, size.height * 0.18f),
            end = Offset(size.width * 0.25f, size.height * 0.82f),
            strokeWidth = 1.6f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.25f, size.height * 0.18f),
            end = Offset(size.width * 0.78f, size.height * 0.5f),
            strokeWidth = 1.6f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.25f, size.height * 0.82f),
            end = Offset(size.width * 0.78f, size.height * 0.5f),
            strokeWidth = 1.6f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun SignalBars(
    bars: Int,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((6 + index * 3).dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (index < bars) color else Ink200.copy(alpha = 0.5f)
                    )
            )
        }
    }
}

private fun deviceDisplayName(device: DeviceState?, userName: String): String {
    return "${device?.deviceName ?: "Aether 01"} · $userName"
}

private fun defaultWeeklyWearBars(): List<WeeklyWearBar> {
    return listOf(
        WeeklyWearBar("周四", 5.1f),
        WeeklyWearBar("周五", 8.0f),
        WeeklyWearBar("周六", 2.8f),
        WeeklyWearBar("周日", 4.4f),
        WeeklyWearBar("周一", 8.5f),
        WeeklyWearBar("周二", 6.1f),
        WeeklyWearBar("今日", 2.6f, isToday = true),
    )
}

private fun formatWearDuration(hours: Float): String {
    val totalMinutes = (hours * 60).toInt()
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return "${h}小时${m}分钟"
}

private fun fallbackScannedDevices(currentDevice: DeviceState?): List<ScannedDevice> {
    val currentName = currentDevice?.deviceName ?: "Aether 01"
    return listOf(
        ScannedDevice(name = "Aether 02", macAddress = "AE:02:00:00", rssi = -48),
        ScannedDevice(name = currentName, macAddress = "AE:01:00:00", rssi = -61),
    )
}

private fun signalBars(rssi: Int): Int {
    return when {
        rssi >= -50 -> 4
        rssi >= -60 -> 3
        rssi >= -72 -> 2
        else -> 1
    }
}

private fun signalLabel(rssi: Int): String {
    return when (signalBars(rssi)) {
        4 -> "NEW"
        3 -> "NEAR"
        2 -> "FOUND"
        else -> "UNKNOWN"
    }
}
