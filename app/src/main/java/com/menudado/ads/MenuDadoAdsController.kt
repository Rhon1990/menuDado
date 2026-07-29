package com.menudado.ads

import android.app.Activity
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MenuDadoAdsController(
    private val activity: Activity,
    private val onAdsReadinessChanged: (Boolean) -> Unit,
    private val onPrivacyOptionsRequirementChanged: (Boolean) -> Unit,
    private val onPrivacyOptionsUnavailable: () -> Unit
) {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(activity)
    private var hasInitializedMobileAds = false

    fun requestConsentAndInitialize() {
        onAdsReadinessChanged(
            shouldReportReady(
                canRequestAds = consentInformation.canRequestAds(),
                hasInitializedMobileAds = hasInitializedMobileAds
            )
        )
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    reportPrivacyOptionsRequirement()
                    initializeIfAllowed()
                }
            },
            {
                reportPrivacyOptionsRequirement()
                initializeIfAllowed()
            }
        )
    }

    fun showPrivacyOptionsForm() {
        onAdsReadinessChanged(false)
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            reportPrivacyOptionsRequirement()
            if (shouldNotifyPrivacyOptionsUnavailable(formError != null)) {
                onPrivacyOptionsUnavailable()
            }
            initializeIfAllowed()
        }
    }

    private fun initializeIfAllowed() {
        if (!consentInformation.canRequestAds()) {
            onAdsReadinessChanged(false)
            return
        }
        if (hasInitializedMobileAds) {
            onAdsReadinessChanged(true)
            return
        }
        hasInitializedMobileAds = true
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(activity) {
                activity.runOnUiThread {
                    onAdsReadinessChanged(
                        shouldReportReady(
                            canRequestAds = consentInformation.canRequestAds(),
                            hasInitializedMobileAds = hasInitializedMobileAds
                        )
                    )
                }
            }
        }
    }

    private fun reportPrivacyOptionsRequirement() {
        onPrivacyOptionsRequirementChanged(
            consentInformation.privacyOptionsRequirementStatus ==
                PrivacyOptionsRequirementStatus.REQUIRED
        )
    }

    companion object {
        internal fun shouldReportReady(canRequestAds: Boolean, hasInitializedMobileAds: Boolean): Boolean {
            return canRequestAds && hasInitializedMobileAds
        }

        internal fun shouldNotifyPrivacyOptionsUnavailable(hasFormError: Boolean): Boolean {
            return hasFormError
        }
    }
}
