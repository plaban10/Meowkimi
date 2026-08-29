package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.LavenderAccent
import com.example.ui.theme.PeachLight
import com.example.viewmodel.MeowViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: MeowViewModel,
    onAuthSuccess: () -> Unit
) {
    val isSignUp = viewModel.isSignUpMode.value
    val isLoading = viewModel.isAuthLoading.value
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(PeachLight, Color.White)
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Branding Header
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "MeowMuscle Gym Cats Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Text(
                text = "MeowMuscle",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = CoralPrimary
            )

            Text(
                text = "Purr-fect Workouts, Heavy Gains 🐾",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Auth Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isSignUp) "Create MeowAccount" else "Welcome Back, Climber!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    // Display Name (Signup mode only)
                    AnimatedVisibility(visible = isSignUp) {
                        OutlinedTextField(
                            value = viewModel.authDisplayName.value,
                            onValueChange = { viewModel.authDisplayName.value = it },
                            label = { Text("Display Name / Cat Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = CoralPrimary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    // Email Input
                    OutlinedTextField(
                        value = viewModel.authEmail.value,
                        onValueChange = { viewModel.authEmail.value = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = CoralPrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Password Input
                    OutlinedTextField(
                        value = viewModel.authPassword.value,
                        onValueChange = { viewModel.authPassword.value = it },
                        label = { Text("Password (6+ chars)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CoralPrimary) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLoading) {
                        CircularProgressIndicator(
                            color = CoralPrimary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else {
                        // Submit Button
                        Button(
                            onClick = { viewModel.handleAuth(onAuthSuccess) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary)
                        ) {
                            Text(
                                text = if (isSignUp) "Sign Up 🐾" else "Sign In 🐾",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        // OR Separator
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Divider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
                            Text(
                                text = " OR ",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Divider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
                        }

                        // Google Sign-In Button
                        val context = androidx.compose.ui.platform.LocalContext.current
                        OutlinedButton(
                            onClick = { viewModel.signInWithGoogle(context, onAuthSuccess) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray),
                            border = BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "🌐  Sign in with Google",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextDark
                                )
                            }
                        }
                    }
                }
            }

            // Mode switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSignUp) "Already have an account? " else "Don't have an account? ",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Text(
                    text = if (isSignUp) "Login here" else "Register here",
                    fontSize = 13.sp,
                    color = CoralPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { viewModel.setSignUpMode(!isSignUp) }
                )
            }
        }
    }
}
