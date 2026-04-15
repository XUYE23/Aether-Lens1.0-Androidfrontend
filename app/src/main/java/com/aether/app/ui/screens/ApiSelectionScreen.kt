package com.aether.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aether.app.data.ApiConfig
import com.aether.app.ui.theme.AetherTheme
import com.aether.app.ui.theme.NeonPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiSelectionScreen(
    configs: List<ApiConfig>,
    activeApiId: String?,
    onSelect: (String?) -> Unit,
    onAddClick: () -> Unit,
    onBack: () -> Unit
) {
    var tempSelectedApiId by remember { mutableStateOf(activeApiId) }
    val hasChanged = tempSelectedApiId != activeApiId

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择 API") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick, containerColor = NeonPurple) {
                Icon(Icons.Filled.Add, contentDescription = "添加配置", tint = androidx.compose.ui.graphics.Color.White)
            }
        },
        bottomBar = {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Button(
                    onClick = { onSelect(tempSelectedApiId) },
                    enabled = hasChanged,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Text("确定", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(configs, key = { it.id }) { config ->
                val isTemp    = config.id == tempSelectedApiId
                val isOldActive = config.id == activeApiId && hasChanged
                val borderColor = when {
                    isTemp      -> NeonPurple
                    isOldActive -> MaterialTheme.colorScheme.error
                    else        -> null
                }
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (borderColor != null) Modifier.border(2.dp, borderColor, RoundedCornerShape(16.dp))
                            else Modifier
                        )
                        .clickable { tempSelectedApiId = config.id },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LetterAvatar(letter = config.providerName.first().uppercaseChar())
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = config.providerName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = config.requestUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PreviewApiSelectionScreen() {
    AetherTheme {
        ApiSelectionScreen(
            configs = listOf(
                ApiConfig(id = "1", providerName = "OpenAI", apiKey = "sk-xxx", requestUrl = "https://api.openai.com/v1/chat/completions"),
                ApiConfig(id = "2", providerName = "DeepSeek", apiKey = "sk-yyy", requestUrl = "https://api.deepseek.com/v1/chat/completions", remark = "性价比首选"),
                ApiConfig(id = "3", providerName = "Moonshot", apiKey = "sk-zzz", requestUrl = "https://api.moonshot.cn/v1/chat/completions")
            ),
            activeApiId = "2",
            onSelect = {},
            onAddClick = {},
            onBack = {}
        )
    }
}
