package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.engine.GamePhysicsEngine
import com.example.engine.TrackManager
import com.example.engine.VehicleState
import com.example.sound.SoundEffectsManager
import kotlin.math.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppScreen {
    GAME,
    GARAGE,
    ADMIN_PANEL,
    AI_LAB
}

data class GameUiState(
    val currentScreen: AppScreen = AppScreen.GAME,
    val selectedGameMode: GameMode = GameMode.PLAYER_DRIVE,
    val selectedTrack: TrackType = TrackType.CYBER_CIRCUIT,
    val availableCars: List<CarModel> = emptyList(),
    val selectedCarId: String = "apex_cyber",
    val upgradesMap: Map<String, CarUpgrade> = emptyMap(),
    val credits: Int = 12500,
    val highScore: Int = 0,
    val adminSettings: AdminSettings = AdminSettings(),
    val isPaused: Boolean = false,
    val isGameOver: Boolean = false,
    val lapRecordSec: Float = 0f,
    val currentLapTimeSec: Float = 0f,
    val totalCoinsCollected: Int = 0,
    val adminPinUnlocked: Boolean = true // Direct access enabled for user
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("apex_ai_car_prefs", Context.MODE_PRIVATE)
    val soundManager = SoundEffectsManager(application)
    val physicsEngine = GamePhysicsEngine()

    private val defaultCars = listOf(
        CarModel(
            id = "apex_cyber",
            name = "Apex Cyber 01",
            description = "Balanced autonomous prototype with precision multi-beam LiDAR sensors.",
            baseTopSpeed = 160f,
            baseAcceleration = 1.0f,
            baseHandling = 1.0f,
            baseSensorRange = 180f,
            price = 0,
            isUnlocked = true,
            primaryColorHex = 0xFF00F0FF,
            accentColorHex = 0xFFFF5722,
            glowColorHex = 0xFF00F0FF
        ),
        CarModel(
            id = "shadow_gt",
            name = "Shadow GT",
            description = "High-torque drift weapon engineered for street circuit supremacy.",
            baseTopSpeed = 180f,
            baseAcceleration = 1.25f,
            baseHandling = 1.15f,
            baseSensorRange = 200f,
            price = 15000,
            isUnlocked = false,
            primaryColorHex = 0xFFFF5722,
            accentColorHex = 0xFFFFD600,
            glowColorHex = 0xFFFF5722
        ),
        CarModel(
            id = "hyperion_ev",
            name = "Hyperion EV",
            description = "Next-gen hypercar with lightning top-speed and quantum neural auto-pilot.",
            baseTopSpeed = 220f,
            baseAcceleration = 1.45f,
            baseHandling = 1.2f,
            baseSensorRange = 240f,
            price = 35000,
            isUnlocked = false,
            primaryColorHex = 0xFF00E676,
            accentColorHex = 0xFF00F0FF,
            glowColorHex = 0xFF00E676
        ),
        CarModel(
            id = "titan_enforcer",
            name = "Titan Enforcer",
            description = "Armored autonomous interceptor with heavy ramming resistance.",
            baseTopSpeed = 150f,
            baseAcceleration = 1.1f,
            baseHandling = 0.9f,
            baseSensorRange = 190f,
            price = 22000,
            isUnlocked = false,
            primaryColorHex = 0xFFB388FF,
            accentColorHex = 0xFFFF3366,
            glowColorHex = 0xFFB388FF
        ),
        CarModel(
            id = "formula_ai",
            name = "Formula AI-X",
            description = "Pure open-wheel racing machine with extreme aerodynamic downforce.",
            baseTopSpeed = 240f,
            baseAcceleration = 1.6f,
            baseHandling = 1.4f,
            baseSensorRange = 260f,
            price = 60000,
            isUnlocked = false,
            primaryColorHex = 0xFFFF3366,
            accentColorHex = 0xFFFFD600,
            glowColorHex = 0xFFFF3366
        )
    )

    private val _uiState = MutableStateFlow(
        GameUiState(
            availableCars = defaultCars,
            credits = prefs.getInt("credits", 15000),
            highScore = prefs.getInt("high_score", 0)
        )
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Game runtime entities
    var playerVehicle: VehicleState? = null
        private set

    val opponentVehicles = mutableListOf<VehicleState>()
    val obstacles = mutableListOf<ObstacleEntity>()
    var waypoints = listOf<Waypoint>()
        private set

    var trackMinX: Float = -260f
    var trackMaxX: Float = 260f

    private var gameLoopJob: Job? = null
    private var lastFrameTime = System.nanoTime()

    // Telemetry flow for UI
    val telemetryFlow = MutableStateFlow(AiTelemetry())

    init {
        loadSavedCars()
        startNewGameSession()
    }

    private fun loadSavedCars() {
        val unlockedSet = prefs.getStringSet("unlocked_cars", setOf("apex_cyber")) ?: setOf("apex_cyber")
        _uiState.update { state ->
            val updated = state.availableCars.map { car ->
                car.copy(isUnlocked = unlockedSet.contains(car.id))
            }
            state.copy(availableCars = updated)
        }
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun setGameMode(mode: GameMode) {
        _uiState.update { it.copy(selectedGameMode = mode) }
        startNewGameSession()
    }

    fun setTrack(track: TrackType) {
        _uiState.update { it.copy(selectedTrack = track) }
        startNewGameSession()
    }

    fun selectCar(carId: String) {
        _uiState.update { it.copy(selectedCarId = carId) }
        startNewGameSession()
    }

    fun togglePause() {
        _uiState.update { it.copy(isPaused = !it.isPaused) }
    }

    fun startNewGameSession() {
        gameLoopJob?.cancel()

        val state = _uiState.value
        val car = state.availableCars.find { it.id == state.selectedCarId } ?: state.availableCars.first()
        val track = state.selectedTrack

        waypoints = TrackManager.createCircuitWaypoints(track)
        trackMinX = -(track.width * 0.5f)
        trackMaxX = track.width * 0.5f

        obstacles.clear()
        obstacles.addAll(
            TrackManager.generateInitialObstacles(track, state.adminSettings.obstacleDensity)
        )

        // Initialize player car
        val isPlayerDriven = (state.selectedGameMode != GameMode.AI_SIMULATION)
        playerVehicle = VehicleState(
            id = "player_1",
            isPlayer = isPlayerDriven,
            x = 0f,
            y = 120f,
            speed = 0f,
            headingAngleDeg = 0f,
            steerAngleDeg = 0f,
            throttle = if (isPlayerDriven) 0f else 1.0f,
            model = car
        )

        // Initialize opponent AI vehicles if battle or multi-car mode
        opponentVehicles.clear()
        if (state.selectedGameMode == GameMode.AI_RACE_BATTLE || state.selectedGameMode == GameMode.AI_SIMULATION) {
            val oppCars = state.availableCars.filter { it.id != car.id }.take(3)
            val offsets = listOf(-80f, 80f, -40f)
            oppCars.forEachIndexed { idx, oppCar ->
                opponentVehicles.add(
                    VehicleState(
                        id = "ai_bot_${idx + 1}",
                        isPlayer = false,
                        x = offsets.getOrElse(idx) { 0f },
                        y = 180f + idx * 80f,
                        speed = 10f,
                        headingAngleDeg = 0f,
                        steerAngleDeg = 0f,
                        throttle = 1.0f,
                        model = oppCar
                    )
                )
            }
        }

        _uiState.update { it.copy(isGameOver = false, isPaused = false, currentLapTimeSec = 0f) }

        startGameLoop()
    }

    private fun startGameLoop() {
        gameLoopJob = viewModelScope.launch {
            lastFrameTime = System.nanoTime()
            var frameCount = 0
            var fpsTimer = 0f
            var calculatedFps = 60

            while (isActive) {
                val now = System.nanoTime()
                val deltaNano = now - lastFrameTime
                lastFrameTime = now

                val dt = (deltaNano / 1_000_000_000f).coerceIn(0.001f, 0.05f) * _uiState.value.adminSettings.simulationSpeed

                fpsTimer += dt
                frameCount++
                if (fpsTimer >= 1.0f) {
                    calculatedFps = frameCount
                    frameCount = 0
                    fpsTimer = 0f
                }

                if (!_uiState.value.isPaused && !_uiState.value.isGameOver) {
                    updateSimulation(dt, calculatedFps)
                }

                delay(16) // Target ~60 FPS
            }
        }
    }

    private fun updateSimulation(dt: Float, currentFps: Int) {
        val player = playerVehicle ?: return
        val admin = _uiState.value.adminSettings

        // Update player car
        physicsEngine.updateVehicle(
            vehicle = player,
            dt = dt,
            adminSettings = admin,
            waypoints = waypoints,
            obstacles = obstacles,
            trackMinX = trackMinX,
            trackMaxX = trackMaxX,
            onCollision = { type ->
                soundManager.playCollisionImpact()
                if (player.health <= 0f && !admin.godMode) {
                    _uiState.update { it.copy(isGameOver = true) }
                }
            },
            onCollectCoin = {
                soundManager.playCoinPickup()
                val newCoins = _uiState.value.credits + 100
                _uiState.update { it.copy(credits = newCoins, totalCoinsCollected = it.totalCoinsCollected + 1) }
                prefs.edit().putInt("credits", newCoins).apply()
            },
            onCollectNitro = {
                soundManager.playNitroEngage()
            }
        )

        // Update opponent AI cars
        for (aiCar in opponentVehicles) {
            physicsEngine.updateVehicle(
                vehicle = aiCar,
                dt = dt,
                adminSettings = admin,
                waypoints = waypoints,
                obstacles = obstacles,
                trackMinX = trackMinX,
                trackMaxX = trackMaxX,
                onCollision = {},
                onCollectCoin = {},
                onCollectNitro = {}
            )
        }

        // Update obstacles & particles
        physicsEngine.updateObstacles(obstacles, dt)
        physicsEngine.updateParticles(dt)

        // Lap time
        _uiState.update { it.copy(currentLapTimeSec = it.currentLapTimeSec + dt) }

        // Closest obstacle distance calculation for telemetry
        var minObsDist = 999f
        for (r in player.activeRays) {
            if (r.isHit && r.hitDistance < minObsDist) {
                minObsDist = r.hitDistance
            }
        }

        // Publish Telemetry
        telemetryFlow.value = AiTelemetry(
            speedKmh = abs(player.speed * 0.9f),
            steeringAngleDeg = player.steerAngleDeg,
            throttlePct = player.throttle * 100f,
            brakePct = player.brake * 100f,
            closestObstacleDist = minObsDist,
            aiDecision = player.aiDecision,
            currentWaypointIndex = player.currentWaypointIdx,
            totalLaps = player.lapsCompleted,
            nitroLevelPct = player.nitroFuel,
            healthPct = player.health,
            score = _uiState.value.totalCoinsCollected * 100 + (player.lapsCompleted * 1000),
            fps = currentFps
        )
    }

    // Touch controls for Player Drive
    fun setPlayerSteer(steer: Float) {
        playerVehicle?.steerAngleDeg = steer.coerceIn(-40f, 40f)
    }

    fun setPlayerThrottle(throttle: Float) {
        playerVehicle?.throttle = throttle.coerceIn(0f, 1f)
        if (throttle > 0f) {
            soundManager.playEnginePulse()
        }
    }

    fun setPlayerBrake(brake: Float) {
        playerVehicle?.brake = brake.coerceIn(0f, 1f)
    }

    fun setPlayerNitro(active: Boolean) {
        playerVehicle?.nitroActive = active
        if (active) soundManager.playNitroEngage()
    }

    // Interactive spawner in AI Lab / Sandbox
    fun spawnObstacleAt(worldX: Float, worldY: Float, type: ObstacleType) {
        obstacles.add(
            ObstacleEntity(
                id = System.currentTimeMillis(),
                x = worldX,
                y = worldY,
                radius = type.radius,
                type = type
            )
        )
        soundManager.playHazardWarning()
    }

    fun clearAllObstacles() {
        obstacles.clear()
    }

    // Garage actions
    fun buyCar(carId: String): Boolean {
        val car = _uiState.value.availableCars.find { it.id == carId } ?: return false
        if (_uiState.value.credits >= car.price && !car.isUnlocked) {
            val newCredits = _uiState.value.credits - car.price
            val updatedCars = _uiState.value.availableCars.map {
                if (it.id == carId) it.copy(isUnlocked = true) else it
            }
            val unlockedSet = updatedCars.filter { it.isUnlocked }.map { it.id }.toSet()
            prefs.edit().putStringSet("unlocked_cars", unlockedSet).putInt("credits", newCredits).apply()

            _uiState.update {
                it.copy(credits = newCredits, availableCars = updatedCars, selectedCarId = carId)
            }
            soundManager.playCoinPickup()
            return true
        }
        return false
    }

    fun upgradeCar(carId: String, statName: String): Boolean {
        val cost = 2000
        if (_uiState.value.credits >= cost) {
            val current = _uiState.value.upgradesMap[carId] ?: CarUpgrade()
            val upgraded = when (statName) {
                "engine" -> current.copy(engineLevel = current.engineLevel + 1)
                "handling" -> current.copy(handlingLevel = current.handlingLevel + 1)
                "sensor" -> current.copy(sensorLevel = current.sensorLevel + 1)
                "nitro" -> current.copy(nitroLevel = current.nitroLevel + 1)
                else -> current
            }
            val newCredits = _uiState.value.credits - cost
            prefs.edit().putInt("credits", newCredits).apply()
            _uiState.update {
                val newMap = it.upgradesMap.toMutableMap()
                newMap[carId] = upgraded
                it.copy(credits = newCredits, upgradesMap = newMap)
            }
            soundManager.playCoinPickup()
            return true
        }
        return false
    }

    // Admin Panel Actions
    fun updateAdminSettings(settings: AdminSettings) {
        _uiState.update { it.copy(adminSettings = settings) }
    }

    fun adminAddCredits(amount: Int) {
        val newCredits = _uiState.value.credits + amount
        prefs.edit().putInt("credits", newCredits).apply()
        _uiState.update { it.copy(credits = newCredits) }
        soundManager.playCoinPickup()
    }

    fun adminUnlockAllCars() {
        val updated = _uiState.value.availableCars.map { it.copy(isUnlocked = true) }
        val allIds = updated.map { it.id }.toSet()
        prefs.edit().putStringSet("unlocked_cars", allIds).apply()
        _uiState.update { it.copy(availableCars = updated) }
        soundManager.playNitroEngage()
    }

    fun adminMaxAllUpgrades() {
        val maxMap = mutableMapOf<String, CarUpgrade>()
        _uiState.value.availableCars.forEach { car ->
            maxMap[car.id] = CarUpgrade(engineLevel = 5, handlingLevel = 5, sensorLevel = 5, nitroLevel = 5)
        }
        _uiState.update { it.copy(upgradesMap = maxMap) }
        soundManager.playNitroEngage()
    }

    fun adminRepairCar() {
        playerVehicle?.health = 100f
        playerVehicle?.isDestroyed = false
        _uiState.update { it.copy(isGameOver = false) }
        soundManager.playCoinPickup()
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
        soundManager.release()
    }
}
