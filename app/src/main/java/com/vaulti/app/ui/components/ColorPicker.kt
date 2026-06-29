package com.vaulti.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val PRESET_COLORS = listOf(
    0xFF6C63FFL,
    0xFF2196F3L,
    0xFF009688L,
    0xFF4CAF50L,
    0xFFFFEB3BL,
    0xFFFF9800L,
    0xFFF44336L,
    0xFFE91E63L,
    0xFF3F51B5L,
    0xFF795548L,
)

@Composable
fun ColorPicker(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var hexInput by remember { mutableStateOf(selectedColor.toString(16).substring(2).uppercase()) }

    LaunchedEffect(hexInput) {
        if (hexInput.length == 6 && hexInput.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' }) {
            onColorSelected(("FF" + hexInput).toLong(16))
        }
    }

    val previewHex = if (hexInput.length == 6 && hexInput.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' }) {
        ("FF" + hexInput).toLong(16)
    } else null

    Column(modifier = modifier) {
        Text(
            text = "Color",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PRESET_COLORS.take(5).forEach { color ->
                    val isSelected = color == selectedColor
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                            .then(
                                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier
                            )
                            .clickable {
                                onColorSelected(color)
                                hexInput = color.toString(16).substring(2).uppercase()
                            }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PRESET_COLORS.drop(5).forEach { color ->
                    val isSelected = color == selectedColor
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                            .then(
                                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier
                            )
                            .clickable {
                                onColorSelected(color)
                                hexInput = color.toString(16).substring(2).uppercase()
                            }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Or enter hex:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = hexInput,
                onValueChange = {
                    val filtered = it.filter { c -> c.isDigit() || c.uppercaseChar() in 'A'..'F' }.take(6).uppercase()
                    hexInput = filtered
                },
                label = { Text("#RRGGBB") },
                prefix = { Text("#") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (previewHex != null) Color(previewHex) else Color(selectedColor))
            )
        }
    }
}
