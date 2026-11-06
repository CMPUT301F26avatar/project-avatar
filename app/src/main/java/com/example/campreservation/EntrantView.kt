package com.example.campreservation

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EntrantView(
    private val entrant: Entrant,
    private val profiles: ProfileRepository,
    private val campsites: CampsiteRepository
) : Screen() {

    override val role = UserRole.ENTRANT

    private var loading by mutableStateOf(true)
    private var message by mutableStateOf<String?>(null)
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
        profiles.getOrCreateProfile(entrant.entrantId) { result ->
            result
                .onSuccess { entrant.profile = it; showProfile() }
                .onFailure { message = "Could not load profile: ${it.message}" }
            loading = false
        }
    }

    private fun save() {
        runCatching { entrant.profile.updateInfo(name, email, phone) }
            .onSuccess {
                entrant.profile.notificationPreference(notificationsOn)
                profiles.updateProfile(entrant.profile)
                message = "Profile saved"
            }
            .onFailure { message = it.message }
    }

    private fun search() {
        searching = true
        entrant.searchCampsites(query.trim(), campsites) { result ->
            result
                .onSuccess { found ->
                    val minGuests = guests.toIntOrNull()
                    results = found.map { it.toRow() }
                        .filter { minGuests == null || it.maxOccupancy >= minGuests }
                    searched = true
                }
                .onFailure { message = "Could not search campsites" }
            searching = false
        }
    }

    @Composable
    override fun Content() {
        LaunchedEffect(Unit) { load() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SearchSection()

            Text("My Profile", style = MaterialTheme.typography.headlineMedium)

            if (loading) {
                CircularProgressIndicator()
            } else {
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
                    label = { Text("Phone number") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Receive notifications", modifier = Modifier.weight(1f))
                    Switch(checked = notificationsOn, onCheckedChange = { notificationsOn = it })
                }
                Button(onClick = { save() }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
            }

            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    }

    @Composable
    private fun SearchSection() {

        Text("Find a campsite", style = MaterialTheme.typography.headlineMedium)
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
            onClick = { navigate("campView/${row.id}") },
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
    id = campName,
    name = campName,
    locationSummary = location.toString(),
    maxOccupancy = maxOccupancy,
    averageRating = 0.0,
    imageUrl = imageUrl
)