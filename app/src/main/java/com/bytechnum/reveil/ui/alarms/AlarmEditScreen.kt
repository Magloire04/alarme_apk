package com.bytechnum.reveil.ui.alarms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bytechnum.reveil.R
import com.bytechnum.reveil.domain.model.Alarm
import com.bytechnum.reveil.domain.model.ChallengeType
import com.bytechnum.reveil.domain.model.Difficulty
import com.bytechnum.reveil.domain.model.SoundId
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(onDone: () -> Unit, viewModel: AlarmEditViewModel = hiltViewModel()) {
    val alarm by viewModel.state.collectAsStateWithLifecycle()
    var isDeleteDialogShown by remember { mutableStateOf(false) }
    val soundPreview = rememberSoundPreview()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (viewModel.isNew) R.string.edit_new_title else R.string.edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(
                innerPadding,
            ).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Recréé au chargement d'une alarme existante pour partir de son heure.
            key(alarm.id) {
                val timeState = rememberTimePickerState(alarm.hour, alarm.minute, is24Hour = true)
                LaunchedEffect(timeState.hour, timeState.minute) { viewModel.setTime(timeState.hour, timeState.minute) }
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(timeState) }
            }
            DaysSelector(selectedDays = alarm.days, onToggle = viewModel::toggleDay)
            OutlinedTextField(
                value = alarm.label,
                onValueChange = viewModel::setLabel,
                label = { Text(stringResource(R.string.label_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SoundSelector(
                selected = alarm.soundId,
                onSelect = viewModel::setSound,
                onListen = { soundPreview.play(alarm.soundId) },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.vibration_label), modifier = Modifier.weight(1f))
                Switch(checked = alarm.hasVibration, onCheckedChange = viewModel::setVibration)
            }
            ChallengeSelector(alarm = alarm, viewModel = viewModel)
            Button(
                onClick = { viewModel.save(onDone) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text(stringResource(R.string.save), style = MaterialTheme.typography.titleMedium) }
            if (!viewModel.isNew) {
                TextButton(onClick = { isDeleteDialogShown = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (isDeleteDialogShown) {
        AlertDialog(
            onDismissRequest = { isDeleteDialogShown = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(onDone) }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { isDeleteDialogShown = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun DaysSelector(selectedDays: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.days_label), style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DayOfWeek.entries.forEach { day ->
                val isSelected = day in selectedDays
                val colors = MaterialTheme.colorScheme
                Surface(
                    onClick = { onToggle(day) },
                    shape = CircleShape,
                    color = if (isSelected) colors.primary else colors.surfaceVariant,
                    contentColor = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                    modifier = Modifier.size(44.dp).semantics { contentDescription = day.fullName() },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(day.letter(), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SoundSelector(selected: SoundId, onSelect: (SoundId) -> Unit, onListen: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.sound_label), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SoundId.entries.forEach { sound ->
                FilterChip(
                    selected = sound == selected,
                    onClick = { onSelect(sound) },
                    label = { Text(stringResource(sound.labelRes())) },
                )
            }
            TextButton(onClick = onListen) { Text(stringResource(R.string.listen)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ChallengeSelector(alarm: Alarm, viewModel: AlarmEditViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.challenge_label), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChallengeType.entries.forEach { challenge ->
                FilterChip(
                    selected = challenge == alarm.challengeType,
                    onClick = { viewModel.setChallenge(challenge) },
                    label = { Text(stringResource(challenge.labelRes())) },
                )
            }
        }
        Text(stringResource(R.string.target_label, alarm.challengeTarget), style = MaterialTheme.typography.titleMedium)
        Slider(
            value = alarm.challengeTarget.toFloat(),
            onValueChange = { viewModel.setTarget(it.toInt()) },
            valueRange = Alarm.MIN_CHALLENGE_TARGET.toFloat()..Alarm.MAX_CHALLENGE_TARGET.toFloat(),
            steps = Alarm.MAX_CHALLENGE_TARGET - Alarm.MIN_CHALLENGE_TARGET - 1,
        )
        Text(stringResource(R.string.difficulty_label), style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            Difficulty.entries.forEachIndexed { index, difficulty ->
                SegmentedButton(
                    selected = difficulty == alarm.difficulty,
                    onClick = { viewModel.setDifficulty(difficulty) },
                    shape = SegmentedButtonDefaults.itemShape(index, Difficulty.entries.size),
                ) { Text(stringResource(difficulty.labelRes())) }
            }
        }
    }
}

private fun SoundId.labelRes(): Int = when (this) {
    SoundId.SOFT -> R.string.sound_soft
    SoundId.CLASSIC -> R.string.sound_classic
    SoundId.STRIDENT -> R.string.sound_strident
}

private fun ChallengeType.labelRes(): Int = when (this) {
    ChallengeType.JUMP -> R.string.challenge_jump
    ChallengeType.SQUAT -> R.string.challenge_squat
    ChallengeType.MARCH -> R.string.challenge_march
    ChallengeType.JACK -> R.string.challenge_jack
    ChallengeType.RANDOM -> R.string.challenge_random
}

private fun Difficulty.labelRes(): Int = when (this) {
    Difficulty.EASY -> R.string.difficulty_easy
    Difficulty.NORMAL -> R.string.difficulty_normal
    Difficulty.HARD -> R.string.difficulty_hard
}
