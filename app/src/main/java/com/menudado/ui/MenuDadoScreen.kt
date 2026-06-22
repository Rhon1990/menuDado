package com.menudado.ui

import androidx.compose.animation.AnimatedVisibility
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.Animatable
import android.graphics.Paint as AndroidPaint
import android.net.Uri
import android.os.Environment
import com.menudado.BuildConfig
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.menudado.R
import com.menudado.domain.DietaryAllergen
import com.menudado.domain.DietaryProfile
import com.menudado.domain.FoodMenu
import com.menudado.domain.HealthAnalysis
import com.menudado.domain.HealthStatus
import com.menudado.domain.MenuAudience
import com.menudado.domain.MealType
import com.menudado.ui.theme.MenuDadoColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.ImageView
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MenuDadoScreen(viewModel: MenuDadoViewModel) {
    val state by viewModel.uiState.collectAsState()
    val result = state.result
    val message = state.message
    val aiRetryAtMillis = state.aiRetryAtMillis
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var previousRolling by remember { mutableStateOf(state.isRolling) }
    var diceFaceIndex by remember { mutableIntStateOf(0) }
    var audienceDetailRoute by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDetailMenuId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingDeleteMenuId by rememberSaveable { mutableStateOf<Long?>(null) }
    var photoPickerMenuId by rememberSaveable { mutableStateOf<Long?>(null) }
    var isPhotoSourceDialogVisible by rememberSaveable { mutableStateOf(false) }
    var pendingCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDestination by rememberSaveable { mutableStateOf(MenuDadoDestination.HOME.name) }
    val pendingAnalysisCount = state.menus.count { it.healthAnalysis == null }
    val destination = MenuDadoDestination.valueOf(selectedDestination)
    val audienceDetail = menuAudienceFromDetailRoute(audienceDetailRoute)
    val homeListState = rememberLazyListState()
    val audienceDetailListState = remember(audienceDetailRoute) { LazyListState() }
    val activeListState = if (menuListStateKeyForAudienceDetail(audienceDetailRoute) == "home") {
        homeListState
    } else {
        audienceDetailListState
    }
    val selectedDetailMenu = selectedDetailMenuId?.let { selectedId ->
        state.menus.firstOrNull { it.id == selectedId }
    }
    val pendingDeleteMenu = pendingDeleteMenuId?.let { pendingId ->
        state.menus.firstOrNull { it.id == pendingId }
    }
    fun saveSelectedPhoto(uriString: String) {
        val menuId = photoPickerMenuId
        if (menuId == null) {
            viewModel.updateEditImageUri(uriString)
        } else {
            viewModel.updateMenuImageUri(menuId, uriString)
        }
    }
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            saveSelectedPhoto(it.toString())
            photoPickerMenuId = null
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { didTakePhoto ->
        val cameraUri = pendingCameraUriString
        if (didTakePhoto && cameraUri != null) {
            saveSelectedPhoto(cameraUri)
        }
        pendingCameraUriString = null
        photoPickerMenuId = null
    }
    fun showPhotoSourceFor(menuId: Long?) {
        photoPickerMenuId = menuId
        isPhotoSourceDialogVisible = true
    }

    BackHandler(enabled = audienceDetailRoute != null) {
        audienceDetailRoute = menuAudienceDetailRouteAfterBack(audienceDetailRoute)
    }

    LaunchedEffect(audienceDetailRoute) {
        if (menuShouldResetAudienceDetailScroll(audienceDetailRoute)) {
            audienceDetailListState.scrollToItem(0)
        }
    }

    LaunchedEffect(state.isRolling) {
        if (previousRolling && !state.isRolling) {
            diceFaceIndex = (diceFaceIndex + 1) % DiceRestPoses.size
        }
        previousRolling = state.isRolling
    }

    LaunchedEffect(state.menus) {
        if (selectedDetailMenuId != null && state.menus.none { it.id == selectedDetailMenuId }) {
            selectedDetailMenuId = null
        }
        if (pendingDeleteMenuId != null && state.menus.none { it.id == pendingDeleteMenuId }) {
            pendingDeleteMenuId = null
        }
    }

    LaunchedEffect(result?.id) {
        if (menuShouldOpenDetailFromDiceResult(result)) {
            selectedDetailMenuId = menuDetailMenuIdAfterDiceResult(
                currentDetailMenuId = selectedDetailMenuId,
                result = result
            )
            viewModel.clearResult()
        }
    }

    if (message != null && aiRetryAtMillis != null && state.isAiRetryNoticeVisible) {
        AiQuotaDialog(
            message = message,
            retryAtMillis = aiRetryAtMillis,
            onDismiss = viewModel::clearMessage
        )
    } else if (message != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearMessage,
            confirmButton = {
                TextButton(onClick = viewModel::clearMessage) {
                    Text(stringResource(id = R.string.common_understood))
                }
            },
            title = { Text(stringResource(id = R.string.app_name)) },
            text = { Text(message) }
        )
    }

    if (state.showOnboarding) {
        OnboardingDialog(
            onSkip = viewModel::skipOnboarding,
            onFinish = viewModel::completeOnboarding
        )
    }

    if (isPhotoSourceDialogVisible) {
        MenuPhotoSourceDialog(
            onTakePhoto = {
                val uri = createMenuCameraImageUri(context)
                pendingCameraUriString = uri.toString()
                isPhotoSourceDialogVisible = false
                cameraLauncher.launch(uri)
            },
            onChooseFromLibrary = {
                isPhotoSourceDialogVisible = false
                photoPickerLauncher.launch(arrayOf("image/*"))
            },
            onDismiss = {
                isPhotoSourceDialogVisible = false
                photoPickerMenuId = null
            }
        )
    }

    if (state.editingMenuId != null) {
        EditMenuDialog(
            state = state,
            onMealTypeChanged = viewModel::setEditMealType,
            onAudienceChanged = viewModel::setEditAudience,
            onNameChanged = viewModel::updateEditName,
            onDescriptionChanged = viewModel::updateEditDescription,
            onNotesChanged = viewModel::updateEditNotes,
            onPickImage = {
                showPhotoSourceFor(menuId = null)
            },
            onSave = viewModel::saveEditedMenu,
            onDismiss = viewModel::cancelEditingMenu
        )
    }

    pendingDeleteMenu?.let { menu ->
        DeleteMenuConfirmationDialog(
            menu = menu,
            onConfirm = {
                pendingDeleteMenuId = null
                if (selectedDetailMenuId == menu.id) {
                    selectedDetailMenuId = null
                }
                viewModel.deleteMenu(menu)
            },
            onDismiss = {
                pendingDeleteMenuId = menuDeleteConfirmationMenuIdAfterDismiss()
            }
        )
    }

    selectedDetailMenu?.let { menu ->
        MenuDetailDialog(
            menu = menu,
            isAnalyzing = state.isAnalyzing,
            aiUsesRemainingToday = state.aiUsesRemainingToday,
            isAiPaused = state.aiRetryAtMillis != null,
            onAnalyze = { viewModel.analyzeExisting(menu) },
            onEdit = {
                selectedDetailMenuId = null
                viewModel.startEditingMenu(menu)
            },
            onDelete = {
                pendingDeleteMenuId = menuDeleteConfirmationMenuIdAfterDeleteClick(menu)
            },
            onDismiss = { selectedDetailMenuId = null }
        )
    }

    BoxWithConstraints(
        modifier = Modifier.hideKeyboardOnTouch(focusManager, keyboardController)
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                MenuDadoDrawer(
                    selectedDestination = destination,
                    drawerWidthDp = menuDadoDrawerWidthDp(maxWidth.value.toInt()),
                    onDestinationSelected = { selected ->
                        if (selected == MenuDadoDestination.ABOUT) {
                            viewModel.trackAboutAppOpened()
                        }
                        selectedDestination = selected.name
                        audienceDetailRoute = null
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            }
        ) {
            LazyColumn(
                state = activeListState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MenuDadoColors.Background),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Header(
                        onMenuClick = {
                            coroutineScope.launch { drawerState.open() }
                        }
                    )
                }

                when (destination) {
                    MenuDadoDestination.PROFILE -> {
                        item {
                            DietaryProfileSection(
                                audience = state.dietaryProfileAudience,
                                audienceAgeRanges = state.audienceAgeRanges,
                                enabledAudienceCount = state.enabledAudiences.size,
                                profile = state.dietaryProfile,
                                onAudienceChanged = viewModel::setDietaryProfileAudience,
                                onAudienceEnabledChanged = viewModel::setDietaryProfileAudienceEnabled,
                                onPregnantChanged = viewModel::setDietaryProfilePregnant,
                                onVeganChanged = viewModel::setDietaryProfileVegan,
                                onHasAllergiesChanged = viewModel::setDietaryProfileHasAllergies,
                                onAllergenToggled = viewModel::toggleDietaryAllergen,
                                onOtherAvoidancesChanged = viewModel::updateDietaryProfileOtherAvoidances
                            )
                        }
                    }
                    MenuDadoDestination.ABOUT -> {
                        item {
                            AboutAppSection()
                        }
                    }
                    MenuDadoDestination.HOME -> {
                        if (audienceDetail != null) {
                            item {
                                MenuAudienceDetailScreen(
                                    audience = audienceDetail,
                                    menus = state.menus,
                                    onBack = { audienceDetailRoute = null },
                                    onOpenMenu = { menu ->
                                        viewModel.trackMenuCardOpened(menu)
                                        selectedDetailMenuId = menu.id
                                    },
                                    onPickImage = { menu ->
                                        showPhotoSourceFor(menuId = menu.id)
                                    }
                                )
                            }
                        } else {
                            item {
                                DiceSection(
                                    filter = state.diceFilter,
                                    audienceFilter = state.diceAudienceFilter,
                                    enabledAudiences = state.enabledAudiences,
                                    isRolling = state.isRolling,
                                    diceFaceIndex = diceFaceIndex,
                                    onFilterChanged = viewModel::setDiceFilter,
                                    onAudienceFilterChanged = viewModel::setDiceAudienceFilter,
                                    onRoll = viewModel::rollDice
                                )
                            }
                            item {
                                MenuForm(
                                    state = state,
                                    enabledAudiences = state.enabledAudiences,
                                    onMealTypeChanged = viewModel::setFormMealType,
                                    onAudienceChanged = viewModel::setFormAudience,
                                    onNameChanged = viewModel::updateName,
                                    onDescriptionChanged = viewModel::updateDescription,
                                    onNotesChanged = viewModel::updateNotes,
                                    onAiBaseIngredientsChanged = viewModel::updateAiBaseIngredients,
                                    onGenerate = viewModel::generateMenuIdea,
                                    onSave = viewModel::saveMenu
                                )
                            }
                            item {
                                Text(
                                    text = stringResource(id = R.string.your_menus),
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (pendingAnalysisCount > 0) {
                                item {
                                    PendingAnalysisButton(
                                        isAnalyzing = state.isAnalyzing,
                                        isAiPaused = state.aiRetryAtMillis != null,
                                        usesRemaining = state.aiUsesRemainingToday,
                                        onAnalyzePending = viewModel::analyzePendingMenus
                                    )
                                }
                            }
                            if (state.menus.isEmpty()) {
                                item {
                                    EmptyState()
                                }
                            } else {
                                item {
                                    MenuCarouselSections(
                                        menus = state.menus,
                                        enabledAudiences = state.enabledAudiences,
                                        onViewMore = { audience ->
                                            audienceDetailRoute = menuAudienceDetailRouteAfterViewMore(audience)
                                        },
                                        onOpenMenu = { menu ->
                                            viewModel.trackMenuCardOpened(menu)
                                            selectedDetailMenuId = menu.id
                                        },
                                        onPickImage = { menu ->
                                            showPhotoSourceFor(menuId = menu.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }
        }
        if (state.isGeneratingMenu) {
            AiGenerationLoadingOverlay()
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.hideKeyboardOnTouch(
    focusManager: FocusManager,
    keyboardController: SoftwareKeyboardController?
): Modifier {
    return pointerInput(focusManager, keyboardController) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.any { it.changedToDownIgnoreConsumed() }) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            }
        }
    }
}

@Composable
private fun MenuDadoDrawer(
    selectedDestination: MenuDadoDestination,
    drawerWidthDp: Int,
    onDestinationSelected: (MenuDadoDestination) -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(drawerWidthDp.dp),
        drawerContainerColor = menuDadoDrawerContainerColor(),
        drawerContentColor = menuDadoDrawerUnselectedTextColor()
    ) {
        val drawerItemColors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = menuDadoDrawerSelectedContainerColor(),
            unselectedContainerColor = Color.Transparent,
            selectedTextColor = menuDadoDrawerSelectedTextColor(),
            unselectedTextColor = menuDadoDrawerUnselectedTextColor()
        )
        val drawerItemShape = RoundedCornerShape(menuDadoDrawerItemCornerRadiusDp().dp)
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 12.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.app_name),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MenuDadoColors.DeepGreen
            )
            NavigationDrawerItem(
                label = { Text(stringResource(id = R.string.nav_home)) },
                selected = selectedDestination == MenuDadoDestination.HOME,
                onClick = { onDestinationSelected(MenuDadoDestination.HOME) },
                shape = drawerItemShape,
                colors = drawerItemColors
            )
            NavigationDrawerItem(
                label = { Text(stringResource(id = R.string.nav_dietary_profile)) },
                selected = selectedDestination == MenuDadoDestination.PROFILE,
                onClick = { onDestinationSelected(MenuDadoDestination.PROFILE) },
                shape = drawerItemShape,
                colors = drawerItemColors
            )
            NavigationDrawerItem(
                label = { Text(stringResource(id = R.string.nav_about)) },
                selected = selectedDestination == MenuDadoDestination.ABOUT,
                onClick = { onDestinationSelected(MenuDadoDestination.ABOUT) },
                shape = drawerItemShape,
                colors = drawerItemColors
            )
        }
    }
}

@Composable
private fun AiGenerationLoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(aiGenerationLoadingOverlayColor())
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = aiGenerationLoadingBlocksTouches(),
                onClick = {}
            )
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = aiGenerationLoadingCardColor()),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AndroidView(
                    factory = { context ->
                        ImageView(context).apply {
                            adjustViewBounds = true
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            setImageResource(aiGenerationLoadingImageRes())
                            (drawable as? Animatable)?.start()
                        }
                    },
                    update = { imageView ->
                        (imageView.drawable as? Animatable)?.start()
                    },
                    modifier = Modifier.size(138.dp)
                )
                Text(
                    text = stringResource(id = aiGenerationLoadingTitleRes()),
                    color = MenuDadoColors.DeepGreen,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(id = aiGenerationLoadingMessageRes()),
                    color = MenuDadoColors.MutedInk,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private enum class MenuDadoDestination {
    HOME,
    PROFILE,
    ABOUT
}

internal fun menuDadoDrawerWidthDp(screenWidthDp: Int): Int =
    maxOf(256, minOf(304, screenWidthDp - 72))

internal fun menuDadoDrawerContainerColor(): Color = MenuDadoColors.Surface

internal fun menuDadoDrawerSelectedContainerColor(): Color = MenuDadoColors.BrandGreen

internal fun menuDadoDrawerSelectedTextColor(): Color = Color.White

internal fun menuDadoDrawerUnselectedTextColor(): Color = MenuDadoColors.Ink

internal fun menuDadoDrawerItemCornerRadiusDp(): Int = 8

internal fun aiGenerationLoadingImageRes(): Int = R.drawable.dado_loading

@StringRes
internal fun aiGenerationLoadingTitleRes(): Int = R.string.ai_generation_loading_title

@StringRes
internal fun aiGenerationLoadingMessageRes(): Int = R.string.ai_generation_loading_message

internal fun aiGenerationLoadingOverlayColor(): Color = MenuDadoColors.DeepGreen.copy(alpha = 0.72f)

internal fun aiGenerationLoadingCardColor(): Color = MenuDadoColors.Surface

internal fun aiGenerationLoadingBlocksTouches(): Boolean = true

internal fun menuDadoHeaderBrandTopPaddingDp(): Int = 0

internal fun menuDadoHeaderBrandStartGapDp(): Int = 6

internal fun menuDadoWordmarkVisualStartInsetDp(): Int = 15

internal fun menuDadoHeaderSymbolSizeDp(): Int = 61

internal fun menuDadoHeaderWordmarkHeightDp(): Int = 34

internal fun menuDadoHeaderSubtitleFontSizeSp(): Int = 14

internal fun menuDadoHeaderSubtitleTopOffsetDp(): Int = -5

internal data class OnboardingStep(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int
)

internal fun onboardingSteps(): List<OnboardingStep> = listOf(
    OnboardingStep(
        titleRes = R.string.onboarding_profile_title,
        bodyRes = R.string.onboarding_profile_body
    ),
    OnboardingStep(
        titleRes = R.string.onboarding_menus_title,
        bodyRes = R.string.onboarding_menus_body
    ),
    OnboardingStep(
        titleRes = R.string.onboarding_ai_title,
        bodyRes = R.string.onboarding_ai_body
    ),
    OnboardingStep(
        titleRes = R.string.onboarding_dice_title,
        bodyRes = R.string.onboarding_dice_body
    )
)

internal fun onboardingStepAfterSwipe(
    currentStep: Int,
    stepCount: Int,
    dragAmount: Float
): Int {
    return when {
        dragAmount <= -ONBOARDING_SWIPE_THRESHOLD -> minOf(currentStep + 1, stepCount - 1)
        dragAmount >= ONBOARDING_SWIPE_THRESHOLD -> maxOf(currentStep - 1, 0)
        else -> currentStep
    }
}

private const val ONBOARDING_SWIPE_THRESHOLD = 56f

internal data class AboutAppInfo(
    val creator: String,
    val contact: String
)

internal fun aboutAppInfo(): AboutAppInfo = AboutAppInfo(
    creator = "Rhonal A. Delgado Padilla",
    contact = "rhonal.delgado@gmail.com"
)

@Composable
private fun AboutAppSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MenuDadoColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(id = R.string.about_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MenuDadoColors.Ink
            )
            Text(
                text = stringResource(id = R.string.about_reason),
                style = MaterialTheme.typography.bodyLarge,
                color = MenuDadoColors.MutedInk
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(id = R.string.about_created_by),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = MenuDadoColors.DeepGreen
                )
                Text(
                    text = aboutAppInfo().creator,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MenuDadoColors.Ink
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(id = R.string.about_contact),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = MenuDadoColors.DeepGreen
                )
                Text(
                    text = aboutAppInfo().contact,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MenuDadoColors.MutedInk
                )
            }
            Text(
                text = stringResource(id = R.string.about_version, BuildConfig.VERSION_NAME),
                modifier = Modifier.align(Alignment.End),
                style = MaterialTheme.typography.labelSmall,
                color = MenuDadoColors.MutedInk
            )
        }
    }
}

