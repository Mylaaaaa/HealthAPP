package com.example.myhealth.presentation.screen.nutrition

import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NutritionScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Nutrition", style = MaterialTheme.typography.h6)
        Divider()
        Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("No nutrition records available in this sample.", style = MaterialTheme.typography.body1)
                Text("You can add this later via Health Connect nutrition APIs.", style = MaterialTheme.typography.body2)
            }
        }
    }
}
