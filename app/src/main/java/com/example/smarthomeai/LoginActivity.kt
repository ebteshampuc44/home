package com.example.smarthomeai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = Firebase.auth
        if (auth.currentUser != null) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        setContent { LoginUI() }
    }
}

@Composable
fun LoginUI() {
    val context = LocalContext.current
    val auth = Firebase.auth
    var selectedTab by remember { mutableIntStateOf(0) }
    var emailInput by remember { mutableStateOf("") }
    var mobileInput by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val greenGradient = Brush.horizontalGradient(
        listOf(Color(0xFF2CF46F), Color(0xFF008F8F))
    )

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {

        // TOP HEADER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF2CF46F), Color(0xFF007A8A))
                    )
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 48.dp)
            ) {
                Text(
                    "HOME CONTROL",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    "MANAGE YOUR DEVICES AND ACCESSORIES",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Canvas(
                    modifier = Modifier.width(260.dp).height(180.dp)
                ) { drawHouseScene() }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Error Message
            if (showError) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE5E5)),
                    modifier = Modifier.width(300.dp)
                ) {
                    Text(
                        errorMessage,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Loading Indicator
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = Color(0xFF2CF46F)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // TAB SWITCHER
            Box(
                modifier = Modifier
                    .width(300.dp)
                    .height(48.dp)
                    .background(Color(0xFFE0E0E0), RoundedCornerShape(50.dp))
                    .padding(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Gmail Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .then(
                                if (selectedTab == 0) Modifier.background(
                                    greenGradient,
                                    RoundedCornerShape(50.dp)
                                ) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = { if (!isLoading) selectedTab = 0 },
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = if (selectedTab == 0) Color.Black else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Gmail",
                                color = if (selectedTab == 0) Color.Black else Color.Gray,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Mobile Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .then(
                                if (selectedTab == 1) Modifier.background(
                                    greenGradient,
                                    RoundedCornerShape(50.dp)
                                ) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = { if (!isLoading) selectedTab = 1 },
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                tint = if (selectedTab == 1) Color.Black else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Mobile",
                                color = if (selectedTab == 1) Color.Black else Color.Gray,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // EMAIL / MOBILE INPUT
            if (selectedTab == 0) {
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    placeholder = { Text("Gmail address", fontSize = 15.sp, color = Color.Gray) },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = Color.Gray)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    textStyle = TextStyle(color = Color.Black, fontSize = 15.sp),
                    shape = RoundedCornerShape(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF2CF46F),
                        unfocusedBorderColor = Color(0xFFDDDDDD),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = Color(0xFF008F8F)
                    ),
                    modifier = Modifier.width(300.dp).height(58.dp),
                    enabled = !isLoading
                )
            } else {
                OutlinedTextField(
                    value = mobileInput,
                    onValueChange = { mobileInput = it },
                    placeholder = { Text("Mobile number", fontSize = 15.sp, color = Color.Gray) },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    textStyle = TextStyle(color = Color.Black, fontSize = 15.sp),
                    shape = RoundedCornerShape(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF2CF46F),
                        unfocusedBorderColor = Color(0xFFDDDDDD),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = Color(0xFF008F8F)
                    ),
                    modifier = Modifier.width(300.dp).height(58.dp),
                    enabled = !isLoading
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // PASSWORD
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("Password", fontSize = 15.sp, color = Color.Gray) },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray)
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                textStyle = TextStyle(color = Color.Black, fontSize = 15.sp),
                shape = RoundedCornerShape(50.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF2CF46F),
                    unfocusedBorderColor = Color(0xFFDDDDDD),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    cursorColor = Color(0xFF008F8F)
                ),
                modifier = Modifier.width(300.dp).height(58.dp),
                enabled = !isLoading
            )

            // FORGOT PASSWORD
            Box(
                modifier = Modifier.width(300.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextButton(onClick = {}) {
                    Text(
                        "Forgot password?",
                        color = Color(0xFF008F8F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SIGN IN BUTTON
            Button(
                onClick = {
                    when {
                        selectedTab == 0 && emailInput.isBlank() -> {
                            showError = true
                            errorMessage = "Please enter your email address"
                        }
                        selectedTab == 1 && mobileInput.isBlank() -> {
                            showError = true
                            errorMessage = "Please enter your mobile number"
                        }
                        password.isBlank() -> {
                            showError = true
                            errorMessage = "Please enter your password"
                        }
                        else -> {
                            showError = false
                            isLoading = true

                            if (selectedTab == 0) {
                                // Email/Password Login with Verification Check
                                auth.signInWithEmailAndPassword(emailInput, password)
                                    .addOnCompleteListener { task ->
                                        isLoading = false
                                        if (task.isSuccessful) {
                                            val user = auth.currentUser

                                            // Check if email is verified
                                            if (user?.isEmailVerified == true) {
                                                val intent = Intent(context, HomeActivity::class.java)
                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                context.startActivity(intent)
                                                (context as? ComponentActivity)?.finish()
                                            } else {
                                                // Email not verified - sign out and show error
                                                auth.signOut()
                                                showError = true
                                                errorMessage = "Please verify your email address first. Check your inbox."
                                            }
                                        } else {
                                            showError = true
                                            errorMessage = task.exception?.message ?: "Login failed"
                                        }
                                    }
                            } else {
                                // Mobile number login - no verification needed
                                val emailForMobile = "$mobileInput@mobileuser.com"
                                auth.signInWithEmailAndPassword(emailForMobile, password)
                                    .addOnCompleteListener { task ->
                                        isLoading = false
                                        if (task.isSuccessful) {
                                            val intent = Intent(context, HomeActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            context.startActivity(intent)
                                            (context as? ComponentActivity)?.finish()
                                        } else {
                                            showError = true
                                            errorMessage = "Invalid mobile number or password. Please sign up first."
                                        }
                                    }
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                modifier = Modifier.width(300.dp).height(54.dp),
                enabled = !isLoading
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(greenGradient, shape = RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isLoading) "Signing In..." else "Sign In",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SIGN UP ROW
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Don't have an account? ", color = Color.Gray, fontSize = 13.sp)
                TextButton(
                    onClick = {
                        if (!isLoading) {
                            context.startActivity(Intent(context, SignupActivity::class.java))
                        }
                    }
                ) {
                    Text(
                        "Sign Up",
                        color = Color(0xFF008F8F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// HOUSE DRAWING FUNCTION
fun DrawScope.drawHouseScene() {
    val w = size.width
    val h = size.height

    drawRoundRect(
        color = Color(0x22000000),
        topLeft = Offset(0f, h * 0.88f),
        size = Size(w, h * 0.12f),
        cornerRadius = CornerRadius(8f)
    )
    drawRoundRect(color = Color(0xFF1A6E40), topLeft = Offset(w * 0.04f, h * 0.68f), size = Size(w * 0.05f, h * 0.22f), cornerRadius = CornerRadius(6f))
    drawCircle(color = Color(0xFF22A05A), radius = w * 0.09f, center = Offset(w * 0.065f, h * 0.62f))
    drawRoundRect(color = Color(0xFF1A6E40), topLeft = Offset(w * 0.91f, h * 0.70f), size = Size(w * 0.05f, h * 0.20f), cornerRadius = CornerRadius(6f))
    drawCircle(color = Color(0xFF22A05A), radius = w * 0.08f, center = Offset(w * 0.935f, h * 0.64f))
    drawRoundRect(color = Color.White, topLeft = Offset(w * 0.20f, h * 0.50f), size = Size(w * 0.60f, h * 0.40f), cornerRadius = CornerRadius(10f))
    val roofPath = Path().apply {
        moveTo(w * 0.14f, h * 0.52f)
        lineTo(w * 0.50f, h * 0.16f)
        lineTo(w * 0.86f, h * 0.52f)
        close()
    }
    drawPath(roofPath, color = Color(0xFFEEEEEE))
    drawRoundRect(color = Color(0xFFDDDDDD), topLeft = Offset(w * 0.62f, h * 0.22f), size = Size(w * 0.08f, h * 0.20f), cornerRadius = CornerRadius(4f))
    drawRoundRect(color = Color(0xFFCCCCCC), topLeft = Offset(w * 0.60f, h * 0.18f), size = Size(w * 0.12f, h * 0.06f), cornerRadius = CornerRadius(4f))
    drawRoundRect(color = Color(0xFF00BFAD), topLeft = Offset(w * 0.43f, h * 0.68f), size = Size(w * 0.14f, h * 0.22f), cornerRadius = CornerRadius(8f))
    drawCircle(color = Color.White, radius = w * 0.012f, center = Offset(w * 0.548f, h * 0.785f))
    drawRoundRect(color = Color(0xFFAEF0DF), topLeft = Offset(w * 0.24f, h * 0.56f), size = Size(w * 0.14f, h * 0.14f), cornerRadius = CornerRadius(6f))
    drawLine(color = Color(0xFF00A085), start = Offset(w * 0.24f, h * 0.63f), end = Offset(w * 0.38f, h * 0.63f), strokeWidth = 2f)
    drawLine(color = Color(0xFF00A085), start = Offset(w * 0.31f, h * 0.56f), end = Offset(w * 0.31f, h * 0.70f), strokeWidth = 2f)
    drawRoundRect(color = Color(0xFFAEF0DF), topLeft = Offset(w * 0.62f, h * 0.56f), size = Size(w * 0.14f, h * 0.14f), cornerRadius = CornerRadius(6f))
    drawLine(color = Color(0xFF00A085), start = Offset(w * 0.62f, h * 0.63f), end = Offset(w * 0.76f, h * 0.63f), strokeWidth = 2f)
    drawLine(color = Color(0xFF00A085), start = Offset(w * 0.69f, h * 0.56f), end = Offset(w * 0.69f, h * 0.70f), strokeWidth = 2f)
    drawCircle(color = Color(0xBB2CF46F), radius = w * 0.018f, center = Offset(w * 0.50f, h * 0.36f))
    drawCircle(color = Color(0x882CF46F), radius = w * 0.013f, center = Offset(w * 0.50f, h * 0.28f))
    drawCircle(color = Color(0x442CF46F), radius = w * 0.009f, center = Offset(w * 0.50f, h * 0.22f))
}