@Composable
private fun DietaryProfileSection(
    audience: MenuAudience,
    audienceAgeRanges: Map<MenuAudience, String>,
    enabledAudienceCount: Int,
    profile: DietaryProfile,
    onAudienceChanged: (MenuAudience) -> Unit,
    onAudienceEnabledChanged: (Boolean) -> Unit,
    onPregnantChanged: (Boolean) -> Unit,
    onVeganChanged: (Boolean) -> Unit,
    onHasAllergiesChanged: (Boolean) -> Unit,
    onAllergenToggled: (DietaryAllergen) -> Unit,
    onOtherAvoidancesChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MenuDadoColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(id = R.string.dietary_profile_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MenuDadoColors.Ink
            )
            Text(
                text = stringResource(id = R.string.dietary_profile_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MenuDadoColors.MutedInk
            )
            ProfileAudienceFilter(
                selected = audience,
                ageRanges = audienceAgeRanges,
                onSelected = { selectedAudience ->
                    onAudienceChanged(selectedAudience)
                }
            )
            if (audience == MenuAudience.BABY) {
                Text(
                    text = stringResource(id = R.string.dietary_profile_baby_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MenuDadoColors.MutedInk
                )
            }
            DietarySwitchRow(
                title = stringResource(id = R.string.dietary_active),
                checked = profile.isEnabled,
                enabled = !profile.isEnabled || enabledAudienceCount > 1,
                onCheckedChange = onAudienceEnabledChanged
            )
            if (audience == MenuAudience.ADULT) {
                DietarySwitchRow(
                    title = stringResource(id = R.string.dietary_pregnant),
                    checked = profile.isPregnant,
                    enabled = profile.isEnabled,
                    onCheckedChange = onPregnantChanged
                )
            }
            DietarySwitchRow(
                title = stringResource(id = R.string.dietary_vegan),
                checked = profile.isVegan,
                enabled = profile.isEnabled,
                onCheckedChange = onVeganChanged
            )
            DietarySwitchRow(
                title = stringResource(id = R.string.dietary_allergies),
                checked = profile.hasAllergies,
                enabled = profile.isEnabled,
                onCheckedChange = onHasAllergiesChanged
            )
            AnimatedVisibility(visible = profile.isEnabled && profile.hasAllergies) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DietaryAllergen.entries.forEach { allergen ->
                        DietaryAllergenRow(
                            allergen = allergen,
                            checked = allergen in profile.allergens,
                            onCheckedChange = { onAllergenToggled(allergen) }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = profile.otherAvoidances,
                onValueChange = onOtherAvoidancesChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.dietary_other_label)) },
                placeholder = { Text(stringResource(id = R.string.dietary_other_placeholder)) },
                enabled = profile.isEnabled,
                minLines = 2
            )
        }
    }
}

