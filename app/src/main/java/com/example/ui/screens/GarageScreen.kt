package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.data.CarModel
import com.example.data.CarUpgrade
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageScreen(
    viewModel: GameViewModel,
    onBackToGame: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val cars = uiState.availableCars
    var selectedCar by remember(uiState.selectedCarId) {
        mutableStateOf(cars.find { it.id == uiState.selectedCarId } ?: cars.first())
    }
    val upgrades = uiState.upgradesMap[selectedCar.id] ?: CarUpgrade()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "CYBER GARAGE",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkNavy)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = GoldYellow,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "${uiState.credits}",
                                color = GoldYellow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToGame,
                        modifier = Modifier.testTag("garage_back_button")
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

            // Car Selector Carousel
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(cars) { car ->
                    val isCurrent = car.id == selectedCar.id
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) DarkSurfaceVariant else DarkSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .width(140.dp)
                            .border(
                                width = if (isCurrent) 2.dp else 1.dp,
                                color = if (isCurrent) NeonCyan else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedCar = car
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(car.primaryColorHex)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                car.name,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                            Text(
                                if (car.isUnlocked) "UNLOCKED" else "${car.price} C",
                                color = if (car.isUnlocked) NeonGreen else GoldYellow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Selected Car Showcase Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(selectedCar.primaryColorHex), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        selectedCar.name,
                        color = Color(selectedCar.primaryColorHex),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                    Text(
                        selectedCar.description,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    // Big Vehicle Visualization Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkNavy)
                            .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = Color(selectedCar.primaryColorHex),
                                modifier = Modifier.size(70.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("LiDAR Sensors: ACTIVE", color = NeonCyan, fontSize = 11.sp)
                                Text("Neon Underglow: ARMED", color = NeonOrange, fontSize = 11.sp)
                                Text("Autonomous Mode: READY", color = NeonGreen, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Performance Stats Bars
                    StatBar(
                        label = "TOP SPEED",
                        value = "${(selectedCar.baseTopSpeed + upgrades.engineLevel * 15).toInt()} km/h",
                        progress = (selectedCar.baseTopSpeed / 300f).coerceIn(0f, 1f),
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StatBar(
                        label = "ACCELERATION",
                        value = "${String.format("%.1f", selectedCar.baseAcceleration + upgrades.engineLevel * 0.1f)} G",
                        progress = (selectedCar.baseAcceleration / 2.0f).coerceIn(0f, 1f),
                        color = NeonOrange
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StatBar(
                        label = "HANDLING / GRIP",
                        value = "Lv. ${upgrades.handlingLevel}",
                        progress = (selectedCar.baseHandling / 2.0f).coerceIn(0f, 1f),
                        color = NeonGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StatBar(
                        label = "LIDAR SENSOR RANGE",
                        value = "${(selectedCar.baseSensorRange + upgrades.sensorLevel * 20).toInt()}m",
                        progress = (selectedCar.baseSensorRange / 350f).coerceIn(0f, 1f),
                        color = NeonPurple
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Select / Purchase Button
                    if (selectedCar.isUnlocked) {
                        val isSelectedNow = uiState.selectedCarId == selectedCar.id
                        Button(
                            onClick = {
                                viewModel.selectCar(selectedCar.id)
                                onBackToGame()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelectedNow) NeonGreen else NeonCyan
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("select_car_button")
                        ) {
                            Text(
                                if (isSelectedNow) "CURRENTLY EQUIPPED" else "EQUIP THIS VEHICLE",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = { viewModel.buyCar(selectedCar.id) },
                            enabled = uiState.credits >= selectedCar.price,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldYellow),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("buy_car_button")
                        ) {
                            Text(
                                "UNLOCK FOR ${selectedCar.price} CREDITS",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Upgrades Section
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "TUNING & WORKSHOP UPGRADES",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        "Upgrade cost: 2,000 Credits per level",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    UpgradeItemRow(
                        title = "Twin-Turbo Quantum Engine",
                        level = upgrades.engineLevel,
                        canUpgrade = uiState.credits >= 2000 && upgrades.engineLevel < 5,
                        onUpgrade = { viewModel.upgradeCar(selectedCar.id, "engine") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    UpgradeItemRow(
                        title = "Aero Downforce & Sport Tires",
                        level = upgrades.handlingLevel,
                        canUpgrade = uiState.credits >= 2000 && upgrades.handlingLevel < 5,
                        onUpgrade = { viewModel.upgradeCar(selectedCar.id, "handling") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    UpgradeItemRow(
                        title = "High-Density LiDAR Sensor Array",
                        level = upgrades.sensorLevel,
                        canUpgrade = uiState.credits >= 2000 && upgrades.sensorLevel < 5,
                        onUpgrade = { viewModel.upgradeCar(selectedCar.id, "sensor") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    UpgradeItemRow(
                        title = "Nitro Nitrous Tank Capacity",
                        level = upgrades.nitroLevel,
                        canUpgrade = uiState.credits >= 2000 && upgrades.nitroLevel < 5,
                        onUpgrade = { viewModel.upgradeCar(selectedCar.id, "nitro") }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBar(label: String, value: String, progress: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = TextSecondary, fontSize = 11.sp)
            Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            color = color,
            trackColor = DarkNavy,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

@Composable
private fun UpgradeItemRow(
    title: String,
    level: Int,
    canUpgrade: Boolean,
    onUpgrade: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkNavy)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text("Level $level / 5", color = NeonCyan, fontSize = 11.sp)
        }
        Button(
            onClick = onUpgrade,
            enabled = canUpgrade,
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(if (level >= 5) "MAXED" else "Upgrade", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
