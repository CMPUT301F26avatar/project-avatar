package com.example.campreservation.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.campreservation.Admin
import com.example.campreservation.Campsite
import com.example.campreservation.Image
import com.example.campreservation.Profile
import com.example.campreservation.Screen
import com.example.campreservation.UserRole

/**
 * VIEW for the administrator (CRC: AdminView).
 *
 *  - Browse and remove campsites, profiles (owners included) and images (US 03.01 - 03.07)
 *  - Review the notification and message logs (US 03.08.01, 03.12.01)
 *  - Switch to "act as entrant" or "act as owner" with the admin's own profile (US 03.11.01). Those two
 *    modes simply show the existing EntrantView and OwnerView, built on the admin's entrant and owner sides.
 *
 * Every removal asks for confirmation first. The view only calls [Admin]; it never touches repositories.
 */
class AdminView(
    private val admin: Admin,
    private val entrantScreen: Screen,
    private val ownerScreen: Screen
) : Screen() {

    private enum class Page { HOME, CAMPSITES, PROFILES, IMAGES, NOTIFICATIONS, MESSAGES, AS_ENTRANT, AS_OWNER }

    /** A removal waiting for the admin to confirm. */
    private class Pending(val title: String, val text: String, val action: () -> Unit)

    override val role = UserRole.ADMIN

    private var page by mutableStateOf(Page.HOME)
    private var loading by mutableStateOf(false)
    private var message by mutableStateOf<String?>(null)
    private var pending by mutableStateOf<Pending?>(null)

    private var campsites by mutableStateOf<List<Campsite>>(emptyList())
    private var images by mutableStateOf<List<Image>>(emptyList())
    private var logLines by mutableStateOf<List<String>>(emptyList())

    init {
        // If the admin deletes their own profile from inside an embedded screen, go back to the role screen
        entrantScreen.onChangeRole = { this@AdminView.onChangeRole() }
        ownerScreen.onChangeRole = { this@AdminView.onChangeRole() }
    }

    override fun refresh() {
        open(page)
    }

    // ---------- navigation and loading ----------

    private fun open(next: Page) {
        message = null
        page = next
        when (next) {
            Page.CAMPSITES -> loadCampsites()
            Page.IMAGES -> loadImages()
            //Page.NOTIFICATIONS -> loadNotifications()
            //Page.MESSAGES -> loadMessages()
            else -> {}   // profiles are a live list; HOME and the embedded screens load nothing here
        }
    }

    private fun loadCampsites() {
        loading = true
        admin.browseCampsites { result ->
            result
                .onSuccess { campsites = it }
                .onFailure { message = "Could not load campsites: ${it.message}" }
            loading = false
        }
    }

    private fun loadImages() {
        loading = true
        admin.browseImages { result ->
            result
                .onSuccess { images = it }
                .onFailure { message = "Could not load images: ${it.message}" }
            loading = false
        }
    }

    // The log entries are shown with toString(); make Notification and Message data classes so it prints their fields
    /*private fun loadNotifications() {
        loading = true
        logLines = emptyList()
        admin.reviewNotifications { result ->
            result
                .onSuccess { list -> logLines = list.map { entry -> entry.toString() } }
                .onFailure { message = "Could not load notifications: ${it.message}" }
            loading = false
        }
    }*/

    /*private fun loadMessages() {
        loading = true
        logLines = emptyList()
        admin.reviewMessages { result ->
            result
                .onSuccess { list -> logLines = list.map { entry -> entry.toString() } }
                .onFailure { message = "Could not load messages: ${it.message}" }
            loading = false
        }
    }*/

    // ---------- removals (each runs only after the admin confirms) ----------

    private fun ask(title: String, text: String, action: () -> Unit) {
        pending = Pending(title, text, action)
    }

    private fun removeCampsite(camp: Campsite) {
        admin.removeCampsite(camp.campsiteId) { result ->
            result
                .onSuccess { message = "Removed \"${camp.campName}\""; loadCampsites() }
                .onFailure { message = "Could not remove campsite: ${it.message}" }
        }
    }

    private fun removeProfile(profile: Profile) {
        admin.removeProfile(profile) { result ->
            result
                .onSuccess { message = "Removed ${profile.name.ifBlank { "profile" }}" }
                .onFailure { message = "Could not remove profile: ${it.message}" }
        }
    }

    private fun removeImage(image: Image) {
        admin.removeImage(image) { result ->
            result
                .onSuccess { message = "Image removed"; loadImages() }
                .onFailure { message = "Could not remove image: ${it.message}" }
        }
    }

    // ---------- UI ----------

    @Composable
    override fun Content() {
        BackHandler(enabled = page != Page.HOME) { open(Page.HOME) }

        when (page) {
            // The embedded screens scroll by themselves, so they must not sit inside another scrolling column
            Page.AS_ENTRANT -> Embedded(entrantScreen)
            Page.AS_OWNER -> Embedded(ownerScreen)
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (page) {
                    Page.HOME -> HomeSection()
                    Page.CAMPSITES -> CampsitesSection()
                    Page.PROFILES -> ProfilesSection()
                    Page.IMAGES -> ImagesSection()
                    Page.NOTIFICATIONS -> LogSection("Notification log")
                    Page.MESSAGES -> LogSection("Message log")
                    else -> {}
                }
                message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            }
        }

        pending?.let { p ->
            AlertDialog(
                onDismissRequest = { pending = null },
                title = { Text(p.title) },
                text = { Text(p.text) },
                confirmButton = { TextButton(onClick = { pending = null; p.action() }) { Text("Remove") } },
                dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } }
            )
        }
    }

    @Composable
    private fun HomeSection() {
        Text("Administrator", style = MaterialTheme.typography.headlineMedium)
        Text("Moderate the app", style = MaterialTheme.typography.titleMedium)
        MenuButton("Browse campsites") { open(Page.CAMPSITES) }
        MenuButton("Browse profiles") { open(Page.PROFILES) }
        MenuButton("Browse images") { open(Page.IMAGES) }
        //MenuButton("Notification log") { open(Page.NOTIFICATIONS) }
        //MenuButton("Message log") { open(Page.MESSAGES) }

        Text("Use the app myself", style = MaterialTheme.typography.titleMedium)
        MenuButton("Act as an entrant") { open(Page.AS_ENTRANT) }
        MenuButton("Act as a campsite owner") { open(Page.AS_OWNER) }
    }

    @Composable
    private fun CampsitesSection() {
        Header("Campsites")
        if (loading) CircularProgressIndicator()
        else if (campsites.isEmpty()) Text("No campsites")

        campsites.forEach { camp ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(camp.campName, style = MaterialTheme.typography.titleMedium)
                    Text("${camp.location} - up to ${camp.maxOccupancy} guests")
                    Text("Owner: ${camp.ownerId}", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = {
                        ask(
                            "Remove \"${camp.campName}\"?",
                            "The listing disappears for everyone. This can't be undone."
                        ) { removeCampsite(camp) }
                    }) { Text("Remove campsite") }
                }
            }
        }
    }

    @Composable
    private fun ProfilesSection() {
        Header("Profiles")
        // Live list: it updates by itself when a profile is added or removed
        val all = admin.browseProfiles()
        if (all.isEmpty()) Text("No profiles")

        all.forEach { p ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(p.name.ifBlank { "(no name)" }, style = MaterialTheme.typography.titleMedium)
                    Text("${p.email} - ${p.role}")
                    Text("Device ID: ${p.deviceId}", style = MaterialTheme.typography.bodySmall)

                    if (p.deviceId == admin.adminId) {
                        Text("This is you")
                    } else {
                        val isOwner = p.role == UserRole.OWNER.name
                        OutlinedButton(onClick = {
                            ask(
                                if (isOwner) "Remove owner ${p.name}?" else "Remove profile ${p.name}?",
                                if (isOwner) "Their profile and all of their campsites will be deleted. This can't be undone."
                                else "Their profile and photo will be deleted. This can't be undone."
                            ) { removeProfile(p) }
                        }) { Text(if (isOwner) "Remove owner" else "Remove profile") }
                    }
                }
            }
        }
    }

    @Composable
    private fun ImagesSection() {
        Header("Images")
        if (loading) CircularProgressIndicator()
        else if (images.isEmpty()) Text("No uploaded images")

        images.forEach { img ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ShowImage(img.url, Modifier.size(80.dp))
                    Column(
                        modifier = Modifier.padding(start = 12.dp).weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            if (img.isProfileImage()) "Profile image" else "Campsite image",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("Uploaded by ${img.uploaderId}", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = {
                            ask("Remove this image?", "The picture is deleted and no longer shown anywhere.") {
                                removeImage(img)
                            }
                        }) { Text("Remove image") }
                    }
                }
            }
        }
    }

    @Composable
    private fun LogSection(title: String) {
        Header(title)
        if (loading) CircularProgressIndicator()
        else if (logLines.isEmpty()) Text("Nothing logged yet")

        logLines.forEach { line ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(line, modifier = Modifier.padding(12.dp))
            }
        }
    }

    @Composable
    private fun Embedded(screen: Screen) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedButton(
                onClick = { open(Page.HOME) },
                modifier = Modifier.padding(start = 16.dp, top = 8.dp)
            ) { Text("Back to admin menu") }
            Box(modifier = Modifier.weight(1f)) { screen.Content() }
        }
    }

    @Composable
    private fun Header(title: String) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = { open(Page.HOME) }) { Text("Back") }
            Text(title, style = MaterialTheme.typography.headlineSmall)
        }
    }

    @Composable
    private fun MenuButton(label: String, onClick: () -> Unit) {
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label) }
    }
}