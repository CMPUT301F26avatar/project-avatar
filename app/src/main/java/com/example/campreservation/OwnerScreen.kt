package com.example.campreservation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

/**
 * Everything a campsite owner sees:
 *  PROFILE  - sign-up / edit profile / delete profile (same profile data as entrants, role = OWNER)
 *  HOME     - welcome, "Register a campsite", and the owner's own campsites (US 02.03.01)
 *  REGISTER - the registration form (US 02.01.01, 02.01.02)
 * The owner is "logged in" by device id, the same way entrants are.
 */
class OwnerScreen(
    private val ownerId: String,
    private val profiles: ProfileRepository,
    private val campsites: CampsiteRepository
) : Screen() {

    private enum class Page { PROFILE, HOME, REGISTER }

    override val role = UserRole.OWNER

    private var page by mutableStateOf(Page.PROFILE)
    private var profile = Profile(deviceId = ownerId, role = UserRole.OWNER.name)
    private var profileExists by mutableStateOf(false)
    private var loading by mutableStateOf(true)
    private var message by mutableStateOf<String?>(null)
    private var confirmingDelete by mutableStateOf(false)

    // profile form
    private var name by mutableStateOf("")
    private var email by mutableStateOf("")
    private var phone by mutableStateOf("")
    private var notificationsOn by mutableStateOf(true)

    // campsite form
    private var campName by mutableStateOf("")
    private var location by mutableStateOf("")
    private var descr by mutableStateOf("")
    private var occupancy by mutableStateOf("")
    private var price by mutableStateOf("")
    private var rules by mutableStateOf("")
    private var cancellation by mutableStateOf("")
    private var tags by mutableStateOf("")

    private var myCampsites by mutableStateOf<List<Campsite>>(emptyList())

    override fun refresh() {
        load()
    }

    // ---------- loading ----------

    private fun showProfile() {
        name = profile.name
        email = profile.email
        phone = profile.phoneNumber
        notificationsOn = profile.notificationsEnabled
    }

    private fun load() {
        loading = true
        profiles.findProfile(ownerId) { result ->
            result
                .onSuccess { found ->
                    if (found != null) {
                        profile = found
                        profileExists = true
                        page = Page.HOME
                        loadCampsites()
                    } else {
                        profile = Profile(deviceId = ownerId, role = UserRole.OWNER.name)
                        profileExists = false
                        page = Page.PROFILE
                    }
                    showProfile()
                }
                .onFailure { message = "Could not load profile: ${it.message}" }
            loading = false
        }
    }

    private fun loadCampsites() {
        campsites.getByOwner(ownerId) { result ->
            result
                .onSuccess { myCampsites = it }
                .onFailure { message = "Could not load your campsites: ${it.message}" }
        }
    }

    // ---------- profile ----------

    private fun validateProfile(): String? {
        if (name.isBlank()) return "Name can not be left empty"
        if (email.isBlank()) return "Email can not be left empty"
        if (!email.trim().matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))) return "Enter a valid email address"
        return null
    }

    private fun saveProfile() {
        val problem = validateProfile()
        if (problem != null) {
            message = problem
            return
        }
        runCatching { profile.updateInfo(name.trim(), email.trim(), phone.trim()) }
            .onSuccess {
                profile.notificationPreference(notificationsOn)
                profile.role = UserRole.OWNER.name   // this is what makes the app open the owner screens next time
                profiles.updateProfile(profile) { result ->
                    result
                        .onSuccess {
                            profileExists = true
                            message = null
                            page = Page.HOME
                            loadCampsites()
                        }
                        .onFailure { message = "Could not save profile: ${it.message}" }
                }
            }
            .onFailure { message = it.message }
    }

    private fun deleteProfile() {
        confirmingDelete = false
        // Take the owner's listings down too, so entrants can't find campsites nobody manages
        //campsites.getByOwner(ownerId) { result ->
          //  result.onSuccess { list -> list.forEach { campsites.delete(it.campsiteId) {} } }
        //}
        profiles.removeProfile(profile) { result ->
            result
                .onSuccess { onChangeRole() }   // back to the role choice screen
                .onFailure { message = "Could not delete profile: ${it.message}" }
        }
    }

    // ---------- campsites ----------

    private fun clearCampsiteForm() {
        campName = ""; location = ""; descr = ""; occupancy = ""
        price = ""; rules = ""; cancellation = ""; tags = ""
    }

    private fun registerCampsite() {
        // The form gives text, so check the numbers before building a Campsite
        val maxGuests = occupancy.trim().toIntOrNull() ?: run {
            message = "Max occupancy must be a whole number"; return
        }
        val nightly = price.trim().toDoubleOrNull() ?: run {
            message = "Price must be a number like 65 or 65.50"; return
        }

        val campsite = Campsite(
            ownerId = ownerId,
            campName = campName.trim(),
            descr = descr.trim(),
            location = location.trim(),
            maxOccupancy = maxGuests,
            pricePerNight = nightly,
            rules = rules.trim(),
            cancellationPolicy = cancellation.trim(),
            tags = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        )

        // save() runs campsite.validationError() first (blank name, blank location, ...)
        campsites.save(campsite) { result ->
            result
                .onSuccess {
                    message = "Registered \"${campsite.campName}\""
                    clearCampsiteForm()
                    loadCampsites()
                    page = Page.HOME
                }
                .onFailure { message = it.message }
        }
    }

    // ---------- UI ----------

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
            when {
                loading -> CircularProgressIndicator()
                page == Page.PROFILE -> ProfileSection()
                page == Page.HOME -> HomeSection()
                else -> RegisterSection()
            }

            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }

        if (confirmingDelete) {
            AlertDialog(
                onDismissRequest = { confirmingDelete = false },
                title = { Text("Delete your profile?") },
                text = { Text("This permanently deletes your profile and all of your campsite listings. This can't be undone.") },
                confirmButton = { TextButton(onClick = { deleteProfile() }) { Text("Delete") } },
                dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") } }
            )
        }
    }

    @Composable
    private fun HomeSection() {
        Text("Welcome, ${profile.name}", style = MaterialTheme.typography.headlineMedium)
        Button(
            onClick = { message = null; page = Page.REGISTER },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Register a campsite") }
        OutlinedButton(
            onClick = { message = null; showProfile(); page = Page.PROFILE },
            modifier = Modifier.fillMaxWidth()
        ) { Text("My profile") }

        Text("My campsites", style = MaterialTheme.typography.titleLarge)
        if (myCampsites.isEmpty()) {
            Text("You haven't registered any campsites yet")
        }
        myCampsites.forEach { camp ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(camp.campName, style = MaterialTheme.typography.titleMedium)
                    Text(camp.location)
                    Text("Up to ${camp.maxOccupancy} guests - $${camp.pricePerNight} per night")
                }
            }
        }
    }

    @Composable
    private fun ProfileSection() {
        Text(
            if (profileExists) "My Profile" else "Set up your owner profile",
            style = MaterialTheme.typography.headlineMedium
        )
        Text("Device ID: $ownerId", style = MaterialTheme.typography.bodySmall)
        Field("Name", name) { name = it }
        Field("Email", email) { email = it }
        Field("Phone number (optional)", phone) { phone = it }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Receive notifications", modifier = Modifier.weight(1f))
            Switch(checked = notificationsOn, onCheckedChange = { notificationsOn = it })
        }
        Button(onClick = { saveProfile() }, modifier = Modifier.fillMaxWidth()) { Text("Save") }

        if (profileExists) {
            OutlinedButton(
                onClick = { message = null; showProfile(); page = Page.HOME },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Cancel") }
            OutlinedButton(
                onClick = { confirmingDelete = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Delete profile") }
        } else {
            OutlinedButton(
                onClick = { onChangeRole() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Change role") }
        }
    }

    @Composable
    private fun RegisterSection() {
        Text("Register a campsite", style = MaterialTheme.typography.headlineMedium)
        Field("Campsite name", campName) { campName = it }
        Field("Location (town or area)", location) { location = it }
        Field("Description / amenities", descr, singleLine = false) { descr = it }
        Field("Max occupancy", occupancy, KeyboardType.Number) { occupancy = it }
        Field("Price per night ($)", price, KeyboardType.Decimal) { price = it }
        Field("Rules", rules, singleLine = false) { rules = it }
        Field("Cancellation policy", cancellation, singleLine = false) { cancellation = it }
        Field("Tags (comma separated)", tags) { tags = it }

        Button(onClick = { registerCampsite() }, modifier = Modifier.fillMaxWidth()) { Text("Publish") }
        OutlinedButton(
            onClick = { message = null; page = Page.HOME },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Cancel") }
    }

    @Composable
    private fun Field(
        label: String,
        value: String,
        keyboardType: KeyboardType = KeyboardType.Text,
        singleLine: Boolean = true,
        onChange: (String) -> Unit
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth()
        )
    }
}