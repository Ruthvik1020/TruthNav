package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BenchmarkRecordEntity
import com.example.data.NavDatabase
import com.example.data.TripRecordEntity
import com.example.engine.GnssInsFusionEngine
import com.example.engine.IoVnbdBenchmarkEngine
import com.example.map.MapLayerType
import com.example.map.TerrainTileEngine
import com.example.model.*
import com.example.sensor.LiveSensorManager
import com.example.voice.VoiceAssistanceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class DataSourceMode {
    LIVE_HARDWARE_SENSORS,
    IO_VNBD_BENCHMARK_SIMULATOR
}

data class MapDisplayOptions(
    val showGroundTruth: Boolean = true,
    val showRawDr: Boolean = true, // Trajectory A (Physics Pure IMU)
    val showAiFusion: Boolean = true, // Trajectory B (AI Corrected)
    val showMapMatched: Boolean = true, // Trajectory C (Map Constrained)
    val showVisualIns: Boolean = true, // Trajectory D (Visual Corrected)
    val showRoadNetwork: Boolean = false,
    val followVehicle: Boolean = true,
    val mapLayerType: MapLayerType = MapLayerType.TERRAIN_TOPO
)

class NavViewModel(application: Application) : AndroidViewModel(application) {

    private val db = NavDatabase.getDatabase(application)
    private val navDao = db.navDao()

    val liveSensorManager = LiveSensorManager(application)
    val fusionEngine = GnssInsFusionEngine()
    val benchmarkEngine = IoVnbdBenchmarkEngine()
    val terrainTileEngine = TerrainTileEngine(application)
    val voiceAssistant = VoiceAssistanceManager(application)

    private val _tileRepaintTrigger = MutableStateFlow(0L)
    val tileRepaintTrigger: StateFlow<Long> = _tileRepaintTrigger.asStateFlow()

    private var previousFusionMode: FusionMode = FusionMode.GNSS_AIDED

    init {
        terrainTileEngine.setOnTileLoadedListener {
            _tileRepaintTrigger.value = System.currentTimeMillis()
        }
    }

    // UI States
    private val _dataSourceMode = MutableStateFlow(DataSourceMode.IO_VNBD_BENCHMARK_SIMULATOR)
    val dataSourceMode: StateFlow<DataSourceMode> = _dataSourceMode.asStateFlow()

    private val _vehicleState = MutableStateFlow(fusionEngine.currentState)
    val vehicleState: StateFlow<VehicleState> = _vehicleState.asStateFlow()

    private val _currentImu = MutableStateFlow(ImuReading())
    val currentImu: StateFlow<ImuReading> = _currentImu.asStateFlow()

    private val _currentGnss = MutableStateFlow<GnssReading?>(null)
    val currentGnss: StateFlow<GnssReading?> = _currentGnss.asStateFlow()

    private val _driftMetrics = MutableStateFlow(DriftMetrics())
    val driftMetrics: StateFlow<DriftMetrics> = _driftMetrics.asStateFlow()

    private val _digitalTwinMetrics = MutableStateFlow(DigitalTwinMetrics())
    val digitalTwinMetrics: StateFlow<DigitalTwinMetrics> = _digitalTwinMetrics.asStateFlow()

    private val _navigationIntegrity = MutableStateFlow(NavigationIntegrityReport())
    val navigationIntegrity: StateFlow<NavigationIntegrityReport> = _navigationIntegrity.asStateFlow()

    private val _gnssSpoofingReport = MutableStateFlow(GnssSpoofingReport())
    val gnssSpoofingReport: StateFlow<GnssSpoofingReport> = _gnssSpoofingReport.asStateFlow()

    private val _roadLayerState = MutableStateFlow(RoadLayerState())
    val roadLayerState: StateFlow<RoadLayerState> = _roadLayerState.asStateFlow()

    private val _undergroundTopologyReport = MutableStateFlow(fusionEngine.currentUndergroundReport)
    val undergroundTopologyReport: StateFlow<UndergroundTopologyReport> = _undergroundTopologyReport.asStateFlow()

    private val _multiHypothesisReport = MutableStateFlow(fusionEngine.currentMultiHypothesisReport)
    val multiHypothesisReport: StateFlow<MultiHypothesisReport> = _multiHypothesisReport.asStateFlow()

    private val _calibration = MutableStateFlow(fusionEngine.calibrator.currentCalibration)
    val calibration: StateFlow<CalibrationAngles> = _calibration.asStateFlow()

