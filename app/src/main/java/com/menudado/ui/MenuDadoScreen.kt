package com.menudado.ui

import androidx.compose.animation.AnimatedVisibility
import android.graphics.Paint as AndroidPaint
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.menudado.R
import com.menudado.domain.DietaryAllergen
import com.menudado.domain.DietaryProfile
import com.menudado.domain.FoodMenu
import com.menudado.domain.HealthAnalysis
import com.menudado.domain.HealthStatus
import com.menudado.domain.MealType
import com.menudado.ui.theme.MenuDadoColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var previousRolling by remember { mutableStateOf(state.isRolling) }
    var diceFaceIndex by remember { mutableIntStateOf(0) }
    var expandedMenuId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedDestination by rememberSaveable { mutableStateOf(MenuDadoDestination.HOME.name) }
    val pendingAnalysisCount = state.menus.count { it.healthAnalysis == null }
    val destination = MenuDadoDestination.valueOf(selectedDestination)

    LaunchedEffect(state.isRolling) {
        if (previousRolling && !state.isRolling) {
            diceFaceIndex = (diceFaceIndex + 1) % DiceRestPoses.size
        }
        previousRolling = state.isRolling
    }

    LaunchedEffect(state.menus) {
        if (expandedMenuId != null && state.menus.none { it.id == expandedMenuId }) {
            expandedMenuId = null
        }
    }

    if (result != null) {
        ResultDialog(
            menu = result,
            diceFaceIndex = diceFaceIndex,
            onDismiss = viewModel::clearResult
        )
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
                    Text("Entendido")
                }
            },
            title = { Text("MenuDado") },
            text = { Text(message) }
        )
    }

    if (state.showOnboarding) {
        OnboardingDialog(
            onSkip = viewModel::skipOnboarding,
            onFinish = viewModel::completeOnboarding
        )
    }

    if (state.editingMenuId != null) {
        EditMenuDialog(
            state = state,
            onMealTypeChanged = viewModel::setEditMealType,
            onNameChanged = viewModel::updateEditName,
            onDescriptionChanged = viewModel::updateEditDescription,
            onNotesChanged = viewModel::updateEditNotes,
            onSave = viewModel::saveEditedMenu,
            onDismiss = viewModel::cancelEditingMenu
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
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            }
        ) {
            LazyColumn(
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
                                profile = state.dietaryProfile,
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
                        item {
                            DiceSection(
                                filter = state.diceFilter,
                                isRolling = state.isRolling,
                                diceFaceIndex = diceFaceIndex,
                                onFilterChanged = viewModel::setDiceFilter,
                                onRoll = viewModel::rollDice
                            )
                        }
                        item {
                            MenuForm(
                                state = state,
                                onMealTypeChanged = viewModel::setFormMealType,
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
                                text = "Tus menus",
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
                            items(items = state.menus, key = { it.id }) { menu ->
                                MenuCard(
                                    menu = menu,
                                    isExpanded = expandedMenuId == menu.id,
                                    isAnalyzing = state.isAnalyzing,
                                    aiUsesRemainingToday = state.aiUsesRemainingToday,
                                    isAiPaused = state.aiRetryAtMillis != null,
                                    onToggleExpanded = {
                                        val nextExpandedMenuId = nextExpandedMenuIdAfterMenuClick(
                                            currentExpandedMenuId = expandedMenuId,
                                            clickedMenuId = menu.id
                                        )
                                        if (nextExpandedMenuId == menu.id) {
                                            viewModel.trackMenuCardOpened(menu)
                                        }
                                        expandedMenuId = nextExpandedMenuId
                                    },
                                    onAnalyze = { viewModel.analyzeExisting(menu) },
                                    onEdit = { viewModel.startEditingMenu(menu) },
                                    onDelete = { viewModel.deleteMenu(menu) }
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }
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
    ModalDrawerSheet(modifier = Modifier.width(drawerWidthDp.dp)) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 12.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "MenuDado",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MenuDadoColors.DeepGreen
            )
            NavigationDrawerItem(
                label = { Text("Inicio") },
                selected = selectedDestination == MenuDadoDestination.HOME,
                onClick = { onDestinationSelected(MenuDadoDestination.HOME) }
            )
            NavigationDrawerItem(
                label = { Text("Perfil alimentario") },
                selected = selectedDestination == MenuDadoDestination.PROFILE,
                onClick = { onDestinationSelected(MenuDadoDestination.PROFILE) }
            )
            NavigationDrawerItem(
                label = { Text("Acerca de la app") },
                selected = selectedDestination == MenuDadoDestination.ABOUT,
                onClick = { onDestinationSelected(MenuDadoDestination.ABOUT) }
            )
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

internal fun menuDadoHeaderBrandTopPaddingDp(): Int = 0

internal fun menuDadoHeaderBrandStartGapDp(): Int = 6

internal fun menuDadoWordmarkVisualStartInsetDp(): Int = 15

internal fun menuDadoHeaderSymbolSizeDp(): Int = 61

internal fun menuDadoHeaderWordmarkHeightDp(): Int = 34

internal fun menuDadoHeaderSubtitleFontSizeSp(): Int = 14

internal fun menuDadoHeaderSubtitleTopOffsetDp(): Int = -5

internal data class OnboardingStep(
    val title: String,
    val body: String
)

internal fun onboardingSteps(): List<OnboardingStep> = listOf(
    OnboardingStep(
        title = "Completa tu perfil",
        body = "Configura tu perfil alimentario con alergias y alimentos que prefieres evitar para que las ideas se adapten mejor a ti."
    ),
    OnboardingStep(
        title = "Guarda tus menus",
        body = "Agrega desayunos, almuerzos y cenas con ingredientes, notas y calorias si quieres."
    ),
    OnboardingStep(
        title = "Personaliza con IA",
        body = "Genera ideas y revisa si son saludables. Tienes hasta 20 ayudas de IA al dia y se renuevan cada manana a las 9 am."
    ),
    OnboardingStep(
        title = "Lanza el dado",
        body = "Escoge desayuno, almuerzo o cena y deja que MenuDado elija por ti."
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
    val title: String,
    val reason: String,
    val creator: String,
    val contact: String,
    val version: String
)

internal fun aboutAppInfo(): AboutAppInfo = AboutAppInfo(
    title = "Acerca de la app",
    reason = "MenuDado se hizo para ayudar en esos momentos en los que no sabes que comer. La app te permite guardar tus menus, elegir una opcion con el dado y apoyarte con IA para encontrar ideas mas saludables sin complicarte.",
    creator = "Rhonal A. Delgado Padilla",
    contact = "rhonal.delgado@gmail.com",
    version = "Version 1.0.0"
)

@Composable
private fun AboutAppSection() {
    val info = remember { aboutAppInfo() }

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
                text = info.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MenuDadoColors.Ink
            )
            Text(
                text = info.reason,
                style = MaterialTheme.typography.bodyLarge,
                color = MenuDadoColors.MutedInk
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Creada por",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = MenuDadoColors.DeepGreen
                )
                Text(
                    text = info.creator,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MenuDadoColors.Ink
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Contacto",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = MenuDadoColors.DeepGreen
                )
                Text(
                    text = info.contact,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MenuDadoColors.MutedInk
                )
            }
            Text(
                text = info.version,
                modifier = Modifier.align(Alignment.End),
                style = MaterialTheme.typography.labelSmall,
                color = MenuDadoColors.MutedInk
            )
        }
    }
}

@Composable
private fun DietaryProfileSection(
    profile: DietaryProfile,
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
                text = "Perfil alimentario",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MenuDadoColors.Ink
            )
            DietarySwitchRow(
                title = "Vegano",
                checked = profile.isVegan,
                onCheckedChange = onVeganChanged
            )
            DietarySwitchRow(
                title = "Alergias",
                checked = profile.hasAllergies,
                onCheckedChange = onHasAllergiesChanged
            )
            AnimatedVisibility(visible = profile.hasAllergies) {
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
                label = { Text("Otros alimentos a evitar") },
                minLines = 2
            )
        }
    }
}

