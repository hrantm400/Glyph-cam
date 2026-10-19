package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "camera_presets")
data class CameraPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val slotCode: String, // "C1", "C2", ...
    val name: String,
    val isVideoPreset: Boolean,
    val zoomRatio: Float,
    val useFrontCamera: Boolean,
    val isoValue: Int, // 0 = AUTO
    val shutterNs: Long, // 0 = AUTO
    val shutterAngleDeg: Float,
    val awbPreset: String,
    val evCompensation: Float,
    val lutId: String,
    val anamorphicLabel: String,
    val aspectGuideLabel: String,
    val glyphModeName: String,
    val glyphBrightness: Int,
    val videoResolution: String,
    val videoFps: Int,
    val hdrVideo: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

/** One photo or video this app wrote to MediaStore, plus what we know about how it was shot. */
@Entity(tableName = "captured_shots")
data class CapturedShotEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uri: String,
    val isVideo: Boolean,
    val displayName: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val durationMs: Long,
    val isoUsed: Int, // 0 = unknown
    val shutterLabel: String,
    val apertureLabel: String,
    val focalLabel: String,
    val zoomRatio: Float,
    val wbLabel: String,
    val evValue: Float,
    val lutApplied: String,
    val lutBaked: Boolean,
    val glyphModeUsed: String,
    val cameraLabel: String,
    val histogramSummary: String, // 16 comma-separated bins computed from the saved file
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface CameraDao {
    @Query("SELECT * FROM camera_presets ORDER BY id ASC")
    fun getAllPresets(): Flow<List<CameraPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: CameraPresetEntity)

    @Query("DELETE FROM camera_presets WHERE id = :id")
    suspend fun deletePreset(id: Int)

    @Query("SELECT COUNT(*) FROM camera_presets")
    suspend fun getPresetCount(): Int

    @Query("SELECT * FROM captured_shots ORDER BY timestamp DESC")
    fun getAllCapturedShots(): Flow<List<CapturedShotEntity>>

    @Query("SELECT * FROM captured_shots")
    suspend fun getAllCapturedShotsOnce(): List<CapturedShotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapturedShot(shot: CapturedShotEntity)

    @Query("DELETE FROM captured_shots WHERE id = :id")
    suspend fun deleteCapturedShot(id: Int)
}

@Database(
    entities = [CameraPresetEntity::class, CapturedShotEntity::class],
    version = 2,
    exportSchema = false
)
abstract class GlyphCamDatabase : RoomDatabase() {
    abstract fun cameraDao(): CameraDao

    companion object {
        @Volatile
        private var INSTANCE: GlyphCamDatabase? = null

        fun getInstance(context: Context): GlyphCamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GlyphCamDatabase::class.java,
                    "glyphcam_pro.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class CameraRepository(private val dao: CameraDao) {
    val presets: Flow<List<CameraPresetEntity>> = dao.getAllPresets()
    val capturedShots: Flow<List<CapturedShotEntity>> = dao.getAllCapturedShots()

    suspend fun savePreset(preset: CameraPresetEntity) = dao.insertPreset(preset)
    suspend fun deletePreset(id: Int) = dao.deletePreset(id)
    suspend fun logCapturedShot(shot: CapturedShotEntity) = dao.insertCapturedShot(shot)
    suspend fun deleteCapturedShot(id: Int) = dao.deleteCapturedShot(id)
    suspend fun allShotsOnce(): List<CapturedShotEntity> = dao.getAllCapturedShotsOnce()

    /** Two starter presets so the C1/C2 chips do something on a fresh install. */
    suspend fun seedFactoryPresetsIfNeeded() {
        if (dao.getPresetCount() == 0) {
            dao.insertPreset(
                CameraPresetEntity(
                    slotCode = "C1",
                    name = "КИНО 24P · TEAL & ORANGE",
                    isVideoPreset = true,
                    zoomRatio = 1.0f,
                    useFrontCamera = false,
                    isoValue = 0,
                    shutterNs = 0L,
                    shutterAngleDeg = 180f,
                    awbPreset = "AUTO",
                    evCompensation = -0.3f,
                    lutId = "kodak_2383",
                    anamorphicLabel = "1.00x",
                    aspectGuideLabel = "2.39:1",
                    glyphModeName = "OFF",
                    glyphBrightness = 60,
                    videoResolution = "UHD_4K",
                    videoFps = 24,
                    hdrVideo = false
                )
            )
            dao.insertPreset(
                CameraPresetEntity(
                    slotCode = "C2",
                    name = "УЛИЦА · Ч/Б",
                    isVideoPreset = false,
                    zoomRatio = 2.0f,
                    useFrontCamera = false,
                    isoValue = 0,
                    shutterNs = 0L,
                    shutterAngleDeg = 180f,
                    awbPreset = "AUTO",
                    evCompensation = -0.7f,
                    lutId = "mono_red",
                    anamorphicLabel = "1.00x",
                    aspectGuideLabel = "БЕЗ РАМКИ",
                    glyphModeName = "OFF",
                    glyphBrightness = 0,
                    videoResolution = "FHD_1080P",
                    videoFps = 30,
                    hdrVideo = false
                )
            )
        }
    }
}
