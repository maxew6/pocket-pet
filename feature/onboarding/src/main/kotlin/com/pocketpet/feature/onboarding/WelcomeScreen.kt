package com.pocketpet.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pocketpet.core.designsystem.canvas.PetCanvas
import com.pocketpet.core.designsystem.canvas.rememberPetAnimationController
import com.pocketpet.core.model.Mood
import com.pocketpet.core.model.PetAppearance
import com.pocketpet.core.model.PetState

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val renderState = rememberPetAnimationController(
                state = PetState.HappyDance,
                mood = Mood.Excited,
                reducedMotion = false,
                feedingPulse = false,
                chargingCelebration = false,
            )
            PetCanvas(
                renderState = renderState,
                appearance = PetAppearance(),
                modifier = Modifier.height(160.dp),
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Meet Pocket Pet",
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "A small, calm companion that lives on your screen — not a chatbot, " +
                    "just a little friend who reacts to your day.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(onClick = onGetStarted, modifier = Modifier.fillMaxWidth()) {
                Text("Get started")
            }
        }
    }
}
