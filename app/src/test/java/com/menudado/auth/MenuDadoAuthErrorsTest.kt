package com.menudado.auth

import androidx.credentials.exceptions.NoCredentialException
import com.menudado.R
import org.junit.Assert.assertEquals
import org.junit.Test

class MenuDadoAuthErrorsTest {
    @Test
    fun `google no credential error uses app localized message instead of sdk message`() {
        assertEquals(
            R.string.auth_google_no_credentials,
            authErrorMessageResId(NoCredentialException("No credentials available"))
        )
    }

    @Test
    fun `unknown auth error falls back to generic localized message`() {
        assertEquals(
            R.string.auth_error_generic,
            authErrorMessageResId(IllegalStateException("Unexpected sdk message"))
        )
    }
}