    private val _mapOptions = MutableStateFlow(MapDisplayOptions())
    val mapOptions: StateFlow<MapDisplayOptions> = _mapOptions.asStateFlow()

    private val _isManualJammingActive = MutableStateFlow(false)
    val isManualJammingActive: StateFlow<Boolean> = _isManualJammingActive.asStateFlow()

    private val _destination = MutableStateFlow<TargetDestination?>(null)
    val destination: StateFlow<TargetDestination?> = _destination.asStateFlow()

    private val _isRecordingTrip = MutableStateFlow(false)
    val isRecordingTrip: StateFlow<Boolean> = _isRecordingTrip.asStateFlow()

    private var tripStartTimeMs = 0L

    // Benchmark State
    private val _selectedScenarioIndex = MutableStateFlow(0)
    val selectedScenarioIndex: StateFlow<Int> = _selectedScenarioIndex.asStateFlow()

    private val _isBenchmarkPlaying = MutableStateFlow(false)
    val isBenchmarkPlaying: StateFlow<Boolean> = _isBenchmarkPlaying.asStateFlow()

    private val _benchmarkTimeSec = MutableStateFlow(0f)
    val benchmarkTimeSec: StateFlow<Float> = _benchmarkTimeSec.asStateFlow()

    val savedTrips: StateFlow<List<TripRecordEntity>> = navDao.getAllTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedBenchmarks: StateFlow<List<BenchmarkRecordEntity>> = navDao.getAllBenchmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var simulationJob: Job? = null
    private var liveSensorJob: Job? = null

    init {
        // Initialize with default scenario
        loadScenario(0)
        startEngineLoop()
    }

    fun setDataSourceMode(mode: DataSourceMode) {
        _dataSourceMode.value = mode
        if (mode == DataSourceMode.LIVE_HARDWARE_SENSORS) {
            benchmarkEngine.pause()
            _isBenchmarkPlaying.value = false
            liveSensorManager.startListening()
            startLiveSensorsLoop()
        } else {
            liveSensorManager.stopListening()
            liveSensorJob?.cancel()
            startEngineLoop()
        }
    }

    fun loadScenario(index: Int) {
        benchmarkEngine.selectScenario(index)
        _selectedScenarioIndex.value = index
        _benchmarkTimeSec.value = 0f
        val scenario = benchmarkEngine.currentScenario
        val firstPt = scenario.groundTruthPoints.firstOrNull() ?: TrajectoryPoint(12.9716, 77.5946)
        fusionEngine.initPosition(firstPt.lat, firstPt.lng, firstPt.headingDeg, firstPt.altitude)
        fusionEngine.mapMatcher.setRoadNetwork(scenario.roadSegments)
        _vehicleState.value = fusionEngine.currentState
        _driftMetrics.value = DriftMetrics()
        _digitalTwinMetrics.value = fusionEngine.getDigitalTwinMetrics(firstPt.lat, firstPt.lng, scenario.title)
        _navigationIntegrity.value = fusionEngine.getNavigationIntegrityReport()
    }

    fun setSimulatedNavState(state: AiNavState?) {
        fusionEngine.setManualNavState(state)
        _navigationIntegrity.value = fusionEngine.getNavigationIntegrityReport()
        if (state != null) {
            voiceAssistant.speak("${state.stateCode}: ${state.title}. ${state.description}")
        } else {
            voiceAssistant.speak("State machine autonomous navigation restored.")
        }
    }

    fun toggleBenchmarkPlayback() {
        if (benchmarkEngine.isPlaying) {
            benchmarkEngine.pause()
            _isBenchmarkPlaying.value = false
        } else {
            benchmarkEngine.play()
            _isBenchmarkPlaying.value = true
            startEngineLoop()
        }
    }

    fun resetBenchmark() {
        benchmarkEngine.reset()
        _isBenchmarkPlaying.value = false
        _benchmarkTimeSec.value = 0f
        loadScenario(_selectedScenarioIndex.value)
    }

    fun setPlaybackSpeed(speed: Float) {
        benchmarkEngine.setSpeed(speed)
    }

    fun relocateVehicle(lat: Double, lng: Double) {
        fusionEngine.initPosition(lat, lng, fusionEngine.currentState.headingDeg)
        _vehicleState.value = fusionEngine.currentState
        voiceAssistant.speak("Vehicle relocated to selected coordinates.")
    }

