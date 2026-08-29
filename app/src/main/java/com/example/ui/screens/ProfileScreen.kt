package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.LavenderAccent
import com.example.viewmodel.MeowViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MeowViewModel,
    onLogoutSuccess: () -> Unit
) {
    val profileState by viewModel.profile.collectAsState()
    val userState by viewModel.currentUser.collectAsState()
    val syncStatusState by viewModel.syncStatus.collectAsState()
    val isSyncing by viewModel.isSyncing
    val lastSyncedTime by viewModel.lastSyncedTimestamp

    val username = profileState?.displayName ?: "Gym Cat 🐾"
    val email = if (userState?.email != null && !userState!!.email!!.contains("anonymous")) {
        userState!!.email!!
    } else {
        "Anonymous Climber • ID: ${userState?.id?.take(8) ?: "Guest"}"
    }

    var isEditingName by remember { mutableStateOf(false) }
    var currentEditName by remember { mutableStateOf(username) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gym Profile 🐈🐾", fontWeight = FontWeight.Bold, fontSize = 20.sp) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // User Avatar Placeholder with playful styling
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFECE7)),
                contentAlignment = Alignment.Center
            ) {
                Text("😸", fontSize = 48.sp)
            }

            // User Display Name & Email
            if (isEditingName) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = currentEditName,
                        onValueChange = { currentEditName = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    IconButton(
                        onClick = {
                            isEditingName = false
                            if (currentEditName.isNotBlank() && userState != null) {
                                viewModel.updateProfileName(currentEditName)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = CoralPrimary)
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.clickable { isEditingName = true }
                ) {
                    Text(
                        text = username,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }

            Text(
                text = email,
                fontSize = 13.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Target Stats Dashboard Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Daily Cat Targets", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Weekly sessions goal", fontSize = 11.sp, color = Color.Gray)
                            Text("4 workouts", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CoralPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Completed this week", fontSize = 11.sp, color = Color.Gray)
                            Text("3 workouts 🐾", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LavenderAccent)
                        }
                    }
                }
            }

            // Sync Options Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Database & Live Cloud Syncing", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                        if (isSyncing) {
                            Text("Syncing in progress...", fontSize = 11.sp, color = CoralPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Last Synced: ${lastSyncedTime ?: "Not synced yet"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (lastSyncedTime != null) Color(0xFF10B981) else Color.Gray
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = syncStatusState,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = { viewModel.performTwoWaySync() },
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CoralPrimary,
                                disabledContainerColor = CoralPrimary.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Syncing...", fontSize = 11.sp, color = Color.White)
                            } else {
                                Icon(Icons.Default.CloudSync, contentDescription = "Sync", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync Now", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Logout CTA Button
            Button(
                onClick = { viewModel.handleLogout(onLogoutSuccess) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF9EBEB))
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, tint = Color.Red)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout Account", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
