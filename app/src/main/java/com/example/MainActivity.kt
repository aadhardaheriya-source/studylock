package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppPreferences
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ScreenTimeScreen
import com.example.ui.screens.StudyTimeScreen
import com.example.ui.theme.StudyLockTheme
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealPrimary

enum class AppScreen {
    ONBOARDING,
    HOME,
    SCREEN_TIME,
    STUDY_TIME
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            StudyLockTheme {
                MainAppContent()
            }
        }
    }
}

@Composable
fun MainAppContent() {
    val context = LocalContext.current
    val initialScreen = if (AppPreferences.isOnboardingCompleted(context)) {
        AppScreen.HOME
    } else {
        AppScreen.ONBOARDING
    }

    var currentScreen by remember { mutableStateOf(initialScreen) }

    // Handle back button on secondary screens
    if (currentScreen != AppScreen.HOME && currentScreen != AppScreen.ONBOARDING) {
        BackHandler {
            currentScreen = AppScreen.HOME
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentScreen != AppScreen.ONBOARDING) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.HOME,
                        onClick = { currentScreen = AppScreen.HOME },
                        icon = {
                            Icon(Icons.Default.Home, contentDescription = "होम")
                        },
                        label = {
                            Text(
                                text = "होम",
                                fontWeight = if (currentScreen == AppScreen.HOME) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_home")
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.SCREEN_TIME,
                        onClick = { currentScreen = AppScreen.SCREEN_TIME },
                        icon = {
                            Icon(Icons.Default.HourglassBottom, contentDescription = "स्क्रीन टाइम")
                        },
                        label = {
                            Text(
                                text = "स्क्रीन टाइम",
                                fontWeight = if (currentScreen == AppScreen.SCREEN_TIME) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_screen_time")
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.STUDY_TIME,
                        onClick = { currentScreen = AppScreen.STUDY_TIME },
                        icon = {
                            Icon(Icons.Default.MenuBook, contentDescription = "स्टडी टाइम")
                        },
                        label = {
                            Text(
                                text = "स्टडी टाइम",
                                fontWeight = if (currentScreen == AppScreen.STUDY_TIME) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_study_time")
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                if (forward) {
                    (slideInHorizontally { width -> width / 3 } + fadeIn()) togetherWith
                            (slideOutHorizontally { width -> -width / 3 } + fadeOut())
                } else {
                    (slideInHorizontally { width -> -width / 3 } + fadeIn()) togetherWith
                            (slideOutHorizontally { width -> width / 3 } + fadeOut())
                }
            },
            label = "screenTransition",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { targetScreen ->
            when (targetScreen) {
                AppScreen.ONBOARDING -> {
                    OnboardingScreen(
                        onFinishOnboarding = {
                            currentScreen = AppScreen.HOME
                        }
                    )
                }
                AppScreen.HOME -> {
                    HomeScreen(
                        onNavigateToScreenTime = { currentScreen = AppScreen.SCREEN_TIME },
                        onNavigateToStudyTime = { currentScreen = AppScreen.STUDY_TIME },
                        onNavigateToOnboarding = { currentScreen = AppScreen.ONBOARDING }
                    )
                }
                AppScreen.SCREEN_TIME -> {
                    ScreenTimeScreen()
                }
                AppScreen.STUDY_TIME -> {
                    StudyTimeScreen()
                }
            }
        }
    }
}
