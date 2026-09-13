package com.example.model
/**
 * Temporal Self-Healing Trajectory Node
 * Used for retrospective trajectory rewinding and RTS / Pose-graph smoothing.
 */
data class TrajectoryPoseNode(
    val timestampMs: Long = System.currentTimeMillis(),
    val lat: Double = 37.7749,
    val lng: Double = -122.4194,
    val altitudeM: Float = 0f,
    val headingDeg: Float = 0f,
    val speedMps: Float = 0f,
    val uncertaintyRadiusM: Float = 1.5f,
    val isOptimized: Boolean = false,
    val originalLat: Double = 37.7749,
    val originalLng: Double = -122.4194,
    val correctionOffsetMeters: Float = 0f
)

/**
 * Temporal Self-Healing Report
 * Retrospective trajectory optimization when GNSS returns after outage.
 */
data class TemporalSelfHealingReport(
    val isHealingActive: Boolean = false,
    val lastHealingTimestamp: Long = 0L,
    val timeWindowSec: Int = 30,
    val totalCorrectionMeters: Float = 0f,
    val maxDiscoveredErrorMeters: Float = 0f,
    val nodesRewoundCount: Int = 0,
    val solverAlgorithm: String = "Rauch-Tung-Striebel (RTS) Backward Smoother + Pose-Graph Spline",
    val convergenceStatus: String = "IDLE (Historical trajectory buffered)",
    val historicalNodes: List<TrajectoryPoseNode> = emptyList(),
    val smoothedNodes: List<TrajectoryPoseNode> = emptyList(),
    val visualSmoothnessGainPct: Int = 94
)

/**
 * Estimator types for Shadow Navigation Ensemble
 */
enum class ShadowEstimatorType {
    IN_EKF,      // Invariant Extended Kalman Filter (Lie Group SE_2(3) / Matrix Lie)
    UKF,         // Unscented Kalman Filter (Sigma-point non-linear transform)
    NEURAL_IO    // Deep Neural Inertial Odometry (Transformer/ResNet temporal sequence)
}

/**
 * Individual Shadow Navigation Estimate
 */
data class ShadowNavEstimate(
    val type: ShadowEstimatorType,
    val name: String,
    val subtitle: String,
    val lat: Double = 37.7749,
    val lng: Double = -122.4194,
    val speedMps: Float = 0f,
    val headingDeg: Float = 0f,
    val uncertaintyMeters: Float = 1.5f,
    val trustScore: Float = 0.85f, // 0.0 to 1.0 dynamic trustworthiness
    val isSelectedWinner: Boolean = false,
    val keyAdvantage: String = "",
    val activeConditionRating: String = "Optimal"
)

/**
 * Shadow Navigation Ensemble & Meta-Selector Report
 */
data class ShadowNavReport(
    val estimates: List<ShadowNavEstimate> = emptyList(),
    val winnerType: ShadowEstimatorType = ShadowEstimatorType.IN_EKF,
    val winnerName: String = "Invariant EKF (InEKF)",
    val metaDecisionReason: String = "Linear highway cruising with steady dynamics favors Lie-group geometric invariant consistency.",
    val ensembleSpreadMeters: Float = 1.2f, // Dispersion among InEKF, UKF, and Neural IO
    val currentEnvironmentCondition: String = "Mixed Urban Canyon with Moderate Vibration",
    val developerModeVisible: Boolean = true
)

enum class FusionMode(val label: String, val description: String) {
    GNSS_AIDED("GNSS + INS AIDED", "Full satellite constellation lock (NavIC/GPS/Galileo) with active Kalman bias calibration"),
    DEAD_RECKONING_AI("AI-ML DEAD RECKONING", "Zero-GNSS outage tracking with Kinematic AI Speed + NHC + Map-Matching"),
    GNSS_OUTAGE_JAMMED("GNSS OUTAGE / JAMMED", "Instant deficit handler triggered: IMU inertial propagation active"),
    CALIBRATING("CALIBRATING MOUNT", "Estimating phone pitch/roll/yaw relative to vehicle driving direction")
}

