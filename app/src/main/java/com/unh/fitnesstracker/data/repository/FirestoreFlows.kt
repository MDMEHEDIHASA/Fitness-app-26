package com.unh.fitnesstracker.data.repository

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Emits the mapped query results now and on every change, including local writes not yet synced. */
internal fun <T> Query.observe(map: (QuerySnapshot) -> List<T>): Flow<List<T>> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        if (snapshot != null) trySend(map(snapshot))
    }
    awaitClose { registration.remove() }
}

/** Emits the mapped document now and on every change, including after a local write. */
internal fun <T> DocumentReference.observe(map: (DocumentSnapshot) -> T?): Flow<T?> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        if (snapshot != null) trySend(map(snapshot))
    }
    awaitClose { registration.remove() }
}

/**
 * Firestore applies a write to its local cache immediately but only completes the Task once the
 * server confirms it, which never happens offline. Workout data must not block the UI on that,
 * so these writes are fire-and-forget and failures are only logged.
 */
internal fun Task<Void>.logFailure(what: String) {
    addOnFailureListener { Log.w("Firestore", "Could not $what", it) }
}
