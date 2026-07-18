package com.menudado

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.stringResource
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
import com.menudado.auth.authErrorMessageResId
import com.menudado.auth.shouldStartGuestModeByDefault
import com.menudado.auth.MenuDadoGuestLimitsRemoteConfig
import com.menudado.ui.MenuDadoScreen
import com.menudado.ui.MenuDadoViewModel
import com.menudado.ui.theme.MenuDadoColors
import com.menudado.ui.theme.MenuDadoTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.menudado.update.MenuDadoAppUpdatePresentationState
import com.menudado.update.MenuDadoAppUpdateStatus
import com.menudado.update.menuDadoAppUpdateStatus
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
                    guestUsageStore = app.guestUsageStore,
                    dietaryProfileStore = app.dietaryProfileStore,
                    onboardingStore = app.onboardingStore,
                    cuisineRotation = app.cuisineRotation
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
        applyMenuDadoEdgeToEdge(window)
        setContent {
            MenuDadoTheme {
                val showSplash by showStartupSplash
                val authScope = rememberCoroutineScope()
                val dietaryProfileHydrationRevision by app.dietaryProfileHydrationRevision.collectAsState()
                var authSession by remember { mutableStateOf(app.authService.currentSession()) }
                var isGuestModeSelected by remember { mutableStateOf(app.authStore.isGuestModeSelected()) }
                var isAuthLoading by remember { mutableStateOf(false) }
                var authErrorMessage by remember { mutableStateOf<String?>(null) }
                var areAdsReady by remember { mutableStateOf(false) }
                var areAdsEnabled by remember { mutableStateOf(false) }
                var areGuestLimitsEnabled by remember { mutableStateOf(true) }
                var areAdsPrivacyOptionsRequired by remember { mutableStateOf(false) }
                var adsPrivacyOptionsMessage by remember { mutableStateOf<String?>(null) }
                var pendingAppUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
                var appUpdatePresentation by remember {
                    mutableStateOf(MenuDadoAppUpdatePresentationState())
                }
                LaunchedEffect(dietaryProfileHydrationRevision) {
                    viewModel.refreshDietaryProfile()
                }
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
                                ?.let { getString(authErrorMessageResId(it)) }
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
                val appUpdateManager = remember {
                    AppUpdateManagerFactory.create(this@MainActivity)
                }
                fun refreshAvailableUpdate() {
                    refreshPlayStoreUpdateState(
                        appUpdateManager = appUpdateManager,
                        onUpdateStateChanged = { updateInfo, status ->
                            pendingAppUpdateInfo = updateInfo
                            appUpdatePresentation = appUpdatePresentation.withStatus(status)
                        },
                        onNoUpdateAvailable = {
                            pendingAppUpdateInfo = null
                            appUpdatePresentation = MenuDadoAppUpdatePresentationState()
                        }
                    )
                }
                val appUpdateLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartIntentSenderForResult()
                ) {
                    refreshAvailableUpdate()
                }
                fun startMenuDadoUpdate() {
                    app.analytics.trackAppUpdatePrompt(appUpdateAnalyticsActionUpdate())
                    val updateInfo = pendingAppUpdateInfo
                    appUpdatePresentation = appUpdatePresentation.copy(
                        isAvailablePromptDismissed = true
                    )
                    if (updateInfo == null) {
                        openMenuDadoPlayStore()
                        return
                    }
                    val didStartUpdate = runCatching {
                        val appUpdateOptions = appUpdateOptionsFor(updateInfo)
                            ?: return@runCatching false
                        appUpdateManager.startUpdateFlowForResult(
                            updateInfo,
                            appUpdateLauncher,
                            appUpdateOptions
                        )
                    }.getOrDefault(false)
                    if (!didStartUpdate) {
                        openMenuDadoPlayStore()
                    }
                }
                val appUpdateInstallStateListener = remember {
                    InstallStateUpdatedListener { installState ->
                        appUpdatePresentation = appUpdatePresentation.withStatus(
                            menuDadoAppUpdateStatus(
                                updateAvailability = UpdateAvailability.UPDATE_AVAILABLE,
                                installStatus = installState.installStatus()
                            )
                        )
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
                val guestLimitsRemoteConfig = remember {
                    MenuDadoGuestLimitsRemoteConfig(
                        onGuestLimitsEnabledChanged = { areEnabled ->
                            areGuestLimitsEnabled = areEnabled
                        }
                    )
                }
                val lifecycleOwner = LocalLifecycleOwner.current
                LaunchedEffect(adsRemoteConfig, aboutRemoteConfig, guestLimitsRemoteConfig) {
                    adsRemoteConfig.fetchAdsEnabled()
                    aboutRemoteConfig.fetchAboutContent()
                    guestLimitsRemoteConfig.fetchGuestLimitsEnabled()
                }
                LaunchedEffect(appUpdateManager) {
                    refreshAvailableUpdate()
                }
                DisposableEffect(lifecycleOwner, adsRemoteConfig, aboutRemoteConfig, guestLimitsRemoteConfig, appUpdateManager) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (shouldRefreshAdsRemoteConfigOnLifecycleEvent(event)) {
                            adsRemoteConfig.fetchAdsEnabled()
                            aboutRemoteConfig.fetchAboutContent()
                            guestLimitsRemoteConfig.fetchGuestLimitsEnabled()
                            refreshAvailableUpdate()
                        }
                    }
                    appUpdateManager.registerListener(appUpdateInstallStateListener)
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        appUpdateManager.unregisterListener(appUpdateInstallStateListener)
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
                    if (appUpdatePresentation.shouldShowDownloadedPrompt) {
                        LaunchedEffect("downloaded_app_update_prompt") {
                            app.analytics.trackAppUpdatePrompt(appUpdateAnalyticsActionShown())
                        }
                        MenuDadoDownloadedAppUpdateDialog(
                            onInstall = {
                                app.analytics.trackAppUpdatePrompt(appUpdateAnalyticsActionInstall())
                                appUpdateManager.completeUpdate()
                            },
                            onDismiss = {
                                app.analytics.trackAppUpdatePrompt(appUpdateAnalyticsActionLater())
                                appUpdatePresentation = appUpdatePresentation.copy(
                                    isDownloadedPromptDismissed = true
                                )
                            }
                        )
                    } else if (appUpdatePresentation.shouldShowAvailablePrompt) {
                        LaunchedEffect("app_update_prompt") {
                            app.analytics.trackAppUpdatePrompt(appUpdateAnalyticsActionShown())
                        }
                        MenuDadoAppUpdateDialog(
                            onUpdate = ::startMenuDadoUpdate,
                            onDismiss = {
                                app.analytics.trackAppUpdatePrompt(appUpdateAnalyticsActionLater())
                                appUpdatePresentation = appUpdatePresentation.copy(
                                    isAvailablePromptDismissed = true
                                )
                            }
                        )
                    }
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
                        areGuestLimitsEnabled = areGuestLimitsEnabled,
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

    private fun openMenuDadoPlayStore() {
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse(menuDadoPlayStoreMarketUri()))
            .setPackage("com.android.vending")
        runCatching {
            startActivity(marketIntent)
        }.recoverCatching { error ->
            if (error is ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(menuDadoPlayStoreWebUrl())))
            } else {
                throw error
            }
        }
    }

    private companion object {
        const val SPLASH_DURATION_MILLIS = 1_100L
    }
}

