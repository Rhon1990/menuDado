package com.menudado.backend

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

interface MenuDadoBackendSession {
    suspend fun userId(): String?
}

class FirebaseMenuDadoBackendSession(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : MenuDadoBackendSession {
    override suspend fun userId(): String? {
        firebaseAuth.currentUser?.uid?.let { return it }

        val result = firebaseAuth.signInAnonymously().awaitBackendTask()
        return result.user?.uid ?: firebaseAuth.currentUser?.uid
    }
}

object NoOpMenuDadoBackendSession : MenuDadoBackendSession {
    override suspend fun userId(): String? = null
}

internal suspend fun <T> Task<T>.awaitBackendTask(): T {
    return suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener

            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Firebase task failed")
                )
            }
        }
    }
}
