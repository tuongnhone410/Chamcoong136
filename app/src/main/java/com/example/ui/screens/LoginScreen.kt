package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.NeonBlue
import com.example.viewmodel.TimeSnapViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: TimeSnapViewModel,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("timesnap_auth", android.content.Context.MODE_PRIVATE) }
    
    val savedRememberMe = remember { sharedPrefs.getBoolean("remember_me", false) }
    val savedEmail = remember { if (savedRememberMe) sharedPrefs.getString("saved_email", "") ?: "" else "" }
    val savedPassword = remember { if (savedRememberMe) sharedPrefs.getString("saved_password", "") ?: "" else "" }

    var email by remember { mutableStateFlowOf(savedEmail) }
    var password by remember { mutableStateFlowOf(savedPassword) }
    var rememberMe by remember { mutableStateOf(savedRememberMe) }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant Icon Clock Logo
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "App Logo",
                tint = NeonBlue,
                modifier = Modifier
                    .size(80.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "TimeSnap Pro",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Hệ thống Quản lý Chấm công & Lương chuyên nghiệp",
                color = MaterialTheme.colorScheme.onSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (successMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = successMessage ?: "",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Email text field
            OutlinedTextField(
                value = email,
                onValueChange = { 
                    email = it
                    errorMessage = null
                    successMessage = null
                },
                label = { Text("Mã nhân viên / Email") },
                leadingIcon = { Icon(Icons.Default.Person, "User Icon", tint = NeonBlue) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("email_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedLabelColor = NeonBlue,
                    focusedBorderColor = NeonBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.3f),
                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            // Password text field
            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    errorMessage = null
                    successMessage = null
                },
                label = { Text("Mật khẩu") },
                leadingIcon = { Icon(Icons.Default.Lock, "Lock Icon", tint = NeonBlue) },
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(icon, "Toggle Password")
                    }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("password_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedLabelColor = NeonBlue,
                    focusedBorderColor = NeonBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.3f),
                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            // Remember Account (Ghi nhớ tài khoản) checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = NeonBlue,
                        uncheckedColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f),
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("remember_me_checkbox")
                )
                Text(
                    text = "Ghi nhớ tài khoản",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Login Button
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Vui lòng nhập đầu đủ tên đăng nhập và mật khẩu!"
                        return@Button
                    }
                    keyboardController?.hide()
                    isLoading = true
                    coroutineScope.launch {
                        viewModel.authController.loginWithEmail(
                            usernameOrEmail = email,
                            password = password,
                            onSuccess = {
                                sharedPrefs.edit().apply {
                                    putBoolean("remember_me", rememberMe)
                                    if (rememberMe) {
                                        putString("saved_email", email)
                                        putString("saved_password", password)
                                    } else {
                                        remove("saved_email")
                                        remove("saved_password")
                                    }
                                    apply()
                                }
                                isLoading = false
                                onLoginSuccess()
                            },
                            onError = { err ->
                                isLoading = false
                                errorMessage = err
                            }
                        )
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("login_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("ĐĂNG NHẬP", color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Text links for Registration & Reset Password
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = {
                        val targetEmailOrUsername = email.trim()
                        if (targetEmailOrUsername.isEmpty()) {
                            errorMessage = "Vui lòng nhập Mã nhân viên hoặc Email vào ô trên, sau đó bấm 'Quên mật khẩu?' để nhận thư khôi phục!"
                            successMessage = null
                            return@TextButton
                        }
                        errorMessage = null
                        successMessage = null
                        isLoading = true
                        coroutineScope.launch {
                            viewModel.authController.sendPasswordResetEmail(
                                usernameOrEmail = targetEmailOrUsername,
                                onSuccess = {
                                    isLoading = false
                                    successMessage = "Hệ thống đã gửi một Email khôi phục mật khẩu thành công. Vui lòng kiểm tra hộp thư của bạn!"
                                },
                                onError = { err ->
                                    isLoading = false
                                    errorMessage = err
                                }
                            )
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text("Quên mật khẩu?", color = NeonBlue, fontSize = 13.sp)
                }

                TextButton(onClick = onNavigateToRegister) {
                    Text("Đăng ký ngay", color = NeonBlue, fontSize = 13.sp)
                }
            }
        }
    }
}

// HELPER FOR COMPOSE MUTABLESTATEFLOW STATE COUPLING
fun <T> rememberStateFlow(initial: T): MutableState<T> {
    return mutableStateOf(initial)
}

fun mutableStateFlowOf(value: String): MutableState<String> {
    return mutableStateOf(value)
}
