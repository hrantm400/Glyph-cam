package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AntiBanding
import com.example.model.AudioSourceOption
import com.example.model.CapturePriority
import com.example.model.JpegQuality
import com.example.model.PhotoAspect
import com.example.model.ProcessingMode
import com.example.model.RecordingLimit
import com.example.model.VideoBitrate
import com.example.ui.components.ChipShape
import com.example.ui.components.CodeBadge
import com.example.ui.components.FadingScrollRow
import com.example.ui.components.NothingCard
import com.example.ui.components.SectionLabel
import com.example.ui.components.StudioHeader
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingSilver
import com.example.ui.theme.NothingSurface
import com.example.ui.theme.NothingWhite
import com.example.viewmodel.ProCameraUiState

/** Persistent preferences. Everything here survives restarts (SharedPreferences). */
@Composable
fun SettingsScreen(
    state: ProCameraUiState,
    onJpegQuality: (JpegQuality) -> Unit,
    onCapturePriority: (CapturePriority) -> Unit,
    onPhotoAspect: (PhotoAspect) -> Unit,
    onVideoBitrate: (VideoBitrate) -> Unit,
    onAudioSource: (AudioSourceOption) -> Unit,
    onRecordingLimit: (RecordingLimit) -> Unit,
    onAntiBanding: (AntiBanding) -> Unit,
    onNoiseReduction: (ProcessingMode) -> Unit,
    onEdgeMode: (ProcessingMode) -> Unit,
    onToggleShutterSound: () -> Unit,
    onToggleVolumeKeys: () -> Unit,
    onToggleMirror: () -> Unit,
    onToggleHaptics: () -> Unit,
    onToggleKeepScreenOn: () -> Unit,
    onToggleBakeLut: () -> Unit,
    onBackToCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToCamera)
    val caps = state.capabilities
    val cameraUnknown = caps.cameraLabel.isEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(bottom = 24.dp)
    ) {
        StudioHeader(
            title = "Настройки",
            subtitle = "СОХРАНЯЮТСЯ МЕЖДУ ЗАПУСКАМИ",
            onBack = onBackToCamera,
            backTag = "settings_back_btn"
        ) {
            CodeBadge(text = "PRO", color = NothingRed)
        }

        SettingsSection(title = "Фото") {
            ChoiceSetting(
                title = "Качество JPEG",
                hint = "100 — максимум деталей и самый большой файл",
                choices = JpegQuality.entries.map { v -> Choice(v.label, state.jpegQuality == v) { onJpegQuality(v) } }
            )
            ChoiceSetting(
                title = "Приоритет съёмки",
                hint = state.capturePriority.descRu + if (!caps.zslSupported && !cameraUnknown) " · ZSL эта камера не поддерживает" else "",
                choices = CapturePriority.entries.map { v ->
                    Choice(v.labelRu, state.capturePriority == v, enabled = v != CapturePriority.ZERO_LAG || caps.zslSupported || cameraUnknown) {
                        onCapturePriority(v)
                    }
                }
            )
            ChoiceSetting(
                title = "Формат кадра",
                hint = state.photoAspect.descRu,
                choices = PhotoAspect.entries.map { v -> Choice(v.label, state.photoAspect == v) { onPhotoAspect(v) } }
            )
            SwitchSetting(
                title = "Запекать LUT в JPEG",
                hint = "Снимок сохраняется уже с выбранным цветовым профилем; RAW не трогается",
                checked = state.bakeLutIntoPhotos,
                onToggle = onToggleBakeLut,
                tag = "switch_bake_lut"
            )
            SwitchSetting(
                title = "Зеркалить селфи",
                hint = "Фронтальная камера сохраняет фото и видео такими, какими вы видите их на экране",
                checked = state.mirrorSelfie,
                onToggle = onToggleMirror,
                tag = "switch_mirror_selfie"
            )
        }

        SettingsSection(title = "Видео") {
            ChoiceSetting(
                title = "Битрейт",
                hint = "АВТО подбирает битрейт под разрешение и частоту кадров; выше битрейт — чище картинка и больше файл",
                choices = VideoBitrate.entries.map { v -> Choice(v.label, state.videoBitrate == v) { onVideoBitrate(v) } }
            )
            ChoiceSetting(
                title = "Микрофон",
                hint = state.audioSource.descRu,
                choices = AudioSourceOption.entries.map { v -> Choice(v.labelRu, state.audioSource == v) { onAudioSource(v) } }
            )
            ChoiceSetting(
                title = "Лимит записи",
                hint = "Запись останавливается сама и файл сохраняется, когда время вышло",
                choices = RecordingLimit.entries.map { v -> Choice(v.label, state.recordingLimit == v) { onRecordingLimit(v) } }
            )
            Text(
                text = "Частота кадров, slow-motion (120–960 к/с) и разрешение выбираются на чипах над видоискателем: телефон сам сообщает, какие сочетания умеет.",
                style = MaterialTheme.typography.bodySmall,
                color = NothingSilver
            )
        }

        SettingsSection(title = "Обработка сенсора") {
            ChoiceSetting(
                title = "Антимерцание",
                hint = "Убирает полосы от ламп 50/60 Гц",
                choices = AntiBanding.entries.map { v ->
                    Choice(v.labelRu, state.antiBanding == v, enabled = cameraUnknown || caps.antiBandingModes.contains(v)) { onAntiBanding(v) }
                }
            )
            ChoiceSetting(
                title = "Шумоподавление",
                hint = "«Выкл» оставляет больше фактуры для последующей обработки",
                choices = ProcessingMode.entries.map { v ->
                    Choice(v.labelRu, state.noiseReduction == v, enabled = cameraUnknown || caps.noiseReductionModes.contains(v)) { onNoiseReduction(v) }
                }
            )
            ChoiceSetting(
                title = "Резкость",
                hint = "Программное усиление контуров в JPEG",
                choices = ProcessingMode.entries.map { v ->
                    Choice(v.labelRu, state.edgeMode == v, enabled = cameraUnknown || caps.edgeModes.contains(v)) { onEdgeMode(v) }
                }
            )
            Text(
                text = "Пункты, которых нет у текущей камеры, затемнены. Экспозицию, фокус и баланс белого настраивайте на шкалах под видоискателем.",
                style = MaterialTheme.typography.bodySmall,
                color = NothingSilver
            )
        }

        SettingsSection(title = "Управление") {
            SwitchSetting(
                title = "Звук затвора",
                hint = "Щелчок при снимке и сигнал старта/остановки записи",
                checked = state.shutterSound,
                onToggle = onToggleShutterSound,
                tag = "switch_shutter_sound"
            )
            SwitchSetting(
                title = "Кнопки громкости — затвор",
                hint = "Любая кнопка громкости делает снимок или запускает запись",
                checked = state.volumeKeyShutter,
                onToggle = onToggleVolumeKeys,
                tag = "switch_volume_shutter"
            )
            SwitchSetting(
                title = "Виброотклик",
                hint = "Короткая вибрация на затворе, таймере и фокусе",
                checked = state.hapticsEnabled,
                onToggle = onToggleHaptics,
                tag = "switch_haptics"
            )
            SwitchSetting(
                title = "Не гасить экран",
                hint = "Экран остаётся включённым, пока открыта камера",
                checked = state.keepScreenOn,
                onToggle = onToggleKeepScreenOn,
                tag = "switch_keep_screen_on"
            )
        }

        SettingsSection(title = "О приложении") {
            Text(
                text = "GlyphCam Pro · CameraX 1.5 · Glyph Matrix SDK 2.0",
                style = MaterialTheme.typography.bodyMedium,
                color = NothingWhite
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (cameraUnknown) "Камера ещё не запускалась" else caps.cameraLabel +
                    (if (caps.manualSensor) " · ручная экспозиция" else " · только авто-экспозиция") +
                    (if (caps.manualFocus) " · ручной фокус" else ""),
                style = MaterialTheme.typography.bodySmall,
                color = NothingSilver
            )
        }
    }
}