@Composable
private fun DietarySwitchRow(
    title: String,
    checked: Boolean,
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
            color = MenuDadoColors.Ink
        )
        MenuDadoBrandSwitch(checked = checked, onCheckedChange = onCheckedChange)
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
            text = allergen.label,
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
                Text(if (canRetry) "Entendido" else "Cerrar")
            }
        },
        title = {
            Text(
                text = if (canRetry) "Ya puedes reintentar con IA" else "IA descansando un momento",
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
                        text = if (canRetry) "La pausa termino" else "Reintento protegido",
                        color = MenuDadoColors.Ink,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = if (canRetry) {
                        "Cierra este aviso y vuelve a tocar la opcion de IA que querias usar."
                    } else {
                        "Evitamos hacer llamadas repetidas para no alargar la espera. Cuando la pausa termine, este aviso cambiara para decirte que puedes reintentar."
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
                contentDescription = "Abrir menu",
                tint = Color.White
            )
        }
        Image(
            painter = painterResource(id = R.drawable.menu_dado_symbol),
            contentDescription = "Logo MenuDado",
            modifier = Modifier.size(menuDadoHeaderSymbolSizeDp().dp),
            contentScale = ContentScale.Fit
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.menu_dado_wordmark),
                contentDescription = "MenuDado",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(menuDadoHeaderWordmarkHeightDp().dp)
                    .offset(x = (-menuDadoWordmarkVisualStartInsetDp()).dp),
                alignment = Alignment.CenterStart,
                contentScale = ContentScale.Fit
            )
            Text(
                text = "Que comemos hoy?",
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
    isRolling: Boolean,
    diceFaceIndex: Int,
    onFilterChanged: (MealType?) -> Unit,
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
                    text = "Que comer hoy",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MenuDadoColors.Ink
                )
                Text(
                    text = "Escoge desayuno, almuerzo o cena y deja que el dado elija.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MenuDadoColors.MutedInk
                )
            }
            MealTypeFilter(
                selected = filter,
                includeAll = false,
                onSelected = onFilterChanged
            )
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
                        text = if (isRolling) "Lanzando..." else "Lanzar dado",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isRolling) "Mezclando tus menus" else "Elegir que comer hoy",
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
    onMealTypeChanged: (MealType) -> Unit,
    onNameChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onAiBaseIngredientsChanged: (String) -> Unit,
    onGenerate: () -> Unit,
    onSave: () -> Unit
) {
    var selectedModeName by rememberSaveable { mutableStateOf(MenuFormMode.Manual.name) }
    val selectedMode = MenuFormMode.valueOf(selectedModeName)
    val hasAiDraft = selectedMode == MenuFormMode.Ai && state.calories != null
    val canUseFormActions = state.formMealType != null && !state.isAnalyzing && !state.isGeneratingMenu

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
                    text = "Agregar menu",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Elige si quieres escribirlo tu o generar una idea saludable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MenuDadoColors.MutedInk
                )
            }
            MenuFormModeSelector(
                selected = selectedMode,
                onSelected = { mode -> selectedModeName = mode.name }
            )
            MealTypeFilter(
                selected = state.formMealType,
                includeAll = false,
                onSelected = { mealType -> if (mealType != null) onMealTypeChanged(mealType) }
            )
            if (state.formMealType == null) {
                Text(
                    text = "Selecciona desayuno, almuerzo o cena.",
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
                        text = "Guardar menu",
                        onSave = onSave
                    )
                }
                MenuFormMode.Ai -> {
                    OutlinedTextField(
                        value = state.aiBaseIngredients,
                        onValueChange = onAiBaseIngredientsChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Ingredientes base opcionales") },
                        placeholder = { Text("Ej. berenjena, tomate") },
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
                                usesRemaining = state.aiUsesRemainingToday
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
                                text = "Guardar menu",
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
    onNameChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
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
                        text = "Editar menu",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MenuDadoColors.Ink
                    )
                    Text(
                        text = "Ajusta este menu y vuelve a analizarlo si cambiaste la receta.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MenuDadoColors.MutedInk
                    )
                }
                MealTypeFilter(
                    selected = state.editMealType,
                    includeAll = false,
                    onSelected = { mealType -> if (mealType != null) onMealTypeChanged(mealType) }
                )
                OutlinedTextField(
                    value = state.editName,
                    onValueChange = onNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre del plato o menu") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.editDescription,
                    onValueChange = onDescriptionChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ingredientes o descripcion") },
                    minLines = 2
                )
                OutlinedTextField(
                    value = state.editNotes,
                    onValueChange = onNotesChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notas opcionales") },
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
                            text = "Cancelar",
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
                            text = "Guardar cambios",
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

private enum class MenuFormMode(val label: String) {
    Manual("Escribir menu"),
    Ai("Generar con IA")
}

@Composable
private fun MenuFormModeSelector(
    selected: MenuFormMode,
    onSelected: (MenuFormMode) -> Unit
) {
    MenuDadoSegmentedSwitch(
        options = MenuFormMode.entries.map { mode ->
            MenuDadoSegmentOption(value = mode, label = mode.label)
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
        label = { Text("Nombre del plato o menu") },
        singleLine = true
    )
    OutlinedTextField(
        value = state.description,
        onValueChange = onDescriptionChanged,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Ingredientes o descripcion") },
        minLines = 2
    )
    OutlinedTextField(
        value = state.notes,
        onValueChange = onNotesChanged,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Notas opcionales") },
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
    onCheckedChange: (Boolean) -> Unit
) {
    val trackColor = if (checked) {
        MenuDadoColors.BrandGreen
    } else {
        MenuDadoColors.SoftSand
    }
    val knobColor = if (checked) {
        MenuDadoColors.EggYellow
    } else {
        MenuDadoColors.Surface
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
                    if (checked) MenuDadoColors.DeepGreen.copy(alpha = 0.38f) else MenuDadoColors.OutlineBrown.copy(alpha = 0.28f)
                ),
                RoundedCornerShape(18.dp)
            )
            .clickable { onCheckedChange(!checked) }
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
                    .background(if (checked) MenuDadoColors.DeepGreen else MenuDadoColors.OutlineBrown.copy(alpha = 0.48f))
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
            add(MenuDadoSegmentOption(value = null, label = "Todos"))
        }
        addAll(MealType.entries.map { mealType ->
            MenuDadoSegmentOption(value = mealType, label = mealType.label)
        })
    }

    MenuDadoSegmentedSwitch(
        options = options,
        selected = selected,
        onSelected = onSelected
    )
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
            text = "$calories kcal aprox.",
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
            text = "Aun no tienes menus. Agrega uno y luego lanza el dado.",
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
                usesRemaining = usesRemaining
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false
        )
    }
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
                        text = menu.mealType.label,
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
                        contentDescription = if (isExpanded) "Ocultar menu" else "Ver menu",
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
                                        usesRemaining = aiUsesRemainingToday
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

