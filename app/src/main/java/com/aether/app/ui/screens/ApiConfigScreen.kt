package com.aether.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aether.app.data.ApiConfig
import com.aether.app.ui.theme.AetherTheme
import com.aether.app.ui.theme.NeonPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiConfigScreen(
    onSave: (ApiConfig) -> Unit,
    onBack: () -> Unit
) {
    var providerName by remember { mutableStateOf("") }
    var remark       by remember { mutableStateOf("") }
    var websiteUrl   by remember { mutableStateOf("") }
    var apiKey       by remember { mutableStateOf("") }
    var requestUrl   by remember { mutableStateOf("") }
    var submitted    by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加自定义配置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ConfigField(
                value = providerName, onValueChange = { providerName = it },
                label = "供应商名称 *",
                isError = submitted && providerName.isBlank(), errorText = "供应商名称不能为空"
            )
            ConfigField(
                value = apiKey, onValueChange = { apiKey = it },
                label = "API Key *",
                isError = submitted && apiKey.isBlank(), errorText = "API Key 不能为空"
            )
            ConfigField(
                value = requestUrl, onValueChange = { requestUrl = it },
                label = "请求地址 *",
                isError = submitted && requestUrl.isBlank(), errorText = "请求地址不能为空"
            )
            ConfigField(value = remark, onValueChange = { remark = it }, label = "备注（选填）")
            ConfigField(value = websiteUrl, onValueChange = { websiteUrl = it }, label = "官网地址（选填）")

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    submitted = true
                    if (providerName.isNotBlank() && apiKey.isNotBlank() && requestUrl.isNotBlank()) {
                        onSave(ApiConfig(
                            providerName = providerName.trim(),
                            remark       = remark.trim(),
                            websiteUrl   = websiteUrl.trim(),
                            apiKey       = apiKey.trim(),
                            requestUrl   = requestUrl.trim()
                        ))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
            ) {
                Text("保存配置", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ConfigField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false,
    errorText: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        isError = isError,
        supportingText = if (isError) {
            { Text(errorText, color = MaterialTheme.colorScheme.error) }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonPurple,
            errorBorderColor = MaterialTheme.colorScheme.error
        )
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PreviewApiConfigScreen() {
    AetherTheme {
        ApiConfigScreen(onSave = {}, onBack = {})
    }
}
