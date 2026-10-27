package com.bytechnum.reveil.ui.ringing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bytechnum.reveil.R
import com.bytechnum.reveil.ringing.RingingUiState
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun RingingScreen(state: RingingUiState, onStopProvisional: () -> Unit) {
    val time by produceState(LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(1_000)
        }
    }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primary) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = time.format(timeFormatter), style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(16.dp))
            Text(
                text = state.label.ifBlank { stringResource(R.string.ringing_default_message) },
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(64.dp))
            Button(
                onClick = onStopProvisional,
                modifier = Modifier.fillMaxWidth().height(72.dp),
            ) {
                Text(
                    text = stringResource(R.string.ringing_stop_provisional),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}
