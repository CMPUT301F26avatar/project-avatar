package com.example.campreservation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class UserRole { ENTRANT, OWNER, ADMIN }

@Composable
fun CampView(
    campsite: Campsite,
    role: UserRole,
    lotteryActive: Boolean,
    isOnWaitlist: Boolean,
    onJoinWaitlist: () -> Unit,
    onLeaveWaitlist: () -> Unit,
    onEditCampsite: () -> Unit,
    onManageLottery: () -> Unit,
    onRemoveCampsite: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedButton(onClick = onBack) { Text("Back") }

        // TODO: show campsite.imageUrl here

        Text(campsite.campName)
        Text("Price: $${campsite.pricePerNight} per night")
        Text("Max occupancy: ${campsite.maxOccupancy}")
        Text("Location: ${campsite.location}")
        Text(campsite.descr)

        if (campsite.tags.isNotEmpty()) {
            Text("Tags: ${campsite.tags.joinToString(", ")}")
        }

        Text("Rules: ${campsite.rules}")
        Text("Cancellation policy: ${campsite.cancellationPolicy}")
        
        // Entrants: join or leave waitlist, only if a lottery is active
        if (role == UserRole.ENTRANT && lotteryActive) {
            if (isOnWaitlist) {
                Button(onClick = onLeaveWaitlist) { Text("Leave waitlist") }
            } else {
                Button(onClick = onJoinWaitlist) { Text("Join waitlist") }
            }
        }

        // Owners: edit campsite and manage lottery
        if (role == UserRole.OWNER) {
            Button(onClick = onEditCampsite) { Text("Edit campsite") }
            Button(onClick = onManageLottery) { Text("Manage lottery") }
        }

        // Admins: remove campsite
        if (role == UserRole.ADMIN) {
            Button(onClick = onRemoveCampsite) { Text("Remove campsite") }
        }
    }
}