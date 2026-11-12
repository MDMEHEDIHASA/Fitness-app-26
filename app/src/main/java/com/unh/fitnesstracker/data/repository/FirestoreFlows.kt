package com.unh.fitnesstracker.data.repository

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Emits the mapped query results now and on every change, including local writes not yet synced. */
internal fun <T> Query.observe(map: (QuerySnapshot) -> List<T>): Flow<List<T>> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            Log.w(TAG, "Query listener failed: ${error.toFirestoreMessage()}", error)
            close(error)
            return@addSnapshotListener
        }
        if (snapshot != null) {
            val mapped = runCatching { map(snapshot) }.getOrElse {
                Log.w(TAG, "Could not map query snapshot", it)
                emptyList()
            }
            trySend(mapped)
        }
    }
    awaitClose { registration.remove() }
}

/** Emits the mapped document now and on every change, including after a local write. */
internal fun <T> DocumentReference.observe(map: (DocumentSnapshot) -> T?): Flow<T?> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            Log.w(TAG, "Document listener failed: ${error.toFirestoreMessage()}", error)
            close(error)
            return@addSnapshotListener
        }
        if (snapshot != null) {
            val mapped = runCatching { map(snapshot) }.getOrElse {
                Log.w(TAG, "Could not map document ${snapshot.id}", it)
                null
            }
            trySend(mapped)
        }
    }
    awaitClose { registration.remove() }
}

internal fun <T : Any> DocumentSnapshot.toData(type: Class<T>): T? =
    runCatching { toObject(type) }
        .onFailure { Log.w(TAG, "Could not parse ${type.simpleName} ($id)", it) }
        .getOrNull()

/**
 * Firestore applies a write to its local cache immediately but only completes the Task once the
 * server confirms it, which never happens offline. Workout data must not block the UI on that,
 * so these writes are fire-and-forget and failures are only logged.
 */
internal fun Task<Void>.logFailure(what: String) {
    addOnFailureListener { Log.w(TAG, "Could not $what: ${it.toFirestoreMessage()}", it) }
}

internal fun Throwable.isFirestoreBlocked(): Boolean {
    val firestore = this as? FirebaseFirestoreException ?: return false
    return firestore.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
        firestore.code == FirebaseFirestoreException.Code.UNAUTHENTICATED
}

internal fun Throwable.toFirestoreMessage(): String {
    val firestore = this as? FirebaseFirestoreException
    return when (firestore?.code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Firestore blocked this read. In Firebase Console → Firestore → Rules, publish the rules from firestore.rules."
        FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
            "Firestore needs an index for this query. Open the link in Logcat, or deploy docs/firestore.indexes.json."
        FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Couldn't reach Firestore. Check your connection and try again."
        else -> localizedMessage ?: "Couldn't load data. Check your connection and try again."
    }
}

private const val TAG = "Firestore"
