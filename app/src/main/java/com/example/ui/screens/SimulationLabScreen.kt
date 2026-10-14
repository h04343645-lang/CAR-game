package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.ObstacleType
import com.example.ui.components.GameCanvas
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationLabScreen(
    viewModel: GameViewModel,
    onBackToGame: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val telemetry by viewModel.telemetryFlow.collectAsState()
    var selectedSpawnType by remember { mutableStateOf(ObstacleType.BARRIER) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "AI RAYCAST SENSOR LAB",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            "Interactive Sandbox & Hazard Simulator",
                            color = NeonCyan,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToGame,
                        modifier = Modifier.testTag("lab_back_button")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.clearAllObstacles() },
                        modifier = Modifier.testTag("lab_clear_obstacles")
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear", tint = DangerRed)
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
            // Interactive Canvas: Tap anywhere to spawn selected obstacle!
            GameCanvas(
                playerVehicle = viewModel.playerVehicle,
                opponentVehicles = viewModel.opponentVehicles,
                obstacles = viewModel.obstacles,
                waypoints = viewModel.waypoints,
                particles = viewModel.physicsEngine.particles,
                adminSettings = uiState.adminSettings,
                trackMinX = viewModel.trackMinX,
                trackMaxX = viewModel.trackMaxX,
                onTapWorld = { wx, wy ->
                    viewModel.spawnObstacleAt(wx, wy, selectedSpawnType)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Top Telemetry Card Overlay
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.92f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .align(Alignment.TopCenter)
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "AI Decision: ${telemetry.aiDecision}",
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            "LiDAR Rays: ${uiState.adminSettings.sensorRayCount} | Nearest Hazard: ${if (telemetry.closestObstacleDist < 900) "${telemetry.closestObstacleDist.toInt()}m" else "Clear"}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        "${telemetry.speedKmh.toInt()} km/h",
                        color = NeonOrange,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }
            }

            // Bottom Spawner Control Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(DarkSurface.copy(alpha = 0.94f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "TAP CANVAS TO SPAWN OBSTACLE & TEST AI REACTION:",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val spawnOptions = listOf(
                        Pair(ObstacleType.BARRIER, "Barrier"),
                        Pair(ObstacleType.ROCK, "Rock"),
                        Pair(ObstacleType.OIL_SLICK, "Oil Slick"),
                        Pair(ObstacleType.TRAFFIC_CAR, "Traffic")
                    )

                    spawnOptions.forEach { (type, label) ->
                        val isSelected = selectedSpawnType == type
                        Button(
                            onClick = { selectedSpawnType = type },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) NeonCyan else DarkNavy
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .border(
                                    1.dp,
                                    if (isSelected) NeonCyan else Color(0xFF2A3655),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                label,
                                color = if (isSelected) Color.Black else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
