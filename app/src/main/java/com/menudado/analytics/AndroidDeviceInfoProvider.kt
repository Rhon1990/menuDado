package com.menudado.analytics

import android.os.Build
import java.util.Locale
import java.util.TimeZone

object AndroidDeviceInfoProvider {
    fun current(): DeviceInfo {
        return DeviceInfo(
            manufacturer = Build.MANUFACTURER.orUnknown(),
            model = Build.MODEL.orUnknown(),
            androidVersion = (Build.VERSION.RELEASE ?: Build.VERSION.SDK_INT.toString()).orUnknown(),
            localeCountry = Locale.getDefault().country.ifBlank { "unknown" },
            timeZone = TimeZone.getDefault().id.ifBlank { "unknown" }
        )
    }

    private fun String?.orUnknown(): String {
        return this?.takeIf { it.isNotBlank() } ?: "unknown"
    }
}
