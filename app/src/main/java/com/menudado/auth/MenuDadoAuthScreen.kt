package com.menudado.auth

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.menudado.R
import com.menudado.ui.theme.MenuDadoColors

enum class MenuDadoAuthFormMode {
    REGISTER,
    SIGN_IN
}

@Composable
fun MenuDadoAuthFormScreen(
    mode: MenuDadoAuthFormMode,
    isLoading: Boolean,
    errorMessage: String?,
    onSubmit: (String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onSwitchMode: (MenuDadoAuthFormMode) -> Unit
) {
    var email by rememberSaveable(mode.name) { mutableStateOf("") }
    var password by rememberSaveable(mode.name) { mutableStateOf("") }
    var repeatedPassword by rememberSaveable(mode.name) { mutableStateOf("") }
    val isRegisterMode = mode == MenuDadoAuthFormMode.REGISTER
    val passwordMatches = !isRegisterMode || password == repeatedPassword
    val canSubmit = email.isNotBlank() &&
        password.length >= MIN_PASSWORD_LENGTH &&
        passwordMatches &&
        !isLoading

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = stringResource(
                id = if (isRegisterMode) {
                    R.string.auth_create_account_title
                } else {
                    R.string.auth_sign_in_title
                }
            ),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MenuDadoColors.Ink
        )
        GoogleSignInButton(
            isLoading = isLoading,
            onClick = onGoogleSignIn
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            label = { Text(stringResource(id = R.string.auth_email_required)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            label = { Text(stringResource(id = R.string.auth_password_required)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        if (isRegisterMode) {
            OutlinedTextField(
                value = repeatedPassword,
                onValueChange = { repeatedPassword = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true,
                label = { Text(stringResource(id = R.string.auth_repeat_password_required)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
        }
        if (!passwordMatches) {
            Text(
                text = stringResource(id = R.string.auth_passwords_do_not_match),
                color = MenuDadoColors.Tomato,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MenuDadoColors.Tomato,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Button(
            onClick = { onSubmit(email, password) },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            enabled = canSubmit,
            colors = ButtonDefaults.buttonColors(containerColor = MenuDadoColors.DeepGreen)
        ) {
            AuthButtonContent(
                isLoading = isLoading,
                text = stringResource(
                    id = if (isRegisterMode) {
                        R.string.auth_register_free
                    } else {
                        R.string.auth_sign_in
                    }
                )
            )
        }
        TextButton(
            onClick = {
                onSwitchMode(
                    if (isRegisterMode) {
                        MenuDadoAuthFormMode.SIGN_IN
                    } else {
                        MenuDadoAuthFormMode.REGISTER
                    }
                )
            },
            enabled = !isLoading,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = stringResource(
                    id = if (isRegisterMode) {
                        R.string.auth_switch_to_sign_in
                    } else {
                        R.string.auth_switch_to_register
                    }
                ),
                color = MenuDadoColors.DeepGreen,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GoogleSignInButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .border(
                width = 1.dp,
                color = MenuDadoColors.OutlineBrown.copy(alpha = 0.28f),
                shape = RoundedCornerShape(8.dp)
            ),
        enabled = !isLoading,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MenuDadoColors.Surface,
            contentColor = MenuDadoColors.Ink
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = "G",
            color = Color(0xFF4285F4),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = stringResource(id = R.string.auth_continue_with_google),
            color = MenuDadoColors.MutedInk,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun AuthButtonContent(isLoading: Boolean, text: String) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = Color.White,
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.size(8.dp))
    }
    Text(text = text)
}

private const val MIN_PASSWORD_LENGTH = 6
