package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppInfo
import com.example.data.AppPreferences
import com.example.ui.components.bounceClick
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.WarningAmber
import com.example.util.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyTimeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isStudyActive by remember { mutableStateOf(AppPreferences.isStudyTimeActive(context)) }
    var selectedDurationMinutes by remember { mutableIntStateOf(AppPreferences.getStudyTimeDurationMinutes(context)) }

    var blockedPackages by remember { mutableStateOf(AppPreferences.getStudyTimeBlockedApps(context)) }
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    var showAddAppsSheet by remember { mutableStateOf(false) }
    var addAppSearchQuery by remember { mutableStateOf("") }
    var showStopConfirmDialog by remember { mutableStateOf(false) }
    var showCelebrationDialog by remember { mutableStateOf(false) }

    // Countdown state
    var remainingMillis by remember { mutableLongStateOf(0L) }
    var totalMillis by remember { mutableLongStateOf(0L) }

    fun refreshInstalledApps() {
        scope.launch(Dispatchers.IO) {
            val apps = UsageStatsHelper.getInstalledApps(context)
            withContext(Dispatchers.Main) {
                installedApps = apps
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshInstalledApps()
    }

    // Timer ticker
    LaunchedEffect(isStudyActive) {
        if (isStudyActive) {
            val endTime = AppPreferences.getStudyTimeEndTime(context)
            val startTime = AppPreferences.getStudyTimeStartTime(context)
            totalMillis = (endTime - startTime).coerceAtLeast(1000L)

            while (isStudyActive) {
                val now = System.currentTimeMillis()
                val left = endTime - now
                if (left <= 0) {
                    remainingMillis = 0L
                    AppPreferences.stopStudyTime(context)
                    isStudyActive = false
                    showCelebrationDialog = true
                    break
                } else {
                    remainingMillis = left
                }
                delay(1000L)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isStudyActive) {
            // Active Countdown Screen
            StudyActiveTimerView(
                remainingMillis = remainingMillis,
                totalMillis = totalMillis,
                onStopClicked = { showStopConfirmDialog = true }
            )
        } else {
            // Configuration & Start Screen
            StudySetupView(
                selectedDurationMinutes = selectedDurationMinutes,
                onDurationChanged = { selectedDurationMinutes = it },
                blockedPackages = blockedPackages,
                installedApps = installedApps,
                onAddAppClicked = {
                    addAppSearchQuery = ""
                    showAddAppsSheet = true
                },
                onRemoveBlockedApp = { pkg ->
                    AppPreferences.removeStudyTimeBlockedApp(context, pkg)
                    blockedPackages = AppPreferences.getStudyTimeBlockedApps(context)
                },
                onStartStudy = {
                    AppPreferences.startStudyTime(context, selectedDurationMinutes)
                    isStudyActive = true
                }
            )
        }
    }

    // Confirmation dialog for stopping early
    if (showStopConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showStopConfirmDialog = false },
            title = {
                Text(
                    text = "स्टडी टाइम समाप्त करें?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "क्या आप वाकई अपना फोकस सेशन जल्दी समाप्त करना चाहते हैं? ऐप्स तुरंत अनलॉक हो जाएंगे।",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        AppPreferences.stopStudyTime(context)
                        isStudyActive = false
                        showStopConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_stop_study_button")
                ) {
                    Text("हाँ, समाप्त करें")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showStopConfirmDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("रद्द करें")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Celebration dialog on completion
    if (showCelebrationDialog) {
        AlertDialog(
            onDismissRequest = { showCelebrationDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "शाबाश",
                        tint = SuccessGreen,
                        modifier = Modifier.size(36.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "शाबाश! 🎉",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "आपने सफलतापूर्वक अपना स्टडी सेशन पूरा कर लिया है! सभी ऐप्स अब अनलॉक कर दिए गए हैं।",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { showCelebrationDialog = false },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("celebration_done_button")
                ) {
                    Text("बहुत बढ़िया!", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Add Apps Sheet
    if (showAddAppsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddAppsSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "स्टडी टाइम में लॉक करने के लिए ऐप जोड़ें",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = addAppSearchQuery,
                    onValueChange = { addAppSearchQuery = it },
                    placeholder = { Text("ऐप खोजें...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (addAppSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { addAppSearchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "साफ़ करें")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                val availableToAdd = remember(addAppSearchQuery, installedApps, blockedPackages) {
                    installedApps.filter { app ->
                        !blockedPackages.contains(app.packageName) &&
                                (addAppSearchQuery.isBlank() || app.appName.contains(addAppSearchQuery, ignoreCase = true))
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableToAdd, key = { it.packageName }) { app ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIconView(icon = app.icon, appName = app.appName, size = 40)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        AppPreferences.addStudyTimeBlockedApp(context, app.packageName)
                                        blockedPackages = AppPreferences.getStudyTimeBlockedApps(context)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("जोड़ें")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudySetupView(
    selectedDurationMinutes: Int,
    onDurationChanged: (Int) -> Unit,
    blockedPackages: Set<String>,
    installedApps: List<AppInfo>,
    onAddAppClicked: () -> Unit,
    onRemoveBlockedApp: (String) -> Unit,
    onStartStudy: () -> Unit
) {
    val blockedAppsList = remember(blockedPackages, installedApps) {
        val appMap = installedApps.associateBy { it.packageName }
        blockedPackages.map { pkg ->
            appMap[pkg] ?: AppInfo(packageName = pkg, appName = pkg)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header
            Text(
                text = "स्टडी टाइम मोड",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "पढ़ाई की अवधि चुनें, विचलित करने वाले ऐप्स ब्लॉक रहेंगे",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Duration selector card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "स्टडी अवधि चुनें",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset chips
                    val presets = listOf(
                        15 to "15 मिनट",
                        25 to "25 मिनट (पोमोडोरो)",
                        45 to "45 मिनट",
                        60 to "1 घंटा",
                        90 to "1.5 घंटे",
                        120 to "2 घंटे",
                        180 to "3 घंटे"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.take(3).forEach { (mins, label) ->
                            FilterChip(
                                selected = selectedDurationMinutes == mins,
                                onClick = { onDurationChanged(mins) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.drop(3).take(4).forEach { (mins, label) ->
                            FilterChip(
                                selected = selectedDurationMinutes == mins,
                                onClick = { onDurationChanged(mins) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "समय: ${UsageStatsHelper.formatMinutesToHindi(selectedDurationMinutes)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Slider(
                        value = selectedDurationMinutes.toFloat(),
                        onValueChange = { onDurationChanged(it.toInt()) },
                        valueRange = 15f..360f,
                        steps = 22,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("study_duration_slider")
                    )
                }
            }
        }

        // YouTube special rule banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = TealPrimary.copy(alpha = 0.12f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TealPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = TealAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "यूट्यूब स्पेशल नियम",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TealAccent
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "स्टडी टाइम में यूट्यूब पर केवल पढ़ाई के वीडियो चलेंगे। विचलित करने वाले शॉर्ट्स (Shorts) स्क्रीन पर आते ही अपने आप बंद हो जाएँगे।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Blocked apps list section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "लॉक होने वाले ऐप (${blockedAppsList.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "स्टडी सेशन के दौरान ये ऐप पूरी तरह बंद रहेंगे",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onAddAppClicked,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.bounceClick { onAddAppClicked() }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ऐप जोड़ें", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (blockedAppsList.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "कोई ऐप ब्लॉक सूची में नहीं है। आप ऊपर बटन दबाकर किसी भी गेम या सोशल ऐप को जोड़ सकते हैं।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(blockedAppsList, key = { it.packageName }) { app ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIconView(icon = app.icon, appName = app.appName, size = 42)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "स्टडी टाइम में लॉक रहेगा",
                                style = MaterialTheme.typography.bodySmall,
                                color = AlertRed.copy(alpha = 0.8f)
                            )
                        }

                        IconButton(
                            onClick = { onRemoveBlockedApp(app.packageName) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "हटाएं",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Start button
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onStartStudy,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .bounceClick { onStartStudy() }
                    .testTag("start_study_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "स्टडी टाइम शुरू करें",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun StudyActiveTimerView(
    remainingMillis: Long,
    totalMillis: Long,
    onStopClicked: () -> Unit
) {
    val progress = if (totalMillis > 0) (remainingMillis.toFloat() / totalMillis).coerceIn(0f, 1f) else 0f

    val totalSeconds = (remainingMillis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    val timeFormatted = if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "halo")
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Status header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SuccessGreen.copy(alpha = 0.15f))
                    .border(1.dp, SuccessGreen.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "स्टडी टाइम चालू है",
                        style = MaterialTheme.typography.labelLarge,
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "गहन एकाग्रता सत्र",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Circular Animated Countdown
        Box(
            modifier = Modifier
                .size(270.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background glow halo
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TealAccent.copy(alpha = 0.15f * haloPulse),
                            Color.Transparent
                        )
                    )
                )

                // Track ring
                drawArc(
                    color = Color(0xFF1B283D),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
                )

                // Progress gradient ring
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            TealPrimary,
                            TealAccent,
                            CyanSecondary,
                            TealPrimary
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Digital Time Text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 38.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "समय बाकी है",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Inspirational quote card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "\"कड़ी मेहनत और एकाग्रता ही सफलता की कुंजी है।\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "सोशल ऐप्स और यूट्यूब शॉर्ट्स सुरक्षित रूप से बंद हैं।",
                    style = MaterialTheme.typography.labelSmall,
                    color = TealAccent,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Stop Session Button
        Button(
            onClick = onStopClicked,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = AlertRed
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .bounceClick { onStopClicked() }
                .testTag("stop_study_session_button")
        ) {
            Icon(Icons.Default.Stop, contentDescription = null, tint = AlertRed)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "स्टडी समाप्त करें",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AlertRed
            )
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}