@Composable
private fun DietarySwitchRow(
    title: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (enabled) MenuDadoColors.Ink else MenuDadoColors.MutedInk.copy(alpha = 0.62f)
        )
        MenuDadoBrandSwitch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun DietaryAllergenRow(
    allergen: DietaryAllergen,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = stringResource(id = dietaryAllergenLabelRes(allergen)),
            color = MenuDadoColors.Ink,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun AiQuotaDialog(
    message: String,
    retryAtMillis: Long,
    onDismiss: () -> Unit
) {
    var nowMillis by remember(retryAtMillis) { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(retryAtMillis) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
        }
    }

    val remainingMillis = (retryAtMillis - nowMillis).coerceAtLeast(0L)
    val canRetry = remainingMillis == 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MenuDadoColors.BrandGreen)
            ) {
                Text(
                    if (canRetry) {
                        stringResource(id = R.string.common_understood)
                    } else {
                        stringResource(id = R.string.common_close)
                    }
                )
            }
        },
        title = {
            Text(
                text = if (canRetry) {
                    stringResource(id = R.string.ai_retry_ready_title)
                } else {
                    stringResource(id = R.string.ai_retry_wait_title)
                },
                color = MenuDadoColors.Ink,
                fontWeight = FontWeight.Black
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = message,
                    color = MenuDadoColors.MutedInk
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (canRetry) {
                                MenuDadoColors.Avocado.copy(alpha = 0.22f)
                            } else {
                                MenuDadoColors.EggYellow.copy(alpha = 0.18f)
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (canRetry) {
                            stringResource(id = R.string.ai_retry_ready_badge)
                        } else {
                            stringResource(id = R.string.ai_retry_wait_badge)
                        },
                        color = MenuDadoColors.Ink,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = if (canRetry) {
                        stringResource(id = R.string.ai_retry_ready_body)
                    } else {
                        stringResource(id = R.string.ai_retry_wait_body)
                    },
                    color = MenuDadoColors.MutedInk,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    )
}

@Composable
private fun Header(onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MenuDadoColors.HeaderGreen)
            .statusBarsPadding()
            .padding(start = 24.dp, top = 16.dp, end = 20.dp, bottom = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(menuDadoHeaderBrandStartGapDp().dp)
    ) {
        IconButton(
            onClick = onMenuClick
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_menu),
                contentDescription = stringResource(id = R.string.nav_home),
                tint = Color.White
            )
        }
        Image(
            painter = painterResource(id = R.drawable.menu_dado_symbol),
            contentDescription = stringResource(id = R.string.app_name),
            modifier = Modifier.size(menuDadoHeaderSymbolSizeDp().dp),
            contentScale = ContentScale.Fit
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.menu_dado_wordmark),
                contentDescription = stringResource(id = R.string.app_name),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(menuDadoHeaderWordmarkHeightDp().dp)
                    .offset(x = (-menuDadoWordmarkVisualStartInsetDp()).dp),
                alignment = Alignment.CenterStart,
                contentScale = ContentScale.Fit
            )
            Text(
                text = stringResource(id = R.string.header_subtitle),
                modifier = Modifier.offset(y = menuDadoHeaderSubtitleTopOffsetDp().dp),
                color = MenuDadoColors.Cream.copy(alpha = 0.9f),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = menuDadoHeaderSubtitleFontSizeSp().sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DiceSection(
    filter: MealType?,
    audienceFilter: MenuAudience?,
    enabledAudiences: List<MenuAudience>,
    isRolling: Boolean,
    diceFaceIndex: Int,
    onFilterChanged: (MealType?) -> Unit,
    onAudienceFilterChanged: (MenuAudience?) -> Unit,
    onRoll: () -> Unit
) {
    val idleRotation by animateFloatAsState(
        targetValue = if (isRolling) 0f else -4f,
        animationSpec = tween(durationMillis = 300),
        label = "idleDiceRotation"
    )
    var manualDiceRotation by remember { mutableStateOf(DiceDragRotation()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MenuDadoColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(id = R.string.dice_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MenuDadoColors.Ink
                )
                Text(
                    text = stringResource(id = R.string.dice_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MenuDadoColors.MutedInk
                )
            }
            CompactMenuSelectors(
                mealType = filter,
                audience = audienceFilter,
                audiences = enabledAudiences,
                onMealTypeSelected = onFilterChanged,
                onAudienceSelected = onAudienceFilterChanged
            )
            if (enabledAudiences.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.dice_profile_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MenuDadoColors.MutedInk
                )
            }
            Button(
                onClick = onRoll,
                enabled = !isRolling,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MenuDadoColors.Tomato,
                    contentColor = Color.White,
                    disabledContainerColor = MenuDadoColors.Tomato.copy(alpha = 0.72f),
                    disabledContentColor = Color.White
                )
            ) {
                AnimatedDiceFace(
                    isRolling = isRolling,
                    idleRotation = idleRotation,
                    idleFaceIndex = diceFaceIndex,
                    manualRotation = manualDiceRotation,
                    onDrag = { dragX, dragY ->
                        manualDiceRotation = manualDiceRotation.afterDrag(dragX, dragY)
                    }
                )
                Spacer(modifier = Modifier.size(12.dp))
                Column(
                    modifier = Modifier.padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = if (isRolling) {
                            stringResource(id = R.string.dice_rolling)
                        } else {
                            stringResource(id = R.string.dice_roll)
                        },
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isRolling) {
                            stringResource(id = R.string.dice_mixing)
                        } else {
                            stringResource(id = R.string.dice_action)
                        },
                        color = Color.White.copy(alpha = 0.86f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedDiceFace(
    isRolling: Boolean,
    idleRotation: Float,
    idleFaceIndex: Int,
    manualRotation: DiceDragRotation,
    onDrag: (dragX: Float, dragY: Float) -> Unit
) {
    val transition = rememberInfiniteTransition(label = "rollingDice")
    val rollingRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 720, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rollingRotation"
    )
    val rollingScale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 260),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rollingScale"
    )
    val rollingLift by transition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 260),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rollingLift"
    )

    val idlePose = DiceRestPoses[idleFaceIndex % DiceRestPoses.size]

    DiceCube3D(
        modifier = Modifier
            .size(66.dp)
            .pointerInput(isRolling) {
                if (!isRolling) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                }
            }
            .graphicsLayer {
                scaleX = if (isRolling) rollingScale else 1f
                scaleY = if (isRolling) rollingScale else 1f
                translationY = if (isRolling) rollingLift else 0f
            },
        rotationX = if (isRolling) rollingRotation * 1.15f else idlePose.rotationX + manualRotation.x,
        rotationY = if (isRolling) rollingRotation * 0.85f else idlePose.rotationY + manualRotation.y,
        rotationZ = if (isRolling) rollingRotation * 0.35f else idlePose.rotationZ + idleRotation
    )
}

private val DiceRestPoses = listOf(
    DiceRestPose(rotationX = 18f, rotationY = -24f, rotationZ = 0f),
    DiceRestPose(rotationX = 18f, rotationY = 66f, rotationZ = -4f),
    DiceRestPose(rotationX = -72f, rotationY = -24f, rotationZ = 6f),
    DiceRestPose(rotationX = 72f, rotationY = -24f, rotationZ = -8f),
    DiceRestPose(rotationX = 18f, rotationY = 156f, rotationZ = 5f),
    DiceRestPose(rotationX = 18f, rotationY = -114f, rotationZ = -6f)
)

private data class DiceRestPose(
    val rotationX: Float,
    val rotationY: Float,
    val rotationZ: Float
)

@Composable
private fun DiceCube3D(
    modifier: Modifier = Modifier,
    rotationX: Float,
    rotationY: Float,
    rotationZ: Float
) {
    val dishImages = rememberDicePlateImages()
    val dishPaint = remember {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            isDither = true
        }
    }
    val faces = listOf(
        DiceCubeFace(center = Vec3(0f, 0f, 1f), u = Vec3(1f, 0f, 0f), v = Vec3(0f, 1f, 0f), value = 1),
        DiceCubeFace(center = Vec3(1f, 0f, 0f), u = Vec3(0f, 0f, -1f), v = Vec3(0f, 1f, 0f), value = 2),
        DiceCubeFace(center = Vec3(0f, -1f, 0f), u = Vec3(1f, 0f, 0f), v = Vec3(0f, 0f, 1f), value = 3),
        DiceCubeFace(center = Vec3(0f, 1f, 0f), u = Vec3(1f, 0f, 0f), v = Vec3(0f, 0f, -1f), value = 4),
        DiceCubeFace(center = Vec3(-1f, 0f, 0f), u = Vec3(0f, 0f, 1f), v = Vec3(0f, 1f, 0f), value = 5),
        DiceCubeFace(center = Vec3(0f, 0f, -1f), u = Vec3(-1f, 0f, 0f), v = Vec3(0f, 1f, 0f), value = 6)
    )

    Canvas(modifier = modifier) {
        val cubeScale = size.minDimension * 0.34f
        val center = Offset(size.width / 2f, size.height / 2f)
        val distance = 4.2f
        val rotatedFaces = faces.map { face ->
            val corners = face.corners().map { it.rotate(rotationX, rotationY, rotationZ) }
            RotatedDiceFace(
                face = face,
                center = face.center.rotate(rotationX, rotationY, rotationZ),
                u = face.u.rotate(rotationX, rotationY, rotationZ),
                v = face.v.rotate(rotationX, rotationY, rotationZ),
                corners = corners
            )
        }.sortedBy { it.center.z }

        rotatedFaces.forEach { rotated ->
            val projectedCorners = rotated.corners.map { it.project(center, cubeScale, distance) }
            val facePath = roundedFacePath(
                points = projectedCorners,
                radius = size.minDimension * 0.1f
            )
            val shade = 0.74f + ((rotated.center.z + 1f) / 2f).coerceIn(0f, 1f) * 0.22f
            val baseColor = if (rotated.center.y < -0.2f) {
                MenuDadoColors.DicePlateBackground
            } else {
                MenuDadoColors.DiceSideWarm
            }
            drawPath(facePath, baseColor.shade(shade))
            clipPath(facePath) {
                drawDicePlateImage(
                    image = dishImages[rotated.face.value - 1],
                    face = rotated,
                    center = center,
                    cubeScale = cubeScale,
                    distance = distance,
                    paint = dishPaint
                )
                drawPath(facePath, Color.Black.copy(alpha = (1f - shade).coerceIn(0f, 1f) * 0.12f))
            }
            drawPath(
                path = facePath,
                color = MenuDadoColors.DiceAccentBrown.copy(alpha = 0.24f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.8.dp.toPx())
            )
            drawPath(
                path = facePath,
                color = MenuDadoColors.DiceLineBrown.copy(alpha = 0.9f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6.dp.toPx())
            )
        }
    }
}

