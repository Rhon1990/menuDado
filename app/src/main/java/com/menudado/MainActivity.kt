package com.menudado

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.menudado.analytics.AndroidDeviceInfoProvider
import com.menudado.ads.MenuDadoAdsController
import com.menudado.ads.MenuDadoAdsRemoteConfig
import com.menudado.about.MenuDadoAboutContent
import com.menudado.about.MenuDadoAboutRemoteConfig
import com.menudado.auth.shouldStartGuestModeByDefault
import com.menudado.ui.MenuDadoScreen
import com.menudado.ui.MenuDadoViewModel
import com.menudado.ui.theme.MenuDadoColors
import com.menudado.ui.theme.MenuDadoTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val splashHandler = Handler(Looper.getMainLooper())
    private val showStartupSplash = mutableStateOf(true)
    private val hideStartupSplash = Runnable {
        showStartupSplash.value = false
    }

    private val viewModel: MenuDadoViewModel by viewModels {
        val app = application as MenuDadoApplication
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MenuDadoViewModel(
                    repository = app.repository,
                    analytics = app.analytics,
                    aiQuotaRetryStore = app.aiQuotaRetryStore,
                    aiRequestThrottleStore = app.aiRequestThrottleStore,
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
        val app = application as MenuDadoApplication
        showStartupSplash.value = savedInstanceState == null
        splashHandler.removeCallbacks(hideStartupSplash)
        if (showStartupSplash.value) {
            splashHandler.postDelayed(hideStartupSplash, SPLASH_DURATION_MILLIS)
        }
        app.analytics.trackAppOpened(AndroidDeviceInfoProvider.current())
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(menuDadoStatusBarColor()),
            navigationBarStyle = SystemBarStyle.dark(menuDadoNavigationBarColor())
        )
        setContent {
            MenuDadoTheme {
                val showSplash by showStartupSplash
                val authScope = rememberCoroutineScope()
                var authSession by remember { mutableStateOf(app.authService.currentSession()) }
                var isGuestModeSelected by remember { mutableStateOf(app.authStore.isGuestModeSelected()) }
                var isAuthLoading by remember { mutableStateOf(false) }
                var authErrorMessage by remember { mutableStateOf<String?>(null) }
                var areAdsReady by remember { mutableStateOf(false) }
                var areAdsEnabled by remember { mutableStateOf(false) }
                var areAdsPrivacyOptionsRequired by remember { mutableStateOf(false) }
                var adsPrivacyOptionsMessage by remember { mutableStateOf<String?>(null) }
                val defaultAboutContent = remember {
                    MenuDadoAboutContent(
                        description = getString(R.string.about_reason),
                        createdBy = getString(R.string.about_created_by_value),
                        contact = getString(R.string.about_contact_value)
                    )
                }
                var aboutContent by remember { mutableStateOf(defaultAboutContent) }
                fun refreshAuthState() {
                    authSession = app.authService.currentSession()
                    isGuestModeSelected = app.authStore.isGuestModeSelected()
                }
                fun runAuthAction(mergeLocalDataIntoAccount: Boolean = false, action: suspend () -> Unit) {
                    isAuthLoading = true
                    authErrorMessage = null
                    authScope.launch {
                        val result = runCatching { action() }
                        if (result.isSuccess) {
                            refreshAuthState()
                            if (mergeLocalDataIntoAccount && authSession?.isAnonymous == false) {
                                app.prepareLocalDataForSignedInBackendSync()
                            }
                            app.syncBackendNow()
                        } else {
                            authErrorMessage = result.exceptionOrNull()
                                ?.localizedMessage
                                ?: getString(R.string.auth_error_generic)
                        }
                        isAuthLoading = false
                    }
                }
                fun startGoogleSignIn() {
                    val generatedWebClientIdRes = resources.getIdentifier(
                        "default_web_client_id",
                        "string",
                        packageName
                    )
                    val generatedWebClientId = if (generatedWebClientIdRes != 0) {
                        getString(generatedWebClientIdRes).trim()
                    } else {
                        ""
                    }
                    val webClientId = generatedWebClientId.ifBlank {
                        getString(R.string.google_web_client_id).trim()
                    }
                    if (webClientId.isBlank()) {
                        authErrorMessage = getString(R.string.auth_google_unavailable)
                        return
                    }
                    runAuthAction(mergeLocalDataIntoAccount = true) {
                        val googleIdOption = GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(webClientId)
                            .build()
                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()
                        val response = CredentialManager.create(this@MainActivity)
                            .getCredential(
                                context = this@MainActivity,
                                request = request
                            )
                        val credential = response.credential
                        val idToken = if (
                            credential is CustomCredential &&
                            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                        ) {
                            GoogleIdTokenCredential.createFrom(credential.data).idToken
                        } else {
                            null
                        } ?: error(getString(R.string.auth_google_missing_token))
                        app.authService.signInWithGoogleIdToken(idToken)
                    }
                }
                LaunchedEffect(showSplash, authSession?.userId, authSession?.isAnonymous, isGuestModeSelected) {
                    if (!showSplash && shouldStartGuestModeByDefault(authSession, isGuestModeSelected)) {
                        runAuthAction {
                            app.authService.continueAsGuest()
                        }
                    }
                }
                val adsController = remember {
                    MenuDadoAdsController(
                        activity = this@MainActivity,
                        onAdsReady = {
                            areAdsReady = true
                        },
                        onPrivacyOptionsRequirementChanged = { isRequired ->
                            areAdsPrivacyOptionsRequired = isRequired
                        },
                        onPrivacyOptionsUnavailable = {
                            adsPrivacyOptionsMessage = getString(R.string.ads_privacy_options_unavailable)
                        }
                    )
                }
                val adsRemoteConfig = remember {
                    MenuDadoAdsRemoteConfig(
                        onAdsEnabledChanged = { isEnabled ->
                            areAdsEnabled = isEnabled
                            if (!isEnabled) {
                                areAdsReady = false
                                areAdsPrivacyOptionsRequired = false
                            }
                        }
                    )
                }
                val aboutRemoteConfig = remember(defaultAboutContent) {
                    MenuDadoAboutRemoteConfig(
                        defaultContent = defaultAboutContent,
                        onAboutContentChanged = { content ->
                            aboutContent = content
                        }
                    )
                }
                val lifecycleOwner = LocalLifecycleOwner.current
                LaunchedEffect(adsRemoteConfig, aboutRemoteConfig) {
                    adsRemoteConfig.fetchAdsEnabled()
                    aboutRemoteConfig.fetchAboutContent()
                }
                DisposableEffect(lifecycleOwner, adsRemoteConfig, aboutRemoteConfig) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (shouldRefreshAdsRemoteConfigOnLifecycleEvent(event)) {
                            adsRemoteConfig.fetchAdsEnabled()
                            aboutRemoteConfig.fetchAboutContent()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }
                LaunchedEffect(adsController, areAdsEnabled) {
                    if (areAdsEnabled) {
                        adsController.requestConsentAndInitialize()
                    }
                }

                if (showSplash) {
                    MenuDadoSplashScreen()
                } else {
                    val shouldShowAds = MenuDadoAdsRemoteConfig.shouldShowAds(
                        remoteAdsEnabled = areAdsEnabled,
                        areAdsReady = areAdsReady
                    )
                    MenuDadoScreen(
                        viewModel = viewModel,
                        areAdsReady = shouldShowAds,
                        areAdsEnabled = areAdsEnabled,
                        areAdsPrivacyOptionsRequired = shouldShowAdsPrivacyOptionsInNavigation(
                            areAdsEnabled = areAdsEnabled,
                            buildType = BuildConfig.BUILD_TYPE,
                            areAdsPrivacyOptionsRequired = areAdsPrivacyOptionsRequired
                        ),
                        adsPrivacyOptionsMessage = adsPrivacyOptionsMessage,
                        onAdsPrivacyOptionsMessageDismiss = {
                            adsPrivacyOptionsMessage = null
                        },
                        onAdsPrivacyOptionsClick = adsController::showPrivacyOptionsForm,
                        authSession = authSession,
                        isAuthLoading = isAuthLoading,
                        authErrorMessage = authErrorMessage,
                        onAuthSignIn = { email, password ->
                            runAuthAction(mergeLocalDataIntoAccount = true) {
                                app.authService.signInWithEmail(email, password)
                            }
                        },
                        onAuthRegister = { email, password ->
                            runAuthAction(mergeLocalDataIntoAccount = true) {
                                app.authService.registerWithEmail(email, password)
                            }
                        },
                        onAuthGoogleSignIn = {
                            startGoogleSignIn()
                        },
                        onAuthSignOut = {
                            runAuthAction {
                                app.authService.signOut()
                            }
                        },
                        aboutContent = aboutContent,
                        onAuthErrorDismiss = {
                            authErrorMessage = null
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        splashHandler.removeCallbacks(hideStartupSplash)
        super.onDestroy()
    }

    private companion object {
        const val SPLASH_DURATION_MILLIS = 1_100L
    }
}

internal fun menuDadoStatusBarColor(): Int = MenuDadoColors.HeaderGreen.toArgb()

internal fun menuDadoNavigationBarColor(): Int = MenuDadoColors.HeaderGreen.toArgb()

internal fun shouldShowAdsPrivacyOptionsInNavigation(
    areAdsEnabled: Boolean,
    buildType: String,
    areAdsPrivacyOptionsRequired: Boolean
): Boolean {
    return false
}

internal fun shouldRefreshAdsRemoteConfigOnLifecycleEvent(event: Lifecycle.Event): Boolean {
    return event == Lifecycle.Event.ON_RESUME
}

private const val RELEASE_BUILD_TYPE = "release"

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