@Composable
private fun EditMenuButton(
    onEdit: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onEdit,
        modifier = if (compact) {
            modifier
                .width(54.dp)
                .semantics { contentDescription = "Editar menu" }
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
                text = "Editar",
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
    TextButton(
        onClick = onDelete,
        modifier = if (compact) {
            modifier
                .width(54.dp)
                .semantics { contentDescription = "Eliminar menu" }
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
                text = "Eliminar",
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
                text = "Analisis IA",
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
                text = "Sugerencia",
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
            text = status.label,
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
    usesRemaining: Int
): String {
    return when {
        isGenerating -> "Generando idea..."
        isAiPaused -> "IA descansando"
        else -> "Generar idea saludable con IA ($usesRemaining)"
    }
}

private fun analyzeAiButtonText(
    isAnalyzing: Boolean,
    isAiPaused: Boolean,
    usesRemaining: Int
): String {
    return when {
        isAnalyzing -> "Analizando"
        isAiPaused -> "IA descansando"
        else -> "Analizar IA ($usesRemaining)"
    }
}

private fun pendingAnalysisButtonText(
    isAnalyzing: Boolean,
    isAiPaused: Boolean,
    usesRemaining: Int
): String {
    return when {
        isAnalyzing -> "Analizando pendientes"
        isAiPaused -> "IA descansando"
        else -> "Analizar pendientes con IA ($usesRemaining)"
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
                    contentDescription = "Logo MenuDado",
                    modifier = Modifier.size(72.dp),
                    contentScale = ContentScale.Fit
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = selectedStep.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MenuDadoColors.Ink,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = selectedStep.body,
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
                        Text("Omitir")
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
                        Text(if (isLastStep) "Empezar" else "Siguiente")
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultDialog(
    menu: FoodMenu,
    diceFaceIndex: Int,
    onDismiss: () -> Unit
) {
    val dicePose = DiceRestPoses[diceFaceIndex % DiceRestPoses.size]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 22.dp),
            colors = CardDefaults.cardColors(containerColor = MenuDadoColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MenuDadoColors.DeepGreen)
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MenuDadoColors.Cream)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DiceCube3D(
                                modifier = Modifier.size(54.dp),
                                rotationX = dicePose.rotationX,
                                rotationY = dicePose.rotationY,
                                rotationZ = dicePose.rotationZ - 4f
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Hoy toca",
                                color = Color.White.copy(alpha = 0.86f),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = menu.name,
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
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
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MenuDadoColors.Tomato.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = menu.mealType.label,
                                color = MenuDadoColors.Tomato,
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        menuVisibleCalories(menu)?.let { calories ->
                            CaloriesPill(calories = calories)
                        }
                    }

                    DialogSection(title = "Menu elegido") {
                        Text(
                            text = menu.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MenuDadoColors.Ink
                        )
                    }

                    if (menu.notes.isNotBlank()) {
                        DialogSection(title = "Notas") {
                            Text(
                                text = menu.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MenuDadoColors.MutedInk
                            )
                        }
                    }

                    menu.healthAnalysis?.let { analysis ->
                        HealthAnalysisPanel(analysis = analysis)
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MenuDadoColors.BrandGreen,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Cerrar",
                        modifier = Modifier.padding(vertical = 8.dp),
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MenuDadoColors.Background)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MenuDadoColors.DeepGreen,
            fontWeight = FontWeight.Black
        )
        content()
    }
}
