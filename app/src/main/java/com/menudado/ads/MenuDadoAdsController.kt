package com.menudado.ads

import android.app.Activity
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MenuDadoAdsController(
    private val activity: Activity,
    private val onAdsReady: () -> Unit
) {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(activity)
    private var hasInitializedMobileAds = false

    fun requestConsentAndInitialize() {
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    initializeIfAllowed()
                }
            },
            {
                initializeIfAllowed()
            }
        )
    }

    private fun initializeIfAllowed() {
        if (!consentInformation.canRequestAds() || hasInitializedMobileAds) {
            return
        }
        hasInitializedMobileAds = true
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(activity) {
                activity.runOnUiThread {
                    onAdsReady()
                }
            }
        }
    }
}
