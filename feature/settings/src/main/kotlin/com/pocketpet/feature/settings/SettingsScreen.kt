package com.pocketpet.feature.settings

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketpet.core.designsystem.component.PetSectionCard
import com.pocketpet.core.model.AppTheme

@Composable
fun SettingsScreen(
    onOpenPrivacyAbout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val preferences by viewModel.preferences.collectAsState()
    val context = LocalContext.current
    val permissionStatus = viewModel.permissionStatus()

    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)

            PetSectionCard {
                Text(text = "Theme", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    AppTheme.entries.forEachIndexed { index, theme ->
                        SegmentedButton(
                            selected = preferences.theme == theme,
                            onClick = { viewModel.setTheme(theme) },
                            shape = SegmentedButtonDefaults.itemShape(index, AppTheme.entries.size),
                        ) {
                            Text(theme.name)
                        }
                    }
                }
            }

            PetSectionCard {
                SettingsToggleRow(
                    title = "Edge snapping",
                    description = "The pet settles against the nearest screen edge after you let go.",
                    checked = preferences.edgeSnappingEnabled,
                    onCheckedChange = viewModel::setEdgeSnapping,
                )
                Spacer(modifier = Modifier.height(16.dp))
                SettingsToggleRow(
                    title = "Lock position",
                    description = "Stops the pet from being dragged around.",
                    checked = preferences.positionLocked,
                    onCheckedChange = viewModel::setPositionLocked,
                )
            }

            PetSectionCard {
                Text(text = "Quiet hours", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "No speech bubbles or lively animation between " +
                        "${formatHour(preferences.quietHours.startHour)} and " +
                        "${formatHour(preferences.quietHours.endHour)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = preferences.quietHours.enabled,
                        onCheckedChange = viewModel::setQuietHoursEnabled,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Enabled")
                }
            }

            PermissionLinkCard(
                title = "Notification reactions",
                description = "Lets the pet glance up when a notification arrives. It never " +
                    "reads notification content.",
                masterEnabled = preferences.notificationReactionsEnabled,
                onMasterToggle = viewModel::setNotificationReactionsEnabled,
                systemGranted = permissionStatus.notificationListenerGranted,
                onOpenSystemSettings = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
            )

            PermissionLinkCard(
                title = "Accessibility shortcuts",
                description = "Lets the pet open the notification shade or quick settings for " +
                    "you. It never reads screen content or keystrokes.",
                masterEnabled = preferences.accessibilityFeaturesEnabled,
                onMasterToggle = viewModel::setAccessibilityFeaturesEnabled,
                systemGranted = permissionStatus.accessibilityGranted,
                onOpenSystemSettings = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
            )

            PermissionLinkCard(
                title = "Voice commands",
                description = "Push-to-talk only — the microphone is never on in the background.",
                masterEnabled = preferences.voiceCommandsEnabled,
                onMasterToggle = viewModel::setVoiceCommandsEnabled,
                systemGranted = permissionStatus.microphoneGranted,
                onOpenSystemSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(android.net.Uri.fromParts("package", context.packageName, null)),
                    )
                },
            )

            PetSectionCard {
                Text(text = "Battery optimization", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Android or your phone's manufacturer may still pause the overlay to " +
                        "save battery. You can ask it not to, but this isn't guaranteed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }) {
                    Text("Open battery settings")
                }
            }

            TextButton(onClick = onOpenPrivacyAbout, modifier = Modifier.fillMaxWidth()) {
                Text("Privacy & About")
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PermissionLinkCard(
    title: String,
    description: String,
    masterEnabled: Boolean,
    onMasterToggle: (Boolean) -> Unit,
    systemGranted: Boolean,
    onOpenSystemSettings: () -> Unit,
) {
    PetSectionCard {
        SettingsToggleRow(title = title, description = description, checked = masterEnabled, onCheckedChange = onMasterToggle)
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (systemGranted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (systemGranted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (systemGranted) "Enabled in system settings" else "Not yet enabled in system settings",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpenSystemSettings) { Text("Open") }
        }
    }
}

private fun formatHour(hour: Int): String {
    val period = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "$displayHour:00 $period"
}
