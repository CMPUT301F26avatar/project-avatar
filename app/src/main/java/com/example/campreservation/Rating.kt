package com.example.campreservation

/**
 * One entrant's rating of one campsite.
 *
 * Stored under campsites/{campsiteId}/ratings/{entrantId}. Using the entrant's id as the
 * document id means rating the same campsite twice overwrites the old rating, so nobody
 * can rate a campsite more than once.
 *
 * Defaults on every property let Firestore deserialize it.
 *
 * @property ratingId document id; equals [entrantId]
 * @property campsiteId the campsite being rated
 * @property entrantId device id of the person who rated
 * @property stars whole number from 1 to 5
 * @property comment optional text, at most [MAX_COMMENT] characters
 * @property createdAt time the rating was made, in milliseconds
 */
data class Rating(
    val ratingId: String = "",
    val campsiteId: String = "",
    val entrantId: String = "",
    val stars: Int = 0,
    val comment: String = "",
    val createdAt: Long = 0
)