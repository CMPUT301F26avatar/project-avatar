package com.example.campreservation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InboxView(
    private val userId: String,
    private val notifications: NotificationStore,
    private val messages: MessageStore,
    private val service: NotificationService
) : Screen() {

    override val role = UserRole.ENTRANT

    private var items by mutableStateOf<List<Notification>>(emptyList())
    private var inbox by mutableStateOf<List<Message>>(emptyList())
    private var loading by mutableStateOf(true)
    private var status by mutableStateOf<String?>(null)

    override fun refresh() {
        load()
    }

    private fun load() {
        loading = true
        notifications.forRecipient(userId) { result ->
            result
                .onSuccess { found -> items = found }
                .onFailure { e -> status = "Could not load notifications: ${e.message}" }
            loading = false
        }
        messages.forRecipient(userId) { result ->
            result
                .onSuccess { found -> inbox = found }
                .onFailure { e -> status = "Could not load messages: ${e.message}" }
        }
    }

    private fun sendTest() {
        service.sendTest(userId) { result ->
            result
                .onSuccess { delivered ->
                    status = if (delivered) "Test notification sent"
                    else "Notifications are turned off in your profile"
                    load()
                }
                .onFailure { e -> status = "Could not send test notification: ${e.message}" }
        }
    }

    private fun open(n: Notification) {
        if (n.read) return
        notifications.markRead(n.notificationId) { result ->
            result.onSuccess { items = items.map { if (it.notificationId == n.notificationId) it.copy(read = true) else it } }
        }
    }

    private fun open(m: Message) {
        if (m.read) return
        messages.markRead(m.messageId) { result ->
            result.onSuccess { inbox = inbox.map { if (it.messageId == m.messageId) it.copy(read = true) else it } }
        }
    }

    private fun format(time: Long) =
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(time))

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
            Text("Inbox", style = MaterialTheme.typography.headlineMedium)

            Button(
                onClick = { sendTest() },
                modifier = Modifier.fillMaxWidth().testTag("sendTestButton")
            ) { Text("Send test notification") }

            status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            if (loading) CircularProgressIndicator()

            Text("Notifications", style = MaterialTheme.typography.titleLarge)
            if (!loading && items.isEmpty()) Text("No notifications")
            items.forEach { n ->
                Card(onClick = { open(n) }, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            n.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (n.read) FontWeight.Normal else FontWeight.Bold
                        )
                        Text(n.body)
                        Text(format(n.timestamp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Text("Messages", style = MaterialTheme.typography.titleLarge)
            if (!loading && inbox.isEmpty()) Text("No messages")
            inbox.forEach { m ->
                Card(onClick = { open(m) }, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "From ${m.senderId}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (m.read) FontWeight.Normal else FontWeight.Bold
                        )
                        Text(m.text)
                        Text(format(m.timestamp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}