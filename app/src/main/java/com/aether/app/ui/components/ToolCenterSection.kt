package com.aether.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aether.app.data.ToolState
import kotlinx.coroutines.delay

private val DotAuthorizedColor   = Color(0xFF4CAF50)
private val DotUnauthorizedColor = Color(0xFF9E9E9E)

// ══════════════════════════════════════════════════════════════════════════════
//  单个工具行卡片
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun ToolCenterCard(
    tool: ToolState,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dotColor by animateColorAsState(
        targetValue   = if (tool.isAuthorized) DotAuthorizedColor else DotUnauthorizedColor,
        animationSpec = tween(400),
        label         = "dot_color_${tool.id}"
    )

    val iconScale = remember { Animatable(1f) }
    LaunchedEffect(tool.isAuthorized) {
        if (tool.isAuthorized) {
            iconScale.snapTo(1.35f)
            iconScale.animateTo(
                targetValue   = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessMedium
                )
            )
        }
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = tool.icon,
                    contentDescription = tool.name,
                    modifier           = Modifier
                        .size(22.dp)
                        .scale(iconScale.value),
                    tint = if (tool.isAuthorized) tool.iconTint
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text       = tool.name,
                    style      = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color      = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Switch(
                    checked         = tool.isAuthorized,
                    onCheckedChange = { onToggle() },
                    colors          = SwitchDefaults.colors(
                        checkedTrackColor   = DotAuthorizedColor,
                        checkedThumbColor   = Color.White,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  工具列表区域（含交错淡入动画）
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun ToolCenterSection(
    toolItems: List<ToolState>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier            = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        toolItems.forEachIndexed { index, tool ->
            key(tool.id) {
                val fadeAlpha = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    delay(index * 100L)
                    fadeAlpha.animateTo(1f, animationSpec = tween(300))
                }
                Box(modifier = Modifier.alpha(fadeAlpha.value)) {
                    ToolCenterCard(
                        tool     = tool,
                        onToggle = { onToggle(tool.id) }
                    )
                }
            }
        }
    }
}
