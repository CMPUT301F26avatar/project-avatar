package com.example.campreservation

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class EntrantLottery (
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val collectionName: String = "entrants",
    private val random: Random = Random.Default
){
    // entrant document id -> name
    private val entrants = linkedMapOf<String, String>()

    /** Replaces the current pool with whatever is in Firestore. */
    suspend fun loadEntrants() {
        val snapshot = db.collection(collectionName).get().await()
        entrants.clear()
        snapshot.documents.forEach { doc ->
            doc.getString("name")?.let { entrants[doc.id] = it }
        }
    }

    fun entrantCount(): Int = entrants.size

    fun withdraw(id: String): Boolean = entrants.remove(id) != null


    fun draw(): Pair<String, String> {
        check(entrants.isNotEmpty()) { "Cannot draw a winner with no entrants." }
        val winner = entrants.entries.elementAt(random.nextInt(entrants.size))
        return winner.key to winner.value
    }

    /** Picks a winner and removes them from the pool (useful for runner-up draws). */
    fun drawAndRemove(): Pair<String, String> {
        val winner = draw()
        entrants.remove(winner.first)
        return winner
    }
}

// Example usage from a coroutine (e.g. viewModelScope.launch { ... })
//suspend fun runLottery() {
//    val lottery = EntrantLottery()
//    lottery.loadEntrants()
//    val (id, name) = lottery.draw()
//    println("Winner: $name ($id)")
//}