enum class MountPreset(val displayName: String, val pitch: Float, val roll: Float, val yaw: Float) {
    DASHBOARD_HOLDER("Dashboard Mobile Holder (60° upright)", 60f, 0f, 0f),
    WINDSHIELD_MOUNT("Windshield Suction Mount (45° tilt)", 45f, 0f, 0f),
    AC_VENT_CRADLE("AC Vent Magnetic Cradle (75° upright)", 75f, -5f, 0f),
    CENTER_CONSOLE("Flat Center Console (0° horizontal)", 0f, 0f, 0f),
    CUSTOM_DYNAMIC("Auto Dynamic Alignment (Adaptive DCM)", 0f, 0f, 0f)
}

enum class TrajectoryType {
    GROUND_TRUTH,
    RAW_IMU_DR,
    AI_INS_FUSION,
    MAP_MATCHED,
    TRAJECTORY_A_PHYSICS_ONLY,
    TRAJECTORY_B_AI_CORRECTED,
    TRAJECTORY_C_MAP_CONSTRAINED,
    TRAJECTORY_D_VISUAL_CORRECTED,
    MASTER_FUSION
}

/**
 * AI Navigation State Machine (10 States)
 * Replaces binary GPS ON / OFF with nuanced integrity & threat awareness.
 */
enum class AiNavState(
    val stateNumber: Int,
    val stateCode: String,
    val title: String,
    val integrityMode: String,
    val gnssTrustStatus: String,
    val description: String
) {
    STATE_0_FULL_GNSS(
        0, "STATE 0", "Full GNSS", "GNSS-AIDED INS", "TRUSTED",
        "Full satellite constellation locked (NavIC + GPS L1/L5). Active carrier-phase Kalman updates."
    ),
    STATE_1_GNSS_DEGRADED(
        1, "STATE 1", "GNSS degraded", "DEGRADED FUSION", "DEGRADED",
        "High HDOP (> 3.5) / Low SNR. Covariance inflation & heavier weight on IMU forward velocity."
    ),
    STATE_2_MULTIPATH_SUSPECTED(
        2, "STATE 2", "GNSS multipath suspected", "MULTIPATH MITIGATION", "SUSPECTED",
        "Sudden pseudorange innovation spike without corresponding IMU acceleration. Multipath gated."
    ),
    STATE_3_SPOOFING_SUSPECTED(
        3, "STATE 3", "GNSS spoofing suspected", "ANTI-SPOOFING REJECTION", "UNTRUSTED",
        "Unphysical Doppler drift vs vehicle chassis IMU. Satellite updates fully rejected."
    ),
    STATE_4_GNSS_OUTAGE(
        4, "STATE 4", "GNSS outage", "AI-INS DEAD RECKONING", "UNTRUSTED",
        "Total signal blackout / tunnel. Instant deficit handler triggered (<10ms)."
    ),
    STATE_5_IMU_DEGRADING(
        5, "STATE 5", "IMU confidence degrading", "INERTIAL DRIFT WARNING", "UNTRUSTED",
        "Prolonged outage (>30s). Gyro random-walk accumulation; bounding via AI Speed & ZUPT."
    ),
    STATE_6_MAP_RECOVERY(
        6, "STATE 6", "Map recovery", "MAP-AIDED RECOVERY", "UNTRUSTED",
        "Vehicle turn / topological road node match verified. Error ellipse shrunk by road geometry."
    ),
    STATE_7_VISUAL_RECOVERY(
        7, "STATE 7", "Visual recovery", "VISION-INERTIAL RELOC", "UNTRUSTED",
        "Camera optical flow / visual-inertial speed match confirmed. Forward velocity drift dampened."
    ),
    STATE_8_GNSS_REACQUISITION(
        8, "STATE 8", "GNSS re-acquisition", "INNOVATION TESTING", "ACQUIRING",
        "Raw pseudoranges detected. Chi-squared innovation residual test active before re-lock."
    ),
    STATE_9_TRAJECTORY_RECONCILIATION(
        9, "STATE 9", "Trajectory reconciliation", "TRAJECTORY RECONCILIATION", "RESTORED",
        "Kalman backward smoothing applied. Continuous path reconciled without jump discontinuity."
    )
}

