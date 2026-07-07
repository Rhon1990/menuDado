package com.menudado.auth

import androidx.annotation.StringRes
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.menudado.R

@StringRes
internal fun authErrorMessageResId(error: Throwable): Int {
    return when (error) {
        is NoCredentialException -> R.string.auth_google_no_credentials
        is GetCredentialException -> R.string.auth_google_unavailable
        is FirebaseAuthWeakPasswordException -> R.string.auth_weak_password
        is FirebaseAuthUserCollisionException -> R.string.auth_email_already_in_use
        is FirebaseAuthInvalidCredentialsException,
        is FirebaseAuthInvalidUserException -> R.string.auth_invalid_credentials
        is FirebaseNetworkException -> R.string.auth_network_error
        else -> R.string.auth_error_generic
    }
}
