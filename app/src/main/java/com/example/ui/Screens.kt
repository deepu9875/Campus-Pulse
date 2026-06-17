package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// --- Simple State Router ---
sealed interface Screen {
    object Launch : Screen
    object StudentLogin : Screen
    object StudentRegister : Screen
    object OrganizerLogin : Screen
    object OrganizerRegister : Screen
    object Home : Screen
    object CollegeSelection : Screen
    data class EventDetail(val event: EventEntity) : Screen
    object FoodStalls : Screen
    object MyTickets : Screen
    object AIChat : Screen
    object OrganizerDashboard : Screen
    object AdminDashboard : Screen
    object NoticeBoard : Screen
    object Profile : Screen
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationContainer(
    viewModel: CampusPulseViewModel,
    modifier: Modifier = Modifier
) {
    val navHistory = remember { mutableStateListOf<Screen>(Screen.Launch) }
    val currentScreen = navHistory.lastOrNull() ?: Screen.Launch

    // Custom back handler to support backstack popping
    BackHandler(enabled = navHistory.size > 1) {
        if (navHistory.size > 1) {
            navHistory.removeAt(navHistory.size - 1)
        }
    }

    val navigateTo: (Screen) -> Unit = { screen ->
        // Avoid duplicate active screens in backstack
        if (screen is Screen.Home) {
            navHistory.clear()
            navHistory.add(Screen.Home)
        } else {
            navHistory.add(screen)
        }
    }

    val navigateBack: () -> Unit = {
        if (navHistory.size > 1) {
            navHistory.removeAt(navHistory.size - 1)
        }
    }

    // Standard Scaffold with Dynamic Bottom Bar or Navigation Rail for Expanded screens and TopBar
    val userState by viewModel.userState.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun showSnack(msg: String) {
        scope.launch {
            snackbarHostState.showSnackbar(msg)
        }
    }

    var showOpeningSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1600)
        showOpeningSplash = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (currentScreen != Screen.Launch &&
                currentScreen != Screen.StudentLogin &&
                currentScreen != Screen.StudentRegister &&
                currentScreen != Screen.OrganizerLogin &&
                currentScreen != Screen.OrganizerRegister
            ) {
                CampusTopBar(
                    currentScreen = currentScreen,
                    currentUser = userState,
                    unreadNotifCount = notifications.size,
                    navigateBack = navigateBack,
                    navigateTo = navigateTo,
                    onLogout = {
                        viewModel.logout()
                        navHistory.clear()
                        navHistory.add(Screen.Launch)
                        showSnack("Logged out successfully")
                    },
                    modifier = Modifier.testTag("app_top_bar")
                )
            }
        },
        bottomBar = {
            // Only show bottom navigation on home/interactive tabs
            if (currentScreen == Screen.Home ||
                currentScreen == Screen.FoodStalls ||
                currentScreen == Screen.MyTickets ||
                currentScreen == Screen.AIChat ||
                currentScreen == Screen.Profile
            ) {
                CampusBottomNavigationBar(
                    currentScreen = currentScreen,
                    currentUser = userState,
                    onNavigate = { target -> navigateTo(target) },
                    modifier = Modifier.testTag("app_bottom_bar")
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = modifier.background(MaterialTheme.colorScheme.background)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) togetherWith
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is Screen.Launch -> LaunchScreen(
                        onStudentSelect = { navigateTo(Screen.StudentLogin) },
                        onOrganizerSelect = { navigateTo(Screen.OrganizerLogin) },
                        onAdminSelect = {
                            viewModel.loginAsAdmin()
                            navigateTo(Screen.Home)
                        }
                    )
                    is Screen.StudentLogin -> StudentLoginScreen(
                        viewModel = viewModel,
                        navigateBack = navigateBack,
                        onLoginSuccess = {
                            navigateTo(Screen.Home)
                            showSnack("Welcome to Campus Pulse!")
                        },
                        onNavigateToRegister = { navigateTo(Screen.StudentRegister) }
                    )
                    is Screen.StudentRegister -> StudentRegisterScreen(
                        viewModel = viewModel,
                        navigateBack = navigateBack,
                        onRegisterSuccess = {
                            navigateTo(Screen.Home)
                            showSnack("Account created! Enjoy Campus Pulse.")
                        }
                    )
                    is Screen.OrganizerLogin -> OrganizerLoginScreen(
                        viewModel = viewModel,
                        navigateBack = navigateBack,
                        onLoginSuccess = {
                            navigateTo(Screen.Home)
                            showSnack("Organizer Dashboard activated")
                        },
                        onNavigateToRegister = { navigateTo(Screen.OrganizerRegister) }
                    )
                    is Screen.OrganizerRegister -> OrganizerRegisterScreen(
                        viewModel = viewModel,
                        navigateBack = navigateBack,
                        onRegisterSuccess = {
                            navigateTo(Screen.Home)
                            showSnack("Organizer verification submitted to administrators.")
                        }
                    )
                    is Screen.Home -> HomeScreen(
                        viewModel = viewModel,
                        onSelectEvent = { navigateTo(Screen.EventDetail(it)) },
                        onSelectCollegesTab = { navigateTo(Screen.CollegeSelection) },
                        onSelectLiveNoticeTab = { navigateTo(Screen.NoticeBoard) }
                    )
                    is Screen.CollegeSelection -> CollegeSelectionScreen(
                        viewModel = viewModel,
                        navigateBack = navigateBack,
                        onSelectEvent = { navigateTo(Screen.EventDetail(it)) }
                    )
                    is Screen.EventDetail -> EventDetailsScreen(
                        event = screen.event,
                        viewModel = viewModel,
                        navigateBack = navigateBack,
                        onRegistrationSuccess = { showSnack("Seat successfully booked! Generating QR code...") }
                    )
                    is Screen.FoodStalls -> FoodStallsScreen(
                        viewModel = viewModel,
                        onShowMessage = { showSnack(it) }
                    )
                    is Screen.MyTickets -> MyTicketsScreen(
                        viewModel = viewModel
                    )
                    is Screen.AIChat -> AIChatScreen(
                        viewModel = viewModel
                    )
                    is Screen.OrganizerDashboard -> OrganizerDashboardScreen(
                        viewModel = viewModel,
                        onShowMessage = { showSnack(it) }
                    )
                    is Screen.AdminDashboard -> AdminDashboardScreen(
                        viewModel = viewModel,
                        onShowMessage = { showSnack(it) }
                    )
                    is Screen.NoticeBoard -> NoticeBoardScreen(
                        viewModel = viewModel,
                        onShowMessage = { showSnack(it) }
                    )
                    is Screen.Profile -> ProfileScreen(
                        viewModel = viewModel,
                        onShowMessage = { showSnack(it) }
                    )
                }
            }
        }
    }

    // Beautiful custom Opening Splash Transition panel
        AnimatedVisibility(
            visible = showOpeningSplash,
            enter = fadeIn(animationSpec = tween(durationMillis = 300)),
            exit = fadeOut(animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background) // Use correct theme background (Scandinavian light #F5F5F7 or Dark)
                    .clickable(enabled = false) {}, // Intercept clicks during opening
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    // Pulsing animated scaling icon badge
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 0.95f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )

                    Box(
                        modifier = Modifier
                            .size((96 * scale).dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Logo",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "CAMPUS PULSE",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 4.sp,
                            color = MaterialTheme.colorScheme.onBackground // Adapts to theme (No-compromise EditorialNearBlack #1C1C1E)
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "EVENT DISCOVERY • CO-ORDINATION • FOOD PRE-BOOKING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f) // Adapts to theme
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    // Minimalist smooth line progress indicator representing resource caching
                    LinearProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                        modifier = Modifier
                            .width(150.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }
    }
}

