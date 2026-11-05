/*
 * OwnerView.kt
 *
 * Purpose: the VIEW for the owner side (CRC card: OwnerView). It is an observer of CampsiteOwner: when the
 * model changes, update() copies what is needed into Compose state, which redraws the screens. The view
 * contains the Android UI; when the user does something it calls the controller and nothing else. It never
 * changes the model itself.
 *
 * MVC roles: "How is the current state displayed?", "Which UI components are shown?" and "Who delegates the
 * user interaction to the controller?" belong here. The screens themselves are in OwnerScreens.kt.
 *
 * Screens: O1 MyCampsitesScreen, O2 CampsiteFormScreen, O3 CampsiteOverviewScreen, O4 LotteryFormScreen,
 * O5 LotteryManageScreen.
 *
 * Connecting (see the notes, "Connecting MVC"):
 *     val model = CampsiteOwner(...); val controller = OwnerController(model)
 *     val view = OwnerView(controller); model.addView(view); controller.refresh.execute()
 *     setContent { view.Content() }
 *
 * OUTSTANDING ISSUES
 *  - The view only copies state from the model; a Firestore snapshot listener would make it real-time.
 */
package com.example.campreservation.view

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.campreservation.controller.OwnerController
import com.example.campreservation.model.Campsite
import com.example.campreservation.model.CampsiteOwner
import com.example.campreservation.model.Lottery
import com.example.campreservation.model.Reservation
import com.example.campreservation.model.ReservationFilter
import com.example.campreservation.model.ValidationError
import com.example.campreservation.model.WaitlistEntry
import com.example.campreservation.mvc.TView
import java.time.LocalDate

class OwnerView(private val controller: OwnerController) : TView<CampsiteOwner> {

    // What the screens display. Compose redraws whenever one of these changes.
    var campsites by mutableStateOf<List<Campsite>>(emptyList())
        private set
    var lotteries by mutableStateOf<List<Lottery>>(emptyList())
        private set
    var selectedCampsite by mutableStateOf<Campsite?>(null)
        private set
    var qrPayload by mutableStateOf<String?>(null)
        private set
    var bookedDates by mutableStateOf<Set<LocalDate>>(emptySet())
        private set
    var reservationLists by mutableStateOf<Map<ReservationFilter, List<Reservation>>>(emptyMap())
        private set
    var selectedLottery by mutableStateOf<Lottery?>(null)
        private set
    var waitlist by mutableStateOf<List<WaitlistEntry>>(emptyList())
        private set
    var errors by mutableStateOf<List<ValidationError>>(emptyList())
        private set
    var message by mutableStateOf<String?>(null)
        private set

    /** Called by the model after every state change. Copies the model's state; never modifies the model. */
    override fun update(model: CampsiteOwner) {
        campsites = model.ownedCampsites
        lotteries = model.ownedLotteries
        selectedCampsite = model.selectedCampsite
        qrPayload = model.selectedQrPayload
        bookedDates = model.bookedDates
        reservationLists = model.reservationsByFilter()
        selectedLottery = model.selectedLottery
        waitlist = model.waitlist
        errors = model.errors
        message = model.message
    }

    /** The owner's whole UI: five screens and the navigation between them. */
    @Composable
    fun Content() {
        val nav = rememberNavController()
        val snackbar = remember { SnackbarHostState() }
        LaunchedEffect(Unit) { controller.refresh.execute() }
        LaunchedEffect(message) {
            message?.let { snackbar.showSnackbar(it); controller.dismissMessage.execute() }
        }
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
                composable("home") {
                    MyCampsitesScreen(
                        campsites = campsites,
                        lotteries = lotteries,
                        onAdd = { controller.clearErrors.execute(); nav.navigate("campsiteForm") },
                        onManage = { id -> controller.openCampsite(id); nav.navigate("campsite") },
                        onOpenLottery = { id -> controller.openLottery(id); nav.navigate("lottery") },
                    )
                }
                composable("campsiteForm") {
                    CampsiteFormScreen(
                        existing = null,
                        errors = errors,
                        onSave = { form -> controller.saveCampsite(form, null) { nav.popBackStack() } },
                        onBack = { controller.clearErrors.execute(); nav.popBackStack() },
                    )
                }
                composable("campsiteEdit") {
                    CampsiteFormScreen(
                        existing = selectedCampsite,
                        errors = errors,
                        onSave = { form -> controller.saveCampsite(form, selectedCampsite?.id) { nav.popBackStack() } },
                        onBack = { controller.clearErrors.execute(); nav.popBackStack() },
                    )
                }
                composable("campsite") {
                    CampsiteOverviewScreen(
                        campsite = selectedCampsite,
                        qrPayload = qrPayload,
                        bookedDates = bookedDates,
                        reservationLists = reservationLists,
                        onEdit = { controller.clearErrors.execute(); nav.navigate("campsiteEdit") },
                        onCreateLottery = { controller.clearErrors.execute(); nav.navigate("lotteryForm") },
                        onBack = { nav.popBackStack() },
                    )
                }
                composable("lotteryForm") {
                    LotteryFormScreen(
                        campsites = campsites,
                        preselectedCampsiteId = selectedCampsite?.id,
                        existing = null,
                        errors = errors,
                        onSave = { form -> controller.saveLottery(form, null) { nav.popBackStack() } },
                        onBack = { controller.clearErrors.execute(); nav.popBackStack() },
                    )
                }
                composable("lotteryEdit") {
                    LotteryFormScreen(
                        campsites = campsites,
                        preselectedCampsiteId = null,
                        existing = selectedLottery,
                        errors = errors,
                        onSave = { form -> controller.saveLottery(form, selectedLottery?.id) { nav.popBackStack() } },
                        onBack = { controller.clearErrors.execute(); nav.popBackStack() },
                    )
                }
                composable("lottery") {
                    LotteryManageScreen(
                        lottery = selectedLottery,
                        waitlist = waitlist.map { it.displayName to it.joinedAt },
                        onEdit = { controller.clearErrors.execute(); nav.navigate("lotteryEdit") },
                        onBack = { nav.popBackStack() },
                    )
                }
            }
        }
    }
}