@Composable
private fun rememberDicePlateImages(): List<ImageBitmap> {
    val plate1 = ImageBitmap.imageResource(R.drawable.dice_plate_1)
    val plate2 = ImageBitmap.imageResource(R.drawable.dice_plate_2)
    val plate3 = ImageBitmap.imageResource(R.drawable.dice_plate_3)
    val plate4 = ImageBitmap.imageResource(R.drawable.dice_plate_4)
    val plate5 = ImageBitmap.imageResource(R.drawable.dice_plate_5)
    val plate6 = ImageBitmap.imageResource(R.drawable.dice_plate_6)
    return remember(plate1, plate2, plate3, plate4, plate5, plate6) {
        listOf(
            plate1,
            plate2,
            plate3,
            plate4,
            plate5,
            plate6
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDicePlateImage(
    image: ImageBitmap,
    face: RotatedDiceFace,
    center: Offset,
    cubeScale: Float,
    distance: Float,
    paint: AndroidPaint
) {
    val imageHalfSize = 0.74f
    val topLeft = (face.center + face.u * -imageHalfSize + face.v * -imageHalfSize).project(center, cubeScale, distance)
    val topRight = (face.center + face.u * imageHalfSize + face.v * -imageHalfSize).project(center, cubeScale, distance)
    val bottomLeft = (face.center + face.u * -imageHalfSize + face.v * imageHalfSize).project(center, cubeScale, distance)
    val bottomRight = (face.center + face.u * imageHalfSize + face.v * imageHalfSize).project(center, cubeScale, distance)
    val verts = floatArrayOf(
        topLeft.x,
        topLeft.y,
        topRight.x,
        topRight.y,
        bottomLeft.x,
        bottomLeft.y,
        bottomRight.x,
        bottomRight.y
    )
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawBitmapMesh(
            image.asAndroidBitmap(),
            1,
            1,
            verts,
            0,
            null,
            0,
            paint
        )
    }
}

private fun roundedFacePath(points: List<Offset>, radius: Float): Path {
    val maxCornerRadius = points.zipWithNextCircular()
        .minOf { (start, end) -> start.distanceTo(end) }
        .coerceAtLeast(1f) * 0.24f
    val cornerRadius = radius.coerceAtMost(maxCornerRadius)
    return Path().apply {
        points.forEachIndexed { index, corner ->
            val previous = points[(index - 1 + points.size) % points.size]
            val next = points[(index + 1) % points.size]
            val start = corner.toward(previous, cornerRadius)
            val end = corner.toward(next, cornerRadius)
            if (index == 0) {
                moveTo(start.x, start.y)
            } else {
                lineTo(start.x, start.y)
            }
            quadraticTo(corner.x, corner.y, end.x, end.y)
        }
        close()
    }
}

private fun List<Offset>.zipWithNextCircular(): List<Pair<Offset, Offset>> {
    return indices.map { index -> this[index] to this[(index + 1) % size] }
}

private fun Offset.toward(target: Offset, distance: Float): Offset {
    val dx = target.x - x
    val dy = target.y - y
    val length = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    val ratio = (distance / length).coerceIn(0f, 1f)
    return Offset(x + dx * ratio, y + dy * ratio)
}

private fun Offset.distanceTo(other: Offset): Float {
    val dx = other.x - x
    val dy = other.y - y
    return sqrt(dx * dx + dy * dy)
}

private data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun times(value: Float) = Vec3(x * value, y * value, z * value)
}

private data class DiceCubeFace(
    val center: Vec3,
    val u: Vec3,
    val v: Vec3,
    val value: Int
) {
    fun corners(): List<Vec3> {
        return listOf(
            center + u * -1f + v * -1f,
            center + u * 1f + v * -1f,
            center + u * 1f + v * 1f,
            center + u * -1f + v * 1f
        )
    }
}

private data class RotatedDiceFace(
    val face: DiceCubeFace,
    val center: Vec3,
    val u: Vec3,
    val v: Vec3,
    val corners: List<Vec3>
)

private fun Vec3.rotate(xDegrees: Float, yDegrees: Float, zDegrees: Float): Vec3 {
    val xRad = xDegrees.toRadians()
    val yRad = yDegrees.toRadians()
    val zRad = zDegrees.toRadians()

    val cosX = cos(xRad)
    val sinX = sin(xRad)
    val afterX = Vec3(
        x = x,
        y = y * cosX - z * sinX,
        z = y * sinX + z * cosX
    )

    val cosY = cos(yRad)
    val sinY = sin(yRad)
    val afterY = Vec3(
        x = afterX.x * cosY + afterX.z * sinY,
        y = afterX.y,
        z = -afterX.x * sinY + afterX.z * cosY
    )

    val cosZ = cos(zRad)
    val sinZ = sin(zRad)
    return Vec3(
        x = afterY.x * cosZ - afterY.y * sinZ,
        y = afterY.x * sinZ + afterY.y * cosZ,
        z = afterY.z
    )
}

private fun Vec3.project(center: Offset, scale: Float, distance: Float): Offset {
    val perspective = distance / (distance - z)
    return Offset(
        x = center.x + x * scale * perspective,
        y = center.y + y * scale * perspective
    )
}

private fun Float.toRadians(): Float = this / 180f * PI.toFloat()

private fun Color.shade(factor: Float): Color {
    return Color(
        red = red * factor,
        green = green * factor,
        blue = blue * factor,
        alpha = alpha
    )
}

@Composable
private fun MenuForm(
    state: MenuDadoUiState,
    enabledAudiences: List<MenuAudience>,
    onMealTypeChanged: (MealType) -> Unit,
    onAudienceChanged: (MenuAudience) -> Unit,
    onNameChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onAiBaseIngredientsChanged: (String) -> Unit,
    onGenerate: () -> Unit,
    onSave: () -> Unit
) {
    var selectedModeName by rememberSaveable { mutableStateOf(MenuFormMode.Manual.name) }
    val selectedMode = MenuFormMode.valueOf(selectedModeName)
    val hasAiDraft = selectedMode == MenuFormMode.Ai && (state.calories != null || state.isGeneratingMenu)
    val canUseFormActions = state.formMealType != null &&
        state.formAudience != null &&
        !state.isAnalyzing &&
        !state.isGeneratingMenu

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MenuDadoColors.FormSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(id = R.string.form_add_menu),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = stringResource(id = R.string.form_add_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MenuDadoColors.MutedInk
                )
            }
            MenuFormModeSelector(
                selected = selectedMode,
                onSelected = { mode -> selectedModeName = mode.name }
            )
            CompactMenuSelectors(
                mealType = state.formMealType,
                audience = state.formAudience,
                audiences = enabledAudiences,
                onMealTypeSelected = onMealTypeChanged,
                onAudienceSelected = onAudienceChanged
            )
            if (state.formMealType == null) {
                Text(
                    text = stringResource(id = R.string.form_select_meal),
                    style = MaterialTheme.typography.bodySmall,
                    color = MenuDadoColors.MutedInk
                )
            }
            if (state.formAudience == null) {
                Text(
                    text = if (enabledAudiences.isEmpty()) {
                        stringResource(id = R.string.dice_profile_hint)
                    } else {
                        stringResource(id = R.string.form_select_audience)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MenuDadoColors.MutedInk
                )
            }
            when (selectedMode) {
                MenuFormMode.Manual -> {
                    MenuTextFields(
                        state = state,
                        onNameChanged = onNameChanged,
                        onDescriptionChanged = onDescriptionChanged,
                        onNotesChanged = onNotesChanged
                    )
                    SaveMenuButton(
                        enabled = canUseFormActions,
                        text = stringResource(id = R.string.common_save_menu),
                        onSave = onSave
                    )
                }
                MenuFormMode.Ai -> {
                    OutlinedTextField(
                        value = state.aiBaseIngredients,
                        onValueChange = onAiBaseIngredientsChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(id = R.string.form_base_ingredients)) },
                        placeholder = { Text(stringResource(id = R.string.form_base_ingredients_placeholder)) },
                        minLines = 1
                    )
                    OutlinedButton(
                        onClick = onGenerate,
                        enabled = canUseFormActions,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = generateAiButtonText(
                                isGenerating = state.isGeneratingMenu,
                                isAiPaused = state.aiRetryAtMillis != null,
                                usesRemaining = state.aiUsesRemainingToday,
                                generatingText = stringResource(id = R.string.ai_generating),
                                restingText = stringResource(id = R.string.ai_resting),
                                availableText = stringResource(
                                    id = R.string.ai_generate_with_count,
                                    state.aiUsesRemainingToday
                                )
                            ),
                            modifier = Modifier.padding(vertical = 6.dp),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                    AnimatedVisibility(visible = hasAiDraft) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            MenuTextFields(
                                state = state,
                                onNameChanged = onNameChanged,
                                onDescriptionChanged = onDescriptionChanged,
                                onNotesChanged = onNotesChanged
                            )
                            SaveMenuButton(
                                enabled = canUseFormActions,
                                text = stringResource(id = R.string.common_save_menu),
                                onSave = onSave
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditMenuDialog(
    state: MenuDadoUiState,
    onMealTypeChanged: (MealType) -> Unit,
    onAudienceChanged: (MenuAudience) -> Unit,
    onNameChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onPickImage: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MenuDadoColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(id = R.string.edit_menu_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MenuDadoColors.Ink
                    )
                    Text(
                        text = stringResource(id = R.string.edit_menu_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MenuDadoColors.MutedInk
                    )
                }
                MealTypeFilter(
                    selected = state.editMealType,
                    includeAll = false,
                    onSelected = { mealType -> if (mealType != null) onMealTypeChanged(mealType) }
                )
                AudienceFilter(
                    selected = state.editAudience,
                    audiences = (state.enabledAudiences + listOfNotNull(state.editAudience)).distinct(),
                    includeAll = false,
                    onSelected = { audience -> if (audience != null) onAudienceChanged(audience) }
                )
                MenuPhotoSelector(
                    imageUri = state.editImageUri,
                    mealType = state.editMealType ?: MealType.BREAKFAST,
                    audience = state.editAudience ?: MenuAudience.ADULT,
                    onPickImage = onPickImage
                )
                OutlinedTextField(
                    value = state.editName,
                    onValueChange = onNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(id = R.string.field_menu_name)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.editDescription,
                    onValueChange = onDescriptionChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(id = R.string.field_menu_description)) },
                    minLines = 2
                )
                OutlinedTextField(
                    value = state.editNotes,
                    onValueChange = onNotesChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(id = R.string.field_menu_notes)) },
                    minLines = 1
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.common_cancel),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MenuDadoColors.BrandGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = stringResource(id = R.string.common_save_changes),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private enum class MenuFormMode {
    Manual,
    Ai
}

@Composable
private fun MenuFormModeSelector(
    selected: MenuFormMode,
    onSelected: (MenuFormMode) -> Unit
) {
    MenuDadoSegmentedSwitch(
        options = MenuFormMode.entries.map { mode ->
            MenuDadoSegmentOption(
                value = mode,
                label = when (mode) {
                    MenuFormMode.Manual -> stringResource(id = R.string.form_mode_manual)
                    MenuFormMode.Ai -> stringResource(id = R.string.form_mode_ai)
                }
            )
        },
        selected = selected,
        onSelected = onSelected
    )
}

@Composable
private fun MenuTextFields(
    state: MenuDadoUiState,
    onNameChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit
) {
    OutlinedTextField(
        value = state.name,
        onValueChange = onNameChanged,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(id = R.string.field_menu_name)) },
        singleLine = true
    )
    OutlinedTextField(
        value = state.description,
        onValueChange = onDescriptionChanged,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(id = R.string.field_menu_description)) },
        minLines = 2
    )
    OutlinedTextField(
        value = state.notes,
        onValueChange = onNotesChanged,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(id = R.string.field_menu_notes)) },
        minLines = 1
    )
}