private fun refreshPlayStoreUpdateState(
    appUpdateManager: AppUpdateManager,
    onUpdateStateChanged: (AppUpdateInfo, MenuDadoAppUpdateStatus) -> Unit,
    onNoUpdateAvailable: () -> Unit
) {
    appUpdateManager.appUpdateInfo
        .addOnSuccessListener { appUpdateInfo ->
            val status = menuDadoAppUpdateStatus(
                updateAvailability = appUpdateInfo.updateAvailability(),
                installStatus = appUpdateInfo.installStatus()
            )
            if (status != MenuDadoAppUpdateStatus.NOT_AVAILABLE) {
                onUpdateStateChanged(appUpdateInfo, status)
            } else {
                onNoUpdateAvailable()
            }
        }
}

@Composable
private fun MenuDadoAppUpdateDialog(
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onUpdate,
                colors = ButtonDefaults.buttonColors(containerColor = MenuDadoColors.BrandGreen)
            ) {
                Text(stringResource(id = R.string.app_update_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.app_update_later))
            }
        },
        title = {
            Text(stringResource(id = R.string.app_update_title))
        },
        text = {
            Text(stringResource(id = R.string.app_update_body))
        }
    )
}

@Composable
private fun MenuDadoDownloadedAppUpdateDialog(
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onInstall,
                colors = ButtonDefaults.buttonColors(containerColor = MenuDadoColors.BrandGreen)
            ) {
                Text(stringResource(id = R.string.app_update_install_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.app_update_later))
            }
        },
        title = {
            Text(stringResource(id = R.string.app_update_downloaded_title))
        },
        text = {
            Text(stringResource(id = R.string.app_update_downloaded_body))
        }
    )
}

internal fun menuDadoStatusBarColor(): Int = MenuDadoColors.HeaderGreen.toArgb()

internal fun menuDadoNavigationBarColor(): Int = MenuDadoColors.HeaderGreen.toArgb()

internal fun menuDadoDecorFitsSystemWindows(): Boolean = false

internal fun menuDadoDisplayCutoutMode(): Int =
    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS

@Suppress("DEPRECATION")
internal fun applyMenuDadoEdgeToEdge(window: Window) {
    WindowCompat.setDecorFitsSystemWindows(window, menuDadoDecorFitsSystemWindows())
    window.statusBarColor = menuDadoStatusBarColor()
    window.navigationBarColor = menuDadoNavigationBarColor()
    WindowCompat.getInsetsController(window, window.decorView).apply {
        isAppearanceLightStatusBars = false
        isAppearanceLightNavigationBars = false
    }
    applyMenuDadoDisplayCutoutMode(window)
}

internal fun applyMenuDadoDisplayCutoutMode(window: Window) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
    window.attributes = window.attributes.apply {
        layoutInDisplayCutoutMode = menuDadoDisplayCutoutMode()
    }
}

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

internal fun appUpdateAnalyticsScreen(): String = "app_update_prompt"

internal fun appUpdateAnalyticsActionShown(): String = "shown"

internal fun appUpdateAnalyticsActionUpdate(): String = "update"

internal fun appUpdateAnalyticsActionLater(): String = "later"

internal fun appUpdateAnalyticsActionInstall(): String = "install"

internal fun menuDadoPlayStorePackageName(): String = "com.menudado"

internal fun menuDadoPlayStoreMarketUri(): String {
    return "market://details?id=${menuDadoPlayStorePackageName()}"
}

internal fun menuDadoPlayStoreWebUrl(): String {
    return "https://play.google.com/store/apps/details?id=${menuDadoPlayStorePackageName()}"
}

internal fun preferredMenuDadoAppUpdateType(isFlexibleAllowed: Boolean): Int? {
    return AppUpdateType.FLEXIBLE.takeIf { isFlexibleAllowed }
}

private fun appUpdateOptionsFor(appUpdateInfo: AppUpdateInfo): AppUpdateOptions? {
    val appUpdateType = preferredMenuDadoAppUpdateType(
        isFlexibleAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
    ) ?: return null
    return AppUpdateOptions.newBuilder(appUpdateType).build()
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
