package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseApp
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.MeowViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.data.remote.FirebaseClient.initFirebase(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContainer()
            }
        }
    }
}

// Defines screens for simple state-based routing
sealed class AppScreen {
    object Home : AppScreen()
    object Exercises : AppScreen()
    object Workout : AppScreen()
    object History : AppScreen()
    object Profile : AppScreen()
    object ExercisePicker : AppScreen()
    object ExerciseDetail : AppScreen()
}

@Composable
fun MainAppContainer() {
    val viewModel: MeowViewModel = viewModel()
    val currentUserState by viewModel.currentUser.collectAsState()

    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }

    // Navigation back stack for Exercise Picker/Detail sub-routing
    val previousScreenStack = remember { mutableStateListOf<AppScreen>() }

    fun navigateTo(screen: AppScreen) {
        previousScreenStack.add(currentScreen)
        currentScreen = screen
    }

    fun navigateBack() {
        if (previousScreenStack.isNotEmpty()) {
            currentScreen = previousScreenStack.removeLast()
        } else {
            currentScreen = AppScreen.Home
        }
    }

    BackHandler(enabled = currentScreen != AppScreen.Home || previousScreenStack.isNotEmpty()) {
        navigateBack()
    }

    if (currentUserState == null) {
        // Authenticate Gate
        AuthScreen(viewModel = viewModel, onAuthSuccess = { currentScreen = AppScreen.Home })
    } else {
        // Authenticated Dashboard with standard M3 scaffold & Bottom Navigation
        Scaffold(
            bottomBar = {
                if (currentScreen != AppScreen.ExercisePicker && currentScreen != AppScreen.ExerciseDetail) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .navigationBarsPadding()
                    ) {
                        Divider(color = Slate100, thickness = 1.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BottomTabItem(
                                icon = "🏠",
                                label = "Home",
                                isSelected = currentScreen == AppScreen.Home,
                                onClick = {
                                    previousScreenStack.clear()
                                    currentScreen = AppScreen.Home
                                }
                            )

                            BottomTabItem(
                                icon = "📚",
                                label = "Exercises",
                                isSelected = currentScreen == AppScreen.Exercises,
                                onClick = {
                                    previousScreenStack.clear()
                                    currentScreen = AppScreen.Exercises
                                }
                            )

                            BottomTabItem(
                                icon = "🏋️",
                                label = "Workout",
                                isSelected = currentScreen == AppScreen.Workout,
                                onClick = {
                                    previousScreenStack.clear()
                                    currentScreen = AppScreen.Workout
                                }
                            )

                            BottomTabItem(
                                icon = "📅",
                                label = "History",
                                isSelected = currentScreen == AppScreen.History,
                                onClick = {
                                    previousScreenStack.clear()
                                    currentScreen = AppScreen.History
                                }
                            )

                            BottomTabItem(
                                icon = "😺",
                                label = "Profile",
                                isSelected = currentScreen == AppScreen.Profile,
                                onClick = {
                                    previousScreenStack.clear()
                                    currentScreen = AppScreen.Profile
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(BackgroundLight)
            ) {
                // Cross-Fade Animated Router
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "screens"
                ) { screen ->
                    when (screen) {
                        is AppScreen.Home -> HomeScreen(
                            viewModel = viewModel,
                            onStartWorkoutClick = { currentScreen = AppScreen.Workout },
                            onExploreExercisesClick = { currentScreen = AppScreen.Exercises },
                            onExerciseClick = { ex ->
                                viewModel.loadExerciseStats(ex)
                                navigateTo(AppScreen.ExerciseDetail)
                            }
                        )

                        is AppScreen.Exercises -> ExercisesScreen(
                            viewModel = viewModel,
                            onExerciseClick = { ex ->
                                viewModel.loadExerciseStats(ex)
                                navigateTo(AppScreen.ExerciseDetail)
                            },
                            onStartWorkoutWithExercise = { ex ->
                                if (!viewModel.isActiveWorkoutInProgress.value) {
                                    viewModel.startNewWorkout()
                                }
                                viewModel.addExerciseToActiveWorkout(ex)
                                currentScreen = AppScreen.Workout
                            }
                        )

                        is AppScreen.Workout -> StartWorkoutScreen(
                            viewModel = viewModel,
                            onAddExerciseClick = { navigateTo(AppScreen.ExercisePicker) },
                            onFinishWorkoutSuccess = { currentScreen = AppScreen.History }
                        )

                        is AppScreen.History -> HistoryScreen(
                            viewModel = viewModel,
                            onNavigateToWorkout = { currentScreen = AppScreen.Workout }
                        )

                        is AppScreen.Profile -> ProfileScreen(
                            viewModel = viewModel,
                            onLogoutSuccess = { currentScreen = AppScreen.Home }
                        )

                        is AppScreen.ExercisePicker -> ExercisePickerScreen(
                            viewModel = viewModel,
                            onBackClick = { navigateBack() },
                            onExerciseSelected = { ex ->
                                if (viewModel.isActiveWorkoutInProgress.value) {
                                    viewModel.addExerciseToActiveWorkout(ex)
                                }
                                navigateBack()
                            }
                        )

                        is AppScreen.ExerciseDetail -> ExerciseDetailScreen(
                            viewModel = viewModel,
                            onBackClick = { navigateBack() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomTabItem(
    icon: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CoralPrimary)
                    .size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        } else {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 18.sp,
                    modifier = Modifier.alpha(0.4f)
                )
            }
        }
        Text(
            text = label.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) CoralPrimary else com.example.ui.theme.TextLight,
            letterSpacing = 0.5.sp
        )
    }
}