private class Choice(
    val label: String,
    val selected: Boolean,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    SectionLabel(text = title, modifier = Modifier.padding(top = 6.dp))
    Spacer(modifier = Modifier.height(6.dp))
    NothingCard(modifier = Modifier.fillMaxWidth()) {
        content()
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun ChoiceSetting(title: String, hint: String?, choices: List<Choice>) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = NothingWhite)
        Spacer(modifier = Modifier.height(6.dp))
        FadingScrollRow(modifier = Modifier.fillMaxWidth(), spacing = 6.dp, fade = false) {
            choices.forEach { c ->
                Text(
                    text = c.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = when {
                        c.selected -> NothingBlack
                        !c.enabled -> NothingSilver.copy(alpha = 0.45f)
                        else -> NothingWhite
                    },
                    maxLines = 1,
                    modifier = Modifier
                        .clip(ChipShape)
                        .background(if (c.selected) NothingWhite else NothingSurface)
                        .border(1.dp, if (c.selected) NothingWhite else NothingBorder, ChipShape)
                        .clickable(enabled = c.enabled) { c.onClick() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("setting_${title}_${c.label}")
                )
            }
        }
        if (hint != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = hint, style = MaterialTheme.typography.bodySmall, color = NothingSilver)
        }
    }
}

@Composable
private fun SwitchSetting(title: String, hint: String, checked: Boolean, onToggle: () -> Unit, tag: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ChipShape)
            .clickable { onToggle() }
            .padding(vertical = 6.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = NothingWhite)
            Text(text = hint, style = MaterialTheme.typography.bodySmall, color = NothingSilver)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = NothingBlack,
                checkedTrackColor = NothingRed,
                checkedBorderColor = NothingRed,
                uncheckedThumbColor = NothingSilver,
                uncheckedTrackColor = NothingSurface,
                uncheckedBorderColor = NothingBorder
            )
        )
    }
}