@Composable
private fun SaveMenuButton(
    enabled: Boolean,
    text: String,
    onSave: () -> Unit
) {
    Button(
        onClick = onSave,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MenuDadoColors.BrandGreen,
            contentColor = Color.White
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 8.dp),
            fontWeight = FontWeight.Bold
        )
    }
}

private data class MenuDadoSegmentOption<T>(
    val value: T,
    val label: String
)

@Composable
private fun <T> MenuDadoSegmentedSwitch(
    options: List<MenuDadoSegmentOption<T>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MenuDadoColors.Cream)
            .border(
                BorderStroke(1.dp, MenuDadoColors.OutlineBrown.copy(alpha = 0.24f)),
                RoundedCornerShape(12.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { option ->
            val isSelected = option.value == selected
            val containerColor = if (isSelected) MenuDadoColors.BrandGreen else Color.Transparent
            val textColor = if (isSelected) Color.White else MenuDadoColors.DeepGreen
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(containerColor)
                    .clickable { onSelected(option.value) }
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 8.dp else 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isSelected) MenuDadoColors.EggYellow else MenuDadoColors.OutlineBrown.copy(alpha = 0.42f)
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun MenuDadoBrandSwitch(
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val trackColor = when {
        !enabled -> MenuDadoColors.SoftSand.copy(alpha = 0.48f)
        checked -> MenuDadoColors.BrandGreen
        else -> MenuDadoColors.SoftSand
    }
    val knobColor = when {
        !enabled -> MenuDadoColors.Surface.copy(alpha = 0.72f)
        checked -> MenuDadoColors.EggYellow
        else -> MenuDadoColors.Surface
    }
    val knobAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    Box(
        modifier = Modifier
            .width(64.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(trackColor)
            .border(
                BorderStroke(
                    1.dp,
                    when {
                        !enabled -> MenuDadoColors.OutlineBrown.copy(alpha = 0.14f)
                        checked -> MenuDadoColors.DeepGreen.copy(alpha = 0.38f)
                        else -> MenuDadoColors.OutlineBrown.copy(alpha = 0.28f)
                    }
                ),
                RoundedCornerShape(18.dp)
            )
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(4.dp),
        contentAlignment = knobAlignment
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(knobColor)
                .border(
                    BorderStroke(1.dp, Color.White.copy(alpha = 0.72f)),
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when {
                            !enabled -> MenuDadoColors.OutlineBrown.copy(alpha = 0.28f)
                            checked -> MenuDadoColors.DeepGreen
                            else -> MenuDadoColors.OutlineBrown.copy(alpha = 0.48f)
                        }
                    )
            )
        }
    }
}

@Composable
private fun MealTypeFilter(
    selected: MealType?,
    includeAll: Boolean,
    onSelected: (MealType?) -> Unit
) {
    val options = buildList<MenuDadoSegmentOption<MealType?>> {
        if (includeAll) {
            add(MenuDadoSegmentOption(value = null, label = stringResource(id = R.string.common_all)))
        }
        addAll(MealType.entries.map { mealType ->
            MenuDadoSegmentOption(value = mealType, label = stringResource(id = mealTypeLabelRes(mealType)))
        })
    }

    MenuDadoSegmentedSwitch(
        options = options,
        selected = selected,
        onSelected = onSelected
    )
}

@Composable
private fun CompactMenuSelectors(
    mealType: MealType?,
    audience: MenuAudience?,
    audiences: List<MenuAudience>,
    onMealTypeSelected: (MealType) -> Unit,
    onAudienceSelected: (MenuAudience) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CompactDropdownSelector(
            label = stringResource(id = R.string.selector_meal),
            value = mealType?.let { stringResource(id = mealTypeLabelRes(it)) }
                ?: stringResource(id = R.string.common_choose),
            options = MealType.entries,
            optionLabel = { stringResource(id = mealTypeLabelRes(it)) },
            onSelected = onMealTypeSelected,
            modifier = Modifier.weight(1f)
        )
        CompactDropdownSelector(
            label = stringResource(id = R.string.selector_audience),
            value = audience?.let { stringResource(id = menuAudienceLabelRes(it)) }
                ?: stringResource(id = R.string.common_choose),
            options = audiences,
            optionLabel = { stringResource(id = menuAudienceLabelRes(it)) },
            onSelected = onAudienceSelected,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun <T> CompactDropdownSelector(
    label: String,
    value: String,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val enabled = options.isNotEmpty()

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MenuDadoColors.Cream)
                .border(
                    BorderStroke(1.dp, MenuDadoColors.OutlineBrown.copy(alpha = 0.24f)),
                    RoundedCornerShape(8.dp)
                )
                .clickable(enabled = enabled) { expanded = true }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MenuDadoColors.MutedInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (enabled) value else stringResource(id = R.string.common_no_options),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = if (enabled) MenuDadoColors.DeepGreen else MenuDadoColors.MutedInk.copy(alpha = 0.62f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "▾",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (enabled) MenuDadoColors.Tomato else MenuDadoColors.MutedInk.copy(alpha = 0.62f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun AudienceFilter(
    selected: MenuAudience?,
    audiences: List<MenuAudience> = MenuAudience.entries,
    includeAll: Boolean,
    onSelected: (MenuAudience?) -> Unit
) {
    val options = buildList<MenuDadoSegmentOption<MenuAudience?>> {
        if (includeAll) {
            add(MenuDadoSegmentOption(value = null, label = stringResource(id = R.string.common_all)))
        }
        addAll(audiences.map { audience ->
            MenuDadoSegmentOption(value = audience, label = stringResource(id = menuAudienceLabelRes(audience)))
        })
    }

    MenuDadoSegmentedSwitch(
        options = options,
        selected = selected,
        onSelected = onSelected
    )
}

@Composable
private fun ProfileAudienceFilter(
    selected: MenuAudience,
    ageRanges: Map<MenuAudience, String>,
    onSelected: (MenuAudience) -> Unit
) {
    val options = MenuAudience.entries.map { audience ->
        MenuDadoSegmentOption(
            value = audience,
            label = stringResource(id = menuAudienceLabelRes(audience))
        )
    }
    val selectedRange = ageRanges[selected].orEmpty().ifBlank { selected.defaultAgeRange }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MenuDadoSegmentedSwitch(
            options = options,
            selected = selected,
            onSelected = onSelected
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MenuAudience.entries.forEach { audience ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (audience == selected) {
                        Text(
                            text = selectedRange,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MenuDadoColors.MutedInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CaloriesPill(calories: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MenuDadoColors.EggYellow.copy(alpha = 0.24f))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = stringResource(id = R.string.calories_approx, calories),
            color = MenuDadoColors.DeepGreen,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun EmptyState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MenuDadoColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.empty_menus),
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MenuDadoColors.MutedInk
        )
    }
}

@Composable
private fun PendingAnalysisButton(
    isAnalyzing: Boolean,
    isAiPaused: Boolean,
    usesRemaining: Int,
    onAnalyzePending: () -> Unit
) {
    OutlinedButton(
        onClick = onAnalyzePending,
        enabled = !isAnalyzing,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = pendingAnalysisButtonText(
                isAnalyzing = isAnalyzing,
                isAiPaused = isAiPaused,
                usesRemaining = usesRemaining,
                analyzingText = stringResource(id = R.string.ai_analyzing_pending),
                restingText = stringResource(id = R.string.ai_resting),
                availableText = stringResource(id = R.string.ai_analyze_pending_with_count, usesRemaining)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false
        )
    }
}

@Composable
private fun MenuCarouselSections(
    menus: List<FoodMenu>,
    enabledAudiences: List<MenuAudience>,
    onViewMore: (MenuAudience) -> Unit,
    onOpenMenu: (FoodMenu) -> Unit,
    onPickImage: (FoodMenu) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        menuCarouselAudiencesWithMenus(menus, enabledAudiences).forEach { audience ->
            MenuCarouselSection(
                audience = audience,
                menus = menuCarouselVisibleMenus(
                    menus = menus,
                    audience = audience,
                    isExpanded = false
                ),
                showToggle = menuCarouselShowsToggle(menus, audience),
                onViewMore = { onViewMore(audience) },
                onOpenMenu = onOpenMenu,
                onPickImage = onPickImage
            )
        }
    }
}

@Composable
private fun MenuCarouselSection(
    audience: MenuAudience,
    menus: List<FoodMenu>,
    showToggle: Boolean,
    onViewMore: () -> Unit,
    onOpenMenu: (FoodMenu) -> Unit,
    onPickImage: (FoodMenu) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = menuAudienceLabelRes(audience)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MenuDadoColors.Ink
            )
            if (showToggle) {
                TextButton(
                    onClick = onViewMore,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.view_more),
                        color = MenuDadoColors.DeepGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = menus, key = { it.id }) { menu ->
                MenuCarouselItem(
                    menu = menu,
                    onOpenMenu = { onOpenMenu(menu) },
                    onPickImage = { onPickImage(menu) }
                )
            }
        }
    }
}

@Composable
private fun MenuCarouselItem(
    menu: FoodMenu,
    onOpenMenu: () -> Unit,
    onPickImage: () -> Unit,
    modifier: Modifier = Modifier.width(150.dp)
) {
    Card(
        modifier = modifier
            .clickable(onClick = onOpenMenu)
            .semantics { contentDescription = menu.name },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MenuCoverImage(
                menu = menu,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp),
                showMealTypeLabel = true,
                onPickImage = onPickImage
            )
            Text(
                text = menu.name,
                modifier = Modifier.height(menuCarouselItemTitleHeightDp().dp),
                color = MenuDadoColors.Ink,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.height(menuCarouselItemMetaRowHeightDp().dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactHealthChip(status = menu.healthAnalysis?.status ?: HealthStatus.UNKNOWN)
                menuVisibleCalories(menu)?.let { calories ->
                    CompactCaloriesText(calories = calories)
                }
            }
        }
    }
}

@Composable
private fun MenuAudienceDetailScreen(
    audience: MenuAudience,
    menus: List<FoodMenu>,
    onBack: () -> Unit,
    onOpenMenu: (FoodMenu) -> Unit,
    onPickImage: (FoodMenu) -> Unit
) {
    val groups = menuAudienceDetailGroups(menus, audience)
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TextButton(
                    onClick = onBack,
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.common_back),
                        color = MenuDadoColors.DeepGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = stringResource(id = menuAudienceLabelRes(audience)),
                    color = MenuDadoColors.Ink,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
            }
            Text(
                text = stringResource(id = R.string.menu_count, groups.sumOf { it.menus.size }),
                color = MenuDadoColors.MutedInk,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        groups.forEach { group ->
            MenuAudienceMealGroupSection(
                group = group,
                onOpenMenu = onOpenMenu,
                onPickImage = onPickImage
            )
        }
    }
}

