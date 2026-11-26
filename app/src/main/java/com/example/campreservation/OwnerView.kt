package com.example.campreservation.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.campreservation.Campsite
import com.example.campreservation.CampsiteOwner
import com.example.campreservation.Lottery
import com.example.campreservation.controller.OwnerController

class OwnerView(private val controller: OwnerController) {

    var campsites by mutableStateOf<List<Campsite>>(emptyList())
        private set
    var lotteries by mutableStateOf<List<Lottery>>(emptyList())
        private set
    var selectedCampsite by mutableStateOf<Campsite?>(null)
        private set
    var qrPayload by mutableStateOf<String?>(null)
        private set
    var errors by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var message by mutableStateOf<String?>(null)
        private set

    fun update(model: CampsiteOwner) {
        campsites = model.ownedCampsites
        lotteries = model.lotteries
        selectedCampsite = model.selectedCampsite
        qrPayload = model.selectedQrPayload
        errors = model.errors
        message = model.message
    }

    @Composable
    fun Content() {
        val nav = rememberNavController()
        val snackbar = remember { SnackbarHostState() }
        LaunchedEffect(Unit) { controller.refresh() }
        LaunchedEffect(message) {
            message?.let { snackbar.showSnackbar(it); controller.dismissMessage() }
        }
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
                composable("home") { CampsiteListScreen(nav) }
                composable("lotteries") { LotteryListScreen(nav) }
                composable("lotteryForm") { LotteryFormScreen(nav) }
            }
        }
    }

    @Composable
    private fun CampsiteListScreen(nav: NavController) {
        Column(Modifier.padding(16.dp)) {
            Text("Your campsites")
            LazyColumn {
                items(campsites) { c ->
                    ListItem(
                        headlineContent = { Text(c.campName) },
                        modifier = Modifier.clickable {
                            controller.openCampsite(c.campsiteId)
                            nav.navigate("lotteries")
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun LotteryListScreen(nav: NavController) {
        Column(Modifier.padding(16.dp)) {
            Text(selectedCampsite?.campName ?: "Campsite")
            Button(onClick = { nav.navigate("lotteryForm") }) { Text("New lottery") }
            LazyColumn {
                items(lotteries) { l ->
                    ListItem(
                        headlineContent = { Text("${l.bookingStart} to ${l.bookingEnd}") },
                        supportingContent = { Text("${l.winnerCount} winners") }
                    )
                }
            }
        }
    }

    @Composable
    private fun LotteryFormScreen(nav: NavController) {
        var start by remember { mutableStateOf("") }
        var end by remember { mutableStateOf("") }
        var winners by remember { mutableStateOf("1") }
        LaunchedEffect(Unit) { controller.clearErrors() }

        Column(Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = start, onValueChange = { start = it },
                label = { Text("Booking start (YYYY-MM-DD)") },
                isError = "dates" in errors
            )
            OutlinedTextField(
                value = end, onValueChange = { end = it },
                label = { Text("Booking end (YYYY-MM-DD)") },
                isError = "dates" in errors,
                supportingText = { errors["dates"]?.let { Text(it) } }
            )
            OutlinedTextField(
                value = winners, onValueChange = { winners = it },
                label = { Text("Number of winners") },
                isError = "winnerCount" in errors,
                supportingText = { errors["winnerCount"]?.let { Text(it) } }
            )
            Button(onClick = {
                controller.createLottery(start, end, winners.toIntOrNull() ?: 0) {
                    nav.popBackStack()
                }
            }) { Text("Publish lottery") }
        }
    }
}