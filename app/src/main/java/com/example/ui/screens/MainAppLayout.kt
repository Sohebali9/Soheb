package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Flashcard
import com.example.data.model.PyqPrediction
import com.example.data.model.UserProfile
import com.example.data.model.WeaknessTopic
import com.example.data.model.CommunityPost
import com.example.data.model.CommunityComment
import com.example.data.model.MockTest
import com.example.data.model.MockQuestion
import com.example.ui.theme.AccentGold
import com.example.ui.theme.ProbHigh
import com.example.ui.theme.ProbLow
import com.example.ui.theme.ProbMedium
import com.example.ui.theme.SecondaryBlue
import com.example.ui.viewmodel.ExamViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainAppLayout(viewModel: ExamViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf("home") }
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isPremium = (userProfile?.tier ?: 0) == 1

    var showPremiumDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "PYQ Predictor AI",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    // Quick Active Exam target chip
                    userProfile?.let { prof ->
                        AssistChip(
                            onClick = { currentTab = "profile" },
                            label = { Text(prof.targetExam, fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )

                        // Pro / Trial Badge
                        TextButton(
                            onClick = { showPremiumDialog = true }
                        ) {
                            if (isPremium) {
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = AccentGold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PRO",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "GET PRO",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("app_top_bar")
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == "home",
                    onClick = { currentTab = "home" },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", maxLines = 1) },
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = currentTab == "upload",
                    onClick = { currentTab = "upload" },
                    icon = { Icon(Icons.Default.CloudUpload, contentDescription = "Upload") },
                    label = { Text("Upload", maxLines = 1) },
                    modifier = Modifier.testTag("nav_upload")
                )
                NavigationBarItem(
                    selected = currentTab == "predictions",
                    onClick = { currentTab = "predictions" },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Predictions") },
                    label = { Text("Predictions", maxLines = 1) },
                    modifier = Modifier.testTag("nav_predictions")
                )
                NavigationBarItem(
                    selected = currentTab == "viva",
                    onClick = { currentTab = "viva" },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = "Viva AI") },
                    label = { Text("Viva AI", maxLines = 1) },
                    modifier = Modifier.testTag("nav_viva")
                )
                NavigationBarItem(
                    selected = currentTab == "revision",
                    onClick = { currentTab = "revision" },
                    icon = { Icon(Icons.Default.Collections, contentDescription = "Revision") },
                    label = { Text("Revision", maxLines = 1) },
                    modifier = Modifier.testTag("nav_revision")
                )
                NavigationBarItem(
                    selected = currentTab == "profile",
                    onClick = { currentTab = "profile" },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Profile") },
                    label = { Text("Profile", maxLines = 1) },
                    modifier = Modifier.testTag("nav_profile")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentTab) {
                "home" -> HomeScreen(viewModel, onNavigateToTab = { currentTab = it })
                "upload" -> UploadScreen(viewModel, onNavigateToPredictions = { currentTab = "predictions" })
                "predictions" -> PredictionsScreen(viewModel, onNavigateToUpload = { currentTab = "upload" }, showUpgradeModal = { showPremiumDialog = true }, onNavigateToTab = { currentTab = it })
                "viva" -> VivaScreen(viewModel)
                "revision" -> RevisionScreen(viewModel)
                "profile" -> ProfileScreen(viewModel)
                "forums" -> StudyForumScreen(viewModel)
                "mock_tests" -> MockTestScreen(viewModel)
            }

            // Global Freemium Subscription Dialog
            if (showPremiumDialog) {
                AlertDialog(
                    onDismissRequest = { showPremiumDialog = false },
                    title = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.OfflineBolt,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Unlock Pro Mentor Predictor",
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Gain an unfair advantage in MBBS, NEET PG, INICET, UPSC, or SSC papers with advanced prediction models.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = ProbHigh, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Advanced AI prediction models (92% repeat accuracy)", fontSize = 13.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = ProbHigh, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Unlimited PDF uploads & OCR parsing", fontSize = 13.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = ProbHigh, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Unlimited interactive AI Viva with rigorous feedback", fontSize = 13.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = ProbHigh, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ad-free experience & multi-device sync", fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Card(
                                    onClick = {
                                        viewModel.makePremium(true)
                                        showPremiumDialog = false
                                        Toast.makeText(context, "Pro License Activated (Simulated)!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Monthly Plan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("₹299/mo", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                        Text("Best for finals", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Card(
                                    onClick = {
                                        viewModel.makePremium(true)
                                        showPremiumDialog = false
                                        Toast.makeText(context, "Pro Annual License Activated (Simulated)!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.5.dp, AccentGold, RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Super Saver 🎉", fontWeight = FontWeight.Bold, color = AccentGold, fontSize = 10.sp)
                                        Text("Yearly Plan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("₹1,999/yr", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                        Text("Save 45%!", fontSize = 10.sp, color = ProbHigh)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                // Toggle dynamic demo license
                                viewModel.makePremium(!isPremium)
                                showPremiumDialog = false
                                Toast.makeText(
                                    context,
                                    if (!isPremium) "Premium Mode Enabled!" else "Premium Disabled. Free Tier active.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isPremium) ProbLow else ProbHigh)
                        ) {
                            Text(if (isPremium) "Deactivate Pro Mode" else "Simulate Pro Upgrade")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPremiumDialog = false }) {
                            Text("Not Now")
                        }
                    }
                )
            }
        }
    }
}

// ==========================================
// 1. HOME SCREEN SECTION
// ==========================================
@Composable
fun HomeScreen(viewModel: ExamViewModel, onNavigateToTab: (String) -> Unit) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val predictions by viewModel.predictions.collectAsStateWithLifecycle()
    val weaknesses by viewModel.weaknessFlow.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming Streak Panel
        userProfile?.let { prof ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Welcome back, ${prof.name}!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Preparing for ${prof.targetExam}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // XP Progress line
                            LinearProgressIndicator(
                                progress = { (prof.xpPoints % 500) / 500f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = AccentGold,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Rank: Level ${prof.xpPoints / 250 + 1} Scholar  •  ${prof.xpPoints} Total XP",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Streak Flame representation
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Study Streak",
                                tint = AccentGold,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "${prof.studyStreak} Days",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "STREAK",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // AI READINESS SCORE - Custom Canvas-based arc and indicators
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "AI Exam Readiness Score",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier.size(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val targetColor = if (prof.readinessScore >= 80) ProbHigh else if (prof.readinessScore >= 60) ProbMedium else ProbLow
                            Canvas(modifier = Modifier.size(130.dp)) {
                                drawArc(
                                    color = Color.LightGray.copy(alpha = 0.3f),
                                    startAngle = 135f,
                                    sweepAngle = 270f,
                                    useCenter = false,
                                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = targetColor,
                                    startAngle = 135f,
                                    sweepAngle = 270f * (prof.readinessScore / 100f),
                                    useCenter = false,
                                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${prof.readinessScore}%",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = if (prof.readinessScore >= 80) "EXAM READY" else if (prof.readinessScore >= 60) "ON TRACK" else "REVISE CORE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = targetColor
                                )
                            }
                        }

                        Text(
                            text = "Based on predicted trends, strengths, weaknesses and solved active cards.",
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        }

        // Action Quick Links
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onNavigateToTab("upload") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Analyze PYQ", maxLines = 1)
                    }

                    Button(
                        onClick = { onNavigateToTab("viva") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI Viva Tutor", maxLines = 1)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onNavigateToTab("forums") },
                        modifier = Modifier.weight(1f).testTag("nav_forums_quick"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Forum, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Study Sub-Groups", maxLines = 1)
                    }

                    Button(
                        onClick = { onNavigateToTab("mock_tests") },
                        modifier = Modifier.weight(1f).testTag("nav_mock_tests_quick"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Feed, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI Mock Practice", maxLines = 1)
                    }
                }
            }
        }

        // Daily Revision Tasks reminders list
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly Revision Planner",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                TextButton(onClick = { onNavigateToTab("revision") }) {
                    Text("View Gaps (${weaknesses.size})")
                }
            }
        }

        if (weaknesses.isEmpty()) {
            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            tint = ProbHigh,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Awesome! No syllabus weaknesses detected yet.",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            "Identify Gaps by clicking Analyze PYQ scanner.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(weaknesses.take(3)) { gap ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (gap.priorityLevel == "High") ProbLow.copy(0.15f) else ProbMedium.copy(0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (gap.priorityLevel == "High") ProbLow else ProbMedium,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(gap.subject, fontWeight = FontWeight.Black, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(gap.priorityLevel, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = if (gap.priorityLevel == "High") ProbLow else ProbMedium)
                            }
                            Text(gap.topic, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(gap.dailyRevisionPlan, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Live Examiner trend statistics section
        item {
            Text(
                "Examiner General Subject Weightage",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Expected General Distribution",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val weights = listOf(
                        Triple("Clinical / Core Paper", 42, ProbHigh),
                        Triple("Allied Subjects / Electives", 28, SecondaryBlue),
                        Triple("General Syllabus / Case studies", 20, ProbMedium),
                        Triple("Others / General Aptitude", 10, Color.Gray)
                    )

                    weights.forEach { (sub, percentage, clr) ->
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(sub, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("$percentage%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = clr)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { percentage / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = clr,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. PYQ UPLOAD & ANALYSIS SCREEN
// ==========================================
@Composable
fun UploadScreen(viewModel: ExamViewModel, onNavigateToPredictions: () -> Unit) {
    val context = LocalContext.current
    var sourceTitle by remember { mutableStateOf("") }
    var documentText by remember { mutableStateOf("") }
    val isJobRunning by viewModel.isProcessingJob.collectAsStateWithLifecycle()
    val uploadFeedback by viewModel.uploadingStatus.collectAsStateWithLifecycle()

    val keyboardController = LocalSoftwareKeyboardController.current

    // Preset PDF templates so students can play instantly
    val mockTemplates = listOf(
        Triple(
            "NEET_PG_Path_Meds_2025.pdf",
            "Cardiogenic shock appeared 7 times in 10 years. Minimal change disease pathology diagnostic questions. Metformin side-effects and drug interaction indices.",
            "NEET PG Mock PYQ Case Series"
        ),
        Triple(
            "UPSC_CivilServices_GS2.pdf",
            "Fundamental Rights Article 21 and Privacy extensions. Green Hydrogen decarbonization frameworks. Panchayati Raj 73rd constitutional amendments.",
            "UPSC Main Syllabus Extract"
        ),
        Triple(
            "MBBS_Pathology_Term_2.pdf",
            "Nephrotic Syndrome long cases high expected. Oral Antidiabetic agents pharmacology classifications. Appendicitis clinical descriptors and biopsy standards.",
            "MBBS Specimen Paper"
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("upload_screen_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "AI PYQ Analysis & PDF Extractor",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Upload previous years papers, question PDFs, syllabus screenshots, or past lecture notes. The AI extracts questions, categorizes topics and predicts repetitiveness.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Mock paper selectors
        item {
            Text(
                "Select a High-Yield Sample Paper to Extract:",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(mockTemplates) { (fileName, rawText, title) ->
                    Card(
                        onClick = {
                            sourceTitle = title
                            documentText = rawText
                            Toast.makeText(context, "Loaded template content into editor!", Toast.LENGTH_SHORT).show()
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (sourceTitle == title) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(0.4f)
                        ),
                        modifier = Modifier.width(180.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = ProbLow, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    fileName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(title, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Input forms
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Manual Input / OCR Editor", fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = sourceTitle,
                        onValueChange = { sourceTitle = it },
                        label = { Text("E.g. Cardiology Mock Examination 2026") },
                        placeholder = { Text("Enter a title or exam label") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("source_title_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    OutlinedTextField(
                        value = documentText,
                        onValueChange = { documentText = it },
                        label = { Text("Paste paper content, questions list or notes text") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("notes_content_input"),
                        maxLines = 6,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                    )

                    // simulated upload buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                sourceTitle = "Extracted Image Text"
                                documentText = "Cardiogenic shock appeared 7 times in last 10 years. Predict FIGO staging questions for Ovarian Carcinomas. High repeat in acute pancreatitis clinical descriptors."
                                Toast.makeText(context, "OCR Extraction finished (Simulated)!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Camera OCR")
                        }

                        Button(
                            onClick = {
                                if (sourceTitle.trim().isEmpty() || documentText.trim().isEmpty()) {
                                    Toast.makeText(context, "Please enter a valid title and text content!", Toast.LENGTH_SHORT).show()
                                } else {
                                    keyboardController?.hide()
                                    viewModel.simulatePyqUpload(sourceTitle, documentText)
                                }
                            },
                            enabled = !isJobRunning,
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("analyze_pyq_button")
                        ) {
                            if (isJobRunning) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Analyzing...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AI Extract & Run")
                            }
                        }
                    }
                }
            }
        }

        // Processing results feedback
        uploadFeedback?.let { feedback ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (feedback.contains("Successfully")) ProbHigh.copy(0.12f) else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (feedback.contains("Successfully")) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (feedback.contains("Successfully")) ProbHigh else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI Engine Output", fontWeight = FontWeight.Bold)
                            }

                            TextButton(onClick = { viewModel.clearUploadStatus() }) {
                                Text("Clear")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(feedback, fontSize = 13.sp)

                        if (feedback.contains("Successfully") || feedback.contains("Syllabus")) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onNavigateToPredictions() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Review Gen-Predictions")
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }

        // Gamified Tip callout
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Stars, contentDescription = null, tint = AccentGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Bonus: Every parsed paper awards +40 XP points and updates your readiness meter!",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ==========================================
// 3. PREDICTIONS HUB (WITH FREEMIUM ACCESS)
// ==========================================
@Composable
fun PredictionsScreen(
    viewModel: ExamViewModel,
    onNavigateToUpload: () -> Unit,
    showUpgradeModal: () -> Unit,
    onNavigateToTab: (String) -> Unit
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isPremium = (userProfile?.tier ?: 0) == 1
    val predictions by viewModel.predictions.collectAsStateWithLifecycle()
    val isProcessingJob by viewModel.isProcessingJob.collectAsStateWithLifecycle()

    var activeExpandedId by remember { mutableIntStateOf(-1) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("predictions_screen_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "AI Exam Predictions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Trending repeating concepts for upcoming ${userProfile?.targetExam ?: "Exam"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AssistChip(
                    onClick = onNavigateToUpload,
                    label = { Text("Refresh") },
                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        if (predictions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.HourglassEmpty, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No analysis predictions loaded.", fontWeight = FontWeight.Bold)
                        Text(
                            "Compile predictions by uploading past syllabus files.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateToUpload) {
                            Text("Go to Scanner Upload")
                        }
                    }
                }
            }
        } else {
            items(predictions.take(20)) { pred ->
                // First 2 elements are free; everything else is premium gated
                val isUnlocked = isPremium || predictions.indexOf(pred) < 2

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("prediction_card_${pred.id}"),
                    onClick = {
                        if (isUnlocked) {
                            activeExpandedId = if (activeExpandedId == pred.id) -1 else pred.id
                        } else {
                            showUpgradeModal()
                        }
                    }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = pred.subject,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }

                            // Repeat frequency count indicators
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(pred.repetitionCount, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = pred.topic,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = pred.question,
                            fontSize = 13.sp,
                            maxLines = if (activeExpandedId == pred.id) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Score and level banner or block
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val probBg = when (pred.probability) {
                                    "High" -> ProbHigh
                                    "Medium" -> ProbMedium
                                    else -> ProbLow
                                }
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(probBg)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${pred.probability} Probability",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (!isUnlocked) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = AccentGold, modifier = Modifier.size(13.dp))
                                    Text("PRO ONLY", fontSize = 10.sp, color = AccentGold, fontWeight = FontWeight.Black)
                                }
                            } else {
                                Text(
                                    text = if (activeExpandedId == pred.id) "Show Less" else "Reveal Answer Key",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Expanded Section (Model Answers and Mnemonics option buttons)
                        if (isUnlocked && activeExpandedId == pred.id) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Why is this predicted?", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            Text(pred.justification, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 4.dp))

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("AI high-yield Model Answer key", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ProbHigh)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(pred.modelAnswer, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Add weakness trigger
                                    TextButton(
                                        onClick = {
                                            viewModel.identifyWeaknessAndGeneratePlan(pred.subject, pred.topic, "I struggle explaining standard answers to this predicted question.")
                                            Toast.makeText(context, "Added to revision list!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.testTag("add_to_revision_btn")
                                    ) {
                                        Icon(Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Revision")
                                    }

                                    // Forums Discussion linking!
                                    TextButton(
                                        onClick = {
                                            viewModel.navigateToDiscussion(pred)
                                            Toast.makeText(context, "Redirecting to Discussion Forum subgroup...", Toast.LENGTH_SHORT).show()
                                            onNavigateToTab("forums")
                                        },
                                        modifier = Modifier.testTag("navigate_discussion_btn")
                                    ) {
                                        Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Discuss Forum")
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.generateFlashcardsForPrediction(pred.subject, pred.topic)
                                            Toast.makeText(context, "AI is drafting 3 active flashcards...", Toast.LENGTH_SHORT).show()
                                        },
                                        enabled = !isProcessingJob,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.testTag("make_cards_btn")
                                    ) {
                                        Icon(Icons.Default.Collections, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Make Cards")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. INTERACTIVE VIVA AI SIMULATOR
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VivaScreen(viewModel: ExamViewModel) {
    val activeQuestionIndex by viewModel.vivaQuestionIndex.collectAsStateWithLifecycle()
    val vivaQueryDraft by viewModel.vivaUserDraft.collectAsStateWithLifecycle()
    val activeMode by viewModel.vivaMode.collectAsStateWithLifecycle()
    val evaluationResult by viewModel.vivaEvaluationResult.collectAsStateWithLifecycle()
    val isEvaluating by viewModel.isEvaluatingViva.collectAsStateWithLifecycle()

    val currentList = viewModel.getVivaQuestions()
    val currentQuestion = currentList.getOrNull(activeQuestionIndex) ?: "Ready to join?"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("viva_screen_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Interactive AI Viva Simulator",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Simulate interactive oral testing with standard medical professors, UPSC panel officers, or fast SSC board members.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Examiner panel type selection tabs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Easy", "Professor Mode", "Rapid-fire Mode").forEach { modeLabel ->
                    val selected = activeMode == modeLabel
                    Button(
                        onClick = { viewModel.changeVivaMode(modeLabel) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            modeLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Active Question Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "ACTIVE QUESTION  •  Q${activeQuestionIndex + 1}/${currentList.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )

                        TextButton(onClick = { viewModel.nextVivaQuestion() }) {
                            Text("Skip/Next")
                            Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        currentQuestion,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Draft response text field and submit block
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Your Verbal Response Transcription",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    OutlinedTextField(
                        value = vivaQueryDraft,
                        onValueChange = { viewModel.updateVivaDraft(it) },
                        placeholder = { Text("List key symptoms, cellular updates or provisions. E.g., 'Metformin activates AMPK, reduces gluconeogenesis, causes diarrhea...'") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                            .testTag("viva_response_input"),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mimic Voice recording trigger
                        IconButton(
                            onClick = {
                                viewModel.updateVivaDraft(
                                    when (activeQuestionIndex) {
                                        0 -> "Nephrotic syndrome is characterized by massive proteinuria exceeding 3.5 grams daily, hypoalbuminemia below 3, generalized edema, and lipiduria. Light microscopy shows minimal effacement in MCD while electron microscopy shows visceral epithelial foot process flattening."
                                        1 -> "In active cardiogenic shock we need core inotropic support with Dobutamine or Norepinephrine. Key targets involve stabilizing mean arterial pressure above 65 with invasive intra-aortic balloon counters if needed."
                                        else -> "SGLT2 inhibitors block sodium glucose cotransporter protein, preventing glucose reabsorption. Contraindicated in severe eGFR reductions below 30 or recurrent diabetic ketoacidosis history."
                                    }
                                )
                            }
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Mic, contentDescription = "Simulate Speak", tint = ProbLow, modifier = Modifier.size(28.dp))
                                Text("Speak", fontSize = 9.sp)
                            }
                        }

                        Button(
                            onClick = { viewModel.submitVivaAnswer() },
                            enabled = !isEvaluating && vivaQueryDraft.trim().isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                                .testTag("viva_evaluate_button")
                        ) {
                            if (isEvaluating) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Evaluating response...")
                            } else {
                                Icon(Icons.Default.Grading, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Submit to Examiner")
                            }
                        }
                    }
                }
            }
        }

        // Graded Evaluation details output
        evaluationResult?.let { eval ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (eval.score >= 7) ProbHigh.copy(0.15f) else ProbLow.copy(0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${eval.score}/10",
                                        fontWeight = FontWeight.Black,
                                        color = if (eval.score >= 7) ProbHigh else ProbLow
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Examiner Verdict", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.nextVivaQuestion() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Next Ques")
                                Icon(Icons.Default.NavigateNext, contentDescription = null)
                            }
                        }

                        Text(eval.feedback, style = MaterialTheme.typography.bodyMedium)

                        HorizontalDivider()

                        // Recognized / Missed keyword metrics indices
                        if (eval.keywordsMatched.isNotEmpty()) {
                            Text("Keywords Mentioned (+XP Bonus):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ProbHigh)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                eval.keywordsMatched.forEach { kw ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(kw, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        if (eval.keywordsMissed.isNotEmpty()) {
                            Text("Standard Keywords Missed:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ProbLow)
                            FlowRow(
                                maxItemsInEachRow = 3,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                eval.keywordsMissed.forEach { kw ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(kw, fontSize = 11.sp, color = ProbLow) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Model Standard Answer key:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(0.05f))
                        ) {
                            Text(
                                eval.modelAnswer,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. REVISION CENTRAL (FLASHCARDS + STUDY GAPS)
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RevisionScreen(viewModel: ExamViewModel) {
    val context = LocalContext.current
    val cardList by viewModel.flashcardsFlow.collectAsStateWithLifecycle()
    val gapList by viewModel.weaknessFlow.collectAsStateWithLifecycle()

    var activeViewTab by remember { mutableStateOf("cards") } // cards / weakness
    var activeSubcategory by remember { mutableStateOf("All") }

    // Manual Custom Flashcard trigger dialog state
    var showAddCardDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("revision_screen_column"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Personalized Study Space",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Active recall flashcards paired with actionable weakness analysis logs generated automatically from your PYQs.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Tab selection (Cards vs Weaknesses)
            item {
                TabRow(
                    selectedTabIndex = if (activeViewTab == "cards") 0 else 1,
                    containerColor = Color.Transparent
                ) {
                    Tab(
                        selected = activeViewTab == "cards",
                        onClick = { activeViewTab = "cards" },
                        text = { Text("Flashcards (${cardList.size})", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeViewTab == "weakness",
                        onClick = { activeViewTab = "weakness" },
                        text = { Text("Weakness Gaps (${gapList.size})", fontWeight = FontWeight.Bold) }
                    )
                }
            }

            if (activeViewTab == "cards") {
                // Flashcards Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Your Active Recall Decks", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Button(
                            onClick = { showAddCardDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Card", fontSize = 12.sp)
                        }
                    }
                }

                if (cardList.isEmpty()) {
                    item {
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No Flashcards Created Yet", fontWeight = FontWeight.Bold)
                                Text(
                                    "Navigate to Predictions to extract smart cards, or click Add Card to write customized notes.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(cardList) { card ->
                        var isAnswerVisible by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                                .testTag("flashcard_item_${card.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(0.12f)
                                        ) {
                                            Text(
                                                card.subject,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(card.topic, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteCard(card.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ProbLow, modifier = Modifier.size(16.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Question:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = card.question,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                if (isAnswerVisible) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                                    Text(
                                        text = "Active Recall Solution Key:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ProbHigh
                                    )
                                    Text(
                                        text = card.answer,
                                        fontSize = 14.sp
                                    )

                                    card.mnemonic?.let { mnemonicText ->
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = AccentGold.copy(0.1f))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(8.dp)
                                            ) {
                                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(mnemonicText, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                isAnswerVisible = false
                                                viewModel.reviewFlashcard(card.id)
                                                Toast.makeText(context, "Marked Got it! +10 XP awarded.", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ProbHigh),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("I Got It!")
                                        }

                                        OutlinedButton(
                                            onClick = { isAnswerVisible = false },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Study Again")
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { isAnswerVisible = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Text("Reveal Active Recall", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Weaknesses Gaps Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Identified Gaps & Study Gaps", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                        TextButton(
                            onClick = {
                                viewModel.identifyWeaknessAndGeneratePlan("Cardiology", "MAP Target Ranges", "Incorrect options on mean arterial boundary calculations.")
                                Toast.makeText(context, "Generated test weakness plan!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("+ Quick Diagnostic")
                        }
                    }
                }

                if (gapList.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProbHigh, modifier = Modifier.size(44.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Perfect Syllabus Coverage!", fontWeight = FontWeight.Bold)
                                Text(
                                    "No weakness study gaps logged. Keep up the perfect streak!",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(gapList) { gap ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("weakness_item_${gap.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(100.dp),
                                            color = if (gap.priorityLevel == "High") ProbLow.copy(0.12f) else ProbMedium.copy(0.12f)
                                        ) {
                                            Text(
                                                "${gap.priorityLevel} Priority",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (gap.priorityLevel == "High") ProbLow else ProbMedium,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = gap.subject,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    TextButton(
                                        onClick = { viewModel.resolveWeaknessNow(gap.id) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = ProbHigh)
                                    ) {
                                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Resolved (+50 XP)")
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = gap.topic,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "Problem Area: ${gap.weaknessDescription}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            "AI Personalized Study Plan:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(gap.dailyRevisionPlan, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Flashcard Dialog Modal wrapper (placed inside Box safely outside LazyColumn lambda)
        if (showAddCardDialog) {
            var inputQ by remember { mutableStateOf("") }
            var inputA by remember { mutableStateOf("") }
            var inputMnemonic by remember { mutableStateOf("") }
            var inputSubj by remember { mutableStateOf("Medicine") }
            var inputTopic by remember { mutableStateOf("General") }

            AlertDialog(
                onDismissRequest = { showAddCardDialog = false },
                title = { Text("Draft Custom Card") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = inputSubj,
                            onValueChange = { inputSubj = it },
                            label = { Text("Subject") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = inputTopic,
                            onValueChange = { inputTopic = it },
                            label = { Text("Topic Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = inputQ,
                            onValueChange = { inputQ = it },
                            label = { Text("Front: Question / Concept") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = inputA,
                            onValueChange = { inputA = it },
                            label = { Text("Back: Answer Key") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = inputMnemonic,
                            onValueChange = { inputMnemonic = it },
                            label = { Text("Helper Mnemonic (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (inputQ.isEmpty() || inputA.isEmpty()) {
                                Toast.makeText(context, "Filled forms are required!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.addCustomFlashcard(inputQ, inputA, inputMnemonic, inputSubj, inputTopic)
                                showAddCardDialog = false
                                Toast.makeText(context, "Saved Custom Card!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Save Card")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCardDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// ==========================================
// 6. ASPIRANT PROFILE & LEADERBOARDS SCREEN
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(viewModel: ExamViewModel) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isPremium = (userProfile?.tier ?: 0) == 1

    var profileNameInput by remember { mutableStateOf("") }
    var selectedTargetConfig by remember { mutableStateOf("") }

    LaunchedEffect(userProfile) {
        userProfile?.let {
            profileNameInput = it.name
            selectedTargetConfig = it.targetExam
        }
    }

    val examOptions = listOf("Class 10 CBSE", "Class 12 Board", "JEE Main", "NEET UG", "MBBS University", "NEET PG", "INICET", "UPSC", "SSC", "State PCS")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_screen_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Aspirant Profile Control",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Profile details fields card
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Basic Credentials", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = profileNameInput,
                        onValueChange = {
                            profileNameInput = it
                            if (it.isNotEmpty()) viewModel.setProfileName(it)
                        },
                        label = { Text("Aspirant Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_field"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Target Examination Syllabus Profile:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    FlowRow(
                        maxItemsInEachRow = 3,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        examOptions.forEach { examOption ->
                            val active = selectedTargetConfig == examOption
                            FilterChip(
                                selected = active,
                                onClick = {
                                    selectedTargetConfig = examOption
                                    viewModel.setTargetExam(examOption)
                                    Toast.makeText(context, "Syllabus updated to: $examOption", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text(examOption) }
                            )
                        }
                    }
                }
            }
        }

        // Gamified Aspirants Leaderboard representation
        item {
            Text(
                "Live Aspirant Weekly Leaderboard (India)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val fakeLeaderboard = listOf(
                        Triple("1. Dr. Sneha Verma", "1,840 XP", "NEET PG"),
                        Triple("2. Abhishek Raj (You)", "${userProfile?.xpPoints ?: 150} XP", userProfile?.targetExam ?: "NEET PG"),
                        Triple("3. Pragati Mehra", "950 XP", "UPSC"),
                        Triple("4. Vikas Chaddha", "810 XP", "SSC"),
                        Triple("5. Ranjini Swamy", "760 XP", "INICET")
                    )

                    fakeLeaderboard.forEach { (name, xpText, examLabel) ->
                        val isUser = name.contains("You")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isUser) "${userProfile?.name ?: "Aspirant"} (You)" else name.substring(3),
                                    fontWeight = if (isUser) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                                Text(examLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Text(
                                text = if (isUser) "${userProfile?.xpPoints ?: 150} XP" else xpText,
                                fontWeight = FontWeight.Bold,
                                color = if (isUser) MaterialTheme.colorScheme.primary else AccentGold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Subscription details display (with demo downgrade fallback)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Membership Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPremium) "PYQ Predictor AI Pro License: Active" else "Free Account Tier (Limited Predictions)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPremium) ProbHigh else Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.makePremium(!isPremium)
                            Toast.makeText(context, "Membership Stated Changed!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isPremium) ProbLow else ProbHigh)
                    ) {
                        Text(if (isPremium) "Switch to Free Tier Demo" else "Simulate Pro Upgrade")
                    }
                }
            }
        }
    }
}

// FlowRow support
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    maxItemsInEachRow: Int = Int.MAX_VALUE,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable FlowRowScope.() -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        maxItemsInEachRow = maxItemsInEachRow,
        horizontalArrangement = horizontalArrangement,
        content = content
    )
}

// ==========================================
// 7. STUDY COMMUNITY FORUM SCREEN (REDDIT STYLE)
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudyForumScreen(viewModel: ExamViewModel) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val selectedSubgroup by viewModel.selectedForumSubgroup.collectAsStateWithLifecycle()
    val posts by viewModel.forumPosts.collectAsStateWithLifecycle()
    val activePost by viewModel.activeThreadPost.collectAsStateWithLifecycle()
    val activeThreadId by viewModel.activeThreadId.collectAsStateWithLifecycle()
    val comments by viewModel.threadComments.collectAsStateWithLifecycle()

    var showCreatePostDialog by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }

    val subgroupsList = listOf(
        "Class 10 CBSE", "Class 12 Board", "JEE Main", "NEET UG",
        "MBBS University", "NEET PG", "INICET", "UPSC", "SSC", "State PCS"
    )

    if (activeThreadId != null && activePost != null) {
        // --- VIEWING SINGLE POST / COMMENTS ENGINE ---
        val post = activePost!!
        Scaffold(
            topBar = {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.selectThread(null) },
                            modifier = Modifier.testTag("back_to_forum_list_btn")
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Subgroup: ${post.examType}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Topic: ${post.topic}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("u/${post.author}", fontWeight = FontWeight.Medium, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(13.dp), tint = ProbHigh)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${post.upvotes - post.downvotes} points", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(post.title, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(post.content, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.upvotePost(post.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Upvote", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    Text("${post.upvotes}", fontWeight = FontWeight.Bold)
                                    IconButton(
                                        onClick = { viewModel.downvotePost(post.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Downvote", tint = MaterialTheme.colorScheme.tertiary)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("Answers & Comments (${post.commentCount})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 4.dp))
                    }

                    if (comments.isEmpty()) {
                        item {
                            Text("No answers posted yet. Help your peer by answering below!", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
                        }
                    } else {
                        items(comments) { comment ->
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("u/${comment.author}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                        Text("Best Answer", fontSize = 10.sp, fontWeight = FontWeight.Black, color = ProbHigh)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(comment.content, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Add comment input row
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text("Write your prediction response...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("comment_input_field"),
                            maxLines = 3
                        )
                        IconButton(
                            onClick = {
                                if (commentText.isNotEmpty()) {
                                    viewModel.postComment(commentText)
                                    commentText = ""
                                    Toast.makeText(context, "Comment published! +15 XP", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("comment_submit_btn")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Submit Comment")
                        }
                    }
                }
            }
        }
    } else {
        // --- SCREEN FEED LISTS OVERVIEW ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Study Subridges", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Select your focus subgroup prep peer forum", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Button(
                    onClick = { showCreatePostDialog = true },
                    modifier = Modifier.testTag("create_post_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Post")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subgroups horizontal sliding list of chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(subgroupsList) { examItem ->
                    val isSelected = selectedSubgroup == examItem
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectForumSubgroup(examItem) },
                        label = { Text(examItem) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Trending Threads in r/${selectedSubgroup}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))

            if (posts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("r/${selectedSubgroup} is quiet today.", fontWeight = FontWeight.Bold)
                        Text("Be the first to draft a peer prediction query!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(posts) { post ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("thread_card_${post.id}")
                                .clickable { viewModel.selectThread(post) }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(post.subject, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                    }
                                    Text("u/${post.author}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(post.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(post.content, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${post.upvotes} Upvotes", fontSize = 12.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Comment, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${post.commentCount} Comments", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- CREATE POST DIALOG ---
    if (showCreatePostDialog) {
        var inputSubj by remember { mutableStateOf("General") }
        var inputTopic by remember { mutableStateOf("") }
        var inputTitle by remember { mutableStateOf("") }
        var inputContent by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreatePostDialog = false },
            title = { Text("Draft New Thread: r/${selectedSubgroup}") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = inputSubj,
                        onValueChange = { inputSubj = it },
                        label = { Text("Subject (e.g., Pathology, Physics)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputTopic,
                        onValueChange = { inputTopic = it },
                        label = { Text("Topic Concept Tag") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputTitle,
                        onValueChange = { inputTitle = it },
                        label = { Text("Post Question Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputContent,
                        onValueChange = { inputContent = it },
                        label = { Text("Topic specifics details...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputTopic.isEmpty() || inputTitle.isEmpty() || inputContent.isEmpty()) {
                            Toast.makeText(context, "All key fields are required!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.createPost(inputSubj, inputTopic, inputTitle, inputContent)
                            showCreatePostDialog = false
                            Toast.makeText(context, "Thread published successfully! +20 XP", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("submit_post_btn")
                ) {
                    Text("Publish Thread")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePostDialog = false }) {
                    Text("Discard")
                }
            }
        )
    }
}

// ==========================================
// 8. AI MOCK PRACTICE EXAMS SCREEN
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MockTestScreen(viewModel: ExamViewModel) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val testsList by viewModel.mockTests.collectAsStateWithLifecycle()
    val activeTestId by viewModel.activeMockTestId.collectAsStateWithLifecycle()
    val activeTest by viewModel.activeMockTestObj.collectAsStateWithLifecycle()
    val questions by viewModel.mockQuestions.collectAsStateWithLifecycle()
    val currentQuestionIndex by viewModel.currentMockQuestionIndex.collectAsStateWithLifecycle()
    val answersMap by viewModel.mockAnswersMap.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGeneratingMockTest.collectAsStateWithLifecycle()
    val remainingSeconds by viewModel.mockTestTimerRemainingSeconds.collectAsStateWithLifecycle()

    var showQuizConfigDialog by remember { mutableStateOf(false) }

    if (activeTestId != null && activeTest != null) {
        // --- SCREEN 1: ACTIVE QUIZ TIMER OR REVIEW SESSION ---
        val test = activeTest!!
        val isCompleted = test.isCompleted

        Scaffold(
            topBar = {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.closeActiveMockTestSession() },
                                modifier = Modifier.testTag("close_test_session_btn")
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Exit to listing")
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isCompleted) "ANALYSIS BOARD" else "PRACTICE IN PROGRESS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(test.examType, fontWeight = FontWeight.Bold)
                            }

                            if (!isCompleted) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (remainingSeconds < 60) ProbLow else MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    val mins = remainingSeconds / 60
                                    val secs = remainingSeconds % 60
                                    Text(
                                        text = String.format("%02d:%02d", mins, secs),
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        color = if (remainingSeconds < 60) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            } else {
                                Text("${test.scoreObtained} / ${test.totalQuestions} Qs", fontWeight = FontWeight.ExtraBold, color = ProbHigh)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        if (!isCompleted && questions.isNotEmpty()) {
                            LinearProgressIndicator(
                                progress = { (currentQuestionIndex + 1) / questions.size.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (questions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val q = questions.getOrNull(currentQuestionIndex)
                    if (q != null) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (isCompleted) {
                                item {
                                    val pct = test.accuracyPercentage
                                    val mTitle: String
                                    val mText: String
                                    val mIcon: androidx.compose.ui.graphics.vector.ImageVector
                                    val mIconColor: androidx.compose.ui.graphics.Color
                                    val mBgColor: androidx.compose.ui.graphics.Color

                                    if (pct == 100) {
                                        mTitle = "PERFECT 100% SCORE! 🏆"
                                        mText = "Flawless victory! You answered every single prediction correctly. Your conceptual depth is absolutely legendary. Keep this hot streak burning for the active exams!"
                                        mIcon = Icons.Default.Stars
                                        mIconColor = AccentGold
                                        mBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    } else if (pct >= 80) {
                                        mTitle = "EXCELLENT JOB! 🚀"
                                        mText = "Magnificent performance! Your mastery is crystal clear, and you are outstandingly close to ultimate perfection. Revise the small tripped wire below!"
                                        mIcon = Icons.Default.CheckCircle
                                        mIconColor = ProbHigh
                                        mBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                    } else if (pct >= 50) {
                                        mTitle = "ON THE RIGHT TRACK! 📈"
                                        mText = "Great effort! You grasped the high-yield core trends. Double check any gaps on this topic, add them to your revision list, and you'll dominate the upcoming levels in no time!"
                                        mIcon = Icons.Default.TrendingUp
                                        mIconColor = AccentGold
                                        mBgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.18f)
                                    } else {
                                        mTitle = "VALUABLE PRACTICE GAP! ⚡"
                                        mText = "Setbacks are merely insights to build supreme mastery. This practice showed you exactly where the gaps are—patch them using Flashcards or the AI Viva Tutor and bounce back!"
                                        mIcon = Icons.Default.MenuBook
                                        mIconColor = ProbLow
                                        mBgColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.18f)
                                    }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("score_motivation_card"),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = mBgColor),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, mIconColor.copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(mIconColor.copy(alpha = 0.15f), shape = CircleShape)
                                                    .padding(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = mIcon,
                                                    contentDescription = "Motivation Icon",
                                                    modifier = Modifier.size(24.dp),
                                                    tint = mIconColor
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = mTitle,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    letterSpacing = 0.5.sp
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = mText,
                                                    fontSize = 12.sp,
                                                    lineHeight = 17.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(100.dp),
                                                        color = mIconColor.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = "Score Accuracy: $pct%",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (pct >= 50) mIconColor else MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Question header tags
                            item {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(shape = RoundedCornerShape(100.dp), color = MaterialTheme.colorScheme.primary.copy(0.12f)) {
                                        Text(q.subject, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.primary)
                                    }
                                    Surface(shape = RoundedCornerShape(100.dp), color = MaterialTheme.colorScheme.secondary.copy(0.12f)) {
                                        Text(q.topic, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }

                            // The question textual content
                            item {
                                BorderCard(
                                    border = 1.dp,
                                    borderColor = MaterialTheme.colorScheme.outline.copy(0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Question ${currentQuestionIndex + 1} of ${questions.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(q.questionText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            // Options A, B, C, D
                            item {
                                val userAnswersChosen = answersMap[q.id]
                                val trueCorrectAnswer = q.correctAnswer

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val choices = listOf(
                                        "A" to q.optionA,
                                        "B" to q.optionB,
                                        "C" to q.optionC,
                                        "D" to q.optionD
                                    )

                                    choices.forEach { (optionCode, optionText) ->
                                        val isChosen = userAnswersChosen == optionCode
                                        val isCorrect = optionCode == trueCorrectAnswer

                                        val containerBg = if (isCompleted) {
                                            if (isCorrect) ProbHigh.copy(alpha = 0.15f)
                                            else if (isChosen) ProbLow.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surface
                                        } else {
                                            if (isChosen) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                            else MaterialTheme.colorScheme.surface
                                        }

                                        val borderBorderColor = if (isCompleted) {
                                            if (isCorrect) ProbHigh
                                            else if (isChosen) ProbLow
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        } else {
                                            if (isChosen) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        }

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(
                                                    width = if (isChosen || (isCompleted && isCorrect)) 2.dp else 1.dp,
                                                    color = borderBorderColor,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable(enabled = !isCompleted) {
                                                    viewModel.selectMockOption(q.id, optionCode)
                                                },
                                            colors = CardDefaults.cardColors(containerColor = containerBg)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = isChosen,
                                                    onClick = { if (!isCompleted) viewModel.selectMockOption(q.id, optionCode) },
                                                    enabled = !isCompleted
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(optionText, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            // Score justification / explanation review notes of corrected answer keys
                            if (isCompleted) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("AI Topic Explanation Note", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(q.explanation, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Navigation Control Buttons card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { viewModel.prevMockQuestion() },
                                    enabled = currentQuestionIndex > 0
                                ) {
                                    Text("Previous")
                                }

                                if (!isCompleted && currentQuestionIndex == questions.size - 1) {
                                    Button(
                                        onClick = {
                                            viewModel.submitMockTestResult()
                                            Toast.makeText(context, "Mock results computed successfully! Scorecard updated.", Toast.LENGTH_LONG).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ProbHigh),
                                        modifier = Modifier.testTag("submit_mock_test_btn")
                                    ) {
                                        Text("Submit Exam")
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.nextMockQuestion() },
                                        enabled = currentQuestionIndex < questions.size - 1
                                    ) {
                                        Text("Next")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // --- SCREEN 2: MOCKS DASHBOARD OVERVIEW AND GEN TRIGGERS ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("AI Predictive Practice Tests", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Exams built dynamically on trending PYQ concepts", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Button(
                    onClick = { showQuizConfigDialog = true },
                    modifier = Modifier.testTag("init_mock_exam_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Exam")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Seeding / loading progress
            if (isGenerating) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Gemini AI is parsing high-yield PYQ patterns...", fontWeight = FontWeight.Bold)
                        Text("Drafting complex options, correct options, and detailed justification keys...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text("Historical Practice Scorecard", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 10.dp))

            if (testsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Feed, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No practice exams recorded.", fontWeight = FontWeight.Bold)
                        Text("Launch a new mock test above using syllabus prediction trends!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(testsList) { test ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("mock_test_card_${test.id}")
                                .clickable { viewModel.launchMockTestSession(test) }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(test.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = if (test.isCompleted) ProbHigh.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = if (test.isCompleted) "Completed" else "Incomplete",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (test.isCompleted) ProbHigh else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = ProbHigh)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${test.scoreObtained} / ${test.totalQuestions} Right", fontSize = 12.sp)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${test.timeLimitMinutes} Mins", fontSize = 12.sp)
                                    }

                                    if (test.isCompleted) {
                                        Text("Score accuracy: ${test.accuracyPercentage}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- NEW EXAM GENERATOR DIALOG CONFIG ---
    if (showQuizConfigDialog) {
        var selectedExamType by remember { mutableStateOf(userProfile?.targetExam ?: "Class 10 CBSE") }
        var questionCount by remember { mutableFloatStateOf(3f) }
        var durationMinutes by remember { mutableFloatStateOf(5f) }

        val examTypesList = listOf(
            "Class 10 CBSE", "Class 12 Board", "JEE Main", "NEET UG",
            "MBBS University", "NEET PG", "INICET", "UPSC", "SSC", "State PCS"
        )

        AlertDialog(
            onDismissRequest = { showQuizConfigDialog = false },
            title = { Text("Configure Dynamic Predictive Exam") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Select syllabus to examine:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        examTypesList.forEach { type ->
                            val selected = selectedExamType == type
                            FilterChip(
                                selected = selected,
                                onClick = { selectedExamType = type },
                                label = { Text(type) }
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("Number of Questions: ${questionCount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Slider(
                        value = questionCount,
                        onValueChange = { questionCount = it },
                        valueRange = 3f..10f,
                        steps = 6
                    )

                    Text("Time Limit: ${durationMinutes.toInt()} minutes", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Slider(
                        value = durationMinutes,
                        onValueChange = { durationMinutes = it },
                        valueRange = 2f..15f,
                        steps = 12
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.startGeneratingMockTest(selectedExamType, questionCount.toInt(), durationMinutes.toInt())
                        showQuizConfigDialog = false
                    },
                    modifier = Modifier.testTag("submit_exam_generation_btn")
                ) {
                    Text("Generate Exam")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuizConfigDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Auxiliary BorderCard container definition
@Composable
fun BorderCard(
    border: androidx.compose.ui.unit.Dp,
    borderColor: Color,
    shape: RoundedCornerShape,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = border, color = borderColor, shape = shape)
            .background(color = MaterialTheme.colorScheme.surface, shape = shape)
    ) {
        content()
    }
}



