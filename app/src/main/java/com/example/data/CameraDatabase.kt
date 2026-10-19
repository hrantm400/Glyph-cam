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
    val slotCode: String, // e.g., "C1", "C2", "C3", "C4"
    val name: String,
    val isVideoPreset: Boolean,
    val lensId: String,
    val isoValue: Int, // 0 = AUTO
    val shutterDenominator: Int, // 0 = AUTO, e.g. 48 for 1/48s
    val shutterAngleDeg: Float, // e.g. 180f
    val wbKelvin: Int, // 0 = AWB
    val evCompensation: Float,
    val lutId: String,
    val anamorphicLabel: String,
    val aspectGuideLabel: String,
    val glyphModeName: String,
    val glyphBrightness: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "captured_shots")
data class CapturedShotEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val filePath: String,
    val isVideo: Boolean,
    val durationSec: Int,
    val titleCode: String, // e.g. "NTH2_C001_RAW"
    val lensInfo: String,
    val resolutionLabel: String,
    val formatBadge: String,
    val isoUsed: Int,
    val shutterLabel: String,
    val wbKelvin: Int,
    val apertureLabel: String,
    val evValue: Float,
    val lutApplied: String,
    val glyphModeUsed: String,
    val averageIre: Int,
    val dynamicRangeStops: Float,
    val bitrateMbps: Int,
    val audioPeakDb: Float,
    val histogramSummary: String, // comma-separated 16 bins for instant mini-scope rendering
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapturedShot(shot: CapturedShotEntity)

    @Query("DELETE FROM captured_shots WHERE id = :id")
    suspend fun deleteCapturedShot(id: Int)

    @Query("SELECT COUNT(*) FROM captured_shots")
    suspend fun getShotCount(): Int
}

@Database(
    entities = [CameraPresetEntity::class, CapturedShotEntity::class],
    version = 1,
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

    suspend fun seedFactoryPresetsIfNeeded() {
        if (dao.getPresetCount() == 0) {
            val defaults = listOf(
                CameraPresetEntity(
                    slotCode = "C1",
                    name = "NIGHT CINE // 24P 180°",
                    isVideoPreset = true,
                    lensId = "main",
                    isoValue = 800,
                    shutterDenominator = 48,
                    shutterAngleDeg = 180f,
                    wbKelvin = 3200,
                    evCompensation = -0.3f,
                    lutId = "cyber_2099",
                    anamorphicLabel = "1.33x ANA",
                    aspectGuideLabel = "2.39:1 SCOPE",
                    glyphModeName = "PORTRAIT_CATCHLIGHT",
                    glyphBrightness = 65
                ),
                CameraPresetEntity(
                    slotCode = "C2",
                    name = "IMX890 STREET RAW // B&W",
                    isVideoPreset = false,
                    lensId = "cine35",
                    isoValue = 100,
                    shutterDenominator = 500,
                    shutterAngleDeg = 90f,
                    wbKelvin = 5600,
                    evCompensation = -0.7f,
                    lutId = "mono_red",
                    anamorphicLabel = "1.00x SPH",
                    aspectGuideLabel = "Off",
                    glyphModeName = "OFF",
                    glyphBrightness = 0
                ),
                CameraPresetEntity(
                    slotCode = "C3",
                    name = "JN1 MACRO 4CM // RING FILL",
                    isVideoPreset = false,
                    lensId = "ultra",
                    isoValue = 200,
                    shutterDenominator = 250,
                    shutterAngleDeg = 180f,
                    wbKelvin = 5200,
                    evCompensation = 0.0f,
                    lutId = "nothing_std",
                    anamorphicLabel = "1.00x SPH",
                    aspectGuideLabel = "1:1 MEDIUM",
                    glyphModeName = "CAMERA_RING_MACRO",
                    glyphBrightness = 90
                ),
                CameraPresetEntity(
                    slotCode = "C4",
                    name = "N-LOG 12-STOP // KODAK 2383",
                    isVideoPreset = true,
                    lensId = "main",
                    isoValue = 400,
                    shutterDenominator = 48,
                    shutterAngleDeg = 180f,
                    wbKelvin = 5600,
                    evCompensation = 0.0f,
                    lutId = "n_log_flat",
                    anamorphicLabel = "1.55x SCOPE",
                    aspectGuideLabel = "2.39:1 SCOPE",
                    glyphModeName = "TALLY_AND_AUDIO_VU",
                    glyphBrightness = 45
                )
            )
            defaults.forEach { dao.insertPreset(it) }
        }
        if (dao.getShotCount() == 0) {
            dao.insertCapturedShot(
                CapturedShotEntity(
                    filePath = "CALIBRATION_REF_IMX890_DNG",
                    isVideo = false,
                    durationSec = 0,
                    titleCode = "NTH2_0001_IMX890.DNG",
                    lensInfo = "24mm SONY IMX890 • f/1.88 OIS",
                    resolutionLabel = "50MP (8192×6144)",
                    formatBadge = "DNG 12-BIT",
                    isoUsed = 100,
                    shutterLabel = "1/250s",
                    wbKelvin = 5600,
                    apertureLabel = "f/1.88",
                    evValue = -0.3f,
                    lutApplied = "N-LOG",
                    glyphModeUsed = "FILL 100%",
                    averageIre = 52,
                    dynamicRangeStops = 12.4f,
                    bitrateMbps = 0,
                    audioPeakDb = -18.0f,
                    histogramSummary = "8,14,26,42,58,74,88,94,86,70,54,40,28,18,10,4"
                )
            )
            dao.insertCapturedShot(
                CapturedShotEntity(
                    filePath = "CALIBRATION_REF_CINE_MOV",
                    isVideo = true,
                    durationSec = 18,
                    titleCode = "NTH2_A002_CINE24P.MP4",
                    lensInfo = "35mm IMX890 CINE • 1.33x ANA",
                    resolutionLabel = "4K DCI (4096×2160)",
                    formatBadge = "H.265 10-BIT",
                    isoUsed = 800,
                    shutterLabel = "180.0° (1/48s)",
                    wbKelvin = 3200,
                    apertureLabel = "f/1.88",
                    evValue = 0.0f,
                    lutApplied = "K-2383",
                    glyphModeUsed = "VU+REC",
                    averageIre = 46,
                    dynamicRangeStops = 11.9f,
                    bitrateMbps = 180,
                    audioPeakDb = -6.4f,
                    histogramSummary = "15,28,45,62,78,82,75,64,52,44,36,28,22,16,9,3"
                )
            )
        }
    }
}
