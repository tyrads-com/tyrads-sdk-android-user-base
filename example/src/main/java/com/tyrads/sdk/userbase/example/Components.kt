package com.tyrads.sdk.userbase.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .background(ExampleColors.card, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ExampleColors.cardTitleText)
        Spacer(Modifier.padding(top = 6.dp))
        content()
    }
}

@Composable
fun Hint(text: String) {
    Text(
        text,
        fontSize = 11.sp,
        color = ExampleColors.hintText,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

@Composable
fun LabeledInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(modifier = Modifier.padding(bottom = 10.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ExampleColors.inputLabel)
        Spacer(Modifier.padding(top = 4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = TextStyle(fontSize = 14.sp, color = ExampleColors.inputText),
            cursorBrush = SolidColor(ExampleColors.buttonPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .background(ExampleColors.inputBackground, RoundedCornerShape(8.dp))
                .border(1.dp, ExampleColors.inputBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(placeholder, fontSize = 14.sp, color = ExampleColors.placeholder)
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
fun ActionButton(
    label: String,
    onClick: () -> Unit,
    loading: Boolean = false,
    enabled: Boolean = true,
    danger: Boolean = false,
) {
    val isEnabled = enabled && !loading
    val bg = if (danger) ExampleColors.buttonDanger else ExampleColors.buttonPrimary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .background(if (isEnabled) bg else bg.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.heightIn(max = 16.dp))
        } else {
            Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EnvToggleRow(active: String, enabled: Boolean, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        EnvToggleButton("Production", active == "production", enabled) { onSelect("production") }
        EnvToggleButton("Staging", active == "staging", enabled) { onSelect("staging") }
    }
}

@Composable
private fun RowScope.EnvToggleButton(label: String, active: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val borderColor = if (active) ExampleColors.envButtonActiveBg else ExampleColors.inputBorder
    Box(
        modifier = Modifier
            .weight(1f)
            .background(if (active) ExampleColors.envButtonActiveBg else ExampleColors.inputBackground, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) Color.White else ExampleColors.envButtonText.copy(alpha = if (enabled) 1f else 0.45f),
        )
    }
}

@Composable
fun StatusPill(label: String) {
    Box(
        modifier = Modifier
            .background(ExampleColors.pillBackground, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, color = ExampleColors.pillText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

data class JsonResult(val ok: Boolean, val payload: String)

@Composable
fun JsonOutput(result: JsonResult?) {
    if (result == null) return
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .background(if (result.ok) ExampleColors.jsonBox else ExampleColors.jsonBoxError, RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (result.ok) "RESPONSE" else "ERROR",
                    color = ExampleColors.jsonLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (copied) "Copied ✓" else "Copy",
                    color = ExampleColors.copyLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        clipboard.setText(AnnotatedString(result.payload))
                        copied = true
                        scope.launch {
                            delay(1500)
                            copied = false
                        }
                    },
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(result.payload, color = ExampleColors.jsonText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