/**
 * Evaluator Navigation Integrity Report
 */
data class NavigationIntegrityReport(
    val state: AiNavState = AiNavState.STATE_0_FULL_GNSS,
    val mode: String = "MAP-AIDED DEAD RECKONING",
    val gnssTrust: String = "TRUSTED",
    val imuConfidencePercent: Int = 96,
    val mapConfidencePercent: Int = 94,
    val cameraConfidenceStatus: String = "STANDBY",
    val barometerConfidencePercent: Int = 90,
    val estimatedErrorMeters: Float = 1.8f,
    val isEvaluatorManualOverride: Boolean = false
)

/**
 * Digital Twin Continuous Trajectory Benchmark
 * Continuously evaluates Trajectories A, B, C, D and Master Fusion against Ground Truth
 */
data class DigitalTwinMetrics(
    val pureInsErrorMeters: Float = 0f,      // Trajectory A (Physics only)
    val aiInsErrorMeters: Float = 0f,        // Trajectory B (AI corrected)
    val mapInsErrorMeters: Float = 0f,       // Trajectory C (Map constrained)
    val visualInsErrorMeters: Float = 0f,    // Trajectory D (Visual corrected)
    val fusionEstimateErrorMeters: Float = 0f, // NavSense-X Master Fusion
    val pureInsDriftRateMps: Float = 0.45f,
    val aiInsReductionPercent: Float = 68f,
    val fusionReductionPercent: Float = 88f,
    val isOutageActive: Boolean = false,
    val activeScenarioName: String = "Urban Blackout"
)

data class ImuReading(
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 9.81f,
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val magX: Float = 0f,
    val magY: Float = 0f,
    val magZ: Float = 0f,
    val timestampNs: Long = System.nanoTime()
)

data class GnssReading(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val altitude: Double = 0.0,
    val speedMps: Float = 0f,
    val bearingDeg: Float = 0f,
    val accuracyMeters: Float = 2.5f,
    val satellitesInUse: Int = 18,
    val hdop: Float = 0.8f,
    val timestampMs: Long = System.currentTimeMillis(),
    val isValid: Boolean = true
)

data class CalibrationAngles(
    val pitchDeg: Float = 60f,
    val rollDeg: Float = 0f,
    val yawDeg: Float = 0f,
    val isCalibrated: Boolean = true,
    val mountPreset: MountPreset = MountPreset.DASHBOARD_HOLDER,
    val confidence: Float = 0.95f
)

data class VehicleState(
    val lat: Double = 12.9716,
    val lng: Double = 77.5946,
    val altitude: Double = 920.0,
    val speedMps: Float = 0f,
    val headingDeg: Float = 0f,
    val pitchDeg: Float = 0f,
    val rollDeg: Float = 0f,
    val accelLongitudinal: Float = 0f,
    val accelLateral: Float = 0f,
    val accelVertical: Float = 0f,
    val yawRateDegPerSec: Float = 0f,
    val fusionMode: FusionMode = FusionMode.GNSS_AIDED,
    val accuracyMeters: Float = 1.8f,
    val isZuptActive: Boolean = false,
    val isZaruActive: Boolean = false,
    val roadSnapped: Boolean = false,
    val currentRoadName: String = "Outer Ring Road (NH 44)",
    val distanceTraveledMeters: Float = 0f,
    val roadLayerId: String = "L0",
    val roadLayerLevel: String = "L0",
    val roadLayerConfidence: Int = 94,
    val relativeAltitudeMeters: Float = 0.0f,
    val verticalVelocityMps: Float = 0.0f,
    val leadingCandidateProbability: Float = 0.61f,
    val multiHypothesisSummary: String = "Candidate 1: Main (61%) · Candidate 2: Service (24%) · Candidate 3: Flyover (15%)",
    val undergroundRoadLocked: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
) {
    val speedKmph: Float get() = speedMps * 3.6f
    val velocity3D: String get() = "v_x: ${"%.1f".format(speedMps * kotlin.math.cos(Math.toRadians(headingDeg.toDouble())))} m/s, v_y: ${"%.1f".format(speedMps * kotlin.math.sin(Math.toRadians(headingDeg.toDouble())))} m/s, v_z: ${"%.2f".format(verticalVelocityMps)} m/s"
}

