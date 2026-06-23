package com.menudado.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun MenuDadoBannerAd(
    isReady: Boolean,
    modifier: Modifier = Modifier
) {
    if (!MenuDadoAdsConfig.isEnabled || !isReady) {
        return
    }

    val context = LocalContext.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val bannerWidthDp = MenuDadoAdsConfig.homeInlineBannerWidthDp(screenWidthDp)
    val horizontalPadding: Dp = MenuDadoAdsConfig.HOME_INLINE_BANNER_HORIZONTAL_PADDING_DP.dp
    val adView = remember(bannerWidthDp) {
        AdView(context).apply {
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, bannerWidthDp))
            adUnitId = MenuDadoAdsConfig.HOME_INLINE_BANNER_AD_UNIT_ID
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose {
            adView.destroy()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 2.dp)
    ) {
        AndroidView(
            factory = { adView },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
