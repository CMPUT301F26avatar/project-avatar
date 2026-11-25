package com.example.campreservation
//so that I can push for pull request
import com.example.campreservation.model.Owner

/**
 * What Admin needs from the image feature (US 03.03.01, 03.06.01).
 * The team's ImageRepository should implement this.
 */
interface ImageSource {
    fun getAll(onResult: (Result<List<Image>>) -> Unit)

    /** Deletes the file from Storage and the image record. Admin clears the campsite/profile reference itself. */
    fun delete(image: Image, onResult: (Result<Unit>) -> Unit)
}

/**
 * A read-only log (US 03.08.01, 03.12.01).
 * NotificationLog and MessageLog should implement this.
 */
interface LogSource<T> {
    fun getAll(onResult: (Result<List<T>>) -> Unit)
}

/**
 * The administrator (CRC card: Admin).
 *
 * An admin can also be an entrant and an owner with their own admin profile (US 03.11.01), so Admin
 * implements both [EntrantInterface] and [Owner] by delegating to an [Entrant] and a [CampsiteOwner]
 * that share the admin's device id. Anything those two can do, Admin can do, and when either interface
 * grows, Admin picks it up automatically.
 *
 * The moderation features are Admin's own:
 *  - browse and remove campsites            (US 03.04.01, 03.01.01)
 *  - browse and remove profiles             (US 03.05.01, 03.02.01)
 *  - remove campsite owners                 (US 03.07.01)
 *  - browse and remove images               (US 03.06.01, 03.03.01)
 *  - review notification and message logs   (US 03.08.01, 03.12.01)
 *
 * Wiring example (MainActivity):
 *   val admin = Admin(
 *       adminId = deviceId,
 *       entrant = Entrant(entrantId = deviceId),
 *       owner = CampsiteOwner(ownerId = deviceId, campsites = campsiteRepo),
 *       campsites = campsiteRepo, profileRepo = profileRepo,
 *       images = imageRepo, notificationLog = notificationLog, messageLog = messageLog,
 *   )
 *
 * NOTE: this only decides what the app offers. Real protection comes from Firestore Security Rules,
 * because anyone can modify an app on their own phone.
 */
class Admin(
    val adminId: String,
    private val entrant: Entrant,
    private val owner: CampsiteOwner,
    private val campsites: CampsiteRepository,
    private val profileRepo: ProfileRepository,
    private val images: ImageSource,
    //private val notificationLog: LogSource<Notification>,   // needs the Notification class
    //private val messageLog: LogSource<Message>               // needs the Message class
) : EntrantInterface by entrant, Owner by owner {

    init {
        // One admin profile: the entrant side and the owner side must be the same device
        require(entrant.entrantId == adminId && owner.ownerId == adminId) {
            "Admin's entrant and owner must use the admin's id"
        }
    }

    val role = UserRole.ADMIN

    /** For AdminView's "act as entrant" and "act as owner" switch (US 03.11.01): reuse the existing views. */
    val entrantSide: Entrant get() = entrant
    val ownerSide: CampsiteOwner get() = owner

    // ---- campsites (US 03.04.01, 03.01.01) --------------------------------------------------------------

    fun browseCampsites(onResult: (Result<List<Campsite>>) -> Unit) = campsites.getAll(onResult)

    fun removeCampsite(campsiteId: String, onDone: (Result<Unit>) -> Unit = {}) =
        campsites.delete(campsiteId, onDone)

    // ---- profiles and owners (US 03.05.01, 03.02.01, 03.07.01) ------------------------------------------

    /** Live list of every profile. */
    fun browseProfiles(): List<Profile> = profileRepo.getProfiles().toList()

    /** Removes a profile. An owner's profile also takes down their campsites, so no listing is left unmanaged. */
    fun removeProfile(profile: Profile, onDone: (Result<Unit>) -> Unit = {}) {
        if (profile.role == UserRole.OWNER.name) removeOwner(profile.deviceId, onDone)
        else profileRepo.removeProfile(profile, onDone)
    }

    /** Removes an owner who violated app policy: all of their campsites, then their profile. */
    fun removeOwner(ownerId: String, onDone: (Result<Unit>) -> Unit = {}) {
        campsites.getByOwner(ownerId) { found ->
            found.onFailure { onDone(Result.failure(it)); return@getByOwner }
            found.getOrNull()?.forEach { campsites.delete(it.campsiteId) {} }

            profileRepo.findProfile(ownerId) { lookup ->
                lookup
                    .onSuccess { profile ->
                        if (profile == null) {
                            onDone(Result.failure(IllegalArgumentException("No profile found for owner $ownerId")))
                        } else {
                            profileRepo.removeProfile(profile, onDone)
                        }
                    }
                    .onFailure { onDone(Result.failure(it)) }
            }
        }
    }

    // ---- images (US 03.06.01, 03.03.01) -----------------------------------------------------------------

    fun browseImages(onResult: (Result<List<Image>>) -> Unit) = images.getAll(onResult)

    /** Deletes the image, then clears the link to it so no campsite or profile keeps a broken picture. */
    fun removeImage(image: Image, onDone: (Result<Unit>) -> Unit = {}) {
        images.delete(image) { deleted ->
            if (deleted.isFailure) {
                onDone(deleted)
                return@delete
            }
            clearReference(image, onDone)
        }
    }

    private fun clearReference(image: Image, onDone: (Result<Unit>) -> Unit) {
        when {
            image.isProfileImage() -> {
                profileRepo.removeProfileImage(image.ownerRefId)
                onDone(Result.success(Unit))
            }
            image.isCampsiteImage() -> campsites.getById(image.ownerRefId) { lookup ->
                lookup
                    .onSuccess { camp ->
                        if (camp == null) onDone(Result.success(Unit))
                        else campsites.save(camp.copy(imageUrl = null), onDone)
                    }
                    .onFailure { onDone(Result.failure(it)) }
            }
            else -> onDone(Result.success(Unit))
        }
    }

    // ---- logs (US 03.08.01, 03.12.01) -------------------------------------------------------------------

    //fun reviewNotifications(onResult: (Result<List<Notification>>) -> Unit) = notificationLog.getAll(onResult)

    //fun reviewMessages(onResult: (Result<List<Message>>) -> Unit) = messageLog.getAll(onResult)
}