data class TrajectoryPoint(
    val lat: Double,
    val lng: Double,
    val altitude: Double = 0.0,
    val speedKmph: Float = 0f,
    val headingDeg: Float = 0f,
    val type: TrajectoryType = TrajectoryType.AI_INS_FUSION,
    val roadLayerId: String = "L0",
    val relativeAltitudeMeters: Float = 0f,
    val timestampMs: Long = System.currentTimeMillis()
)

data class RoadNode(
    val id: String,
    val lat: Double,
    val lng: Double
)

data class RoadSegment(
    val id: String,
    val name: String,
    val startNode: RoadNode,
    val endNode: RoadNode,
    val speedLimitKmph: Int = 60,
    val lanes: Int = 3,
    val layerId: String = "L0",
    val layerLevel: String = "L0",
    val nominalAltitudeMeters: Float = 0f
) {
    val bearingDeg: Float
        get() {
            val dLng = Math.toRadians(endNode.lng - startNode.lng)
            val lat1 = Math.toRadians(startNode.lat)
            val lat2 = Math.toRadians(endNode.lat)
            val y = Math.sin(dLng) * Math.cos(lat2)
            val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng)
            var b = Math.toDegrees(Math.atan2(y, x)).toFloat()
            if (b < 0) b += 360f
            return b
        }
}

/**
 * 3D Road Layer State (Barometer + IMU vertical tracking for multi-layer road networks)
 */
data class RoadLayerState(
    val levelCode: String = "L0",               // "L2" (Flyover), "L0" (Surface), "L-1" (Underpass), "B2" (Underground)
    val layerName: String = "Surface Grade Arterial",
    val relativeAltitudeMeters: Float = 0.0f,   // e.g. -6.8m, +12.0m
    val absoluteAltitudeMslMeters: Double = 920.0,
    val confidencePercent: Int = 93,
    val verticalVelocityMps: Float = 0.0f,
    val atmosphericPressureHpa: Float = 1013.25f,
    val baselinePressureHpa: Float = 1013.25f,
    val pitchAngleDeg: Float = 0f,
    val isStackedRoadScenario: Boolean = false,
    val detectedStructure: String = "Surface Roadway"
)

/**
 * GNSS Spoofing & Jamming Security Report
 * Detects adversarial signal characteristics, kinematic disagreement, and rejects corrupted coordinates.
 */
data class GnssSpoofingReport(
    val isSpoofingDetected: Boolean = false,
    val gnssIntegrityPercent: Int = 98,          // Plunges to 9% under spoofing attack
    val gnssSpeedKmph: Float = 0f,
    val imuSpeedKmph: Float = 0f,
    val speedDeltaKmph: Float = 0f,
    val headingDisagreementDeg: Float = 0f,
    val positionJumpMeters: Float = 0f,
    val cn0PatternAbnormal: Boolean = false,
    val agcAbnormal: Boolean = false,
    val averageCn0DbHz: Float = 39.5f,
    val agcLevelDb: Float = -2.1f,
    val statusSummary: String = "ALL SIGNALS NOMINAL (GNSS TRUSTED)",
    val spoofedLat: Double = 0.0,
    val spoofedLng: Double = 0.0,
    val isSpoofingSimulationActive: Boolean = false,
    val actionTaken: String = "GNSS TRUSTED (FUSION ACTIVE)"
)

data class DriftMetrics(
    val currentDriftMeters: Float = 0f,
    val maxDriftMeters: Float = 0f,
    val distanceTraveledMeters: Float = 0f,
    val outageDurationSeconds: Float = 0f,
    val rawDriftMeters: Float = 0f,
    val relativeDriftPercent: Float = 0f,
    val speedRmseMps: Float = 0.28f,
    val zuptEventsCount: Int = 0,
    val benchmarkPassed: Boolean = true
)

