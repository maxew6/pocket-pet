package com.pocketpet.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ColumnScope.weight
import androidx.compose.foundation.layout.RowScope.weight
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketpet.core.designsystem.canvas.PetCanvas
import com.pocketpet.core.designsystem.canvas.rememberPetAnimationController
import com.pocketpet.core.designsystem.component.PetSectionCard
import com.pocketpet.core.designsystem.component.PetStatBar
import com.pocketpet.core.designsystem.theme.Honey
import com.pocketpet.core.designsystem.theme.SoftCoral
import com.pocketpet.core.model.PetState

@Composable
fun HomeScreen(
    onOpenCustomization: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.snapshot.collectAsState()
    val preferences by viewModel.preferences.collectAsState()

    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = preferences.appearance.name, style = MaterialTheme.typography.headlineMedium)

            PetSectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val renderState = rememberPetAnimationController(
                        state = snapshot.state,
                        mood = snapshot.mood,
                        reducedMotion = preferences.appearance.reducedMotion,
                        feedingPulse = false,
                        chargingCelebration = false,
                    )
                    PetCanvas(
                        renderState = renderState,
                        appearance = preferences.appearance,
                        modifier = Modifier.height(96.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = snapshot.mood.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = snapshot.state.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                PetStatBar(label = "Hunger", value = 1f - snapshot.needs.hunger, color = SoftCoral)
                Spacer(modifier = Modifier.height(10.dp))
                PetStatBar(label = "Energy", value = snapshot.needs.energy, color = Honey)
            }

            PetSectionCard {
                Text(text = "Overlay", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (preferences.overlayEnabled) {
                        "Pocket Pet is showing over your other apps."
                    } else if (!viewModel.hasOverlayPermission()) {
                        "Grant the overlay permission from Settings to turn this on."
                    } else {
                        "Pocket Pet is hidden right now."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = viewModel::toggleOverlay,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = viewModel.hasOverlayPermission(),
                ) {
                    Icon(
                        imageVector = if (preferences.overlayEnabled) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (preferences.overlayEnabled) "Hide overlay" else "Show overlay")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton(
                    label = "Feed",
                    icon = Icons.Filled.Restaurant,
                    onClick = viewModel::feed,
                    modifier = Modifier.weight(1f),
                )
                ActionButton(
                    label = "Play",
                    icon = Icons.Filled.PlayArrow,
                    onClick = viewModel::play,
                    modifier = Modifier.weight(1f),
                )
                ActionButton(
                    label = if (snapshot.state == PetState.Sleeping) "Wake" else "Sleep",
                    icon = if (snapshot.state == PetState.Sleeping) Icons.Filled.WbSunny else Icons.Filled.Bedtime,
                    onClick = viewModel::toggleSleep,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onOpenCustomization, modifier = Modifier.weight(1f)) {
                    Text("Customize")
                }
                Button(onClick = onOpenSettings, modifier = Modifier.weight(1f)) {
                    Text("Settings")
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(onClick = onClick, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = label)
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
