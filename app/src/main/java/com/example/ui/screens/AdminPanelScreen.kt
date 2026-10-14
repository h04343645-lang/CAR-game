package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdminSettings
import com.example.data.TrackType
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    viewModel: GameViewModel,
    onBackToGame: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val telemetry by viewModel.telemetryFlow.collectAsState()
    val admin = uiState.adminSettings
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = NeonOrange,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "ADMIN & CHEATS PANEL",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                "Master Vehicle & AI Engine Controller",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToGame,
                        modifier = Modifier.testTag("admin_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = NeonCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        containerColor = DarkNavy
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. AD-FREE STATUS BANNER (Fulfills user request: "remove ads")
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonGreen, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            "AD ENGINE STATUS: REMOVED & CLEAN",
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            "All advertisements, tracking, and interstitial banners are permanently disabled. 100% ad-free premium experience active.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // 2. LIVE DIAGNOSTIC TELEMETRY HUD
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "LIVE TELEMETRY MONITOR",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            "${telemetry.fps} FPS",
                            color = NeonGreen,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryBadge("SPEED", "${telemetry.speedKmh.toInt()} km/h", NeonOrange)
                        TelemetryBadge("STEER", "${telemetry.steeringAngleDeg.toInt()}°", NeonCyan)
                        TelemetryBadge("HEALTH", "${telemetry.healthPct.toInt()}%", if (telemetry.healthPct > 30) NeonGreen else DangerRed)
                        TelemetryBadge("LAPS", "${telemetry.totalLaps}", GoldYellow)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "AI Branch: ${telemetry.aiDecision}",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 3. INSTANT CHEATS & ECONOMY
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "INSTANT CHEATS & ECONOMY",
                        color = GoldYellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.adminAddCredits(50000) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, GoldYellow, RoundedCornerShape(8.dp))
                                .testTag("cheat_add_credits")
                        ) {
                            Text("+50K Coins", color = GoldYellow, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { viewModel.adminUnlockAllCars() },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                                .testTag("cheat_unlock_cars")
                        ) {
                            Text("Unlock All", color = NeonCyan, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.adminMaxAllUpgrades() },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, NeonPurple, RoundedCornerShape(8.dp))
                                .testTag("cheat_max_upgrades")
                        ) {
                            Text("Max Upgrades", color = NeonPurple, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { viewModel.adminRepairCar() },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkNavy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, NeonGreen, RoundedCornerShape(8.dp))
                                .testTag("cheat_repair_car")
                        ) {
                            Text("Revive / Heal", color = NeonGreen, fontSize = 12.sp)
                        }
                    }
                }
            }

            // 4. GOD MODE & MASTER TOGGLES
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "MASTER OVERRIDES",
                        color = DangerRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    AdminToggleRow(
                        title = "God Mode (Invincible)",
                        subtitle = "Zero collision damage and ramming power",
                        checked = admin.godMode,
                        onCheckedChange = { viewModel.updateAdminSettings(admin.copy(godMode = it)) }
                    )

                    HorizontalDivider(color = Color(0xFF2A3655), modifier = Modifier.padding(vertical = 8.dp))

                    AdminToggleRow(
                        title = "Infinite Nitro Boost",
                        subtitle = "Never run out of turbo nitrous fuel",
                        checked = admin.infiniteNitro,
                        onCheckedChange = { viewModel.updateAdminSettings(admin.copy(infiniteNitro = it)) }
                    )

                    HorizontalDivider(color = Color(0xFF2A3655), modifier = Modifier.padding(vertical = 8.dp))

                    AdminToggleRow(
                        title = "No Track Wall Collisions",
                        subtitle = "Pass freely along track boundaries without slowdown",
                        checked = admin.noWallCollisions,
                        onCheckedChange = { viewModel.updateAdminSettings(admin.copy(noWallCollisions = it)) }
                    )

                    HorizontalDivider(color = Color(0xFF2A3655), modifier = Modifier.padding(vertical = 8.dp))

                    AdminToggleRow(
                        title = "Player Auto-Avoid Copilot",
                        subtitle = "AI raycast assist gently corrects steering near hazards",
                        checked = admin.autoAvoidAssistInPlayerMode,
                        onCheckedChange = { viewModel.updateAdminSettings(admin.copy(autoAvoidAssistInPlayerMode = it)) }
                    )
                }
            }

            // 5. AI RAYCAST & LIDAR TUNING (Unity CarEngine parameters)
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "AI RAYCAST / LIDAR SENSOR TUNING",
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Ray Count selector (3, 5, 7)
                    Text("Sensor Ray Count: ${admin.sensorRayCount} Rays", color = TextPrimary, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(3, 5, 7).forEach { count ->
                            val isSelected = admin.sensorRayCount == count
                            OutlinedButton(
                                onClick = { viewModel.updateAdminSettings(admin.copy(sensorRayCount = count)) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) NeonCyan.copy(alpha = 0.2f) else DarkNavy
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) NeonCyan else Color.Gray)
                                )
                            ) {
                                Text("$count Rays", color = if (isSelected) NeonCyan else TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    AdminSliderRow(
                        title = "Sensor Detection Range",
                        valueText = "${(admin.sensorRangeMultiplier * 100).toInt()}% (${(180 * admin.sensorRangeMultiplier).toInt()}m)",
                        value = admin.sensorRangeMultiplier,
                        range = 0.6f..2.5f,
                        onValueChange = { viewModel.updateAdminSettings(admin.copy(sensorRangeMultiplier = it)) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AdminSliderRow(
                        title = "Sensor Angle Spread",
                        valueText = "${admin.sensorSpreadAngle.toInt()}°",
                        value = admin.sensorSpreadAngle,
                        range = 15f..60f,
                        onValueChange = { viewModel.updateAdminSettings(admin.copy(sensorSpreadAngle = it)) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AdminSliderRow(
                        title = "AI Steer Avoidance Force",
                        valueText = "${String.format("%.1f", admin.steerAvoidanceSensitivity)}x",
                        value = admin.steerAvoidanceSensitivity,
                        range = 0.5f..3.0f,
                        onValueChange = { viewModel.updateAdminSettings(admin.copy(steerAvoidanceSensitivity = it)) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    AdminToggleRow(
                        title = "Show Laser Beams on Screen",
                        subtitle = "Visualize real-time LiDAR raycasts and hit contacts",
                        checked = admin.showSensorLasers,
                        onCheckedChange = { viewModel.updateAdminSettings(admin.copy(showSensorLasers = it)) }
                    )
                }
            }

            // 6. VEHICLE PHYSICS & CLOCK SPEED
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "PHYSICS & SIMULATION CLOCK",
                        color = NeonOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    AdminSliderRow(
                        title = "Top Speed Multiplier",
                        valueText = "${String.format("%.1f", admin.speedMultiplier)}x",
                        value = admin.speedMultiplier,
                        range = 1.0f..4.0f,
                        onValueChange = { viewModel.updateAdminSettings(admin.copy(speedMultiplier = it)) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AdminSliderRow(
                        title = "Acceleration Multiplier",
                        valueText = "${String.format("%.1f", admin.accelMultiplier)}x",
                        value = admin.accelMultiplier,
                        range = 1.0f..3.0f,
                        onValueChange = { viewModel.updateAdminSettings(admin.copy(accelMultiplier = it)) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AdminSliderRow(
                        title = "Simulation Game Speed",
                        valueText = "${String.format("%.1f", admin.simulationSpeed)}x",
                        value = admin.simulationSpeed,
                        range = 0.5f..2.0f,
                        onValueChange = { viewModel.updateAdminSettings(admin.copy(simulationSpeed = it)) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AdminSliderRow(
                        title = "Obstacle Density",
                        valueText = "${String.format("%.1f", admin.obstacleDensity)}x",
                        value = admin.obstacleDensity,
                        range = 0.2f..2.5f,
                        onValueChange = { viewModel.updateAdminSettings(admin.copy(obstacleDensity = it)) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.clearAllObstacles() },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Purge All Obstacles on Track", color = Color.White)
                    }
                }
            }

            // Return to game button
            Button(
                onClick = onBackToGame,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("apply_and_return_button")
            ) {
                Text("APPLY SETTINGS & RESUME RACE", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TelemetryBadge(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkNavy)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, color = TextSecondary, fontSize = 10.sp)
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun AdminToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NeonCyan,
                checkedTrackColor = DarkSurfaceVariant,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = DarkNavy
            )
        )
    }
}

@Composable
private fun AdminSliderRow(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = TextPrimary, fontSize = 13.sp)
            Text(valueText, color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = NeonCyan,
                activeTrackColor = NeonCyan,
                inactiveTrackColor = Color(0xFF2A3655)
            )
        )
    }
}