data class BenchmarkScenario(
    val id: String,
    val title: String,
    val description: String,
    val locationName: String,
    val totalDurationSeconds: Int,
    val outageStartSec: Int,
    val outageEndSec: Int,
    val totalDistanceMeters: Float,
    val maxSpeedKmph: Float,
    val roadSegments: List<RoadSegment>,
    val groundTruthPoints: List<TrajectoryPoint>
)

data class TargetDestination(
    val lat: Double,
    val lng: Double,
    val label: String = "Destination Target",
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * 3D Structural Classification of Road Types
 */
enum class RoadStructureType(val label: String) {
    SURFACE_MAIN_CARRIAGEWAY("Surface Main Carriageway (L0)"),
    PARALLEL_SERVICE_ROAD("Parallel Service Road (L0)"),
    APPROACH_DESCENT_RAMP("Approach / Descent Ramp (L0 -> B1)"),
    TUNNEL_PORTAL_INGRESS("Tunnel Portal Ingress"),
    SUBTERRANEAN_TUNNEL_ROAD("Subterranean Tunnel Express (B1)"),
    ELEVATED_FLYOVER_DECK("Elevated Flyover Deck (L2)"),
    UNDERPASS_CUT("Underpass Trench (L-1)")
}

/**
 * Rich 3D Topological Road Segment with Connectivity Graph
 */
data class RoadTopologySegment(
    val id: String,
    val name: String,
    val structureType: RoadStructureType,
    val startNode: RoadNode,
    val endNode: RoadNode,
    val layerLevel: String = "L0",
    val nominalAltitudeMeters: Float = 0f,
    val verticalGradePercent: Float = 0f,      // e.g. -7.0% for descending ramps
    val speedLimitKmph: Int = 50,
    val lanes: Int = 2,
    val connectedToSegmentIds: List<String> = emptyList(),
    val isUnderground: Boolean = false
) {
    val bearingDeg: Float
        get() {
            val dLng = Math.toRadians(endNode.lng - startNode.lng)
            val lat1 = Math.toRadians(startNode.lat)
            val lat2 = Math.toRadians(endNode.lat)
            val y = Math.sin(dLng) * Math.cos(lat2)
            val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng)
            var b = Math.toDegrees(Math.atan2(y, x)).toFloat()
            if (b < 0) b += 360f
            return b
        }
}

/**
 * Rolling Motion History Step for Trajectory-to-Topology Reasoning
 */