@Composable
private fun MenuAudienceMealGroupSection(
    group: MenuAudienceMealGroup,
    onOpenMenu: (FoodMenu) -> Unit,
    onPickImage: (FoodMenu) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(id = mealTypeLabelRes(group.mealType)),
            color = MenuDadoColors.Ink,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black
        )
        group.menus.chunked(2).forEach { rowMenus ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                rowMenus.forEach { menu ->
                    MenuCarouselItem(
                        menu = menu,
                        onOpenMenu = { onOpenMenu(menu) },
                        onPickImage = { onPickImage(menu) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowMenus.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MenuCoverImage(
    menu: FoodMenu,
    modifier: Modifier = Modifier,
    showMealTypeLabel: Boolean,
    isLoading: Boolean = false,
    onPickImage: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val bitmap = remember(menu.imageUri) {
        menu.imageUri?.let { uri ->
            runCatching {
                if (uri.startsWith("content://") || uri.startsWith("file://")) {
                    context.contentResolver.openInputStream(uri.toUri())?.use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                } else {
                    BitmapFactory.decodeFile(uri)
                }
            }.getOrNull()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(menu.mealType.carouselCoverColor()),
        contentAlignment = Alignment.BottomStart
    ) {
        val hasBitmap = bitmap != null
        if (hasBitmap) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f))
            )
        } else {
            MenuImageSkeleton(isLoading = isLoading)
        }

        if (showMealTypeLabel && hasBitmap) {
            MenuCoverMealTypeLabel(menu = menu)
        }

        if (onPickImage != null) {
            MenuPhotoActionButton(
                hasImage = hasBitmap,
                onPickImage = onPickImage,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
private fun BoxScope.MenuCoverMealTypeLabel(menu: FoodMenu) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        menuCoverLabelGradientColor(menu).copy(alpha = 0.86f),
                        menuCoverLabelGradientColor(menu).copy(alpha = 0.58f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(
            text = stringResource(id = mealTypeLabelRes(menu.mealType)),
            color = menuCoverMealTypeLabelTextColor(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MenuPhotoActionButton(
    hasImage: Boolean,
    onPickImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actionDescription = stringResource(id = menuPhotoActionContentDescriptionRes(hasImage))
    IconButton(
        onClick = onPickImage,
        modifier = modifier
            .padding(menuPhotoActionButtonInsetDp().dp)
            .size(menuPhotoActionButtonSizeDp().dp)
            .background(menuPhotoActionButtonBackgroundColor(), CircleShape)
            .semantics {
                contentDescription = actionDescription
            }
    ) {
        Icon(
            painter = painterResource(id = menuPhotoActionIconRes()),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = menuPhotoActionIconTint()
        )
    }
}

@Composable
private fun MenuPhotoSourceDialog(
    onTakePhoto: () -> Unit,
    onChooseFromLibrary: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = menuPhotoSourceSheetHorizontalPaddingDp().dp)
                .navigationBarsPadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MenuDadoColors.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.menu_dado_symbol),
                            contentDescription = null,
                            modifier = Modifier.size(42.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(id = R.string.photo_source_title),
                                color = MenuDadoColors.Ink,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = stringResource(id = R.string.photo_source_subtitle),
                                color = MenuDadoColors.MutedInk,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    MenuPhotoSourceOption(
                        iconRes = R.drawable.ic_photo_camera,
                        title = stringResource(id = R.string.photo_take),
                        description = stringResource(id = R.string.photo_take_description),
                        onClick = onTakePhoto
                    )
                    MenuPhotoSourceOption(
                        iconRes = R.drawable.ic_photo_library,
                        title = stringResource(id = R.string.photo_library),
                        description = stringResource(id = R.string.photo_library_description),
                        onClick = onChooseFromLibrary
                    )

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.common_cancel),
                            color = MenuDadoColors.DeepGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuPhotoSourceOption(
    iconRes: Int,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(menuPhotoSourceOptionMinHeightDp().dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MenuDadoColors.Cream.copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MenuDadoColors.OutlineBrown.copy(alpha = 0.18f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MenuDadoColors.Avocado.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MenuDadoColors.DeepGreen
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    color = MenuDadoColors.Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    color = MenuDadoColors.MutedInk,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MenuImageSkeleton(
    isLoading: Boolean
) {
    val alpha = if (isLoading) {
        val transition = rememberInfiniteTransition(label = "menuImageSkeleton")
        val pulse by transition.animateFloat(
            initialValue = 0.46f,
            targetValue = 0.82f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 820),
                repeatMode = RepeatMode.Reverse
            ),
            label = "menuImageSkeletonPulse"
        )
        pulse
    } else {
        1f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F1F1)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = menuImagePlaceholderDrawableRes()),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(menuImagePlaceholderSizePercent() / 100f)
                .alpha(if (isLoading) alpha else 1f),
            contentScale = ContentScale.Fit,
            colorFilter = if (menuImagePlaceholderUsesColorTint()) {
                ColorFilter.tint(Color(0xFF9E9E9E))
            } else {
                null
            }
        )
    }
}

@Composable
private fun MenuPhotoSelector(
    imageUri: String?,
    mealType: MealType,
    audience: MenuAudience,
    onPickImage: () -> Unit
) {
    val previewMenu = FoodMenu(
        name = stringResource(id = R.string.photo_add),
        mealType = mealType,
        audience = audience,
        description = "",
        imageUri = imageUri
    )

    MenuCoverImage(
        menu = previewMenu,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        showMealTypeLabel = false,
        onPickImage = onPickImage
    )
}

@Composable
private fun CompactHealthChip(status: HealthStatus) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(status.accentColor().copy(alpha = 0.24f))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = stringResource(id = healthStatusLabelRes(status)),
            color = MenuDadoColors.Ink,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false
        )
    }
}

@Composable
private fun CompactCaloriesText(calories: Int) {
    Text(
        text = stringResource(id = R.string.calories_short, calories),
        color = MenuDadoColors.MutedInk,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        softWrap = false
    )
}

@Composable
private fun MenuDetailDialog(
    menu: FoodMenu,
    isAnalyzing: Boolean,
    aiUsesRemainingToday: Int,
    isAiPaused: Boolean,
    onAnalyze: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 14.dp),
            colors = CardDefaults.cardColors(containerColor = MenuDadoColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                    contentAlignment = Alignment.BottomStart
                ) {
                    MenuCoverImage(
                        menu = menu,
                        modifier = Modifier.fillMaxSize(),
                        showMealTypeLabel = false
                    )
                    MenuDetailHeroScrim(menu = menu)
                    MenuDetailHeroText(menu = menu)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HealthChip(status = menu.healthAnalysis?.status ?: HealthStatus.UNKNOWN)
                        menuVisibleCalories(menu)?.let { calories ->
                            CaloriesPill(calories = calories)
                        }
                    }
                    Text(
                        text = menu.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MenuDadoColors.Ink
                    )
                    if (menu.notes.isNotBlank()) {
                        Text(
                            text = menu.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MenuDadoColors.MutedInk
                        )
                    }
                    menu.healthAnalysis?.let { analysis ->
                        HealthAnalysisPanel(analysis = analysis)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (menu.healthAnalysis == null) {
                        OutlinedButton(
                            onClick = onAnalyze,
                            enabled = !isAnalyzing,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = analyzeAiButtonText(
                                    isAnalyzing = isAnalyzing,
                                    isAiPaused = isAiPaused,
                                    usesRemaining = aiUsesRemainingToday,
                                    analyzingText = stringResource(id = R.string.ai_analyzing),
                                    restingText = stringResource(id = R.string.ai_resting),
                                    availableText = stringResource(
                                        id = R.string.ai_analyze_with_count,
                                        aiUsesRemainingToday
                                    )
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                        EditMenuButton(onEdit = onEdit, compact = true)
                        DeleteMenuButton(onDelete = onDelete, compact = true)
                    } else {
                        EditMenuButton(
                            onEdit = onEdit,
                            compact = false,
                            modifier = Modifier.weight(1f)
                        )
                        DeleteMenuButton(
                            onDelete = onDelete,
                            compact = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, bottom = 14.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.common_close),
                        color = MenuDadoColors.DeepGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteMenuConfirmationDialog(
    menu: FoodMenu,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(id = R.string.common_delete),
                    color = MenuDadoColors.Tomato,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(id = R.string.common_cancel),
                    color = MenuDadoColors.DeepGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        title = {
            Text(
                text = stringResource(id = menuDeleteConfirmationTitleRes()),
                color = MenuDadoColors.Ink,
                fontWeight = FontWeight.Black
            )
        },
        text = {
            Text(
                text = stringResource(id = menuDeleteConfirmationMessageRes(), menu.name),
                color = MenuDadoColors.Ink
            )
        },
        containerColor = MenuDadoColors.Surface
    )
}

@Composable
private fun MenuCard(
    menu: FoodMenu,
    isExpanded: Boolean,
    isAnalyzing: Boolean,
    aiUsesRemainingToday: Int,
    isAiPaused: Boolean,
    onToggleExpanded: () -> Unit,
    onAnalyze: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val headerHealthStatus = menuHeaderHealthStatus(menu, isExpanded)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onToggleExpanded)
            .semantics {
                contentDescription = if (isExpanded) {
                    "Contraer menu ${menu.name}"
                } else {
                    "Expandir menu ${menu.name}"
                }
            },
        colors = CardDefaults.cardColors(containerColor = MenuDadoColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = menu.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MenuDadoColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${stringResource(id = mealTypeLabelRes(menu.mealType))} · ${stringResource(id = menuAudienceLabelRes(menu.audience))}",
                        color = MenuDadoColors.Tomato
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        headerHealthStatus?.let { status ->
                            HealthChip(status = status)
                        }
                        menuVisibleCalories(menu)?.let { calories ->
                            CaloriesPill(calories = calories)
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(34.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = menuExpandToggleIconRes(isExpanded)),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        colorFilter = ColorFilter.tint(MenuDadoColors.BrandGreen)
                    )
                }
            }
            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = menu.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MenuDadoColors.Ink
                    )
                    if (menu.notes.isNotBlank()) {
                        Text(
                            text = menu.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MenuDadoColors.MutedInk
                        )
                    }
                    menu.healthAnalysis?.let { analysis ->
                        HealthAnalysisPanel(analysis = analysis)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (menu.healthAnalysis == null) {
                            OutlinedButton(
                                onClick = onAnalyze,
                                enabled = !isAnalyzing,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = analyzeAiButtonText(
                                        isAnalyzing = isAnalyzing,
                                        isAiPaused = isAiPaused,
                                        usesRemaining = aiUsesRemainingToday,
                                        analyzingText = stringResource(id = R.string.ai_analyzing),
                                        restingText = stringResource(id = R.string.ai_resting),
                                        availableText = stringResource(
                                            id = R.string.ai_analyze_with_count,
                                            aiUsesRemainingToday
                                        )
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }
                            EditMenuButton(
                                onEdit = onEdit,
                                compact = true
                            )
                            DeleteMenuButton(
                                onDelete = onDelete,
                                compact = true
                            )
                        } else {
                            EditMenuButton(
                                onEdit = onEdit,
                                compact = false,
                                modifier = Modifier.weight(1f)
                            )
                            DeleteMenuButton(
                                onDelete = onDelete,
                                compact = false,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun nextExpandedMenuIdAfterMenuClick(
    currentExpandedMenuId: Long?,
    clickedMenuId: Long
): Long? {
    return if (currentExpandedMenuId == clickedMenuId) null else clickedMenuId
}

internal fun menuShouldOpenDetailFromDiceResult(result: FoodMenu?): Boolean {
    return result != null
}

internal fun menuDetailMenuIdAfterDiceResult(
    currentDetailMenuId: Long?,
    result: FoodMenu?
): Long? {
    return result?.id ?: currentDetailMenuId
}

internal fun menuDeleteConfirmationMenuIdAfterDeleteClick(menu: FoodMenu): Long {
    return menu.id
}

internal fun menuDeleteConfirmationMenuIdAfterDismiss(): Long? = null

@StringRes
internal fun menuDeleteConfirmationTitleRes(): Int = R.string.delete_menu_confirmation_title

@StringRes
internal fun menuDeleteConfirmationMessageRes(): Int = R.string.delete_menu_confirmation_message

internal fun menuCarouselVisibleMenus(
    menus: List<FoodMenu>,
    audience: MenuAudience,
    isExpanded: Boolean
): List<FoodMenu> {
    val audienceMenus = menus
        .filter { it.audience == audience }
        .sortedByDescending { it.createdAt }

    return if (isExpanded) {
        audienceMenus
    } else {
        audienceMenus.take(MenuCarouselCollapsedLimit)
    }
}

internal fun menuCarouselAudiencesWithMenus(
    menus: List<FoodMenu>,
    enabledAudiences: List<MenuAudience>
): List<MenuAudience> {
    return MenuAudience.entries.filter { audience ->
        audience in enabledAudiences &&
        menus.any { it.audience == audience }
    }
}

internal fun menuCarouselShowsToggle(menus: List<FoodMenu>, audience: MenuAudience): Boolean {
    return menus.any { it.audience == audience }
}

internal fun nextExpandedCarouselAudience(
    currentExpandedAudience: MenuAudience?,
    clickedAudience: MenuAudience
): MenuAudience? {
    return if (currentExpandedAudience == clickedAudience) null else clickedAudience
}

internal data class MenuAudienceMealGroup(
    val mealType: MealType,
    val menus: List<FoodMenu>
)

internal fun menuAudienceDetailRoute(audience: MenuAudience): String = audience.name

internal fun menuAudienceDetailRouteAfterViewMore(audience: MenuAudience): String {
    return menuAudienceDetailRoute(audience)
}

internal fun menuAudienceFromDetailRoute(route: String?): MenuAudience? {
    return route?.let { runCatching { MenuAudience.valueOf(it) }.getOrNull() }
}

internal fun menuAudienceDetailRouteAfterBack(currentRoute: String?): String? {
    return if (currentRoute == null) null else null
}

internal fun menuListStateKeyForAudienceDetail(route: String?): String {
    return if (route == null) "home" else "audience-detail"
}

internal fun menuShouldResetAudienceDetailScroll(route: String?): Boolean {
    return route != null
}

internal fun menuAudienceDetailGroups(
    menus: List<FoodMenu>,
    audience: MenuAudience
): List<MenuAudienceMealGroup> {
    return MealType.entries.mapNotNull { mealType ->
        val mealMenus = menus
            .filter { it.audience == audience && it.mealType == mealType }
            .sortedByDescending { it.createdAt }
        if (mealMenus.isEmpty()) {
            null
        } else {
            MenuAudienceMealGroup(mealType = mealType, menus = mealMenus)
        }
    }
}

@StringRes
internal fun mealTypeLabelRes(mealType: MealType): Int {
    return when (mealType) {
        MealType.BREAKFAST -> R.string.meal_breakfast
        MealType.LUNCH -> R.string.meal_lunch
        MealType.DINNER -> R.string.meal_dinner
    }
}

@StringRes
internal fun menuAudienceLabelRes(audience: MenuAudience): Int {
    return when (audience) {
        MenuAudience.ADULT -> R.string.audience_adult
        MenuAudience.CHILD -> R.string.audience_child
        MenuAudience.BABY -> R.string.audience_baby
    }
}

@StringRes
internal fun healthStatusLabelRes(status: HealthStatus): Int {
    return when (status) {
        HealthStatus.HEALTHY -> R.string.health_healthy
        HealthStatus.IMPROVABLE -> R.string.health_improvable
        HealthStatus.UNHEALTHY -> R.string.health_unhealthy
        HealthStatus.UNKNOWN -> R.string.health_unknown
    }
}

@StringRes
internal fun dietaryAllergenLabelRes(allergen: DietaryAllergen): Int {
    return when (allergen) {
        DietaryAllergen.GLUTEN -> R.string.allergen_gluten
        DietaryAllergen.DAIRY -> R.string.allergen_dairy
        DietaryAllergen.EGG -> R.string.allergen_egg
        DietaryAllergen.TREE_NUTS -> R.string.allergen_tree_nuts
        DietaryAllergen.PEANUT -> R.string.allergen_peanut
        DietaryAllergen.SOY -> R.string.allergen_soy
        DietaryAllergen.FISH -> R.string.allergen_fish
        DietaryAllergen.SHELLFISH -> R.string.allergen_shellfish
        DietaryAllergen.SESAME -> R.string.allergen_sesame
    }
}

internal fun menuImagePlaceholderDrawableRes(): Int = R.drawable.menu_placeholder_logo

internal fun menuImagePlaceholderSizePercent(): Int = 72

internal fun menuImagePlaceholderUsesColorTint(): Boolean = false

internal fun menuCarouselItemTitleHeightDp(): Int = 34

internal fun menuCarouselItemMetaRowHeightDp(): Int = 26

internal fun menuDetailTitleOverlaysImage(): Boolean = true

internal fun menuDetailHeroScrimCoveragePercent(): Int = 100

internal fun menuDetailHeroScrimDirection(): String = "horizontal"

internal fun menuDetailHeroScrimStartAlphaPercent(): Int = 78

internal fun menuDetailHeroScrimEndAlphaPercent(): Int = 18

internal fun menuDetailTitleMaxLines(): Int = 2

internal fun menuPhotoActionIconRes(): Int = R.drawable.ic_photo_camera

internal fun menuPhotoActionButtonSizeDp(): Int = 34

internal fun menuPhotoActionButtonInsetDp(): Int = 2

internal fun menuPhotoActionButtonBackgroundColor(): Color = Color.Transparent

@StringRes
internal fun menuPhotoActionContentDescriptionRes(hasImage: Boolean): Int {
    return if (hasImage) {
        R.string.photo_change_menu
    } else {
        R.string.photo_add_menu
    }
}

@StringRes
internal fun menuPhotoSourceDialogTitleRes(): Int = R.string.photo_source_title

@StringRes
internal fun menuPhotoSourceDialogSubtitleRes(): Int = R.string.photo_source_subtitle

@StringRes
internal fun menuPhotoCameraOptionLabelRes(): Int = R.string.photo_take

@StringRes
internal fun menuPhotoCameraOptionDescriptionRes(): Int = R.string.photo_take_description

@StringRes
internal fun menuPhotoLibraryOptionLabelRes(): Int = R.string.photo_library

@StringRes
internal fun menuPhotoLibraryOptionDescriptionRes(): Int = R.string.photo_library_description

internal fun menuPhotoSourceOptionMinHeightDp(): Int = 76

internal fun menuPhotoSourceSheetHorizontalPaddingDp(): Int = 18

internal fun menuPhotoFileProviderAuthoritySuffix(): String = ".fileprovider"

internal fun menuHeaderHealthStatus(menu: FoodMenu, isExpanded: Boolean): HealthStatus? {
    val analysis = menu.healthAnalysis
    return when {
        analysis == null -> HealthStatus.UNKNOWN
        isExpanded -> null
        else -> analysis.status
    }
}

internal fun menuVisibleCalories(menu: FoodMenu): Int? {
    return if (menu.healthAnalysis == null) null else menu.calories
}

internal fun menuExpandToggleIconRes(isExpanded: Boolean): Int {
    return if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
}

private const val MenuCarouselCollapsedLimit = 10

private fun MealType.carouselCoverColor(): Color {
    return when (this) {
        MealType.BREAKFAST -> MenuDadoColors.EggYellow.copy(alpha = 0.72f)
        MealType.LUNCH -> MenuDadoColors.Tomato.copy(alpha = 0.72f)
        MealType.DINNER -> MenuDadoColors.DeepGreen
    }
}

private fun MealType.carouselCoverTextColor(): Color {
    return when (this) {
        MealType.DINNER -> Color.White
        else -> MenuDadoColors.Ink
    }
}

private fun menuCoverLabelGradientColor(menu: FoodMenu): Color {
    return (menu.healthAnalysis?.status ?: HealthStatus.UNKNOWN).coverLabelGradientColor()
}

private fun HealthStatus.coverLabelGradientColor(): Color {
    return when (this) {
        HealthStatus.HEALTHY -> MenuDadoColors.Avocado
        HealthStatus.IMPROVABLE -> MenuDadoColors.EggYellow
        HealthStatus.UNHEALTHY -> MenuDadoColors.Tomato
        HealthStatus.UNKNOWN -> MenuDadoColors.DeepGreen
    }
}

private fun menuCoverMealTypeLabelTextColor(): Color = Color.White

internal fun menuPhotoActionIconTint(): Color = Color(0xFFA4ADA9)

private fun createMenuCameraImageUri(context: Context): Uri {
    val photoDirectory = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir,
        "menu-photos"
    ).apply { mkdirs() }
    val photoFile = File.createTempFile("menu-photo-", ".jpg", photoDirectory)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}${menuPhotoFileProviderAuthoritySuffix()}",
        photoFile
    )
}

@Composable
private fun BoxScope.MenuDetailHeroScrim(menu: FoodMenu) {
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .fillMaxSize(menuDetailHeroScrimCoveragePercent() / 100f)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        menuCoverLabelGradientColor(menu).copy(
                            alpha = menuDetailHeroScrimStartAlphaPercent() / 100f
                        ),
                        menuCoverLabelGradientColor(menu).copy(alpha = 0.46f),
                        menuCoverLabelGradientColor(menu).copy(
                            alpha = menuDetailHeroScrimEndAlphaPercent() / 100f
                        )
                    )
                )
            )
    )
}

