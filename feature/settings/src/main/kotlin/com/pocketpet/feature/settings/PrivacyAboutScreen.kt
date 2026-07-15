package com.pocketpet.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pocketpet.core.designsystem.component.PetSectionCard

private data class PrivacyPoint(val title: String, val body: String)

private val privacyPoints = listOf(
    PrivacyPoint(
        "No ads, no tracking",
        "Pocket Pet has no advertising and no analytics enabled by default.",
    ),
    PrivacyPoint(
        "Notifications stay private",
        "If you turn on notification reactions, Pocket Pet only ever sees which app posted a " +
            "notification and when — never the title, text, or any other content.",
    ),
    PrivacyPoint(
        "Accessibility is narrow on purpose",
        "The optional accessibility shortcuts only open the notification shade or quick " +
            "settings. They never read what's on your screen, log keystrokes, or look at " +
            "password fields.",
    ),
    PrivacyPoint(
        "Voice is push-to-talk",
        "The microphone only activates while you're holding the voice button. Nothing is " +
            "recorded to disk, and there's no background listening.",
    ),
    PrivacyPoint(
        "Weather uses no account",
        "Weather comes from Open-Meteo's public API, which needs no account, API key, or " +
            "sign-in from you.",
    ),
)

@Composable
fun PrivacyAboutScreen(appVersionName: String, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = "Privacy", style = MaterialTheme.typography.headlineMedium)
            privacyPoints.forEach { point ->
                PetSectionCard {
                    Text(text = point.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = point.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "About", style = MaterialTheme.typography.headlineMedium)
            PetSectionCard {
                Text(text = "Pocket Pet", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Version $appVersionName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "A small, calm desktop companion for your phone. Built with Kotlin, " +
                        "Jetpack Compose, and a lot of squash-and-stretch.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