data class MotionHistoryStep(
    val lat: Double,
    val lng: Double,
    val speedMps: Float,
    val headingDeg: Float,
    val pitchDeg: Float,
    val relativeAltitudeMeters: Float,
    val verticalVelocityMps: Float,
    val yawRateDegPerSec: Float,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Underground Topology Engine Reasoning Report
 * "Don't match a point to a road. Match a motion history to a road topology."
 * Resolves overlapping stacked roads:
 * Road A (Surface) / Road B (Service) / Ramp -> Road C (Subterranean Bottom Road)
 */
data class UndergroundTopologyReport(
    val isUndergroundLocked: Boolean = false,
    val lockedSegmentId: String = "road_c_tunnel",
    val lockedRoadName: String = "Road C - Subterranean Tunnel Express",
    val lockedLayerLevel: String = "B1",
    val confidencePercent: Int = 98,
    val barometricDescentMeters: Float = -12.4f,
    val verticalDescentRateMps: Float = -1.8f,
    val averagePitchDeg: Float = -3.4f,
    val turnHistoryDetected: String = "Off-ramp junction ingress maneuver (right +32° -> straight into portal)",
    val overlappingCandidatesCount: Int = 3,
    val reasoningChain: List<String> = listOf(
        "1. Latitude/Longitude ambiguity: Road A (dist 3.2m), Road B (dist 4.1m), Road C (dist 2.8m) are stacked within 15m corridor.",
        "2. Barometric Descent: Continuous drop of Δz = -12.4m detected over 65m traveled (descent rate -1.8 m/s).",
        "3. Kinematic Pitch: Vehicle pitch sustained -3.4° nose-down, matching the -7.0% approach ramp incline.",
        "4. Turn History & IMU: Angular yaw excursion matches the off-ramp exit curve off Road B into tunnel portal.",
        "5. Road Connectivity: Graph traversal sequence verified: Road B -> Ramp Ingress -> Tunnel Portal -> Road C.",
        "6. TOPOLOGY VERDICT: Deterministically LOCKED to Road C (BOTTOM SUBTERRANEAN ROAD). Surface Roads A & B rejected."
    ),
    val statusSummary: String = "LOCKED TO BOTTOM ROAD (Road C Subterranean) via 6-Factor Motion History"
)

/**
 * Individual Road Candidate Hypothesis in Multi-Hypothesis Localization
 * (Particle Filter + HMM + Bayesian Scoring)
 */
data class LocalizationHypothesis(
    val id: String,
    val roadName: String,
    val structureType: RoadStructureType = RoadStructureType.SURFACE_MAIN_CARRIAGEWAY,
    val layerLevel: String = "L0",
    val probability: Float = 0.33f,             // Normalized P in [0.0, 1.0] (e.g. 0.61, 0.24, 0.15)
    val snappedLat: Double = 0.0,
    val snappedLng: Double = 0.0,
    val lateralOffsetMeters: Float = 0.0f,
    val headingDeltaDeg: Float = 0.0f,
    val altitudeDeltaMeters: Float = 0.0f,
    val particleCount: Int = 50,
    val isLeading: Boolean = false,
    val rank: Int = 1,
    val bayesianLikelihood: Float = 0.85f,
    val hmmTransitionScore: Float = 0.90f
)

/**
 * Multi-Hypothesis Localization Report
 * Never immediately say "The vehicle is here."
 * Instead maintains Candidate 1 (P=0.61), Candidate 2 (P=0.24), Candidate 3 (P=0.15).
 */
data class MultiHypothesisReport(
    val candidates: List<LocalizationHypothesis> = listOf(
        LocalizationHypothesis(
            id = "cand_1_main",
            roadName = "Candidate 1: Main Carriageway",
            structureType = RoadStructureType.SURFACE_MAIN_CARRIAGEWAY,
            layerLevel = "L0",
            probability = 0.61f,
            lateralOffsetMeters = 1.4f,
            headingDeltaDeg = 1.2f,
            altitudeDeltaMeters = 0.2f,
            particleCount = 92,
            isLeading = true,
            rank = 1
        ),
        LocalizationHypothesis(
            id = "cand_2_service",
            roadName = "Candidate 2: Parallel Service Road",
            structureType = RoadStructureType.PARALLEL_SERVICE_ROAD,
            layerLevel = "L0",
            probability = 0.24f,
            lateralOffsetMeters = 14.2f,
            headingDeltaDeg = 3.5f,
            altitudeDeltaMeters = 0.1f,
            particleCount = 36,
            isLeading = false,
            rank = 2
        ),
        LocalizationHypothesis(
            id = "cand_3_flyover",
            roadName = "Candidate 3: Elevated Flyover Deck",
            structureType = RoadStructureType.ELEVATED_FLYOVER_DECK,
            layerLevel = "L2",
            probability = 0.15f,
            lateralOffsetMeters = 2.1f,
            headingDeltaDeg = 1.8f,
            altitudeDeltaMeters = 11.8f,
            particleCount = 22,
            isLeading = false,
            rank = 3
        )
    ),
    val ambiguityContext: String = "Parallel Roads & Stacked Elevated Deck",
    val shannonEntropy: Float = 0.94f,           // High entropy = bifurcation/ambiguity, Low = absolute consensus
    val totalActiveParticles: Int = 150,
    val leadingHypothesis: LocalizationHypothesis = LocalizationHypothesis(
        id = "cand_1_main",
        roadName = "Candidate 1: Main Carriageway",
        probability = 0.61f,
        isLeading = true,
        rank = 1
    ),
    val statusHeadline: String = "Candidate 1: Main Carriageway (P = 0.61) · Candidate 2: Service (P = 0.24) · Candidate 3: Flyover (P = 0.15)"
)


