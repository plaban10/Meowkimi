package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.PeachLight
import com.example.ui.theme.Slate100
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextLight
import com.example.viewmodel.ActiveSet

enum class SetFieldType {
    WEIGHT,
    REPS
}

data class FocusedSetTarget(
    val exerciseId: String,
    val setIndex: Int,
    val fieldType: SetFieldType
)

@Composable
fun SetRowComponent(
    exerciseId: String,
    setIndex: Int,
    setItem: ActiveSet,
    focusedTarget: FocusedSetTarget?,
    onFocusField: (FocusedSetTarget) -> Unit,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onToggleCompleted: () -> Unit
) {
    val isWeightFocused = focusedTarget?.exerciseId == exerciseId &&
            focusedTarget.setIndex == setIndex &&
            focusedTarget.fieldType == SetFieldType.WEIGHT

    val isRepsFocused = focusedTarget?.exerciseId == exerciseId &&
            focusedTarget.setIndex == setIndex &&
            focusedTarget.fieldType == SetFieldType.REPS

    val rowBgColor = when {
        setItem.isCompleted -> Color(0xFFF0FDF4)
        isWeightFocused || isRepsFocused -> Color(0xFFFFF9F7)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(rowBgColor)
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Set Number Badge (with PR badge if applicable)
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when {
                        setItem.isCompleted -> Color(0xFF22C55E)
                        setItem.isPr -> Color(0xFFFF9800)
                        else -> Slate100
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (setItem.isPr && setItem.isCompleted) {
                Text(
                    text = "★",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = Color.White
                )
            } else {
                Text(
                    text = "${setItem.setNumber}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (setItem.isCompleted) Color.White else TextDark
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // 2. Weight (KG) Input Box
        NumericInputBox(
            value = setItem.weight,
            isFocused = isWeightFocused,
            suffix = "kg",
            fieldType = SetFieldType.WEIGHT,
            onBoxClicked = {
                onFocusField(FocusedSetTarget(exerciseId, setIndex, SetFieldType.WEIGHT))
            },
            onValueChange = onWeightChange,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 3. Reps Input Box
        NumericInputBox(
            value = setItem.reps,
            isFocused = isRepsFocused,
            suffix = "reps",
            fieldType = SetFieldType.REPS,
            onBoxClicked = {
                onFocusField(FocusedSetTarget(exerciseId, setIndex, SetFieldType.REPS))
            },
            onValueChange = onRepsChange,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(4.dp))

        // 4. Complete / Checkbox Button & PR Badge (Right side of completion tick)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.widthIn(min = 40.dp)
        ) {
            IconButton(
                onClick = onToggleCompleted,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (setItem.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (setItem.isCompleted) "Completed Set" else "Incomplete Set",
                    tint = if (setItem.isCompleted) Color(0xFF22C55E) else Color(0xFFCBD5E1),
                    modifier = Modifier.size(26.dp)
                )
            }

            AnimatedVisibility(
                visible = setItem.isPr && setItem.isCompleted,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFFFF9800), Color(0xFFFF5722))
                            )
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "PR ⭐",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun NumericInputBox(
    value: String,
    isFocused: Boolean,
    suffix: String,
    fieldType: SetFieldType,
    onBoxClicked: () -> Unit,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isFocused) CoralPrimary else Color(0xFFE2E8F0)
    val borderWidth = if (isFocused) 2.dp else 1.dp
    val bgColor = if (isFocused) Color.White else Color(0xFFFAFAFA)
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(BorderStroke(borderWidth, borderColor), RoundedCornerShape(10.dp))
            .clickable { onBoxClicked() }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = { input ->
                val clean = when (fieldType) {
                    SetFieldType.WEIGHT -> {
                        // Allow digits and at most one decimal point
                        if (input.matches(Regex("^\\d*\\.?\\d*$")) && input.length <= 6) {
                            input
                        } else null
                    }
                    SetFieldType.REPS -> {
                        // Allow only integer digits
                        if (input.matches(Regex("^\\d*$")) && input.length <= 4) {
                            input
                        } else null
                    }
                }
                if (clean != null) {
                    onValueChange(clean)
                }
            },
            textStyle = TextStyle(
                color = Color(0xFF111827), // Explicit high contrast dark text
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (fieldType == SetFieldType.WEIGHT) KeyboardType.Decimal else KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            cursorBrush = SolidColor(CoralPrimary),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = if (fieldType == SetFieldType.WEIGHT) "0.0" else "0",
                            color = Color(0xFF9CA3AF),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun WorkoutNumericKeypad(
    focusedTarget: FocusedSetTarget,
    currentValue: String,
    onValueChange: (String) -> Unit,
    onNextField: () -> Unit,
    onCloseKeypad: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isWeight = focusedTarget.fieldType == SetFieldType.WEIGHT

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = Color.White,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Bar: Set info, quick increment chips, and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PeachLight)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Set ${focusedTarget.setIndex + 1}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoralPrimary
                        )
                    }

                    Text(
                        text = if (isWeight) "Weight (KG)" else "Reps Count",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }

                // Quick Increment Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val quickDeltas = if (isWeight) listOf(2.5, 5.0, 10.0) else listOf(1.0, 5.0, 10.0)
                    quickDeltas.forEach { delta ->
                        AssistChip(
                            onClick = {
                                if (isWeight) {
                                    val current = currentValue.toDoubleOrNull() ?: 0.0
                                    val updated = current + delta
                                    val formatted = if (updated % 1.0 == 0.0) updated.toInt().toString() else "%.1f".format(updated)
                                    onValueChange(formatted)
                                } else {
                                    val current = currentValue.toIntOrNull() ?: 0
                                    val updated = current + delta.toInt()
                                    onValueChange(updated.toString())
                                }
                            },
                            label = {
                                Text(
                                    text = if (delta % 1.0 == 0.0) "+${delta.toInt()}" else "+$delta",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CoralPrimary
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFFFF0EC)),
                            border = BorderStroke(0.5.dp, CoralPrimary.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(28.dp)
                        )
                    }

                    IconButton(
                        onClick = onCloseKeypad,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Keypad", tint = TextLight, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Keypad Grid (Rows 1-4)
            val buttons = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(if (isWeight) "." else "C", "0", "⌫")
            )

            buttons.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { label ->
                        KeypadButton(
                            label = label,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                when (label) {
                                    "⌫" -> {
                                        if (currentValue.isNotEmpty()) {
                                            onValueChange(currentValue.dropLast(1))
                                        }
                                    }
                                    "C" -> {
                                        onValueChange("")
                                    }
                                    "." -> {
                                        if (isWeight) {
                                            if (currentValue.isEmpty()) {
                                                onValueChange("0.")
                                            } else if (!currentValue.contains(".")) {
                                                onValueChange("$currentValue.")
                                            }
                                        }
                                    }
                                    else -> {
                                        // Numeric Digit
                                        if (currentValue == "0") {
                                            onValueChange(label)
                                        } else {
                                            val maxLen = if (isWeight) 6 else 4
                                            if (currentValue.length < maxLen) {
                                                onValueChange(currentValue + label)
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Action Row: Next & Done
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Text("Clear", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = onNextField,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary)
                ) {
                    Text("Next ➜", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = onCloseKeypad,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937))
                ) {
                    Text("Done ✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isAction = label == "⌫" || label == "C"
    val bgColor = if (isAction) Color(0xFFF1F5F9) else Color(0xFFF8FAFC)
    val textColor = if (isAction) Color(0xFF475569) else Color(0xFF0F172A)

    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = if (label == "⌫") 18.sp else 17.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}
