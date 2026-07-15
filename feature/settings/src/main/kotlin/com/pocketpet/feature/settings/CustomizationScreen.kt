package com.pocketpet.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketpet.core.designsystem.canvas.PetCanvas
import com.pocketpet.core.designsystem.canvas.rememberPetAnimationController
import com.pocketpet.core.designsystem.canvas.toFurColor
import com.pocketpet.core.designsystem.component.PetSectionCard
import com.pocketpet.core.model.Accessory
import com.pocketpet.core.model.ColorTone
import com.pocketpet.core.model.Mood
import com.pocketpet.core.model.PetAppearance
import com.pocketpet.core.model.PetState

@Composable
fun CustomizationScreen(modifier: Modifier = Modifier, viewModel: CustomizationViewModel = hiltViewModel()) {
    val preferences by viewModel.preferences.collectAsState()
    val appearance = preferences.appearance

    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(text = "Customize", style = MaterialTheme.typography.headlineMedium)

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val renderState = rememberPetAnimationController(
                    state = PetState.Sitting,
                    mood = Mood.Happy,
                    reducedMotion = appearance.reducedMotion,
                    feedingPulse = false,
                    chargingCelebration = false,
                )
                PetCanvas(renderState = renderState, appearance = appearance, modifier = Modifier.height(140.dp))
            }

            PetSectionCard {
                Text(text = "Name", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = appearance.name,
                    onValueChange = viewModel::setName,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            PetSectionCard {
                Text(text = "Color", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ColorTone.entries.forEach { tone ->
                        ColorSwatch(
                            color = tone.toFurColor(),
                            selected = tone == appearance.colorTone,
                            onClick = { viewModel.setColorTone(tone) },
                        )
                    }
                }
            }

            PetSectionCard {
                Text(text = "Accessory", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Accessory.entries.forEach { accessory ->
                        AccessoryChip(
                            label = accessory.name,
                            selected = accessory == appearance.accessory,
                            onClick = { viewModel.setAccessory(accessory) },
                        )
                    }
                }
            }

            PetSectionCard {
                LabeledSlider(
                    label = "Size",
                    value = appearance.scale,
                    range = PetAppearance.MIN_SCALE..PetAppearance.MAX_SCALE,
                    onValueChange = viewModel::setScale,
                )
                Spacer(modifier = Modifier.height(16.dp))
                LabeledSlider(
                    label = "Opacity",
                    value = appearance.opacity,
                    range = 0.3f..1f,
                    onValueChange = viewModel::setOpacity,
                )
                Spacer(modifier = Modifier.height(16.dp))
                LabeledSlider(
                    label = "Animation speed",
                    value = appearance.animationSpeedMultiplier,
                    range = PetAppearance.MIN_SPEED..PetAppearance.MAX_SPEED,
                    onValueChange = viewModel::setAnimationSpeed,
                )
            }

            PetSectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Reduced motion", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Calms down the pet's animations.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = appearance.reducedMotion, onCheckedChange = viewModel::setReducedMotion)
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (selected) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    )
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
    )
}

@Composable
private fun AccessoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}