@Composable
private fun BoxScope.MenuDetailHeroText(menu: FoodMenu) {
    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "${stringResource(id = mealTypeLabelRes(menu.mealType))} · ${stringResource(id = menuAudienceLabelRes(menu.audience))}",
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MenuDadoColors.Surface.copy(alpha = 0.9f))
                .padding(horizontal = 12.dp, vertical = 7.dp),
            color = MenuDadoColors.Tomato,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black
        )
        Text(
            text = menu.name,
            color = menuCoverMealTypeLabelTextColor(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            maxLines = menuDetailTitleMaxLines(),
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EditMenuButton(
    onEdit: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val editDescription = stringResource(id = R.string.common_edit)
    TextButton(
        onClick = onEdit,
        modifier = if (compact) {
            modifier
                .width(54.dp)
                .semantics { contentDescription = editDescription }
        } else {
            modifier.fillMaxWidth()
        },
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_edit),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(MenuDadoColors.BrandGreen)
        )
        if (!compact) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.common_edit),
                color = MenuDadoColors.BrandGreen,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DeleteMenuButton(
    onDelete: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val deleteDescription = stringResource(id = R.string.common_delete)
    TextButton(
        onClick = onDelete,
        modifier = if (compact) {
            modifier
                .width(54.dp)
                .semantics { contentDescription = deleteDescription }
        } else {
            modifier.fillMaxWidth()
        },
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_delete),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(MenuDadoColors.Tomato)
        )
        if (!compact) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.common_delete),
                color = MenuDadoColors.Tomato,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HealthAnalysisPanel(analysis: HealthAnalysis) {
    val accent = analysis.status.accentColor()
    val background = when (analysis.status) {
        HealthStatus.HEALTHY -> Color(0xFFEAF6DF)
        HealthStatus.IMPROVABLE -> Color(0xFFFFF0C9)
        HealthStatus.UNHEALTHY -> Color(0xFFFFE1D8)
        HealthStatus.UNKNOWN -> MenuDadoColors.SoftSand.copy(alpha = 0.55f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.analysis_ai),
                style = MaterialTheme.typography.labelLarge,
                color = MenuDadoColors.MutedInk,
                fontWeight = FontWeight.Bold
            )
            HealthChip(status = analysis.status)
        }
        Text(
            text = analysis.reason,
            style = MaterialTheme.typography.bodyMedium,
            color = MenuDadoColors.Ink
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                            text = stringResource(id = R.string.analysis_suggestion),
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = analysis.suggestion,
                style = MaterialTheme.typography.bodyMedium,
                color = MenuDadoColors.Ink
            )
        }
    }
}

