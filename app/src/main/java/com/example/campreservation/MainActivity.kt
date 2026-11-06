package com.example.campreservation

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.campreservation.ui.theme.CampReservationTheme
//import com.example.campreservation.controller.OwnerController
//import com.example.campreservation.view.OwnerView

//to push
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // US 01.09.01: the device identifies the user, no username/password
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val profiles = ProfileRepository()
        val campsites = CampsiteRepository()

        setContent {
            CampReservationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AppRoot(deviceId, profiles, campsites)
                    }
                }
            }
        }
    }
}

/**
 * Decides what to show:
 *  - checking Firestore -> spinner
 *  - no saved profile   -> role choice (each role's screen then shows its own sign-up form)
 *  - saved profile      -> the screens for the role stored in that profile
 */
@Composable
fun AppRoot(deviceId: String, profiles: ProfileRepository, campsites: CampsiteRepository) {
    var checking by remember { mutableStateOf(true) }
    var role by remember { mutableStateOf<UserRole?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        profiles.findProfile(deviceId) { result ->
            result
                .onSuccess { found ->
                    role = found?.let { p -> runCatching { UserRole.valueOf(p.role) }.getOrNull() }
                }
                .onFailure { error = "Could not reach the database: ${it.message}" }
            checking = false
        }
    }

    when {
        checking -> CircularProgressIndicator()

        error != null -> Text(error ?: "")

        role == null -> RoleChoiceScreen { chosen -> role = chosen }

        role == UserRole.OWNER -> {
            val screen = remember {
                OwnerScreen(ownerId = deviceId, profiles = profiles, campsites = campsites)
                    .also { it.onChangeRole = { role = null } }
            }
            screen.Content()
        }

        // ENTRANT (ADMIN has no screens yet, so it also lands here)
        else -> {
            val screen = remember {
                EntrantView(Entrant(entrantId = deviceId), profiles, campsites)
                    .also { it.onChangeRole = { role = null } }
            }
            screen.Content()
        }
    }
}

@Composable
fun RoleChoiceScreen(onChoose: (UserRole) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Campsite Reservations", style = MaterialTheme.typography.headlineMedium)
        Text("How will you use the app?")
        Button(onClick = { onChoose(UserRole.ENTRANT) }, modifier = Modifier.fillMaxWidth()) {
            Text("I want to find a campsite")
        }
        Button(onClick = { onChoose(UserRole.OWNER) }, modifier = Modifier.fillMaxWidth()) {
            Text("I own a campsite")
        }
    }
}
