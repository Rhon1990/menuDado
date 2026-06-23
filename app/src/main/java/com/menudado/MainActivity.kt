package com.menudado

import android.os.Bundle
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.menudado.analytics.AndroidDeviceInfoProvider
import com.menudado.ads.MenuDadoAdsController
import com.menudado.ui.MenuDadoScreen
import com.menudado.ui.MenuDadoViewModel
import com.menudado.ui.theme.MenuDadoColors
import com.menudado.ui.theme.MenuDadoTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val viewModel: MenuDadoViewModel by viewModels {
        val app = application as MenuDadoApplication
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MenuDadoViewModel(
                    repository = app.repository,
                    analytics = app.analytics,
                    aiQuotaRetryStore = app.aiQuotaRetryStore,
                    aiDailyUsageStore = app.aiDailyUsageStore,
                    dietaryProfileStore = app.dietaryProfileStore,
                    onboardingStore = app.onboardingStore
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_MenuDado)
        super.onCreate(savedInstanceState)
        (application as MenuDadoApplication).analytics.trackAppOpened(AndroidDeviceInfoProvider.current())
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(MenuDadoColors.HeaderGreen.toArgb()),
            navigationBarStyle = SystemBarStyle.light(
                MenuDadoColors.Background.toArgb(),
                MenuDadoColors.Background.toArgb()
            )
        )
        setContent {
            MenuDadoTheme {
                var showSplash by remember { mutableStateOf(true) }
                var areAdsReady by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    MenuDadoAdsController(this@MainActivity) {
                        areAdsReady = true
                    }.requestConsentAndInitialize()
                    delay(SPLASH_DURATION_MILLIS)
                    showSplash = false
                }

                if (showSplash) {
                    MenuDadoSplashScreen()
                } else {
                    MenuDadoScreen(
                        viewModel = viewModel,
                        areAdsReady = areAdsReady
                    )
                }
            }
        }
    }

    private companion object {
        const val SPLASH_DURATION_MILLIS = 1_100L
    }
}

@Composable
private fun MenuDadoSplashScreen() {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    val logoAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing),
        label = "splashLogoAlpha"
    )
    val logoScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.86f,
        animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing),
        label = "splashLogoScale"
    )
    val logoBlur by animateFloatAsState(
        targetValue = if (visible) 0f else 18f,
        animationSpec = tween(durationMillis = 680, easing = FastOutSlowInEasing),
        label = "splashLogoBlur"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MenuDadoColors.HeaderGreen),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.menu_dado_logo),
            contentDescription = "MenuDado",
            modifier = Modifier
                .size(width = 238.dp, height = 252.dp)
                .blur(logoBlur.dp)
                .graphicsLayer {
                    alpha = logoAlpha
                    scaleX = logoScale
                    scaleY = logoScale
                },
            contentScale = ContentScale.Fit
        )
    }
}
