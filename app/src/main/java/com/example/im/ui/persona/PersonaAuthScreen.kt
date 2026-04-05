package com.example.im.ui.persona

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PersonaAuthScreen(
    onAuthenticated: (name: String) -> Unit,
    vm: AuthViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()

    // Connect as soon as this screen is shown.
    LaunchedEffect(Unit) { vm.connect() }

    // Navigate away when auth succeeds.
    LaunchedEffect(state) {
        if (state is AuthState.Authenticated) {
            onAuthenticated((state as AuthState.Authenticated).name)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding(),
    ) {
        // ── Diagonal red accent strip ─────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .drawBehind {
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width - 48.dp.toPx(), size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(path, PersonaRed)
                },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
        ) {
            Spacer(Modifier.height(32.dp))

            // ── "LOGIN" title ─────────────────────────────────────────────
            Text(
                text = "LOGIN",
                fontFamily = PersonaFont,
                fontWeight = FontWeight.Black,
                fontSize = 64.sp,
                color = Color.White,
                letterSpacing = 4.sp,
            )

            // Red underline
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(4.dp)
                    .drawBehind {
                        val skew = 16.dp.toPx()
                        val path = Path().apply {
                            moveTo(skew, 0f)
                            lineTo(size.width, 0f)
                            lineTo(size.width - skew, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(path, PersonaRed)
                    },
            )

            Spacer(Modifier.height(48.dp))

            // ── Phone input (always visible once connected) ───────────────
            val showPhone = state is AuthState.WaitingPhone ||
                state is AuthState.WaitingCode ||
                state is AuthState.WaitingPassword

            AnimatedVisibility(
                visible = showPhone,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 },
            ) {
                var phone by remember { mutableStateOf("") }
                Column {
                    FieldLabel("PHONE NUMBER")
                    Spacer(Modifier.height(8.dp))
                    ParaInputField(
                        value = phone,
                        onValueChange = { phone = it },
                        placeholder = "+7 999 000-00-00",
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done,
                        onImeAction = { if (phone.isNotBlank()) vm.sendPhone(phone.trim()) },
                        enabled = state is AuthState.WaitingPhone,
                    )
                    Spacer(Modifier.height(20.dp))
                    ParaButton(
                        label = "SEND CODE",
                        enabled = state is AuthState.WaitingPhone && phone.isNotBlank(),
                        onClick = { vm.sendPhone(phone.trim()) },
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Code input ────────────────────────────────────────────────
            AnimatedVisibility(
                visible = state is AuthState.WaitingCode || state is AuthState.WaitingPassword,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 },
            ) {
                var code by remember { mutableStateOf("") }
                Column {
                    FieldLabel("VERIFICATION CODE")
                    Spacer(Modifier.height(8.dp))
                    ParaInputField(
                        value = code,
                        onValueChange = { code = it },
                        placeholder = "- - - - - -",
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                        onImeAction = { if (code.isNotBlank()) vm.sendCode(code.trim()) },
                        enabled = state is AuthState.WaitingCode,
                    )
                    Spacer(Modifier.height(20.dp))
                    ParaButton(
                        label = "VERIFY",
                        enabled = state is AuthState.WaitingCode && code.isNotBlank(),
                        onClick = { vm.sendCode(code.trim()) },
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── 2FA password ──────────────────────────────────────────────
            AnimatedVisibility(
                visible = state is AuthState.WaitingPassword,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 },
            ) {
                var pw by remember { mutableStateOf("") }
                Column {
                    FieldLabel("2FA PASSWORD")
                    Spacer(Modifier.height(8.dp))
                    ParaInputField(
                        value = pw,
                        onValueChange = { pw = it },
                        placeholder = "••••••••",
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        onImeAction = { if (pw.isNotBlank()) vm.sendPassword(pw) },
                        password = true,
                        enabled = state is AuthState.WaitingPassword,
                    )
                    Spacer(Modifier.height(20.dp))
                    ParaButton(
                        label = "CONFIRM",
                        enabled = state is AuthState.WaitingPassword && pw.isNotBlank(),
                        onClick = { vm.sendPassword(pw) },
                    )
                }
            }

            // ── Error / status ────────────────────────────────────────────
            when (state) {
                is AuthState.Error -> {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = (state as AuthState.Error).message,
                        fontFamily = PersonaFont,
                        fontSize = 13.sp,
                        color = PersonaRed,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    ParaButton(label = "RETRY", enabled = true, onClick = { vm.retry() })
                }
                is AuthState.Connecting -> StatusText("CONNECTING...")
                else -> {}
            }
        }
    }
}

// ── Shared sub-components ─────────────────────────────────────────────────────

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        fontFamily = PersonaFont,
        fontWeight = FontWeight.Black,
        fontSize = 11.sp,
        color = Color.White.copy(alpha = 0.55f),
        letterSpacing = 2.sp,
    )
}

@Composable
private fun StatusText(text: String) {
    Spacer(Modifier.height(24.dp))
    Text(
        text = text,
        fontFamily = PersonaFont,
        fontSize = 13.sp,
        color = Color.White.copy(alpha = 0.5f),
    )
}

/**
 * A text field drawn inside a skewed parallelogram with a white border —
 * matching the Persona 5 aesthetic of the chat bubbles.
 */
@Composable
private fun ParaInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
    password: Boolean = false,
    enabled: Boolean = true,
    skew: Dp = 18.dp,
) {
    val focusRequester = remember { FocusRequester() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .drawBehind {
                val skewPx = skew.toPx()
                val path = Path().apply {
                    moveTo(skewPx, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width - skewPx, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                // Fill
                drawPath(path, Color.Black)
                // White border
                drawPath(path, Color.White, style = Stroke(width = 2.5.dp.toPx()))
            }
            .pointerInput(enabled) {
                detectTapGestures(onTap = { if (enabled) focusRequester.requestFocus() })
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            textStyle = TextStyle(
                fontFamily = PersonaFont,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = Color.White,
            ),
            cursorBrush = SolidColor(PersonaRed),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(onAny = { onImeAction() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (skew + 8.dp), end = (skew + 8.dp))
                .focusRequester(focusRequester),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontFamily = PersonaFont,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color.White.copy(alpha = 0.3f),
                        )
                    }
                    inner()
                }
            },
        )
    }
}

/**
 * A red parallelogram button with white caps label — P5 action style.
 */
@Composable
private fun ParaButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    skew: Dp = 18.dp,
) {
    val fillColor = if (enabled) PersonaRed else Color(0xFF440000)
    Box(
        modifier = Modifier
            .fillMaxWidth(0.65f)
            .height(48.dp)
            .drawBehind {
                val skewPx = skew.toPx()
                val path = Path().apply {
                    moveTo(skewPx, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width - skewPx, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(path, fillColor)
                drawPath(path, Color.White, style = Stroke(width = 2.dp.toPx()))
            }
            .pointerInput(enabled) {
                detectTapGestures(onTap = { if (enabled) onClick() })
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontFamily = PersonaFont,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            color = Color.White,
            letterSpacing = 3.sp,
        )
    }
}