    fun setDestination(lat: Double, lng: Double) {
        val dest = TargetDestination(lat, lng)
        _destination.value = dest
        val current = _vehicleState.value
        val distMeters = calculateHaversineDistance(current.lat, current.lng, lat, lng)
        val bearing = calculateBearing(current.lat, current.lng, lat, lng)
        val distStr = if (distMeters >= 1000) "${"%.2f".format(distMeters / 1000.0)} km" else "${distMeters.toInt()} meters"
        voiceAssistant.speak("Destination target set. Distance: $distStr. Bearing: ${bearing.toInt()} degrees.")
    }

    fun clearDestination() {
        _destination.value = null
        voiceAssistant.speak("Destination target cleared.")
    }

    fun syncWithCurrentDeviceLocation(): Boolean {
        setDataSourceMode(DataSourceMode.LIVE_HARDWARE_SENSORS)
        val loc = liveSensorManager.getDeviceLocation()
        if (loc != null) {
            fusionEngine.initPosition(
                lat = loc.latitude,
                lng = loc.longitude,
                headingDeg = if (loc.hasBearing() && loc.bearing != 0f) loc.bearing else fusionEngine.currentState.headingDeg,
                altitude = if (loc.hasAltitude()) loc.altitude else 920.0
            )
            _vehicleState.value = fusionEngine.currentState
            updateMapOptions { it.copy(followVehicle = true) }
            val latStr = "%.5f".format(loc.latitude)
            val lngStr = "%.5f".format(loc.longitude)
            voiceAssistant.speak("Live device GPS acquired. Position synced to $latStr degrees north, $lngStr degrees east.")
            return true
        } else {
            updateMapOptions { it.copy(followVehicle = true) }
            voiceAssistant.speak("Requesting GPS fix. Centering map on current track.")
            return false
        }
    }

