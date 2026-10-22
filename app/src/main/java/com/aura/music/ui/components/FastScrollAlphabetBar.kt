package com.aura.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val AlphabetLetters = (('A'..'Z') + '#').toList()

@Composable
fun FastScrollAlphabetBar(
    availableLetters: Set<Char>,
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    var barHeight by remember { mutableStateOf(0) }
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(28.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .padding(vertical = 4.dp, horizontal = 2.dp)
                .onGloballyPositioned { coordinates ->
                    barHeight = coordinates.size.height
                }
                .pointerInput(barHeight, availableLetters) {
                    detectTapGestures { offset ->
                        if (barHeight > 0) {
                            val itemHeight = barHeight.toFloat() / AlphabetLetters.size
                            val index = (offset.y / itemHeight).toInt().coerceIn(0, AlphabetLetters.lastIndex)
                            val letter = AlphabetLetters[index]
                            activeLetter = letter
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onLetterSelected(letter)
                        }
                    }
                }
                .pointerInput(barHeight, availableLetters) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            if (barHeight > 0) {
                                val itemHeight = barHeight.toFloat() / AlphabetLetters.size
                                val index = (offset.y / itemHeight).toInt().coerceIn(0, AlphabetLetters.lastIndex)
                                val letter = AlphabetLetters[index]
                                activeLetter = letter
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onLetterSelected(letter)
                            }
                        },
                        onDragEnd = {
                            activeLetter = null
                        },
                        onDragCancel = {
                            activeLetter = null
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            if (barHeight > 0) {
                                val itemHeight = barHeight.toFloat() / AlphabetLetters.size
                                val index = (change.position.y / itemHeight).toInt().coerceIn(0, AlphabetLetters.lastIndex)
                                val letter = AlphabetLetters[index]
                                if (letter != activeLetter) {
                                    activeLetter = letter
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onLetterSelected(letter)
                                }
                            }
                        }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AlphabetLetters.forEach { letter ->
                val hasSongs = availableLetters.contains(letter)
                val isSelected = activeLetter == letter

                Text(
                    text = letter.toString(),
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else if (hasSongs) FontWeight.SemiBold else FontWeight.Normal,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        hasSongs -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                    },
                    modifier = Modifier.padding(vertical = 0.5.dp)
                )
            }
        }
    }
}

@Composable
fun FastScrollPopupBubble(
    letter: Char?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = letter != null,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        if (letter != null) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
