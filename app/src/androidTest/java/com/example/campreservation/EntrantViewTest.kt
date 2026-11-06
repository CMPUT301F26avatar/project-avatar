package com.example.campreservation

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EntrantViewTest {

    @get:Rule val rule = createComposeRule()

    // ---------- helpers ----------

    private fun savedProfile() =
        Profile(deviceId = "dev1").also { it.updateInfo("Sam", "sam@example.com", "") }

    private fun profileRepo(existing: Profile?): ProfileRepository {
        val repo = mockk<ProfileRepository>(relaxed = true)
        every { repo.findProfile(any(), any()) } answers {
            secondArg<(Result<Profile?>) -> Unit>().invoke(Result.success(existing))
        }
        every { repo.updateProfile(any(), any()) } answers {
            secondArg<(Result<Unit>) -> Unit>().invoke(Result.success(Unit))
        }
        every { repo.removeProfile(any(), any()) } answers {
            secondArg<(Result<Unit>) -> Unit>().invoke(Result.success(Unit))
        }
        return repo
    }

    private fun campsite(id: String, name: String, max: Int) =
        Campsite(campsiteId = id, campName = name, location = "Banff", maxOccupancy = max)

    private fun campRepo(result: Result<List<Campsite>>): CampsiteRepository {
        val repo = mockk<CampsiteRepository>(relaxed = true)
        every { repo.search(any(), any(), any()) } answers {
            thirdArg<(Result<List<Campsite>>) -> Unit>().invoke(result)
        }
        return repo
    }

    private fun show(
        profiles: ProfileRepository,
        camps: CampsiteRepository = campRepo(Result.success(emptyList()))
    ): EntrantView {
        val view = EntrantView(Entrant("dev1"), profiles, camps)
        rule.setContent { MaterialTheme { view.Content() } }
        rule.waitForIdle()
        return view
    }

    private fun field(label: String) = rule.onNode(hasText(label) and hasSetTextAction())

    // ---------- profile setup ----------

    @Test fun noProfile_showsSetupPage_withoutDeleteButton() {
        show(profileRepo(null))
        rule.onNodeWithText("Set up your profile").assertIsDisplayed()
        rule.onNodeWithText("Delete profile").assertDoesNotExist()
    }

    @Test fun save_withBlankName_showsError() {
        show(profileRepo(null))
        rule.onNodeWithText("Save").performClick()
        rule.onNodeWithText("Name can not be left empty").assertIsDisplayed()
    }

    @Test fun save_withBlankEmail_showsError() {
        show(profileRepo(null))
        field("Name").performTextInput("Sam")
        rule.onNodeWithText("Save").performClick()
        rule.onNodeWithText("Email can not be left empty").assertIsDisplayed()
    }

    @Test fun save_withInvalidEmail_showsError_andDoesNotWrite() {
        val repo = profileRepo(null)
        show(repo)
        field("Name").performTextInput("Sam")
        field("Email").performTextInput("not-an-email")
        rule.onNodeWithText("Save").performClick()
        rule.onNodeWithText("Enter a valid email address").assertIsDisplayed()
        verify(exactly = 0) { repo.updateProfile(any(), any()) }
    }

    @Test fun save_withValidInput_writesOnce_andGoesToHome() {
        val repo = profileRepo(null)
        show(repo)
        field("Name").performTextInput("Sam")
        field("Email").performTextInput("sam@example.com")
        rule.onNodeWithText("Save").performClick()
        rule.waitForIdle()
        verify(exactly = 1) { repo.updateProfile(any(), any()) }
        rule.onNodeWithText("Welcome, Sam").assertIsDisplayed()
    }

    @Test fun save_whenRepositoryFails_showsError_andStaysOnProfile() {
        val repo = profileRepo(null)
        every { repo.updateProfile(any(), any()) } answers {
            secondArg<(Result<Unit>) -> Unit>().invoke(Result.failure(Exception("offline")))
        }
        show(repo)
        field("Name").performTextInput("Sam")
        field("Email").performTextInput("sam@example.com")
        rule.onNodeWithText("Save").performClick()
        rule.onNodeWithText("Could not save profile: offline").assertIsDisplayed()
        rule.onNodeWithText("Set up your profile").assertIsDisplayed()
    }

    @Test fun launch_neverWritesAProfile() {
        val repo = profileRepo(null)
        show(repo)
        verify(exactly = 0) { repo.updateProfile(any(), any()) }
        verify(exactly = 0) { repo.addProfile(any()) }
    }

    // ---------- home ----------

    @Test fun existingProfile_showsHomeWithWelcome() {
        show(profileRepo(savedProfile()))
        rule.onNodeWithText("Welcome, Sam").assertIsDisplayed()
        rule.onNodeWithText("My profile").assertIsDisplayed()
    }

    @Test fun myProfile_opensProfile_andCancelReturnsHome() {
        show(profileRepo(savedProfile()))
        rule.onNodeWithText("My profile").performClick()
        rule.onNodeWithText("My Profile").assertIsDisplayed()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Welcome, Sam").assertIsDisplayed()
    }

    @Test fun loadFailure_showsMessage() {
        val repo = profileRepo(null)
        every { repo.findProfile(any(), any()) } answers {
            secondArg<(Result<Profile?>) -> Unit>().invoke(Result.failure(Exception("boom")))
        }
        show(repo)
        rule.onNodeWithText("Could not load profile: boom").assertIsDisplayed()
    }

    // ---------- delete ----------

    private fun openDeleteDialog(repo: ProfileRepository) {
        show(repo)
        rule.onNodeWithText("My profile").performClick()
        rule.onNodeWithText("Delete profile").performClick()
    }

    @Test fun delete_showsConfirmationDialog() {
        openDeleteDialog(profileRepo(savedProfile()))
        rule.onNodeWithText("Delete your profile?").assertIsDisplayed()
    }

    @Test fun delete_cancel_keepsProfile() {
        val repo = profileRepo(savedProfile())
        openDeleteDialog(repo)
        rule.onNode(hasText("Cancel") and hasAnyAncestor(isDialog())).performClick()
        rule.onNodeWithText("Delete your profile?").assertDoesNotExist()
        verify(exactly = 0) { repo.removeProfile(any(), any()) }
    }

    @Test fun delete_confirm_removesProfile_andReturnsToSetup() {
        val repo = profileRepo(savedProfile())
        openDeleteDialog(repo)
        rule.onNodeWithText("Delete").performClick()
        rule.waitForIdle()
        verify(exactly = 1) { repo.removeProfile(any(), any()) }
        rule.onNodeWithText("Profile deleted").assertIsDisplayed()
        rule.onNodeWithText("Set up your profile").assertIsDisplayed()
    }

    // ---------- search ----------

    @Test fun search_showsResults() {
        val camps = campRepo(Result.success(listOf(campsite("1", "Lakeview", 6), campsite("2", "Pinewood", 2))))
        show(profileRepo(savedProfile()), camps)
        rule.onNodeWithText("Search").performScrollTo().performClick()
        rule.onNodeWithText("Lakeview").assertExists()
        rule.onNodeWithText("Pinewood").assertExists()
    }

    @Test fun search_guestsFilter_hidesSmallCampsites() {
        val camps = campRepo(Result.success(listOf(campsite("1", "Lakeview", 6), campsite("2", "Pinewood", 2))))
        show(profileRepo(savedProfile()), camps)
        field("Guests").performTextInput("4")
        rule.onNodeWithText("Search").performScrollTo().performClick()
        rule.onNodeWithText("Lakeview").assertExists()
        rule.onNodeWithText("Pinewood").assertDoesNotExist()
    }

    @Test fun search_noMatches_showsEmptyMessage() {
        show(profileRepo(savedProfile()), campRepo(Result.success(emptyList())))
        rule.onNodeWithText("Search").performScrollTo().performClick()
        rule.onNodeWithText("No campsites found").assertExists()
    }

    @Test fun search_failure_showsError() {
        show(profileRepo(savedProfile()), campRepo(Result.failure(Exception("x"))))
        rule.onNodeWithText("Search").performScrollTo().performClick()
        rule.onNodeWithText("Could not search campsites").assertExists()
    }

    @Test fun search_unratedCampsite_showsNoRatings() {
        val camps = campRepo(Result.success(listOf(campsite("1", "Lakeview", 6))))
        show(profileRepo(savedProfile()), camps)
        rule.onNodeWithText("Search").performScrollTo().performClick()
        rule.onNodeWithText("No ratings").assertExists()
    }

    // ---------- navigation ----------

    @Test fun tappingCampsiteCard_navigatesToCampView() {
        val camps = campRepo(Result.success(listOf(campsite("c1", "Lakeview", 6))))
        var route: String? = null
        val view = EntrantView(Entrant("dev1"), profileRepo(savedProfile()), camps)
        view.onNavigate = { route = it }
        rule.setContent { MaterialTheme { view.Content() } }

        rule.onNodeWithText("Search").performScrollTo().performClick()
        rule.onNodeWithText("Lakeview").performClick()
        assertEquals("campView/c1", route)
    }
}