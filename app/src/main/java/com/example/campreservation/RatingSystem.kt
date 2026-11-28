package com.example.campreservation

class RatingSystem(
    private val repository: RatingRepository = RatingRepository(),
) {
    /** Loads all ratings of one campsite, newest first. */
    fun load(campsiteId: String, onDone: (Result<List<Rating>>) -> Unit) {
        repository.getForCampsite(campsiteId) { result ->
            onDone(result.map { list -> list.sortedByDescending { it.createdAt } })
        }
    }

    /** Validates and saves the entrant's rating. Failure carries a field-to-message map in the message text. */
    fun submit(
        entrantId: String, campsiteId: String, stars: Int, comment: String,
        onDone: (Result<Rating>) -> Unit
    ) {
        val rating = Rating(entrantId, campsiteId, entrantId, stars, comment.trim(), System.currentTimeMillis())
        val problems = validate(rating)
        if (problems.isNotEmpty()) {
            onDone(Result.failure(IllegalArgumentException(problems.values.joinToString("; "))))
            return
        }
        repository.save(rating) { saved -> onDone(saved.map { rating }) }
    }

    companion object {
        const val MIN_STARS = 1
        const val MAX_STARS = 5
        const val MAX_COMMENT = 500

        fun average(list: List<Rating>): Double? =
            if (list.isEmpty()) null else list.sumOf { it.stars }.toDouble() / list.size

        fun validate(r: Rating): Map<String, String> = buildMap {
            if (r.campsiteId.isBlank()) put("campsite", "Choose a campsite to rate")
            if (r.entrantId.isBlank()) put("entrant", "Missing entrant")
            if (r.stars !in MIN_STARS..MAX_STARS) put("stars", "Choose $MIN_STARS to $MAX_STARS stars")
            if (r.comment.length > MAX_COMMENT) put("comment", "Comment must be $MAX_COMMENT characters or fewer")
        }
    }
}