package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "trip_records")
data class TripRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val distanceMeters: Float,
    val maxSpeedKmph: Float,
    val maxDriftMeters: Float,
    val averageDriftPercent: Float,
    val blackoutDurationSec: Float,
    val zuptEvents: Int,
    val mountPreset: String
)

@Entity(tableName = "benchmark_records")
data class BenchmarkRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scenarioId: String,
    val scenarioName: String,
    val timestampMs: Long,
    val durationSec: Float,
    val distanceMeters: Float,
    val finalDriftMeters: Float,
    val relativeDriftPercent: Float,
    val rawDriftMeters: Float,
    val speedRmseMps: Float,
    val passedBenchmark: Boolean
)

@Dao
interface NavDao {
    @Query("SELECT * FROM trip_records ORDER BY startTimeMs DESC")
    fun getAllTrips(): Flow<List<TripRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripRecordEntity): Long

    @Query("SELECT * FROM benchmark_records ORDER BY timestampMs DESC")
    fun getAllBenchmarks(): Flow<List<BenchmarkRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBenchmark(record: BenchmarkRecordEntity): Long

    @Query("DELETE FROM trip_records")
    suspend fun clearTrips()

    @Query("DELETE FROM benchmark_records")
    suspend fun clearBenchmarks()
}

@Database(
    entities = [TripRecordEntity::class, BenchmarkRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NavDatabase : RoomDatabase() {
    abstract fun navDao(): NavDao

    companion object {
        @Volatile
        private var INSTANCE: NavDatabase? = null

        fun getDatabase(context: android.content.Context): NavDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NavDatabase::class.java,
                    "isro_navdr_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