// --- TopBar Component ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusTopBar(
    currentScreen: Screen,
    currentUser: CurrentUser,
    unreadNotifCount: Int,
    navigateBack: () -> Unit,
    navigateTo: (Screen) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "Campus Pulse",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.sp
                )
            )
        },
        navigationIcon = {
            if (currentScreen != Screen.Home) {
                IconButton(onClick = navigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        },
        actions = {
            Box {
                var expanded by remember { mutableStateOf(false) }
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu"
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("My Profile") },
                        leadingIcon = { Icon(Icons.Default.Person, "Profile") },
                        onClick = {
                            expanded = false
                            navigateTo(Screen.Profile)
                        }
                    )
                    if (currentUser is CurrentUser.Organizer) {
                        DropdownMenuItem(
                            text = { Text("Organizer Dashboard") },
                            leadingIcon = { Icon(Icons.Default.Dashboard, "Dashboard") },
                            onClick = {
                                expanded = false
                                navigateTo(Screen.OrganizerDashboard)
                            }
                        )
                    }
                    if (currentUser is CurrentUser.Admin) {
                        DropdownMenuItem(
                            text = { Text("Admin Panel") },
                            leadingIcon = { Icon(Icons.Default.AdminPanelSettings, "Admin") },
                            onClick = {
                                expanded = false
                                navigateTo(Screen.AdminDashboard)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Logout") },
                        leadingIcon = { Icon(Icons.Default.Logout, "Logout") },
                        onClick = {
                            expanded = false
                            onLogout()
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        modifier = modifier
    )
}

// --- Bottom Navigation Component ---
@Composable
fun CampusBottomNavigationBar(
    currentScreen: Screen,
    currentUser: CurrentUser,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Discover", fontSize = 11.sp) },
            selected = currentScreen == Screen.Home,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ),
            onClick = { onNavigate(Screen.Home) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Restaurant, contentDescription = "Food") },
            label = { Text("Stalls", fontSize = 11.sp) },
            selected = currentScreen == Screen.FoodStalls,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ),
            onClick = { onNavigate(Screen.FoodStalls) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = "Tickets") },
            label = { Text("My Library", fontSize = 11.sp) },
            selected = currentScreen == Screen.MyTickets,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ),
            onClick = { onNavigate(Screen.MyTickets) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.SmartToy, contentDescription = "AI Assistant") },
            label = { Text("Pulse AI", fontSize = 11.sp) },
            selected = currentScreen == Screen.AIChat,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ),
            onClick = { onNavigate(Screen.AIChat) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile", fontSize = 11.sp) },
            selected = currentScreen == Screen.Profile,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ),
            onClick = { onNavigate(Screen.Profile) }
        )
    }
}

// --- Dynamic Confetti Overlay Composable (Micro interaction) ---
@Composable
fun ConfettiOverlay(trigger: Boolean) {
    if (!trigger) return
    val particles = remember {
        List(40) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -0.2f,
                color = Color(
                    Random.nextInt(150, 255),
                    Random.nextInt(80, 200),
                    Random.nextInt(0, 100)
                ),
                speedY = Random.nextFloat() * 8f + 5f,
                speedX = Random.nextFloat() * 4f - 2f,
                size = Random.nextFloat() * 15f + 10f
            )
        }
    }
    
    var progress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(trigger) {
        val animation = TargetAnimation(0f, 1f)
        var current = 0f
        while (current < 1f) {
            delay(16)
            current += 0.02f
            progress = current
        }
    }

    if (progress < 1f) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                val calculatedY = size.height * (p.y + (progress * p.speedY * 0.1f))
                val calculatedX = size.width * (p.x + (progress * p.speedX * 0.05f))
                drawCircle(
                    color = p.color.copy(alpha = (1f - progress).coerceIn(0f, 1f)),
                    radius = p.size,
                    center = Offset(calculatedX, calculatedY)
                )
            }
        }
    }
}

data class ConfettiParticle(
    val x: Float,
    var y: Float,
    val color: Color,
    val speedY: Float,
    val speedX: Float,
    val size: Float
)

class TargetAnimation(val start: Float, val end: Float)

// --- Launch Screen ---
@Composable
fun LaunchScreen(
    onStudentSelect: () -> Unit,
    onOrganizerSelect: () -> Unit,
    onAdminSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .testTag("launch_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 450.dp)
        ) {
            // Hero Space Illustration
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Campus Pulse",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Centralized Hub for Event discovery, secure pre-bookings, live coordinator noticeboards, and smart AI chat assistance.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.tertiary
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Premium Scandinavian Minimalism Float Buttons
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStudentSelect() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE3F2FD)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Student Icon",
                            tint = Color(0xFF1E88E5)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Student Entry",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Discover, register & buy food coupons",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOrganizerSelect() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BusinessCenter,
                            contentDescription = "Organizer Icon",
                            tint = Color(0xFFFB8C00)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "College Organizer",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Publish events, manage stalls & announcements",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Subdued Developer bypass
            Text(
                text = "Super Developer Mode",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .clickable { onAdminSelect() }
                    .padding(8.dp)
            )
        }
    }
}

