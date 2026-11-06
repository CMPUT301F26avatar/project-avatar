package com.example.campreservation

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

class EntrantView(
    private val entrant: Entrant,
    private val profiles: ProfileRepository,
    private val campsites: CampsiteRepository
) : Screen() {

    private enum class Page { PROFILE, HOME }

    override val role = UserRole.ENTRANT

    private var page by mutableStateOf(Page.PROFILE)
    private var profileExists by mutableStateOf(false)
    private var loading by mutableStateOf(true)
    private var message by mutableStateOf<String?>(null)
    private var confirmingDelete by mutableStateOf(false)
    private var selected by mutableStateOf<Campsite?>(null)
    private var campsiteById by mutableStateOf<Map<String, Campsite>>(emptyMap())

    private var name by mutableStateOf("")
    private var email by mutableStateOf("")
    private var phone by mutableStateOf("")
    private var notificationsOn by mutableStateOf(true)

    private var query by mutableStateOf("")
    private var guests by mutableStateOf("")
    private var results by mutableStateOf<List<CampsiteRow>>(emptyList())
    private var searching by mutableStateOf(false)
    private var searched by mutableStateOf(false)

    override fun refresh() {
        load()
    }

    private fun showProfile() {
        val p = entrant.profile
        name = p.name
        email = p.email
        phone = p.phoneNumber
        notificationsOn = p.notificationsEnabled
    }

    private fun load() {
        loading = true
        profiles.findProfile(entrant.entrantId) { result ->
            result
                .onSuccess { found ->
                    if (found != null) {
                        entrant.profile = found
                        profileExists = true
                        page = Page.HOME
                    } else {
                        entrant.profile = Profile(deviceId = entrant.entrantId)
                        profileExists = false
                        page = Page.PROFILE
                    }
                    showProfile()
                }
                .onFailure { message = "Could not load profile: ${it.message}" }
            loading = false
        }
    }

    private fun validate(): String? {
        if (name.isBlank()) return "Name can not be left empty"
        if (email.isBlank()) return "Email can not be left empty"
        if (!email.trim().matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))) return "Enter a valid email address"
        return null
    }

    private fun save() {
        val problem = validate()
        if (problem != null) {
            message = problem
            return
        }
        runCatching { entrant.profile.updateInfo(name.trim(), email.trim(), phone.trim()) }
            .onSuccess {
                entrant.profile.notificationPreference(notificationsOn)
                entrant.profile.role = UserRole.ENTRANT.name
                profiles.updateProfile(entrant.profile) { result ->
                    result
                        .onSuccess {
                            profileExists = true
                            message = null
                            page = Page.HOME
                        }
                        .onFailure { message = "Could not save profile: ${it.message}" }
                }
            }
            .onFailure { message = it.message }
    }

    private fun deleteProfile() {
        confirmingDelete = false
        profiles.removeProfile(entrant.profile) { result ->
            result
                .onSuccess {
                    entrant.profile = Profile(deviceId = entrant.entrantId)
                    showProfile()
                    profileExists = false
                    results = emptyList()
                    searched = false
                    message = "Profile deleted"
                    page = Page.PROFILE
                    onChangeRole()
                }
                .onFailure { message = "Could not delete profile: ${it.message}" }
        }
    }

    private fun search() {
        searching = true

        entrant.searchCampsites(query.trim(), campsites, guests.toIntOrNull()) { result ->
        result
                .onSuccess { found ->
                    //val minGuests = guests.toIntOrNull()
                    //val shown = found.filter { minGuests == null || it.maxOccupancy >= minGuests }
                    campsiteById = found.associateBy { it.campsiteId }
                    results = found.map { it.toRow() }
                    searched = true
                }
                .onFailure { message = "Could not search campsites" }
            searching = false
        }
    }

    @Composable
    override fun Content() {
        LaunchedEffect(Unit) { load() }
        val open = selected
        if (open != null) {
            CampView(
                campsite = open, role = UserRole.ENTRANT,
                lotteryActive = false, isOnWaitlist = false,
                onJoinWaitlist = {}, onLeaveWaitlist = {},
                onEditCampsite = {}, onManageLottery = {}, onRemoveCampsite = {},
                onBack = { selected = null }
            )
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (loading) {
                CircularProgressIndicator()
            } else if (page == Page.PROFILE) {
                ProfileSection()
            } else {
                HomeSection()
            }

            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }

        if (confirmingDelete) {
            AlertDialog(
                onDismissRequest = { confirmingDelete = false },
                title = { Text("Delete your profile?") },
                text = { Text("This permanently deletes your profile and photo. This can't be undone.") },
                confirmButton = {
                    TextButton(onClick = { deleteProfile() }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") }
                }
            )
        }
    }

    @Composable
    private fun HomeSection() {
        Text("Welcome, ${entrant.profile.name}", style = MaterialTheme.typography.headlineMedium)
        OutlinedButton(
            onClick = { message = null; showProfile(); page = Page.PROFILE },
            modifier = Modifier.fillMaxWidth()
        ) { Text("My profile") }
        SearchSection()
    }

    @Composable
    private fun ProfileSection() {
        Text(
            if (profileExists) "My Profile" else "Set up your profile",
            style = MaterialTheme.typography.headlineMedium
        )
        Text("Device ID: ${entrant.entrantId}", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Name") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = phone, onValueChange = { phone = it },
            label = { Text("Phone number (optional)") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Receive notifications", modifier = Modifier.weight(1f))
            Switch(checked = notificationsOn, onCheckedChange = { notificationsOn = it })
        }
        Button(onClick = { save() }, modifier = Modifier.fillMaxWidth()) { Text("Save") }

        if (profileExists) {
            OutlinedButton(
                onClick = { message = null; showProfile(); page = Page.HOME },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Cancel") }
            OutlinedButton(
                onClick = { confirmingDelete = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Delete profile") }

        }
        else {
            OutlinedButton(
                onClick = { onChangeRole() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Change role") }}
    }

    @Composable
    private fun SearchSection() {
        Text("Find a campsite", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            label = { Text("Location") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = guests, onValueChange = { guests = it },
            label = { Text("Guests") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { search() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Search") }

        if (searching) {
            CircularProgressIndicator()
        } else if (searched && results.isEmpty()) {
            Text("No campsites found")
        }
        results.forEach { row -> CampsiteCard(row) }
    }

    @Composable
    private fun CampsiteCard(row: CampsiteRow) {
        Card(
            onClick = { selected = campsiteById[row.id] },
            modifier = Modifier.fillMaxWidth()

        ) {
            Row(modifier = Modifier.padding(12.dp)) {
                ShowImage(row.imageUrl, Modifier.size(80.dp))
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(row.name, style = MaterialTheme.typography.titleMedium)
                    Text(row.locationSummary)
                    Text("Max ${row.maxOccupancy} guests")
                    Text(if (row.averageRating > 0) "%.1f".format(row.averageRating) else "No ratings")
                }
            }
        }
    }
}

data class CampsiteRow(
    val id: String,
    val name: String,
    val locationSummary: String,
    val maxOccupancy: Int,
    val averageRating: Double,
    val imageUrl: String? = null
)

fun Campsite.toRow() = CampsiteRow(
    id = campsiteId,
    name = campName,
    locationSummary = location.toString(),
    maxOccupancy = maxOccupancy,
    averageRating = 0.0,
    imageUrl = imageUrl
)