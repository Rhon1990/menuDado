package com.menudado.appcheck

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.menudado.BuildConfig

fun installMenuDadoAppCheckProvider() {
    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
        menuDadoAppCheckProviderFactory()
    )
}

private fun menuDadoAppCheckProviderFactory(): AppCheckProviderFactory {
    return if (BuildConfig.APP_CHECK_PROVIDER == APP_CHECK_PROVIDER_DEBUG) {
        debugAppCheckProviderFactory() ?: playIntegrityAppCheckProviderFactory()
    } else {
        playIntegrityAppCheckProviderFactory()
    }
}

private fun playIntegrityAppCheckProviderFactory(): AppCheckProviderFactory {
    return PlayIntegrityAppCheckProviderFactory.getInstance()
}

private fun debugAppCheckProviderFactory(): AppCheckProviderFactory? {
    return runCatching {
        Class.forName(DEBUG_PROVIDER_FACTORY_CLASS_NAME)
            .getMethod(DEBUG_PROVIDER_FACTORY_GET_INSTANCE_METHOD)
            .invoke(null) as AppCheckProviderFactory
    }.getOrNull()
}

private const val APP_CHECK_PROVIDER_DEBUG = "debug"
private const val DEBUG_PROVIDER_FACTORY_CLASS_NAME =
    "com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory"
private const val DEBUG_PROVIDER_FACTORY_GET_INSTANCE_METHOD = "getInstance"