// --- Student Login Screen ---
@Composable
fun StudentLoginScreen(
    viewModel: CampusPulseViewModel,
    navigateBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = navigateBack, modifier = Modifier.align(Alignment.Start)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome, Student",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Log in using your official campus registers",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Official College Email") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_email_field"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (statusText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(statusText, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (email.isEmpty()) {
                        statusText = "Please enter your registered email"
                        return@Button
                    }
                    loading = true
                    viewModel.selectStudentLogin(email) { success, msg ->
                        loading = false
                        statusText = msg
                        if (success) {
                            onLoginSuccess()
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("student_login_btn")
            ) {
                if (loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Secure Log In", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("New to Campus Pulse? ")
                Text(
                    text = "Register Free",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }

            // Quick Fill Option for Easy testing
            Spacer(modifier = Modifier.height(32.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Sandbox Bypass (Quick Testing)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            email = "guest@nordicuni.edu"
                            viewModel.registerStudent(
                                name = "Sophia Henderson",
                                email = email,
                                collegeName = "Nordic University of Technology",
                                stream = "Software Engineering",
                                year = "3rd Year",
                                contactNo = "+45 9201920",
                                regNo = "NORD-2024-8291"
                            ) { success, _ ->
                                if (success) onLoginSuccess()
                            }
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Create & Login Sandbox Student", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// --- Student Registration Screen ---
@Composable
fun StudentRegisterScreen(
    viewModel: CampusPulseViewModel,
    navigateBack: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var college by remember { mutableStateOf("") }
    var stream by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var regNo by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 455.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = navigateBack, modifier = Modifier.align(Alignment.Start)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }

            Text(
                text = "Student Registration",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Official College Email") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = college,
                onValueChange = { college = it },
                label = { Text("College Name") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = regNo,
                onValueChange = { regNo = it },
                label = { Text("College Registration Number") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = stream,
                    onValueChange = { stream = it },
                    label = { Text("Stream") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("Year (e.g. 3rd)") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = contact,
                onValueChange = { contact = it },
                label = { Text("Contact Number") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (statusText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(statusText, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    loading = true
                    viewModel.registerStudent(
                        name = name,
                        email = email,
                        collegeName = college,
                        stream = stream,
                        year = year,
                        contactNo = contact,
                        regNo = regNo
                    ) { success, msg ->
                        loading = false
                        statusText = msg
                        if (success) {
                            onRegisterSuccess()
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Complete Registration", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

// --- Organizer Login Screen ---
@Composable
fun OrganizerLoginScreen(
    viewModel: CampusPulseViewModel,
    navigateBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = navigateBack, modifier = Modifier.align(Alignment.Start)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome, Organizer",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Coordinate and publish your campus resources",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Official College Coordinator Email") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Official Password") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (statusText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(statusText, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (email.isEmpty()) {
                        statusText = "Please fill in coordinator email"
                        return@Button
                    }
                    loading = true
                    viewModel.selectOrganizerLogin(email) { success, msg ->
                        loading = false
                        statusText = msg
                        if (success) {
                            onLoginSuccess()
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Secure Log In", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("New Organizer? ")
                Text(
                    text = "Request Credentials",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }

            // Quick seeds
            Spacer(modifier = Modifier.height(32.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Sandbox Coordinator", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Direct entry with preseeded coordinator account.", fontSize = 11.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            email = "admin@nordicuni.edu"
                            viewModel.selectOrganizerLogin(email) { success, _ ->
                                if (success) onLoginSuccess()
                            }
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Use Preseeded: admin@nordicuni.edu", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// --- Organizer Registration Screen ---
@Composable
fun OrganizerRegisterScreen(
    viewModel: CampusPulseViewModel,
    navigateBack: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var college by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var eventName by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 450.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = navigateBack, modifier = Modifier.align(Alignment.Start)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }

            Text(
                text = "Credentials Request",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Submit official college representative proof",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Organizer Representative Name") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Official College Email") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = college,
                onValueChange = { college = it },
                label = { Text("Represented College / University") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = eventName,
                onValueChange = { eventName = it },
                label = { Text("Primary Event Responsibility (e.g. Hackathon)") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = contact,
                onValueChange = { contact = it },
                label = { Text("Official Contact Number") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (statusText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(statusText, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    loading = true
                    viewModel.registerOrganizer(
                        name = name,
                        email = email,
                        collegeName = college,
                        contactNo = contact,
                        eventName = eventName
                    ) { success, msg ->
                        loading = false
                        statusText = msg
                        if (success) {
                            onRegisterSuccess()
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Submit Coordinator Profile", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

// --- Home Screen ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: CampusPulseViewModel,
    onSelectEvent: (EventEntity) -> Unit,
    onSelectCollegesTab: () -> Unit,
    onSelectLiveNoticeTab: () -> Unit
) {
    val events by viewModel.allEvents.collectAsState()
    val techEvents by viewModel.technicalEvents.collectAsState()
    val nonTechEvents by viewModel.nonTechnicalEvents.collectAsState()
    val announcements by viewModel.announcements.collectAsState()
    val userState by viewModel.userState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryTab by remember { mutableStateOf(0) } // 0 = All, 1 = Tech, 2 = Non-Tech

    // Extract unique college names
    val uniqueColleges = remember(events) {
        events.map { it.collegeName }.distinct()
    }

    // Filter events
    val filteredEvents = remember(events, searchQuery, selectedCategoryTab) {
        events.filter { event ->
            val matchesSearch = event.title.contains(searchQuery, ignoreCase = true) ||
                    event.description.contains(searchQuery, ignoreCase = true) ||
                    event.collegeName.contains(searchQuery, ignoreCase = true)
            
            val matchesTab = when (selectedCategoryTab) {
                1 -> event.isTechnical
                2 -> !event.isTechnical
                else -> true
            }
            matchesSearch && matchesTab
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Hero Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CAMPUS PULSE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                letterSpacing = 2.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val user = userState
                        Text(
                            text = when (user) {
                                is CurrentUser.Student -> "Hello, ${user.info.name} 🙌"
                                is CurrentUser.Organizer -> "Portal: ${user.info.organizerName} 💼"
                                is CurrentUser.Admin -> "Console: Administrator 🔐"
                                else -> "Join the Pulse 🚀"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }
                    
                    // Welcome Badge/Logo
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)), RoundedCornerShape(16.dp))
                            .clickable { onSelectLiveNoticeTab() },
                        contentAlignment = Alignment.Center
                    ) {
                        BadgedBox(
                            badge = {
                                if (announcements.isNotEmpty()) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text("${announcements.size}", color = Color.White, fontSize = 9.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = "Announcements",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Discover, Connect and Experience Campus Life.",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                )
            }
        }

        // Live scrolling updates notice Board header
        if (announcements.isNotEmpty()) {
            item {
                ScrollingAnnouncementsBanner(announcements, onClick = onSelectLiveNoticeTab)
            }
        }

        // Search and Filter Bar
        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Hackathons, Colleges, Food Stalls...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("search_field"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Tab Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategorySelectorTab(
                        label = "All Events",
                        selected = selectedCategoryTab == 0,
                        onClick = { selectedCategoryTab = 0 }
                    )
                    CategorySelectorTab(
                        label = "Technical",
                        selected = selectedCategoryTab == 1,
                        onClick = { selectedCategoryTab = 1 }
                    )
                    CategorySelectorTab(
                        label = "Non-Technical",
                        selected = selectedCategoryTab == 2,
                        onClick = { selectedCategoryTab = 2 }
                    )
                }
            }
        }

        // Live Event Counter Badge
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Clock",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Active Pulse Directory",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.8f))
                            )
                            Text(
                                text = "${techEvents.size + nonTechEvents.size} Real-time College Events Host",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                            )
                        }
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = CircleShape
                    ) {
                        Text(
                            "LIVE",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Colleges Horizontal List
        item {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Partner Colleges",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.clickable { onSelectCollegesTab() }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uniqueColleges) { collegeName ->
                        val matchingEventCount = events.count { it.collegeName == collegeName }
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            onClick = { onSelectCollegesTab() },
                            modifier = Modifier.width(170.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.School, contentDescription = "", tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = collegeName,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$matchingEventCount Events scheduled",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.tertiary)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Main Events Listings (Staggered-styled Card list matching Pinterest/Airbnb)
        item {
            Text(
                text = "🔥 Happening & Featured Events",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }

        if (filteredEvents.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = "Empty",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No events match your current coordinates. Try filtering or search parameters!",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(filteredEvents) { event ->
                EventListItemCard(
                    event = event,
                    onItemClick = { onSelectEvent(event) },
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun CategorySelectorTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(24.dp),
        border = if (!selected) BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)) else null,
        modifier = Modifier
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

// Custom simple border stroke mapping removed to use foundation.BorderStroke

// --- Event list item card ---
@Composable
fun EventListItemCard(
    event: EventEntity,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
    ) {
        Column {
            // Fake visual placeholder banner using material color shades
            val bannerColor = if (event.isTechnical) Color(0xFF1A237E) else Color(0xFFD81B60)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(bannerColor, bannerColor.copy(alpha = 0.5f))
                        )
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                // Category Chip
                Text(
                    text = if (event.isTechnical) "Technical" else "Non-Technical",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .background(Color.Black.copy(0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = event.collegeName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = event.date,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (event.regFee == 0.0) "FREE" else "₹${event.regFee.toInt()}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    }
}

// --- Scrolling Announcement Ticker ---
@Composable
fun ScrollingAnnouncementsBanner(
    announcements: List<AnnouncementEntity>,
    onClick: () -> Unit
) {
    val textToShow = announcements.joinToString("   ★   ") { it.text }
    
    // Ticker animation using states
    var offset by remember { mutableStateOf(0f) }
    LaunchedEffect(key1 = announcements) {
        while (true) {
            delay(40)
            offset -= 2f
            if (offset < -1200f) {
                offset = 400f
            }
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFFF6B00).copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Blinking dynamic pulse circle
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF6B00))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "NOTICE:",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color(0xFFFF6B00),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = textToShow,
                fontSize = 12.sp,
                maxLines = 1,
                color = Color(0xFF1D1D1F),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.offset(x = offset.dp)
            )
        }
    }
}

// --- Partner Colleges Selector ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollegeSelectionScreen(
    viewModel: CampusPulseViewModel,
    navigateBack: () -> Unit,
    onSelectEvent: (EventEntity) -> Unit
) {
    val events by viewModel.allEvents.collectAsState()
    
    val collegesList = remember(events) {
        events.map { event ->
            CollegeDetail(
                name = event.collegeName,
                venue = event.venue
            )
        }.distinctBy { it.name }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Partner Institutions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Discover events specific to represents colleges affiliated with Campus Pulse system.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            items(collegesList) { college ->
                val matchingEvents = events.filter { it.collegeName == college.name }
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = "",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    college.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    college.venue,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Events hosted here:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        matchingEvents.forEach { event ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectEvent(event) }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    event.title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(0.12f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (event.regFee == 0.0) "Free" else "₹${event.regFee.toInt()}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class CollegeDetail(val name: String, val venue: String)

// --- Event Details Screen with micro actions, calendar and QR ticket ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(
    event: EventEntity,
    viewModel: CampusPulseViewModel,
    navigateBack: () -> Unit,
    onRegistrationSuccess: () -> Unit
) {
    val registrations by viewModel.studentRegistrations.collectAsState()
    val userState by viewModel.userState.collectAsState()
    
    val isRegistered = remember(registrations) {
        registrations.any { it.eventId == event.id && it.status == "PAID" }
    }
    
    val myTicketRecord = remember(registrations) {
        registrations.firstOrNull { it.eventId == event.id && it.status == "PAID" }
    }

    var triggerConfetti by remember { mutableStateOf(false) }
    var showRegisterWarningDialog by remember { mutableStateOf(false) }
    
    val localSnackbarHostState = remember { SnackbarHostState() }
    val localScope = rememberCoroutineScope()
    fun showLocalSnack(msg: String) {
        localScope.launch {
            localSnackbarHostState.showSnackbar(msg)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(localSnackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Event Coordinates") },
                    navigationIcon = {
                        IconButton(onClick = navigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
            },
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
            ) {
                // Event Banner
                val bannerColor = if (event.isTechnical) Color(0xFF1A237E) else Color(0xFFD81B60)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(bannerColor)
                        .padding(24.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (event.isTechnical) "Technical" else "Non-Technical",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            event.title,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        )
                    }
                }

                Column(modifier = Modifier.padding(24.dp)) {
                    // Registration Summary Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-40).dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("REGISTRATION FEE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                    Text(
                                        if (event.regFee == 0.0) "FREE" else "₹${event.regFee.toInt()} INR",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("PRIZE MONEY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                    Text(
                                        "₹${event.prizeMoney.toInt()} INR",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Schedule, "", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("DATE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                        Text(event.date, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PinDrop, "", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("VENUE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                        Text(event.venue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            // Countdown Mock indicator
                            Text(
                                "Seats Left: ${event.seatsLeft} of ${event.capacity}",
                                fontWeight = FontWeight.Bold,
                                color = if (event.seatsLeft < 30) Color.Red else MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }

                    // Description
                    Text("About the Event", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        event.description,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Rules details
                    Text("Official Guidelines & Rules", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            event.rules.split(". ").forEach { rule ->
                                if (rule.trim().isNotEmpty()) {
                                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text("•", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                                        Text(rule.trim(), style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Guest Speakers
                    Text("Distinguished Coordinates", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(event.guestSpeakers, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)

                    Spacer(modifier = Modifier.height(28.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(24.dp))

                    // Explore Fest Highlights: 3 Dynamic options as requested
                    Text(
                        text = "Explore Attractions",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                    )
                    Text(
                        text = "Select an option to view sub-events or pre-book discount food stalls coupons.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    var selectedAttrTab by remember { mutableStateOf(0) }
                    val attrTabsColors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                    
                    // 3 Custom visually highly polished horizontal selection buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(0, "Technical Events", Icons.Default.Laptop),
                            Triple(1, "Non-Technical", Icons.Default.MusicNote),
                            Triple(2, "Food Stalls", Icons.Default.Restaurant)
                        ).forEach { (idx, title, icon) ->
                            val isSelected = selectedAttrTab == idx
                            Surface(
                                onClick = { selectedAttrTab = idx },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)) else null,
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(icon, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(title, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }

                    when (selectedAttrTab) {
                        0 -> {
                            // Technical events list: workshops, poster design, coding competitions, hackathons, project expo
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                val techSubEvents = listOf(
                                    SubEventDetail("Advanced Core AI & Prompting", "Workshops", "Hands-on engineering using Gemini models, structured output schemas, and zero-shot contexts.", "09:30 AM", "Main Tech Wing Lab", "💻"),
                                    SubEventDetail("Green Campus 36H Hackathon", "Hackathons", "Rapid 36-hour sprint prototype competition centering digital wellness & energy tracking tools.", "11:00 AM", "Auditorium Annex 2", "🤖"),
                                    SubEventDetail("Speed Compose Pro Matchup", "Coding Competitions", "Construct gorgeous, highly accessible, responsive Material Design 3 templates under tight timer constraints.", "02:00 PM", "Sandbox Server Arena", "⚡"),
                                    SubEventDetail("National Future-Prototyping Expo", "Project Expo", "Exhibit fully-functioning hardware tools, drone models, or custom-programmed IoT sandboxes.", "03:30 PM", "Engineering Atrium Hall", "🚀"),
                                    SubEventDetail("Minimalist Clean-Energy Displays", "Poster Design", "Design elegant, high-contrast digital summaries showing creative sustainability action frameworks.", "05:00 PM", "Art Wing Studio C", "📐")
                                )
                                techSubEvents.forEach { sub ->
                                    SubEventCardView(sub)
                                }
                            }
                        }
                        1 -> {
                            // Non-tech events list: games, movie quiz, singing, dance, horror house
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                val nonTechSubEvents = listOf(
                                    SubEventDetail("Console Battle: FIFA Matchups", "Games", "Live tournament with visual projection feeds, dynamic commentary and customized brackets.", "10:00 AM", "Student Activity Lounge", "🎮"),
                                    SubEventDetail("Director's Choice Trivia", "Movie Quiz", "Engage in competitive trivia matching classic soundtracks, hidden scripts & cinematic easter eggs.", "01:30 PM", "Lakeside Deck Lounge", "🍿"),
                                    SubEventDetail("Pure Bliss Sunset Acoustic Open", "Singing Competition", "Vocal combat supported purely by custom-arranged rhythmic acoustic instrumentations.", "04:00 PM", "Lakeside Amphitheater", "🎤"),
                                    SubEventDetail("Spotlight Streetstyle Face-Off", "Dance Competition", "Highly interactive freestyle crew dancing battle under direct theater spotlights.", "05:30 PM", "Main Assembly Deck", "💃"),
                                    SubEventDetail("Dracula's Maze of Shadows", "Horror House", "Navigate standard darkness! Immersive layout with atmospheric mist, actors and scares.", "All Day", "Old College Cellars", "🧟")
                                )
                                nonTechSubEvents.forEach { sub ->
                                    SubEventCardView(sub)
                                }
                            }
                        }
                        2 -> {
                            // Food stalls list: displayed separately with some images and stalls name. Tap to show menu along with prices, rate stall, feedback, prebook coupons (only if registered!)
                            val stalls by viewModel.foodStalls.collectAsState()
                            var activeStallId by remember(stalls) { mutableStateOf<String?>(stalls.firstOrNull()?.id ?: "stall_1") }

                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("Participating Fast Food Stalls", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                
                                // Beautiful row displaying all food stalls separately
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(stalls) { stall ->
                                        val isSelected = activeStallId == stall.id
                                        Card(
                                            onClick = { activeStallId = stall.id },
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                            ),
                                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                                            shape = RoundedCornerShape(22.dp),
                                            modifier = Modifier.width(135.dp).testTag("event_food_stall_${stall.id}")
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                // Decorative representation of stall image
                                                Box(
                                                    modifier = Modifier
                                                        .size(72.dp)
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color(0xFFF5F5F7)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    val foodEmoji = when (stall.id) {
                                                        "stall_1" -> "🧇"
                                                        "stall_2" -> "☕"
                                                        "stall_3" -> "🥪"
                                                        else -> "🍔"
                                                    }
                                                    Text(foodEmoji, fontSize = 32.sp)
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = stall.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "★ ${stall.rating}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }

                                activeStallId?.let { stallId ->
                                    val currentStall = stalls.firstOrNull { it.id == stallId }
                                    if (currentStall != null) {
                                        Card(
                                            shape = RoundedCornerShape(24.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(20.dp)) {
                                                // Stall Name and Premium Header
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(currentStall.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                                        Text(currentStall.cuisine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                                                    }
                                                    
                                                    // Like Option: Heart Button with Live State Interaction!
                                                    var isLikedLocally by remember(stallId) { mutableStateOf(false) }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        IconButton(
                                                            onClick = {
                                                                isLikedLocally = !isLikedLocally
                                                                viewModel.likeFoodStall(currentStall.id)
                                                                showLocalSnack("Thank you for liking ${currentStall.name}!")
                                                            }
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isLikedLocally) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                                contentDescription = "Like Stall",
                                                                tint = if (isLikedLocally) Color.Red else MaterialTheme.colorScheme.tertiary
                                                            )
                                                        }
                                                    }
                                                }
                                                
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Star, "Rating", tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("${currentStall.rating} Average Rating", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    Spacer(modifier = Modifier.width(16.dp))
                                                    Icon(Icons.Default.ThumbUp, "Likes", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("${currentStall.popularity} Likes", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                                
                                                Spacer(modifier = Modifier.height(16.dp))
                                                HorizontalDivider()
                                                Spacer(modifier = Modifier.height(16.dp))
                                                
                                                // Menu items list displays prices in INR (along with prebooking coupon discount offers!)
                                                Text("Stall Offers and Custom Menu (Prices in INR)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black))
                                                Spacer(modifier = Modifier.height(10.dp))
                                                
                                                val stallCoupons by viewModel.getCouponsForStall(stallId).collectAsState(initial = emptyList())
                                                
                                                if (stallCoupons.isEmpty()) {
                                                    Text("Loading stall offers...", fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary)
                                                } else {
                                                    stallCoupons.forEach { coup ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 6.dp)
                                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f), RoundedCornerShape(14.dp))
                                                                .padding(14.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(coup.itemName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                                Text("Offer Code: ${coup.offers}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                                Spacer(modifier = Modifier.height(2.dp))
                                                                Text("Price: ₹${coup.price.toInt()} INR", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                                                            }
                                                            
                                                            Button(
                                                                onClick = {
                                                                    // Validation Rule: Buying allowed ONLY if registered for this event!
                                                                    if (isRegistered) {
                                                                        viewModel.buyFoodCoupon(coup.id, stallId, "UPI Portal Link") { success, msg ->
                                                                            showLocalSnack(msg)
                                                                        }
                                                                    } else {
                                                                        // Strict restriction rule dialog trigger
                                                                        showRegisterWarningDialog = true
                                                                    }
                                                                },
                                                                shape = RoundedCornerShape(12.dp),
                                                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                                            ) {
                                                                Text("Pre-book Coupon", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                }
                                                
                                                Spacer(modifier = Modifier.height(20.dp))
                                                HorizontalDivider()
                                                Spacer(modifier = Modifier.height(16.dp))
                                                
                                                // Feedback Input Column
                                                Text("Have some thoughts? Leave Feedback", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                
                                                var userFeedbackMsg by remember(stallId) { mutableStateOf("") }
                                                var ratingSelection by remember(stallId) { mutableStateOf(5f) }
                                                
                                                OutlinedTextField(
                                                    value = userFeedbackMsg,
                                                    onValueChange = { userFeedbackMsg = it },
                                                    label = { Text("Describe your dining feedback details") },
                                                    placeholder = { Text("Amazing Nordic waffles, completely crispy...") },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    textStyle = MaterialTheme.typography.bodyMedium,
                                                    shape = RoundedCornerShape(16.dp)
                                                )
                                                
                                                Spacer(modifier = Modifier.height(10.dp))
                                                
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("Score: ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                                        Slider(
                                                            value = ratingSelection,
                                                            onValueChange = { ratingSelection = it },
                                                            valueRange = 1f..5f,
                                                            steps = 3,
                                                            modifier = Modifier.width(120.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("${ratingSelection.toInt()}★", fontSize = 12.sp, fontWeight = FontWeight.Black)
                                                    }
                                                    
                                                    Button(
                                                        onClick = {
                                                            if (userFeedbackMsg.isNotBlank()) {
                                                                viewModel.submitFoodFeedback(
                                                                    stallId = stallId,
                                                                    score = ratingSelection.toDouble(),
                                                                    comment = userFeedbackMsg
                                                                ) { success, msg ->
                                                                    showLocalSnack(msg)
                                                                    userFeedbackMsg = ""
                                                                }
                                                            } else {
                                                                showLocalSnack("Please insert a brief review first!")
                                                            }
                                                        },
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Text("Post Feedback", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                
                                                // Display the live feedback reviews feed of this stall derived from audit logs
                                                val auditLogsState by viewModel.auditLogs.collectAsState()
                                                val activeStallReviews = remember(auditLogsState, stallId) {
                                                    auditLogsState.filter { it.actionType == "FOOD_FEEDBACK" && it.details.contains("Stall ID: $stallId") }
                                                }
                                                
                                                if (activeStallReviews.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Text("Student Reviews Feed", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        activeStallReviews.take(4).forEach { rev ->
                                                            val parsedText = rev.details.substringAfter("Review: ").substringBefore(" | ")
                                                            val parsedScore = rev.details.substringAfter("Score: ").substringBefore(" | ")
                                                            Card(
                                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)),
                                                                shape = RoundedCornerShape(12.dp),
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                Column(modifier = Modifier.padding(12.dp)) {
                                                                    Row(
                                                                        modifier = Modifier.fillMaxWidth(),
                                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                                        verticalAlignment = Alignment.CenterVertically
                                                                    ) {
                                                                        Text(rev.actorEmail, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                                                        Text("${parsedScore.toDoubleOrNull()?.toInt() ?: 5}★", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color(0xFFFFB300))
                                                                    }
                                                                    Spacer(modifier = Modifier.height(2.dp))
                                                                    Text(parsedText, fontSize = 12.sp)
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
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(32.dp))

                    // Ticket Verification QR Reveal or Register button
                    if (isRegistered) {
                        Text(
                            text = "Your Admission Credentials",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CampusTicketQRView(myTicketRecord!!)
                    } else {
                        Text(
                            text = "Ticket Registration Tunnel",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                        )
                        Text(
                            text = "Reserve your slot in this college event instantly. Pre-booking food coupon offers requires this seat to be booked.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Action button
                        Button(
                            onClick = {
                                if (userState is CurrentUser.Student) {
                                    viewModel.registerForEvent(event.id, "UPI Secure Channel") { success, _ ->
                                        if (success) {
                                            triggerConfetti = true
                                            onRegistrationSuccess()
                                            showLocalSnack("Seat successfully booked! Generating your QR ticket...")
                                        }
                                    }
                                } else {
                                    onRegistrationSuccess() // Triggers fallback dialog
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("register_event_btn")
                        ) {
                            Icon(Icons.Default.ConfirmationNumber, "Key")
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                if (userState is CurrentUser.Student) "Book My Seat (INR ${event.regFee.toInt()})" else "Student Login Required",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
        
        // Strict Warning dialog for non-registered students prebooking coupons
        if (showRegisterWarningDialog) {
            AlertDialog(
                onDismissRequest = { showRegisterWarningDialog = false },
                title = { Text("Admission Ticket Required", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
                text = {
                    Text("Validation Rule Failed: You can buy coupons only if you are registered for this event! Please register for '${event.title}' by selecting 'Book My Seat' down below first.")
                },
                confirmButton = {
                    TextButton(onClick = { showRegisterWarningDialog = false }) {
                        Text("Okay", fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(28.dp)
            )
        }
        
        // Confetti Overlayer
        ConfettiOverlay(trigger = triggerConfetti)
    }
}

// Custom neat Ticket card with dash borders (inspired by Apple Wallet)
@Composable
fun CampusTicketQRView(ticket: RegistrationEntity) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "ADMIT ONE TICKET",
                letterSpacing = 2.sp,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                ticket.eventTitle,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Dash divider using custom canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            ) {
                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    pathEffect = pathEffect,
                    strokeWidth = 2f
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Beautiful Minimalist "QR Code" Placeholder
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "QR Code Scanner",
                        tint = Color.Black,
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        ticket.id,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Show this QR ticket at college entrance gate",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

// --- Food Stalls list & Pre-booking coupons validation ---
@Composable
fun FoodStallsScreen(
    viewModel: CampusPulseViewModel,
    onShowMessage: (String) -> Unit
) {
    val stalls by viewModel.foodStalls.collectAsState()
    val activeStudentRegistrations by viewModel.studentRegistrations.collectAsState()
    val userState by viewModel.userState.collectAsState()

    var selectedStallId by remember { mutableStateOf<String?>("stall_1") }
    var buyTransactionStatus by remember { mutableStateOf("") }

    val currentCouponsList by if (selectedStallId != null) {
        viewModel.getCouponsForStall(selectedStallId!!).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList<FoodCouponEntity>()) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("food_stalls_screen"),
        contentPadding = PaddingValues(24.dp)
    ) {
        item {
            Text(
                "Campus Food Court",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black)
            )
            Text(
                "Pre-book customized meal coupons for discount combos on-campus.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Stalls Row selectors
        item {
            Text("Select Food Stall", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(stalls) { stall ->
                    val isSelected = selectedStallId == stall.id
                    Card(
                        onClick = { selectedStallId = stall.id },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(24.dp),
                        border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)) else null,
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.width(160.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Restaurant,
                                    "",
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "★ ${stall.rating}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                stall.name,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                stall.cuisine,
                                fontSize = 11.sp,
                                color = if (isSelected) Color.White.copy(0.8f) else MaterialTheme.colorScheme.tertiary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
            Text("Available Coupon Pre-bookings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (currentCouponsList.isEmpty()) {
            item {
                Text(
                    "No items or offers currently listed in this stall menu.",
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(currentCouponsList) { coupon ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(coupon.itemName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(coupon.offers, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("₹${coupon.price.toInt()} INR", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                        }

                        Button(
                            onClick = {
                                if (userState !is CurrentUser.Student) {
                                    onShowMessage("Student account is required to reserve food coupons.")
                                    return@Button
                                }
                                viewModel.buyFoodCoupon(coupon.id, coupon.stallId, "Google Pay") { success, message ->
                                    onShowMessage(message)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(0.12f))
                        ) {
                            Text("Buy Coupon", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Display validation rules hint to students
        item {
            Spacer(modifier = Modifier.height(32.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, "", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Registration Rule: Coupons can only be purchased if the student has successfully registered for at least one active Campus Pulse event.",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// --- My Tickets, Digital Passes & Coupon library ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyTicketsScreen(
    viewModel: CampusPulseViewModel
) {
    val registrations by viewModel.studentRegistrations.collectAsState()
    val userState by viewModel.userState.collectAsState()

    val (securedPasses, foodCoupons) = remember(registrations) {
        registrations.partition { !it.isPreBookedCoupon }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("my_tickets_screen"),
        contentPadding = PaddingValues(24.dp)
    ) {
        item {
            Text(
                "My Library Passes",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black)
            )
            Text(
                "Your digitally secured seat admissions and prebooked meal coupons.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (userState !is CurrentUser.Student) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ConfirmationNumber, "", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Please log in as a Student to view registered ticket library", textAlign = TextAlign.Center)
                }
            }
        } else {
            item {
                Text("🎟️ Admission Tickets (${securedPasses.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (securedPasses.isEmpty()) {
                item {
                    Text("No events registered yet. Browse the discovery list!", color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(bottom = 24.dp))
                }
            } else {
                items(securedPasses) { ticket ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(ticket.eventTitle, fontWeight = FontWeight.Bold)
                                    Text("Ticket ID: ${ticket.id}", fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (ticket.status == "PAID") Color(0xFFC8E6C9) else Color(0xFFFFCDD2),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        ticket.status,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (ticket.status == "PAID") Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Mini QR helper
                                Icon(Icons.Default.QrCode, "", modifier = Modifier.size(52.dp))
                                
                                Button(
                                    onClick = { viewModel.requestCancelRegistration(ticket.id) },
                                    colors = ButtonDefaults.textButtonColors(),
                                    enabled = ticket.status == "PAID"
                                ) {
                                    Text("Cancel Ticket", color = Color.Red, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("🍔 Food Coupons Library (${foodCoupons.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (foodCoupons.isEmpty()) {
                item {
                    Text("No food coupons booked.", color = MaterialTheme.colorScheme.tertiary)
                }
            } else {
                items(foodCoupons) { coupon ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(coupon.eventTitle, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                Text("Coupon: ${coupon.id}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Default.QrCode, "", modifier = Modifier.size(40.dp))
                        }
                    }
                }
            }
        }
    }
}

// --- AI Chat Helper Screen (Pulse AI Chatroom) ---
@Composable
fun AIChatScreen(
    viewModel: CampusPulseViewModel
) {
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    
    var userTextMsg by remember { mutableStateOf("") }
    val listState = rememberScrollState()

    LaunchedEffect(chatMessages.size) {
        listState.animateScrollTo(listState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("ai_chat_screen")
    ) {
        // Chat Header
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SmartToy, "", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Pulse AI Coordinator", fontWeight = FontWeight.Black, color = Color.White)
                        Text("Answers schedules, rules, food stalls guide etc.", fontSize = 12.sp, color = Color.White.copy(0.8f))
                    }
                }
            }
        }

        // Suggestions horizontal list
        val suggestions = listOf(
            "When is the Hackathon starting?",
            "What criteria restricts food coupons purchase?",
            "View available tech competitions rules",
            "What if I need critical health assistance?"
        )
        
        LazyRow(
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(suggestions) { suggestText ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.clickable {
                        viewModel.sendChatMessage(suggestText)
                    }
                ) {
                    Text(suggestText, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                }
            }
        }

        // Chat list column
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .verticalScroll(listState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (chatMessages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.SmartToy, "", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Pulse AI is fully prepared offline / online. Ask me about dynamic registrations, rule books or coupon discount details!",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                chatMessages.forEach { msg ->
                    ChatBubble(msg)
                }
            }
            if (isChatLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("AI is compiling answer...", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Action Input Bar
        Surface(
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userTextMsg,
                    onValueChange = { userTextMsg = it },
                    placeholder = { Text("Ask Pulse AI assistant...") },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f).testTag("chat_input_field"),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = {
                        if (userTextMsg.isNotEmpty()) {
                            viewModel.sendChatMessage(userTextMsg)
                            userTextMsg = ""
                        }
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(chat: ChatEntity) {
    val containerColor = if (chat.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (chat.isUser) Color.White else MaterialTheme.colorScheme.onSurface
    val alignment = if (chat.isUser) Alignment.End else Alignment.Start
    val shape = if (chat.isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp)
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Card(
            colors = CardDefaults.cardColors(containerColor = containerColor),
            shape = shape
        ) {
            Text(
                text = chat.messageText,
                color = contentColor,
                fontSize = 14.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

// --- Organizer Dashboard ---
@Composable
fun OrganizerDashboardScreen(
    viewModel: CampusPulseViewModel,
    onShowMessage: (String) -> Unit
) {
    val auditLogs by viewModel.auditLogs.collectAsState()
    val events by viewModel.allEvents.collectAsState()

    var eventTitle by remember { mutableStateOf("") }
    var eventDesc by remember { mutableStateOf("") }
    var isTech by remember { mutableStateOf(true) }
    var dateString by remember { mutableStateOf("2026-06-30") }
    var venueString by remember { mutableStateOf("Main Library Hall") }
    var regFeeAmount by remember { mutableStateOf("100") }
    var prizeAmount by remember { mutableStateOf("20000") }
    var capAmount by remember { mutableStateOf("150") }
    
    var rulesStr by remember { mutableStateOf("Be original. Registration passes mandatory.") }
    var speakerStr by remember { mutableStateOf("Prof. Jenkins") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("organizer_dashboard"),
        contentPadding = PaddingValues(24.dp)
    ) {
        item {
            Text("Organizer Portal", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black))
            Text("Publish events and investigate audit activities.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Create event Form
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Publish New College Event", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(value = eventTitle, onValueChange = { eventTitle = it }, label = { Text("Event Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(value = eventDesc, onValueChange = { eventDesc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Category: ")
                        Spacer(modifier = Modifier.width(12.dp))
                        RadioButton(selected = isTech, onClick = { isTech = true })
                        Text("Technical")
                        Spacer(modifier = Modifier.width(12.dp))
                        RadioButton(selected = !isTech, onClick = { isTech = false })
                        Text("Non-Tech")
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = dateString, onValueChange = { dateString = it }, label = { Text("Date") }, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(value = venueString, onValueChange = { venueString = it }, label = { Text("Venue") }, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = regFeeAmount, onValueChange = { regFeeAmount = it }, label = { Text("Fee (₹)") }, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(value = prizeAmount, onValueChange = { prizeAmount = it }, label = { Text("Prize (₹)") }, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(value = capAmount, onValueChange = { capAmount = it }, label = { Text("Capacity limit") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(value = rulesStr, onValueChange = { rulesStr = it }, label = { Text("Rules") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(value = speakerStr, onValueChange = { speakerStr = it }, label = { Text("Guest Speakers") }, modifier = Modifier.fillMaxWidth())
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            viewModel.saveEvent(
                                title = eventTitle,
                                description = eventDesc,
                                isTechnical = isTech,
                                collegeName = "Nordic University of Technology",
                                date = dateString,
                                venue = venueString,
                                regFee = regFeeAmount.toDoubleOrNull() ?: 0.0,
                                prizeMoney = prizeAmount.toDoubleOrNull() ?: 0.0,
                                capacity = capAmount.toIntOrNull() ?: 100,
                                guestSpeakers = speakerStr,
                                rules = rulesStr
                            ) { success, msg ->
                                onShowMessage(msg)
                                if (success) {
                                    eventTitle = ""
                                    eventDesc = ""
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Publish Event", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Delete active events listed
        item {
            Spacer(modifier = Modifier.height(32.dp))
            Text("Active Posted Directory", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (events.isEmpty()) {
            item { Text("No current events active.") }
        } else {
            items(events) { event ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(event.title, fontWeight = FontWeight.Bold)
                            Text("Admissions: ${event.capacity - event.seatsLeft} checked", fontSize = 11.sp)
                        }
                        IconButton(onClick = { viewModel.deleteEvent(event.id) }) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                        }
                    }
                }
            }
        }

        // Active Audit Log History listing
        item {
            Spacer(modifier = Modifier.height(32.dp))
            Text("Coordinator Audit Trails", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (auditLogs.isEmpty()) {
            item { Text("No actions logged yet in audit history schema database.") }
        } else {
            items(auditLogs.take(15)) { log ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(log.actionType, fontWeight = FontWeight.Black, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            Text(log.actorEmail, fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(log.details, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// --- Admin Dashboard Screen ---
@Composable
fun AdminDashboardScreen(
    viewModel: CampusPulseViewModel,
    onShowMessage: (String) -> Unit
) {
    val organizers by viewModel.allOrganizers.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("admin_dashboard"),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Admin Console", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black))
            Text("Certify college coordinators and investigate system-wide activities.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
        }

        item {
            Text("Pending Coordinator Approvals", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        if (organizers.isEmpty()) {
            item {
                Text("No coordinators currently requested approvals on college directory databases.")
            }
        } else {
            items(organizers) { organizer ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(organizer.organizerName, fontWeight = FontWeight.Bold)
                        Text(organizer.collegeName, fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary)
                        Text("Official: ${organizer.email}", fontSize = 11.sp)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (organizer.verified) "CERTIFIED" else "PENDING CLEARANCE",
                                fontWeight = FontWeight.Bold,
                                color = if (organizer.verified) Color(0xFF2E7D32) else Color(0xFFE65100),
                                fontSize = 11.sp
                            )

                            Button(
                                onClick = {
                                    viewModel.toggleOrganizerVerification(organizer.email, !organizer.verified)
                                    onShowMessage("Representative status updated successfully!")
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (organizer.verified) "Revoke Approvals" else "Approve Access")
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("All Logged Transactions & DB Audit Trails", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }

        if (auditLogs.isEmpty()) {
            item {
                Text("No database actions yet.")
            }
        } else {
            items(auditLogs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(log.actionType, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                        Text(log.details, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Actor: ${log.actorEmail}", fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
    }
}

// --- Scrolling Notice Board (Real-time announcements screen) ---
@Composable
fun NoticeBoardScreen(
    viewModel: CampusPulseViewModel,
    onShowMessage: (String) -> Unit
) {
    val announcements by viewModel.announcements.collectAsState()
    val userState by viewModel.userState.collectAsState()
    
    var noticeText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("notice_board"),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Campus Notices Board", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black))
            Text("Real-time college listings, scheduling delayed alerts, and fast updates.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
        }

        if (userState is CurrentUser.Organizer) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Publish New Global Announcement", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = noticeText,
                            onValueChange = { noticeText = it },
                            placeholder = { Text("E.g. Hackathon shifts to Auditorium 2 in 10 minutes!") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                viewModel.postAnnouncement(noticeText) { success, message ->
                                    if (success) {
                                        noticeText = ""
                                    }
                                    onShowMessage(message)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Publish Announcement Notice", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text("Global Notices Streams", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        if (announcements.isEmpty()) {
            item {
                Text("No notice streams recorded.")
            }
        } else {
            items(announcements) { notice ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Campaign, "", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(notice.creatorName, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(notice.text, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

// Sub-event Models and components definitions for attractions exploration tabs
data class SubEventDetail(
    val title: String,
    val type: String,
    val description: String,
    val time: String,
    val venue: String,
    val icon: String
)

@Composable
fun SubEventCardView(sub: SubEventDetail) {
    Card(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(sub.icon, fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = sub.type.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = sub.time,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = sub.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = sub.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PinDrop,
                        contentDescription = "Venue",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = sub.venue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

// --- Dynamic User Profile & Scandinavian Theme Switcher Screen ---
@Composable
fun ProfileScreen(
    viewModel: CampusPulseViewModel,
    onShowMessage: (String) -> Unit
) {
    val userState by viewModel.userState.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header Section: Editorial Title ---
        item {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "My Profile & Settings",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = "Control your theme mode, active credentials, and system metrics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // --- Active Student/Organizer Profile Card ---
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val displayName = when (val user = userState) {
                        is CurrentUser.Student -> user.info.name
                        is CurrentUser.Organizer -> user.info.organizerName
                        is CurrentUser.Admin -> "Administrator Coordinator"
                        else -> "Anonymous Guest"
                    }
                    val displaySubText = when (val user = userState) {
                        is CurrentUser.Student -> user.info.email
                        is CurrentUser.Organizer -> user.info.email
                        is CurrentUser.Admin -> "admin@campuspulse.com"
                        else -> "Log in to link tickets, alerts, and stall selections"
                    }
                    val initials = if (displayName.isNotEmpty()) {
                        displayName.split(" ").filter { it.isNotEmpty() }.take(2)
                            .map { it.first().uppercase() }.joinToString("")
                    } else {
                        "GP"
                    }

                    // Large dynamic circular badge
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = displaySubText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    val badgeLabel = when (userState) {
                        is CurrentUser.Student -> "STUDENT DELEGATE"
                        is CurrentUser.Organizer -> "EVENT ORGANIZER"
                        is CurrentUser.Admin -> "SYSTEM COMPLIANCE ADMIN"
                        else -> "GUEST MODE"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = badgeLabel,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }
        }

        // --- Active Credential Detail list depending of userState ---
        val userItem = userState
        if (userItem is CurrentUser.Student) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Student Credentials",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        ProfileInfoRow(icon = Icons.Default.AccountCircle, label = "University ID / Reg No", value = userItem.info.regNo)
                        Spacer(modifier = Modifier.height(12.dp))
                        ProfileInfoRow(icon = Icons.Default.School, label = "Host College", value = userItem.info.collegeName)
                        Spacer(modifier = Modifier.height(12.dp))
                        ProfileInfoRow(icon = Icons.Default.Settings, label = "Academics Branch", value = userItem.info.stream)
                        Spacer(modifier = Modifier.height(12.dp))
                        ProfileInfoRow(icon = Icons.Default.Info, label = "Year", value = "${userItem.info.year} Year")
                        Spacer(modifier = Modifier.height(12.dp))
                        ProfileInfoRow(icon = Icons.Default.Phone, label = "Contact Phone", value = userItem.info.contactNo)
                    }
                }
            }
        } else if (userItem is CurrentUser.Organizer) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Organizer Credentials",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        ProfileInfoRow(icon = Icons.Default.School, label = "College Hub", value = userItem.info.collegeName)
                        Spacer(modifier = Modifier.height(12.dp))
                        ProfileInfoRow(icon = Icons.Default.Event, label = "Responsible Category", value = userItem.info.eventName)
                        Spacer(modifier = Modifier.height(12.dp))
                        ProfileInfoRow(icon = Icons.Default.Phone, label = "Emergency contact", value = userItem.info.contactNo)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verification status",
                                tint = if (userItem.info.verified) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Coordinator Check",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = if (userItem.info.verified) "Officially Verified Representative" else "Verification Approval Pending",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = if (userItem.info.verified) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        } else if (userItem is CurrentUser.Admin) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Admin Access Control",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        ProfileInfoRow(icon = Icons.Default.AdminPanelSettings, label = "Access Status", value = "Full Command Console Active")
                        Spacer(modifier = Modifier.height(12.dp))
                        ProfileInfoRow(icon = Icons.Default.Lock, label = "Clearance Mode", value = "Standard System Coordinator (Master Overseer)")
                    }
                }
            }
        }

        // --- Theme Configuration Segmented selector (LIGHT / DARK) ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Visual Interface Settings",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Choose interface theme based on your environmental preferences",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val themeSelectionOptions = listOf(
                        Triple("system", "Follow Android System", Icons.Default.Settings),
                        Triple("light", "Scandinavian Light Mode", Icons.Default.LightMode),
                        Triple("dark", "Cosmic Material Dark", Icons.Default.DarkMode)
                    )

                    themeSelectionOptions.forEach { (modeVal, modeTitle, themeIcon) ->
                        val isCurrentlySelected = themeMode == modeVal

                        Surface(
                            onClick = {
                                viewModel.setThemeMode(modeVal)
                                val confirmationMessage = when (modeVal) {
                                    "light" -> "Crisp Scandinavian Light mode enabled!"
                                    "dark" -> "Deep Cosmic Dark mode enabled!"
                                    else -> "Interface automatically syncing with system dynamic mode."
                                }
                                onShowMessage(confirmationMessage)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrentlySelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isCurrentlySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isCurrentlySelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = themeIcon,
                                        contentDescription = modeTitle,
                                        tint = if (isCurrentlySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = modeTitle,
                                    fontWeight = if (isCurrentlySelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCurrentlySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 14.sp
                                )
                                if (isCurrentlySelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (themeMode == "light") {
                            "Scandinavian Light: styled with responsive light palettes (#F5F5F7) featuring warm college primary orange highlights and premium charcoal typography."
                        } else if (themeMode == "dark") {
                            "Deep Cosmic Dark: rendering comfortable night experiences utilizing modern deep carbon canvases (#131314) and balanced soft-contrast borders."
                        } else {
                            "Dynamic Auto mode automatically monitors your operating system settings to seamlessly balance brightness and visibility levels."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // --- System Options Settings Card ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "System Utilities",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onShowMessage("Database Cache Resynced. Storage Optimized.") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Clear Cache & Resync Database",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
