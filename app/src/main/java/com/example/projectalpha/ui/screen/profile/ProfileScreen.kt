package com.example.projectalpha.ui.screen.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.projectalpha.viewmodel.ProfileViewModel // Import placeholder

@Composable
fun ProfileScreen(profileViewModel: ProfileViewModel) {
    val uiState by profileViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Profile Screen", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Username: ${uiState.username}")
        Text("Email: ${uiState.email}")
        Text("Current Streak: ${uiState.currentStreak}")
        Text("Tasks This Month: ${uiState.tasksCompletedThisMonth}")

        if (uiState.isLoading) {
            Text("Loading...")
        }

        uiState.error?.let {
            Text("Error: $it", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
            Button(onClick = { profileViewModel.clearError() }) { Text("Dismiss") }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { profileViewModel.updateUsername("New Mock Name") }) {
            Text("Update Username (Mock)")
        }
        Button(onClick = { profileViewModel.refreshData() }) {
            Text("Refresh Data (Mock)")
        }
    }
}
