package com.menudado.about

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.menudado.BuildConfig

data class MenuDadoAboutContent(
    val description: String,
    val createdBy: String,
    val contact: String
)

class MenuDadoAboutRemoteConfig(
    private val defaultContent: MenuDadoAboutContent,
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance(),
    private val onAboutContentChanged: (MenuDadoAboutContent) -> Unit
) {
    fun fetchAboutContent() {
        onAboutContentChanged(readAboutContent())
        remoteConfig.setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(fetchIntervalSeconds(BuildConfig.DEBUG))
                .build()
        ).addOnCompleteListener {
            remoteConfig.setDefaultsAsync(
                mapOf(
                    KEY_ABOUT_DESCRIPTION to defaultContent.description,
                    KEY_ABOUT_CREATED_BY to defaultContent.createdBy,
                    KEY_ABOUT_CONTACT to defaultContent.contact
                )
            ).addOnCompleteListener {
                remoteConfig.fetchAndActivate().addOnCompleteListener {
                    onAboutContentChanged(readAboutContent())
                }
            }
        }
    }

    private fun readAboutContent(): MenuDadoAboutContent {
        return aboutContentFromRemoteValues(
            description = remoteConfig.getString(KEY_ABOUT_DESCRIPTION),
            createdBy = remoteConfig.getString(KEY_ABOUT_CREATED_BY),
            contact = remoteConfig.getString(KEY_ABOUT_CONTACT),
            fallback = defaultContent
        )
    }

    companion object {
        const val KEY_ABOUT_DESCRIPTION = "about_description"
        const val KEY_ABOUT_CREATED_BY = "about_created_by"
        const val KEY_ABOUT_CONTACT = "about_contact"
        private const val DEBUG_MINIMUM_FETCH_INTERVAL_SECONDS = 0L
        private const val MINIMUM_FETCH_INTERVAL_SECONDS = 3_600L

        fun fetchIntervalSeconds(isDebugBuild: Boolean): Long {
            return if (isDebugBuild) DEBUG_MINIMUM_FETCH_INTERVAL_SECONDS else MINIMUM_FETCH_INTERVAL_SECONDS
        }

        fun aboutContentFromRemoteValues(
            description: String?,
            createdBy: String?,
            contact: String?,
            fallback: MenuDadoAboutContent
        ): MenuDadoAboutContent {
            return MenuDadoAboutContent(
                description = description.remoteTextOrFallback(fallback.description),
                createdBy = createdBy.remoteTextOrFallback(fallback.createdBy),
                contact = contact.remoteTextOrFallback(fallback.contact)
            )
        }
    }
}

private fun String?.remoteTextOrFallback(fallback: String): String {
    return this?.trim()?.takeIf { it.isNotEmpty() } ?: fallback
}
