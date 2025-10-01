package com.example.myhealth.presentation.screen.mind

import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MindScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Mindfulness", style = MaterialTheme.typography.h6)
        Divider()
        Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("No mindfulness records in this sample.", style = MaterialTheme.typography.body1)
                Text("Consider tracking breathing sessions or meditation minutes via a future module.", style = MaterialTheme.typography.body2)
            }
        }
    }
}
