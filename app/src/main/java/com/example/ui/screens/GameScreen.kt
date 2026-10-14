package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameMode
import com.example.data.TrackType
import com.example.ui.components.GameCanvas
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onNavigateToGarage: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToLab: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val telemetry by viewModel.telemetryFlow.collectAsState()
    val admin = uiState.adminSettings

    var showModeMenu by remember { mutableStateOf(false) }
    var showTrackMenu by remember { mutableStateOf(false) }

    // Driving input tracking
    var isSteeringLeft by remember { mutableStateOf(false) }
    var isSteeringRight by remember { mutableStateOf(false) }
    var isThrottling by remember { mutableStateOf(false) }
    var isBraking by remember { mutableStateOf(false) }
    var isNitroPressed by remember { mutableStateOf(false) }

    // Apply continuous input to viewModel
    LaunchedEffect(isSteeringLeft, isSteeringRight) {
        val steer = when {
            isSteeringLeft && !isSteeringRight -> -35f
            isSteeringRight && !isSteeringLeft -> 35f
            else -> 0f
        }
        viewModel.setPlayerSteer(steer)
    }

    LaunchedEffect(isThrottling) {
        viewModel.setPlayerThrottle(if (isThrottling) 1.0f else 0f)
    }

    LaunchedEffect(isBraking) {
        viewModel.setPlayerBrake(if (isBraking) 1.0f else 0f)
    }

    LaunchedEffect(isNitroPressed) {
        viewModel.setPlayerNitro(isNitroPressed)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mode Selector Button
                        Box {
                            OutlinedButton(
                                onClick = { showModeMenu = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = DarkNavy
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(NeonCyan)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("mode_selector_button")
                            ) {
                                Text(
                                    uiState.selectedGameMode.displayName,
                                    color = NeonCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showModeMenu,
                                onDismissRequest = { showModeMenu = false },
                                modifier = Modifier.background(DarkSurface)
                            ) {
                                GameMode.values().forEach { mode ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(mode.displayName, color = TextPrimary, fontWeight = FontWeight.Bold)
                                                Text(mode.description, color = TextSecondary, fontSize = 11.sp)
                                            }
                                        },
                                        onClick = {
                                            viewModel.setGameMode(mode)
                                            showModeMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Credits Display
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkNavy)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = GoldYellow,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${uiState.credits}",
                                color = GoldYellow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                actions = {
                    // Garage Button
                    IconButton(
                        onClick = onNavigateToGarage,
                        modifier = Modifier.testTag("nav_garage_button")
                    ) {
                        Icon(imageVector = Icons.Default.Garage, contentDescription = "Garage", tint = NeonCyan)
                    }

                    // AI Sensor Lab Button
                    IconButton(
                        onClick = onNavigateToLab,
                        modifier = Modifier.testTag("nav_lab_button")
                    ) {
                        Icon(imageVector = Icons.Default.Science, contentDescription = "AI Lab", tint = NeonGreen)
                    }

                    // Admin Panel Button (Dedicated with glowing shield)
                    IconButton(
                        onClick = onNavigateToAdmin,
                        modifier = Modifier.testTag("nav_admin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Panel",
                            tint = NeonOrange
                        )
                    }

                    // Restart / Reset
                    IconButton(
                        onClick = { viewModel.startNewGameSession() },
                        modifier = Modifier.testTag("restart_game_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Restart", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkNavy
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. High Performance Game Canvas (Track, Cars, Obstacles, Laser Raycasts)
            GameCanvas(
                playerVehicle = viewModel.playerVehicle,
                opponentVehicles = viewModel.opponentVehicles,
                obstacles = viewModel.obstacles,
                waypoints = viewModel.waypoints,
                particles = viewModel.physicsEngine.particles,
                adminSettings = admin,
                trackMinX = viewModel.trackMinX,
                trackMaxX = viewModel.trackMaxX,
                modifier = Modifier.fillMaxSize()
            )

            // 2. Top HUD: Speedometer, Health, Nitro, Lap Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Speedometer & Gear Box
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface.copy(alpha = 0.9f))
                            .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("SPEED", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${telemetry.speedKmh.toInt()}",
                            color = NeonCyan,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text("KM / H", color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Center: Health & Nitro Gauges
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface.copy(alpha = 0.9f))
                            .padding(10.dp)
                    ) {
                        // Integrity / Health
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(if (admin.godMode) "ARMOR: GOD MODE" else "ARMOR", color = if (admin.godMode) GoldYellow else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${telemetry.healthPct.toInt()}%", color = if (telemetry.healthPct > 30) NeonGreen else DangerRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { (telemetry.healthPct / 100f).coerceIn(0f, 1f) },
                            color = if (admin.godMode) GoldYellow else if (telemetry.healthPct > 30) NeonGreen else DangerRed,
                            trackColor = DarkNavy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Nitro Boost Gauge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(if (admin.infiniteNitro) "NITRO: INFINITE" else "NITRO", color = NeonOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${telemetry.nitroLevelPct.toInt()}%", color = NeonOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { (telemetry.nitroLevelPct / 100f).coerceIn(0f, 1f) },
                            color = NeonOrange,
                            trackColor = DarkNavy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }

                    // Lap & Score Info
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface.copy(alpha = 0.9f))
                            .border(1.dp, GoldYellow.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("LAP", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("${telemetry.totalLaps + 1}", color = GoldYellow, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text("${telemetry.score} PTS", color = TextPrimary, fontSize = 10.sp)
                    }
                }

                // AI Sensor Status pill
                if (admin.showDebugHud) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurface.copy(alpha = 0.85f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (telemetry.closestObstacleDist < 120f) DangerRed else NeonGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "AI LiDAR: ${telemetry.aiDecision}",
                            color = TextPrimary,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            // 3. Bottom Controls (Steering, Gas, Brake, Nitro)
            if (uiState.selectedGameMode != GameMode.AI_SIMULATION) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Left Side: Steer Left & Right Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Left Arrow
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSteeringLeft) NeonCyan else DarkSurface.copy(alpha = 0.88f)
                            ),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(64.dp)
                                .border(2.dp, NeonCyan, CircleShape)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isSteeringLeft = true
                                            tryAwaitRelease()
                                            isSteeringLeft = false
                                        }
                                    )
                                }
                                .testTag("steer_left_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBackIosNew,
                                contentDescription = "Steer Left",
                                tint = if (isSteeringLeft) Color.Black else NeonCyan
                            )
                        }

                        // Right Arrow
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSteeringRight) NeonCyan else DarkSurface.copy(alpha = 0.88f)
                            ),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(64.dp)
                                .border(2.dp, NeonCyan, CircleShape)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isSteeringRight = true
                                            tryAwaitRelease()
                                            isSteeringRight = false
                                        }
                                    )
                                }
                                .testTag("steer_right_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForwardIos,
                                contentDescription = "Steer Right",
                                tint = if (isSteeringRight) Color.Black else NeonCyan
                            )
                        }
                    }

                    // Right Side: Brake, Nitro & Gas Pedals
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Brake / Reverse
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBraking) DangerRed else DarkSurface.copy(alpha = 0.88f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .size(56.dp)
                                .border(2.dp, DangerRed, RoundedCornerShape(12.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isBraking = true
                                            tryAwaitRelease()
                                            isBraking = false
                                        }
                                    )
                                }
                                .testTag("brake_pedal_button")
                        ) {
                            Text(
                                "STOP",
                                color = if (isBraking) Color.White else DangerRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }

                        // Nitro Turbo Boost
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isNitroPressed) NeonOrange else DarkSurface.copy(alpha = 0.88f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .size(56.dp)
                                .border(2.dp, NeonOrange, RoundedCornerShape(12.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isNitroPressed = true
                                            tryAwaitRelease()
                                            isNitroPressed = false
                                        }
                                    )
                                }
                                .testTag("nitro_boost_button")
                        ) {
                            Text(
                                "NOS",
                                color = if (isNitroPressed) Color.Black else NeonOrange,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }

                        // Accelerator (Gas) Pedal
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isThrottling) NeonGreen else DarkSurface.copy(alpha = 0.88f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .width(68.dp)
                                .height(82.dp)
                                .border(2.dp, NeonGreen, RoundedCornerShape(14.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isThrottling = true
                                            tryAwaitRelease()
                                            isThrottling = false
                                        }
                                    )
                                }
                                .testTag("gas_pedal_button")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardDoubleArrowUp,
                                    contentDescription = null,
                                    tint = if (isThrottling) Color.Black else NeonGreen
                                )
                                Text(
                                    "GAS",
                                    color = if (isThrottling) Color.Black else NeonGreen,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // Autonomous mode indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface.copy(alpha = 0.95f))
                        .border(1.dp, NeonCyan, RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoMode,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "AUTONOMOUS AI RACING ENGINE ENGAGED",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // 4. Game Over Modal Overlay
            if (uiState.isGameOver) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .border(2.dp, DangerRed, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = DangerRed,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "VEHICLE CRITICAL DAMAGE",
                                color = DangerRed,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                            Text(
                                "Your vehicle sustained fatal impact damage.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.startNewGameSession() },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("RETRY RACE", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    viewModel.adminRepairCar()
                                    viewModel.updateAdminSettings(admin.copy(godMode = true))
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("REVIVE WITH GOD MODE (ADMIN)", color = GoldYellow, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