@Composable
private fun HealthChip(status: HealthStatus) {
    val color = status.accentColor()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.28f))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = stringResource(id = healthStatusLabelRes(status)),
            color = MenuDadoColors.Ink,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun HealthStatus.accentColor(): Color {
    return when (this) {
        HealthStatus.HEALTHY -> MenuDadoColors.Avocado
        HealthStatus.IMPROVABLE -> MenuDadoColors.EggYellow
        HealthStatus.UNHEALTHY -> MenuDadoColors.Tomato
        HealthStatus.UNKNOWN -> MenuDadoColors.OutlineBrown
    }
}

private fun generateAiButtonText(
    isGenerating: Boolean,
    isAiPaused: Boolean,
    usesRemaining: Int,
    generatingText: String,
    restingText: String,
    availableText: String
): String {
    return when {
        isGenerating -> generatingText
        isAiPaused -> restingText
        else -> availableText
    }
}

private fun analyzeAiButtonText(
    isAnalyzing: Boolean,
    isAiPaused: Boolean,
    usesRemaining: Int,
    analyzingText: String,
    restingText: String,
    availableText: String
): String {
    return when {
        isAnalyzing -> analyzingText
        isAiPaused -> restingText
        else -> availableText
    }
}

private fun pendingAnalysisButtonText(
    isAnalyzing: Boolean,
    isAiPaused: Boolean,
    usesRemaining: Int,
    analyzingText: String,
    restingText: String,
    availableText: String
): String {
    return when {
        isAnalyzing -> analyzingText
        isAiPaused -> restingText
        else -> availableText
    }
}

@Composable
private fun OnboardingDialog(
    onSkip: () -> Unit,
    onFinish: () -> Unit
) {
    val steps = remember { onboardingSteps() }
    var selectedStepIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedStep = steps[selectedStepIndex]
    val isLastStep = selectedStepIndex == steps.lastIndex
    var swipeDragAmount by remember { mutableStateOf(0f) }

    Dialog(
        onDismissRequest = onSkip,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MenuDadoColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .pointerInput(steps.size, selectedStepIndex) {
                        detectDragGestures(
                            onDragStart = { swipeDragAmount = 0f },
                            onDragEnd = {
                                selectedStepIndex = onboardingStepAfterSwipe(
                                    currentStep = selectedStepIndex,
                                    stepCount = steps.size,
                                    dragAmount = swipeDragAmount
                                )
                                swipeDragAmount = 0f
                            },
                            onDragCancel = { swipeDragAmount = 0f }
                        ) { change, dragAmount ->
                            change.consume()
                            swipeDragAmount += dragAmount.x
                        }
                    }
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.menu_dado_symbol),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    contentScale = ContentScale.Fit
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = selectedStep.titleRes),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MenuDadoColors.Ink,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(id = selectedStep.bodyRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MenuDadoColors.MutedInk,
                        textAlign = TextAlign.Center
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .size(if (index == selectedStepIndex) 10.dp else 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (index == selectedStepIndex) {
                                        MenuDadoColors.BrandGreen
                                    } else {
                                        MenuDadoColors.SoftSand
                                    }
                                )
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(id = R.string.common_skip))
                    }
                    Button(
                        onClick = {
                            if (isLastStep) {
                                onFinish()
                            } else {
                                selectedStepIndex += 1
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MenuDadoColors.BrandGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            if (isLastStep) {
                                stringResource(id = R.string.common_start)
                            } else {
                                stringResource(id = R.string.common_next)
                            }
                        )
                    }
                }
            }
        }
    }
}