    private fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return r * c
    }

    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)
        val y = kotlin.math.sin(deltaLambda) * kotlin.math.cos(phi2)
        val x = kotlin.math.cos(phi1) * kotlin.math.sin(phi2) -
                kotlin.math.sin(phi1) * kotlin.math.cos(phi2) * kotlin.math.cos(deltaLambda)
        val theta = kotlin.math.atan2(y, x)
        return ((Math.toDegrees(theta) + 360.0) % 360.0).toFloat()
    }

    fun toggleManualJamming() {
        val newJam = !_isManualJammingActive.value
        setManualJamming(newJam)
    }

    fun setManualJamming(isJammed: Boolean) {
        _isManualJammingActive.value = isJammed
        fusionEngine.setGnssSignalAvailable(!isJammed)
        if (isJammed) {
            voiceAssistant.announceGnssBlackout()
        } else {
            voiceAssistant.announceGnssRestored()
        }
    }

    fun speakCurrentStatus() {
        val state = _vehicleState.value
        val metrics = _driftMetrics.value
        val isGnss = state.fusionMode == FusionMode.GNSS_AIDED && !_isManualJammingActive.value
        voiceAssistant.speakFullTelemetry(
            speedKmph = state.speedKmph,
            headingDeg = state.headingDeg,
            isGnssActive = isGnss,
            driftPercent = metrics.relativeDriftPercent
        )
    }

    fun setMountPreset(preset: MountPreset) {
        fusionEngine.calibrator.setPreset(preset)
        _calibration.value = fusionEngine.calibrator.currentCalibration
    }

    fun setManualMountAngles(pitch: Float, roll: Float, yaw: Float) {
        fusionEngine.calibrator.setManualAngles(pitch, roll, yaw)
        _calibration.value = fusionEngine.calibrator.currentCalibration
    }

    fun updateMapOptions(transform: (MapDisplayOptions) -> MapDisplayOptions) {
        _mapOptions.value = transform(_mapOptions.value)
    }

    fun toggleTripRecording() {
        if (!_isRecordingTrip.value) {
            _isRecordingTrip.value = true
            tripStartTimeMs = System.currentTimeMillis()
        } else {
            _isRecordingTrip.value = false
            saveCurrentTrip()
        }
    }

    private fun saveCurrentTrip() {
        viewModelScope.launch {
            val metrics = _driftMetrics.value
            val state = _vehicleState.value
            val trip = TripRecordEntity(
                title = "Navigation Trip - ${state.currentRoadName}",
                startTimeMs = tripStartTimeMs,
                endTimeMs = System.currentTimeMillis(),
                distanceMeters = state.distanceTraveledMeters,
                maxSpeedKmph = state.speedKmph.coerceAtLeast(35f),
                maxDriftMeters = metrics.maxDriftMeters,
                averageDriftPercent = metrics.relativeDriftPercent,
                blackoutDurationSec = metrics.outageDurationSeconds,
                zuptEvents = metrics.zuptEventsCount,
                mountPreset = _calibration.value.mountPreset.displayName
            )
            navDao.insertTrip(trip)
        }
    }

    fun saveBenchmarkResult() {
        viewModelScope.launch {
            val scenario = benchmarkEngine.currentScenario
            val metrics = _driftMetrics.value
            val record = BenchmarkRecordEntity(
                scenarioId = scenario.id,
                scenarioName = scenario.title,
                timestampMs = System.currentTimeMillis(),
                durationSec = scenario.totalDurationSeconds.toFloat(),
                distanceMeters = scenario.totalDistanceMeters,
                finalDriftMeters = metrics.currentDriftMeters,
                relativeDriftPercent = metrics.relativeDriftPercent,
                rawDriftMeters = metrics.rawDriftMeters,
                speedRmseMps = metrics.speedRmseMps,
                passedBenchmark = metrics.benchmarkPassed
            )
            navDao.insertBenchmark(record)
        }
    }

    private fun startEngineLoop() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            while (isActive) {
                if (_dataSourceMode.value == DataSourceMode.IO_VNBD_BENCHMARK_SIMULATOR) {
                        if (benchmarkEngine.isPlaying) {
                        val dt = 0.1f // 10Hz simulation step
                        val (imu, gnss, gt) = benchmarkEngine.stepSimulation(dt, fusionEngine)
                        _currentImu.value = imu
                        _currentGnss.value = gnss
                        val newState = fusionEngine.currentState
                        _vehicleState.value = newState
                        _benchmarkTimeSec.value = benchmarkEngine.currentScenarioTimeSec
                        val metrics = fusionEngine.getDriftMetrics(gt.lat, gt.lng)
                        _driftMetrics.value = metrics
                        _digitalTwinMetrics.value = fusionEngine.getDigitalTwinMetrics(gt.lat, gt.lng, benchmarkEngine.currentScenario.title)
                        _navigationIntegrity.value = fusionEngine.getNavigationIntegrityReport()
                        _gnssSpoofingReport.value = fusionEngine.currentSpoofingReport
                        _roadLayerState.value = fusionEngine.currentRoadLayerState
                        _undergroundTopologyReport.value = fusionEngine.currentUndergroundReport
                        _multiHypothesisReport.value = fusionEngine.currentMultiHypothesisReport

                        // Acoustic cues on automatic blackout boundaries
                        if (newState.fusionMode != previousFusionMode) {
                            if (newState.fusionMode == FusionMode.DEAD_RECKONING_AI || newState.fusionMode == FusionMode.GNSS_OUTAGE_JAMMED) {
                                voiceAssistant.announceGnssBlackout(benchmarkEngine.currentScenario.title)
                            } else if (newState.fusionMode == FusionMode.GNSS_AIDED) {
                                voiceAssistant.announceGnssRestored()
                            }
                            previousFusionMode = newState.fusionMode
                        }

                        if (benchmarkEngine.currentScenarioTimeSec >= benchmarkEngine.currentScenario.totalDurationSeconds) {
                            _isBenchmarkPlaying.value = false
                            saveBenchmarkResult()
                            voiceAssistant.speak("Benchmark scenario complete. Final drift is ${"%.2f".format(metrics.currentDriftMeters)} meters.")
                        }
                    }
                }
                delay(100L) // 10Hz loop
            }
        }
    }

    private fun startLiveSensorsLoop() {
        liveSensorJob?.cancel()
        liveSensorJob = viewModelScope.launch {
            while (isActive) {
                if (_dataSourceMode.value == DataSourceMode.LIVE_HARDWARE_SENSORS) {
                    val imu = liveSensorManager.liveImu.value
                    val gnss = liveSensorManager.liveGnss.value

                    _currentImu.value = imu
                    _currentGnss.value = gnss

                    // Dynamic calibration update
                    val isMoving = fusionEngine.currentState.speedMps > 0.5f
                    fusionEngine.calibrator.processCalibrationSample(imu, isMoving)
                    _calibration.value = fusionEngine.calibrator.currentCalibration

                    // Process fusion
                    fusionEngine.processImu(imu, 0.1f)
                    if (gnss != null && !_isManualJammingActive.value) {
                        fusionEngine.processGnss(gnss)
                    }

                    _vehicleState.value = fusionEngine.currentState
                    _driftMetrics.value = fusionEngine.getDriftMetrics(gnss?.lat, gnss?.lng)
                    _digitalTwinMetrics.value = fusionEngine.getDigitalTwinMetrics(gnss?.lat, gnss?.lng, "Live Sensor Mode")
                    _navigationIntegrity.value = fusionEngine.getNavigationIntegrityReport()
                    _gnssSpoofingReport.value = fusionEngine.currentSpoofingReport
                    _roadLayerState.value = fusionEngine.currentRoadLayerState
                    _undergroundTopologyReport.value = fusionEngine.currentUndergroundReport
                    _multiHypothesisReport.value = fusionEngine.currentMultiHypothesisReport
                }
                delay(100L)
            }
        }
    }

    fun toggleUndergroundDescent(active: Boolean) {
        fusionEngine.undergroundTopologyEngine.setSimulatedDescent(active)
        if (active) {
            fusionEngine.barometricEngine.setSimulatedLayer("B1")
            voiceAssistant.speak("Underground topology descent initiated. Barometric pressure increasing. Locking to bottom road.")
        } else {
            fusionEngine.barometricEngine.setSimulatedLayer("L0")
            voiceAssistant.speak("Returning to surface level topology.")
        }
        _roadLayerState.value = fusionEngine.currentRoadLayerState
        _undergroundTopologyReport.value = fusionEngine.undergroundTopologyEngine.evaluateUndergroundTopology(
            fusionEngine.currentState,
            fusionEngine.currentRoadLayerState
        )
    }

    fun setMultiHypothesisScenario(scenarioPreset: String) {
        fusionEngine.multiHypothesisEngine.setScenarioPreset(scenarioPreset)
        _multiHypothesisReport.value = fusionEngine.multiHypothesisEngine.currentReport
        val label = when (scenarioPreset) {
            "PARALLEL_SERVICE_ROAD_SPLIT" -> "Parallel Expressway vs Service Road"
            "STACKED_FLYOVER_BIFURCATION" -> "Stacked Elevated Flyover Ingress"
            "SUBTERRANEAN_TUNNEL_PORTAL" -> "Subterranean Tunnel Bore Fork"
            else -> scenarioPreset
        }
        voiceAssistant.speak("Multi-hypothesis scenario loaded: $label")
    }

    fun toggleSimulatedSpoofingAttack(active: Boolean) {
        fusionEngine.spoofingDetector.setSimulatedSpoofingAttack(active)
        if (active) {
            // Immediately run a spoofed check so report updates instantly
            val gnss = _currentGnss.value ?: GnssReading(
                lat = fusionEngine.currentState.lat + 0.00035,
                lng = fusionEngine.currentState.lng + 0.00025,
                speedMps = 68.0f / 3.6f,
                bearingDeg = (fusionEngine.currentState.headingDeg + 31f) % 360f,
                isValid = true
            )
            fusionEngine.processGnss(gnss)
            _gnssSpoofingReport.value = fusionEngine.currentSpoofingReport
            _navigationIntegrity.value = fusionEngine.getNavigationIntegrityReport()
            voiceAssistant.speak("Warning! GNSS inconsistency detected. Heading disagreement 31 degrees, position jump 27 meters. Spoofed coordinate rejected. INS and Map retained.")
        } else {
            val normalReport = GnssSpoofingReport(
                isSpoofingDetected = false,
                statusSummary = "ALL SIGNALS NOMINAL (GNSS TRUSTED)",
                actionTaken = "GNSS TRUSTED (FUSION ACTIVE)"
            )
            _gnssSpoofingReport.value = normalReport
            _navigationIntegrity.value = fusionEngine.getNavigationIntegrityReport()
            voiceAssistant.speak("Spoofing simulation ended. GNSS signal integrity restored.")
        }
    }

    fun setTargetRoadLayer(layerCode: String?) {
        fusionEngine.barometricEngine.setSimulatedLayer(layerCode)
        _roadLayerState.value = fusionEngine.currentRoadLayerState
        val label = when (layerCode) {
            "L2" -> "Elevated Flyover Level 2 (+12 meters)"
            "L1" -> "Interchange Ramp Level 1 (+6.5 meters)"
            "L0" -> "Surface Grade Arterial (0 meters)"
            "L-1" -> "Underpass Grade Separation (-5.8 meters)"
            "B1" -> "Subsurface Tunnel Level B1 (-9.2 meters)"
            "B2" -> "Deep Underground Level B2 (-14.6 meters)"
            else -> "Automatic Barometric Identification"
        }
        voiceAssistant.speak("Road layer targeted: $label")
    }

    override fun onCleared() {
        super.onCleared()
        liveSensorManager.stopListening()
        voiceAssistant.shutdown()
    }
}
