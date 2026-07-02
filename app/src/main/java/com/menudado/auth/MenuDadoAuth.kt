package com.menudado.auth

import android.content.Context
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.menudado.backend.awaitBackendTask

data class MenuDadoAuthSession(
    val userId: String,
    val email: String?,
    val isAnonymous: Boolean
)

enum class MenuDadoRegisterStrategy {
    LINK_ANONYMOUS_USER,
    CREATE_EMAIL_USER
}

interface MenuDadoAuthStore {
    fun isGuestModeSelected(): Boolean
    fun markGuestModeSelected()
    fun clearGuestModeSelected()
}

class SharedPreferencesMenuDadoAuthStore(context: Context) : MenuDadoAuthStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun isGuestModeSelected(): Boolean {
        return preferences.getBoolean(KEY_GUEST_MODE_SELECTED, false)
    }

    override fun markGuestModeSelected() {
        preferences.edit()
            .putBoolean(KEY_GUEST_MODE_SELECTED, true)
            .apply()
    }

    override fun clearGuestModeSelected() {
        preferences.edit()
            .remove(KEY_GUEST_MODE_SELECTED)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "menu-dado-auth"
        const val KEY_GUEST_MODE_SELECTED = "guest_mode_selected"
    }
}

interface MenuDadoAuthService {
    fun currentSession(): MenuDadoAuthSession?
    suspend fun continueAsGuest(): MenuDadoAuthSession
    suspend fun registerWithEmail(email: String, password: String): MenuDadoAuthSession
    suspend fun signInWithEmail(email: String, password: String): MenuDadoAuthSession
    suspend fun signInWithGoogleIdToken(idToken: String): MenuDadoAuthSession
    suspend fun signOut()
}

class FirebaseMenuDadoAuthService(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val authStore: MenuDadoAuthStore
) : MenuDadoAuthService {
    override fun currentSession(): MenuDadoAuthSession? {
        return firebaseAuth.currentUser?.toMenuDadoAuthSession()
    }

    override suspend fun continueAsGuest(): MenuDadoAuthSession {
        val user = firebaseAuth.currentUser ?: firebaseAuth.signInAnonymously().awaitBackendTask().user
        authStore.markGuestModeSelected()
        return requireNotNull(user ?: firebaseAuth.currentUser) {
            "MenuDado guest session is unavailable"
        }.toMenuDadoAuthSession()
    }

    override suspend fun registerWithEmail(email: String, password: String): MenuDadoAuthSession {
        val normalizedEmail = email.trim()
        val currentUser = firebaseAuth.currentUser
        val user = when (registerStrategyFor(currentUser?.toMenuDadoAuthSession())) {
            MenuDadoRegisterStrategy.LINK_ANONYMOUS_USER -> {
                val credential = EmailAuthProvider.getCredential(normalizedEmail, password)
                requireNotNull(currentUser).linkWithCredential(credential).awaitBackendTask().user
            }
            MenuDadoRegisterStrategy.CREATE_EMAIL_USER -> {
                firebaseAuth.createUserWithEmailAndPassword(normalizedEmail, password).awaitBackendTask().user
            }
        }
        authStore.clearGuestModeSelected()
        return requireNotNull(user ?: firebaseAuth.currentUser) {
            "MenuDado registered session is unavailable"
        }.toMenuDadoAuthSession()
    }

    override suspend fun signInWithEmail(email: String, password: String): MenuDadoAuthSession {
        val user = firebaseAuth
            .signInWithEmailAndPassword(email.trim(), password)
            .awaitBackendTask()
            .user
        authStore.clearGuestModeSelected()
        return requireNotNull(user ?: firebaseAuth.currentUser) {
            "MenuDado signed-in session is unavailable"
        }.toMenuDadoAuthSession()
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): MenuDadoAuthSession {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val currentUser = firebaseAuth.currentUser
        val user = if (currentUser?.isAnonymous == true) {
            try {
                currentUser.linkWithCredential(credential).awaitBackendTask().user
            } catch (error: FirebaseAuthUserCollisionException) {
                firebaseAuth.signInWithCredential(credential).awaitBackendTask().user
            }
        } else {
            firebaseAuth.signInWithCredential(credential).awaitBackendTask().user
        }
        authStore.clearGuestModeSelected()
        return requireNotNull(user ?: firebaseAuth.currentUser) {
            "MenuDado Google session is unavailable"
        }.toMenuDadoAuthSession()
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
        authStore.clearGuestModeSelected()
    }
}

fun shouldStartGuestModeByDefault(
    session: MenuDadoAuthSession?,
    isGuestModeSelected: Boolean
): Boolean {
    return session?.isAnonymous != false && !isGuestModeSelected
}

fun shouldStartMenuDadoBackendSync(
    session: MenuDadoAuthSession?,
    isGuestModeSelected: Boolean
): Boolean {
    return when {
        session == null -> false
        !session.isAnonymous -> true
        else -> isGuestModeSelected
    }
}

fun registerStrategyFor(session: MenuDadoAuthSession?): MenuDadoRegisterStrategy {
    return if (session?.isAnonymous == true) {
        MenuDadoRegisterStrategy.LINK_ANONYMOUS_USER
    } else {
        MenuDadoRegisterStrategy.CREATE_EMAIL_USER
    }
}

private fun FirebaseUser.toMenuDadoAuthSession(): MenuDadoAuthSession {
    return MenuDadoAuthSession(
        userId = uid,
        email = email,
        isAnonymous = isAnonymous
    )
}
