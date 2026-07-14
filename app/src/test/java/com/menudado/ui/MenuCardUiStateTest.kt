package com.menudado.ui

import com.menudado.BuildConfig
import com.menudado.R
import com.menudado.data.toEntity
import com.menudado.domain.FoodMenu
import com.menudado.domain.HealthAnalysis
import com.menudado.domain.HealthStatus
import com.menudado.domain.MealType
import com.menudado.domain.MenuAudience
import com.menudado.ui.theme.MenuDadoColors
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuCardUiStateTest {
    @Test
    fun `muestra chip de analisis en cabecera cuando la tarjeta esta colapsada`() {
        val menuWithoutAnalysis = FoodMenu(
            name = "Tostada",
            mealType = MealType.BREAKFAST,
            description = "Pan con tomate"
        )
        val menuWithAnalysis = menuWithoutAnalysis.copy(
            healthAnalysis = HealthAnalysis(
                status = HealthStatus.IMPROVABLE,
                reason = "Tiene buena base, pero puede mejorar",
                suggestion = "Agrega fruta"
            )
        )

        assertEquals(HealthStatus.UNKNOWN, menuHeaderHealthStatus(menuWithoutAnalysis, isExpanded = false))
        assertEquals(HealthStatus.UNKNOWN, menuHeaderHealthStatus(menuWithoutAnalysis, isExpanded = true))
        assertEquals(HealthStatus.IMPROVABLE, menuHeaderHealthStatus(menuWithAnalysis, isExpanded = false))
        assertNull(menuHeaderHealthStatus(menuWithAnalysis, isExpanded = true))
    }

    @Test
    fun `muestra calorias solo despues del analisis IA`() {
        val menuWithoutAnalysis = FoodMenu(
            name = "Porridge",
            mealType = MealType.BREAKFAST,
            description = "Avena con fruta",
            calories = 480
        )
        val menuWithAnalysis = menuWithoutAnalysis.copy(
            healthAnalysis = HealthAnalysis(
                status = HealthStatus.HEALTHY,
                reason = "Tiene fibra y fruta.",
                suggestion = "Mantener variedad."
            )
        )

        assertNull(menuVisibleCalories(menuWithoutAnalysis))
        assertEquals(480, menuVisibleCalories(menuWithAnalysis))
    }

    @Test
    fun `mantiene la imagen del menu al convertir entre dominio y entidad`() {
        val menu = FoodMenu(
            id = 8L,
            name = "Pasta",
            mealType = MealType.LUNCH,
            description = "Pasta con tomate",
            imageUri = "/local/menu-images/pasta.jpg",
            isFavorite = true
        )

        assertEquals("/local/menu-images/pasta.jpg", menu.toEntity().toDomain().imageUri)
        assertEquals(true, menu.toEntity().toDomain().isFavorite)
    }

    @Test
    fun `usa el logo placeholder de MenuDado para menus sin imagen`() {
        assertEquals(R.drawable.menu_placeholder_logo, menuImagePlaceholderDrawableRes())
        assertEquals(72, menuImagePlaceholderSizePercent())
        assertEquals(false, menuImagePlaceholderUsesColorTint())
    }

    @Test
    fun `foto del menu se decodifica con reduccion de resolucion para portada`() {
        assertEquals(1024, menuCoverImageRequestedPixelSize())
        assertEquals(
            2,
            menuCoverImageBitmapSampleSize(
                sourceWidth = 4096,
                sourceHeight = 3072,
                requestedWidth = 1024,
                requestedHeight = 1024
            )
        )
        assertEquals(
            1,
            menuCoverImageBitmapSampleSize(
                sourceWidth = 800,
                sourceHeight = 600,
                requestedWidth = 1024,
                requestedHeight = 1024
            )
        )
    }

    @Test
    fun `tarjetas del carrusel reservan alturas fijas para alinear chips`() {
        assertEquals(34, menuCarouselItemTitleHeightDp())
        assertEquals(26, menuCarouselItemMetaRowHeightDp())
    }

    @Test
    fun `detalle del menu tapa toda la foto con degradado para leer titulo interno`() {
        assertEquals(true, menuDetailTitleOverlaysImage())
        assertEquals(100, menuDetailHeroScrimCoveragePercent())
        assertEquals("horizontal", menuDetailHeroScrimDirection())
        assertEquals(78, menuDetailHeroScrimStartAlphaPercent())
        assertEquals(18, menuDetailHeroScrimEndAlphaPercent())
        assertEquals(2, menuDetailTitleMaxLines())
    }

    @Test
    fun `boton de camara se superpone dentro de la foto de cards y editor`() {
        assertEquals(R.drawable.ic_photo_camera, menuPhotoActionIconRes())
        assertEquals(34, menuPhotoActionButtonSizeDp())
        assertEquals(2, menuPhotoActionButtonInsetDp())
        assertEquals(Color(0xFFA4ADA9), menuPhotoActionIconTint())
        assertEquals(Color.Transparent, menuPhotoActionButtonBackgroundColor())
        assertEquals(R.string.photo_add_menu, menuPhotoActionContentDescriptionRes(hasImage = false))
        assertEquals(R.string.photo_change_menu, menuPhotoActionContentDescriptionRes(hasImage = true))
        assertEquals(R.string.photo_source_title, menuPhotoSourceDialogTitleRes())
        assertEquals(R.string.photo_source_subtitle, menuPhotoSourceDialogSubtitleRes())
        assertEquals(R.string.photo_take, menuPhotoCameraOptionLabelRes())
        assertEquals(R.string.photo_take_description, menuPhotoCameraOptionDescriptionRes())
        assertEquals(R.string.photo_library, menuPhotoLibraryOptionLabelRes())
        assertEquals(R.string.photo_library_description, menuPhotoLibraryOptionDescriptionRes())
        assertEquals(76, menuPhotoSourceOptionMinHeightDp())
        assertEquals(18, menuPhotoSourceSheetHorizontalPaddingDp())
        assertEquals(menuActionSheetContainerColor(), menuPhotoSourceSheetContainerColor())
        assertEquals(menuActionSheetContentColor(), menuPhotoSourceContentColor())
        assertEquals(true, menuPhotoSourceDismissesOnOutsideTap())
        assertFalse(menuPhotoSourceShowsCancelButton())
        assertTrue(menuPhotoSourceShowsCloseAction())
        assertEquals(R.drawable.ic_close, menuSheetCloseActionIconRes())
        assertEquals(R.string.common_close, menuSheetCloseActionContentDescriptionRes())
        assertEquals(34, menuSheetCloseActionButtonSizeDp())
        assertEquals(Color.White, menuPhotoSourceOptionIconTint())
        assertEquals(Color.White, menuPhotoSourceOptionTitleColor())
        assertEquals(Color.White.copy(alpha = 0.78f), menuPhotoSourceOptionDescriptionColor())
        assertEquals(".fileprovider", menuPhotoFileProviderAuthoritySuffix())
    }

    @Test
    fun `boton de favorito usa el estilo visual de la camara y mantiene rojo al activarse`() {
        assertEquals(34, menuFavoriteActionButtonSizeDp())
        assertEquals(2, menuFavoriteActionButtonInsetDp())
        assertEquals(Color.Transparent, menuFavoriteActionButtonBackgroundColor())
        assertEquals(Color(0xFFA4ADA9), menuFavoriteActionIconTint(isFavorite = false))
        assertEquals(MenuDadoColors.Tomato, menuFavoriteActionIconTint(isFavorite = true))
    }

    @Test
    fun `menu de tres puntos muestra acciones de menu con estilo MenuDado`() {
        assertEquals(R.drawable.ic_more_vertical, menuOverflowActionIconRes())
        assertEquals(34, menuOverflowActionButtonSizeDp())
        assertEquals(2, menuOverflowActionButtonInsetDp())
        assertEquals(Color.Transparent, menuOverflowActionButtonBackgroundColor())
        assertEquals(Color(0xFFA4ADA9), menuOverflowActionIconTint())
        assertEquals(R.string.menu_actions_open, menuOverflowActionContentDescriptionRes())
        assertFalse(menuActionSheetShowsTitle())
        assertEquals(MenuDadoColors.HeaderGreen, menuActionSheetContainerColor())
        assertEquals(Color.White, menuActionSheetContentColor())
        assertEquals(true, menuActionSheetDismissesOnOutsideTap())
        assertTrue(menuActionSheetShowsCloseAction())
        assertEquals(R.drawable.ic_close, menuSheetCloseActionIconRes())
        assertEquals(R.string.common_close, menuSheetCloseActionContentDescriptionRes())
        assertEquals(
            listOf(
                MenuActionSheetAction.PHOTO,
                MenuActionSheetAction.SHARE,
                MenuActionSheetAction.EDIT,
                MenuActionSheetAction.DELETE
            ),
            menuActionSheetActions()
        )
        assertEquals(R.string.menu_action_photo, menuActionSheetActionLabelRes(MenuActionSheetAction.PHOTO))
        assertEquals(R.string.common_share, menuActionSheetActionLabelRes(MenuActionSheetAction.SHARE))
        assertEquals(R.string.common_edit, menuActionSheetActionLabelRes(MenuActionSheetAction.EDIT))
        assertEquals(R.string.common_delete, menuActionSheetActionLabelRes(MenuActionSheetAction.DELETE))
        assertEquals(Color.White, menuActionSheetActionTint(MenuActionSheetAction.PHOTO))
        assertEquals(Color.White, menuActionSheetActionTint(MenuActionSheetAction.SHARE))
        assertEquals(Color.White, menuActionSheetActionTint(MenuActionSheetAction.EDIT))
        assertEquals(Color.White, menuActionSheetActionTint(MenuActionSheetAction.DELETE))
    }

    @Test
    fun `barra inferior de navegacion usa fondo calido`() {
        assertEquals(MenuDadoColors.Background, menuDadoNavigationBarScrimColor())
    }

    @Test
    fun `opciones principales viven en barra inferior calida`() {
        assertEquals(MenuDadoColors.Surface, menuDadoBottomNavigationContainerColor())
        assertEquals(MenuDadoColors.BrandGreen, menuDadoBottomNavigationContentColor(isSelected = true))
        assertEquals(
            MenuDadoColors.MutedInk.copy(alpha = 0.72f),
            menuDadoBottomNavigationContentColor(isSelected = false)
        )
        assertEquals(
            listOf(
                MenuDadoDestination.HOME,
                MenuDadoDestination.PROFILE,
                MenuDadoDestination.MY_ZONE
            ),
            menuDadoBottomNavigationDestinations(areAdsPrivacyOptionsRequired = false)
        )
        assertEquals(
            listOf(
                MenuDadoDestination.HOME,
                MenuDadoDestination.PROFILE,
                MenuDadoDestination.MY_ZONE
            ),
            menuDadoBottomNavigationDestinations(areAdsPrivacyOptionsRequired = true)
        )
        assertEquals(R.string.nav_home, menuDadoBottomNavigationLabelRes(MenuDadoDestination.HOME))
        assertEquals(R.string.nav_dietary_profile, menuDadoBottomNavigationLabelRes(MenuDadoDestination.PROFILE))
        assertEquals(R.string.nav_my_zone, menuDadoBottomNavigationLabelRes(MenuDadoDestination.MY_ZONE))
    }

    @Test
    fun `pantallas internas muestran flecha de retroceso en cabecera`() {
        assertEquals(R.drawable.ic_arrow_back, menuDadoHeaderBackIconRes())
        assertEquals(R.string.common_back, menuDadoHeaderBackContentDescriptionRes())
        assertEquals(
            true,
            shouldShowMenuDadoHeaderBackButton(
                destination = MenuDadoDestination.ABOUT,
                hasAudienceDetail = false,
                hasAuthForm = false
            )
        )
        assertEquals(
            true,
            shouldShowMenuDadoHeaderBackButton(
                destination = MenuDadoDestination.HOME,
                hasAudienceDetail = true,
                hasAuthForm = false
            )
        )
        assertEquals(
            true,
            shouldShowMenuDadoHeaderBackButton(
                destination = MenuDadoDestination.MY_ZONE,
                hasAudienceDetail = false,
                hasAuthForm = true
            )
        )
        assertEquals(
            false,
            shouldShowMenuDadoHeaderBackButton(
                destination = MenuDadoDestination.MY_ZONE,
                hasAudienceDetail = false,
                hasAuthForm = false
            )
        )
    }

    @Test
    fun `mi zona muestra beneficios ampliados de tener cuenta`() {
        assertEquals(
            listOf(
                R.string.my_zone_benefit_reinstall,
                R.string.my_zone_benefit_profile,
                R.string.my_zone_benefit_personalization,
                R.string.my_zone_benefit_favorites,
                R.string.my_zone_benefit_devices,
                R.string.my_zone_benefit_ai_context
            ),
            myZoneAccountBenefitRes()
        )
    }

    @Test
    fun `mi zona muestra privacidad solo en debug`() {
        assertEquals(true, shouldShowMyZonePrivacyOption(buildType = "debug"))
        assertEquals(false, shouldShowMyZonePrivacyOption(buildType = "release"))
        assertEquals(false, shouldShowMyZonePrivacyOption(buildType = "releaseDebuggable"))
    }

    @Test
    fun `mi zona no muestra el nombre tecnico de la variante como cuenta sincronizada`() {
        assertEquals(R.string.my_zone_synced_account_value, myZoneSyncedAccountValueRes())
    }

    @Test
    fun `animacion del dado aterriza exactamente en reposo`() {
        assertEquals(0f, diceRollRemainingSpinDegrees(1f), 0.001f)
        assertEquals(0f, diceRollPulse(1f), 0.001f)
        assertEquals(1f, diceRollScale(1f), 0.001f)
        assertEquals(0f, diceRollLift(1f), 0.001f)
    }

    @Test
    fun `verdes principales usan un unico verde MenuDado`() {
        assertEquals(MenuDadoColors.BrandGreen, MenuDadoColors.HeaderGreen)
        assertEquals(MenuDadoColors.BrandGreen, MenuDadoColors.DeepGreen)
        assertEquals(MenuDadoColors.BrandGreen, menuActionSheetContainerColor())
        assertEquals(MenuDadoColors.Background, menuDadoNavigationBarScrimColor())
    }

    @Test
    fun `tocar un menu cerrado lo convierte en el unico expandido`() {
        assertEquals(7L, nextExpandedMenuIdAfterMenuClick(currentExpandedMenuId = null, clickedMenuId = 7L))
        assertEquals(9L, nextExpandedMenuIdAfterMenuClick(currentExpandedMenuId = 7L, clickedMenuId = 9L))
    }

    @Test
    fun `tocar el menu expandido lo colapsa`() {
        assertNull(nextExpandedMenuIdAfterMenuClick(currentExpandedMenuId = 7L, clickedMenuId = 7L))
    }

    @Test
    fun `usa iconos distintos para ver y ocultar menu`() {
        assertEquals(R.drawable.ic_expand_more, menuExpandToggleIconRes(isExpanded = false))
        assertEquals(R.drawable.ic_expand_less, menuExpandToggleIconRes(isExpanded = true))
    }

    @Test
    fun `mi zona usa flecha lateral para enlaces y sin flecha para cerrar sesion`() {
        assertEquals(R.drawable.ic_arrow_forward, myZoneNavigationActionIconRes())
        assertNull(myZoneSignOutActionIconRes())
    }

    @Test
    fun `carrusel muestra los diez ultimos menus del publico cuando esta colapsado`() {
        val menus = (1L..12L).map { index ->
            FoodMenu(
                id = index,
                name = "Menu $index",
                mealType = MealType.BREAKFAST,
                audience = MenuAudience.ADULT,
                description = "Descripcion $index",
                createdAt = index
            )
        } + FoodMenu(
            id = 20L,
            name = "Menu niño",
            mealType = MealType.LUNCH,
            audience = MenuAudience.CHILD,
            description = "Descripcion niño",
            createdAt = 20L
        )

        val visible = menuCarouselVisibleMenus(
            menus = menus,
            audience = MenuAudience.ADULT,
            isExpanded = false
        )

        assertEquals((12L downTo 3L).toList(), visible.map { it.id })
    }

    @Test
    fun `carrusel muestra favoritos primero y luego menus recientes`() {
        val menus = listOf(
            FoodMenu(id = 1L, name = "Reciente", mealType = MealType.BREAKFAST, audience = MenuAudience.ADULT, description = "A", createdAt = 30L),
            FoodMenu(id = 2L, name = "Favorito antiguo", mealType = MealType.BREAKFAST, audience = MenuAudience.ADULT, description = "B", createdAt = 1L, isFavorite = true),
            FoodMenu(id = 3L, name = "Favorito nuevo", mealType = MealType.BREAKFAST, audience = MenuAudience.ADULT, description = "C", createdAt = 20L, isFavorite = true)
        )

        val visible = menuCarouselVisibleMenus(
            menus = menus,
            audience = MenuAudience.ADULT,
            isExpanded = true
        )

        assertEquals(listOf(3L, 2L, 1L), visible.map { it.id })
    }

    @Test
    fun `carrusel mantiene llave de scroll cuando solo cambia el orden por favoritos`() {
        val previousMenus = listOf(
            FoodMenu(id = 1L, name = "Menu normal", mealType = MealType.LUNCH, audience = MenuAudience.CHILD, description = "A", createdAt = 1L),
            FoodMenu(id = 2L, name = "Menu favorito", mealType = MealType.LUNCH, audience = MenuAudience.CHILD, description = "B", createdAt = 2L)
        )
        val currentMenus = listOf(
            previousMenus[1].copy(isFavorite = true),
            previousMenus[0]
        )

        assertEquals(menuCarouselScrollResetKey(previousMenus), menuCarouselScrollResetKey(currentMenus))
    }

    @Test
    fun `seccion favoritos muestra solo menus favoritos ordenados por recientes`() {
        val menus = listOf(
            FoodMenu(id = 1L, name = "Normal", mealType = MealType.BREAKFAST, audience = MenuAudience.ADULT, description = "A", createdAt = 30L),
            FoodMenu(id = 2L, name = "Favorito viejo", mealType = MealType.LUNCH, audience = MenuAudience.ADULT, description = "B", createdAt = 1L, isFavorite = true),
            FoodMenu(id = 3L, name = "Favorito nuevo", mealType = MealType.DINNER, audience = MenuAudience.CHILD, description = "C", createdAt = 20L, isFavorite = true)
        )

        assertEquals(listOf(3L, 2L), menuFavoriteMenus(menus).map { it.id })
        assertTrue(menuShouldShowFavoriteSection(menus))
        assertFalse(menuShouldShowFavoriteSection(listOf(menus.first())))
    }

    @Test
    fun `carrusel muestra todos los menus del publico cuando esta expandido`() {
        val menus = listOf(
            FoodMenu(id = 1L, name = "Adulto antiguo", mealType = MealType.BREAKFAST, audience = MenuAudience.ADULT, description = "A", createdAt = 1L),
            FoodMenu(id = 2L, name = "Niño", mealType = MealType.LUNCH, audience = MenuAudience.CHILD, description = "N", createdAt = 5L),
            FoodMenu(id = 3L, name = "Adulto nuevo", mealType = MealType.DINNER, audience = MenuAudience.ADULT, description = "B", createdAt = 10L)
        )

        val visible = menuCarouselVisibleMenus(
            menus = menus,
            audience = MenuAudience.ADULT,
            isExpanded = true
        )

        assertEquals(listOf(3L, 1L), visible.map { it.id })
    }

    @Test
    fun `solo crea secciones de carrusel para publicos activos con menus`() {
        val menus = listOf(
            FoodMenu(id = 1L, name = "Adulto", mealType = MealType.BREAKFAST, audience = MenuAudience.ADULT, description = "A"),
            FoodMenu(id = 2L, name = "Peques", mealType = MealType.LUNCH, audience = MenuAudience.CHILD, description = "P"),
            FoodMenu(id = 3L, name = "Bebe", mealType = MealType.LUNCH, audience = MenuAudience.BABY, description = "B")
        )

        assertEquals(
            listOf(MenuAudience.ADULT, MenuAudience.BABY),
            menuCarouselAudiencesWithMenus(
                menus = menus,
                enabledAudiences = listOf(MenuAudience.ADULT, MenuAudience.BABY)
            )
        )
    }

    @Test
    fun `ver mas abre pantalla del publico tocado y permite volver`() {
        assertEquals(MenuAudience.ADULT.name, menuAudienceDetailRoute(MenuAudience.ADULT))
        assertEquals(MenuAudience.ADULT.name, menuAudienceDetailRouteAfterViewMore(MenuAudience.ADULT))
        assertEquals(MenuAudience.ADULT, menuAudienceFromDetailRoute(MenuAudience.ADULT.name))
        assertNull(menuAudienceFromDetailRoute(null))
        assertNull(menuAudienceDetailRouteAfterBack(MenuAudience.ADULT.name))
        assertEquals("home", menuListStateKeyForAudienceDetail(null))
        assertEquals("audience-detail", menuListStateKeyForAudienceDetail(MenuAudience.ADULT.name))
        assertEquals(true, menuShouldResetAudienceDetailScroll(MenuAudience.ADULT.name))
        assertEquals(false, menuShouldResetAudienceDetailScroll(null))
    }

    @Test
    fun `carrusel muestra accion ver mas aunque haya diez o menos menus`() {
        val menus = listOf(
            FoodMenu(id = 1L, name = "Adulto", mealType = MealType.BREAKFAST, audience = MenuAudience.ADULT, description = "A")
        )

        assertEquals(true, menuCarouselShowsToggle(menus, MenuAudience.ADULT))
        assertEquals(false, menuCarouselShowsToggle(menus, MenuAudience.CHILD))
    }

    @Test
    fun `pantalla ver mas agrupa menus del publico por comida y ordena por recientes`() {
        val menus = listOf(
            FoodMenu(id = 1L, name = "Adulto cena", mealType = MealType.DINNER, audience = MenuAudience.ADULT, description = "A", createdAt = 1L),
            FoodMenu(id = 2L, name = "Niño desayuno", mealType = MealType.BREAKFAST, audience = MenuAudience.CHILD, description = "N", createdAt = 20L),
            FoodMenu(id = 3L, name = "Adulto desayuno viejo", mealType = MealType.BREAKFAST, audience = MenuAudience.ADULT, description = "B", createdAt = 3L),
            FoodMenu(id = 4L, name = "Adulto desayuno nuevo", mealType = MealType.BREAKFAST, audience = MenuAudience.ADULT, description = "C", createdAt = 30L)
        )

        val groups = menuAudienceDetailGroups(menus, MenuAudience.ADULT)

        assertEquals(listOf(MealType.BREAKFAST, MealType.DINNER), groups.map { it.mealType })
        assertEquals(listOf(4L, 3L), groups[0].menus.map { it.id })
        assertEquals(listOf(1L), groups[1].menus.map { it.id })
    }

    @Test
    fun `texto compartido del menu incluye datos utiles sin imagen local`() {
        val menu = FoodMenu(
            id = 1L,
            name = "Tostas Mediterraneas",
            mealType = MealType.LUNCH,
            audience = MenuAudience.ADULT,
            description = "Pan integral, garbanzos y tomate.",
            notes = "Servir frio.",
            healthAnalysis = HealthAnalysis(
                status = HealthStatus.HEALTHY,
                reason = "Aporta fibra.",
                suggestion = "Agrega limon."
            ),
            calories = 450,
            imageUri = "content://menu/photo/1"
        )

        assertEquals(
            """
            MenuDado

            Almuerzo para Adulto
            Tostas Mediterraneas

            Ingredientes o descripcion:
            Pan integral, garbanzos y tomate.

            Notas:
            Servir frio.

            Saludable · 450 kcal aprox.
            """.trimIndent(),
            menuShareText(menu)
        )
    }

    @Test
    fun `barra inferior reserva espacio tactil suficiente`() {
        assertEquals(8, menuDadoBottomNavigationHorizontalPaddingDp())
        assertEquals(8, menuDadoBottomNavigationVerticalPaddingDp())
        assertEquals(58, menuDadoBottomNavigationMinHeightDp())
        assertEquals(24, menuDadoBottomNavigationIconSizeDp())
    }

    @Test
    fun `barra inferior usa colores y formas de MenuDado`() {
        assertEquals(MenuDadoColors.Surface, menuDadoBottomNavigationContainerColor())
        assertEquals(MenuDadoColors.BrandGreen, menuDadoBottomNavigationContentColor(isSelected = true))
        assertEquals(
            MenuDadoColors.MutedInk.copy(alpha = 0.72f),
            menuDadoBottomNavigationContentColor(isSelected = false)
        )
        assertEquals(false, menuDadoBottomNavigationPressOverlayEnabled())
        assertEquals(16, menuDadoBottomNavigationItemCornerRadiusDp())
        assertEquals(24, menuDadoBottomNavigationIndicatorWidthDp(isSelected = true))
        assertEquals(0, menuDadoBottomNavigationIndicatorWidthDp(isSelected = false))
        assertEquals(3, menuDadoBottomNavigationIndicatorHeightDp())
    }

    @Test
    fun `ctas de mi zona y autenticacion usan nombres cerrados`() {
        assertEquals("open_account_benefits", myZoneAccountBenefitsCta())
        assertEquals("close_account_benefits", myZoneCloseAccountBenefitsCta())
        assertEquals("start_register", myZoneStartRegisterCta())
        assertEquals("start_sign_in", myZoneStartSignInCta())
        assertEquals("submit_email_auth", authSubmitEmailCta())
        assertEquals("submit_google_auth", authSubmitGoogleCta())
        assertEquals("switch_auth_mode", authSwitchModeCta())
        assertEquals("sign_out", authSignOutCta())
    }

    @Test
    fun `generacion IA muestra overlay bloqueante con dado 3D de MenuDado`() {
        assertTrue(aiGenerationLoadingUsesMenuDadoDiceCube())
        assertEquals(4, aiGenerationLoadingDiceFaceIndex(buttonDiceFaceIndex = 4))
        assertEquals(R.string.ai_generation_loading_title, aiGenerationLoadingTitleRes())
        assertEquals(R.string.ai_generation_loading_message, aiGenerationLoadingMessageRes())
        assertEquals(MenuDadoColors.DeepGreen.copy(alpha = 0.72f), aiGenerationLoadingOverlayColor())
        assertEquals(MenuDadoColors.Surface, aiGenerationLoadingCardColor())
        assertEquals(true, aiGenerationLoadingBlocksTouches())
    }

    @Test
    fun `dado IA bloqueado explica que falta seleccionar publico y ofrece escribir menu`() {
        assertEquals(
            R.string.dice_ai_blocked_missing_audience,
            aiDiceDisabledReasonRes(
                hasMealType = true,
                hasAudience = false,
                isAiPaused = false
            )
        )
        assertEquals(R.string.dice_ai_blocked_notice_title, contextualDiceDisabledReasonTitleRes())
        assertEquals(MenuDadoColors.Tomato, contextualDiceDisabledReasonTitleColor())
        assertEquals(MenuDadoColors.Ink, contextualDiceDisabledReasonBodyColor())
        assertEquals(MenuDadoColors.EggYellow.copy(alpha = 0.24f), contextualDiceDisabledReasonContainerColor())
        assertEquals(MenuDadoColors.OutlineBrown.copy(alpha = 0.24f), contextualDiceDisabledReasonBorderColor())
    }

    @Test
    fun `dado IA bloqueado explica que faltan selecciones antes de generar`() {
        assertEquals(
            R.string.dice_ai_blocked_missing_meal_and_audience,
            aiDiceDisabledReasonRes(
                hasMealType = false,
                hasAudience = false,
                isAiPaused = false
            )
        )
        assertEquals(
            R.string.dice_ai_blocked_missing_meal,
            aiDiceDisabledReasonRes(
                hasMealType = false,
                hasAudience = true,
                isAiPaused = false
            )
        )
        assertNull(
            aiDiceDisabledReasonRes(
                hasMealType = true,
                hasAudience = true,
                isAiPaused = false
            )
        )
    }

    @Test
    fun `texto principal del boton de dado permite dos lineas para no recortar contador IA`() {
        assertEquals(2, contextualDicePrimaryTextMaxLines())
        assertTrue(contextualDicePrimaryTextSoftWrap())
    }

    @Test
    fun `boton de dado deshabilitado usa color calido de marca distinto del naranja activo`() {
        assertEquals(MenuDadoColors.Tomato, contextualDiceEnabledContainerColor())
        assertEquals(MenuDadoColors.SoftSand, contextualDiceDisabledContainerColor())
        assertFalse(contextualDiceDisabledContainerColor() == MenuDadoColors.Tomato.copy(alpha = 0.72f))
        assertEquals(MenuDadoColors.Ink, contextualDiceContentColor(enabled = false))
    }

    @Test
    fun `animacion IA del dado sigue girando y desacelera sin detenerse`() {
        assertTrue(aiDiceRollingSpeedMultiplier(elapsedMillis = 0L) > aiDiceRollingSpeedMultiplier(elapsedMillis = 8_000L))
        assertTrue(aiDiceRollingSpeedMultiplier(elapsedMillis = 30_000L) > 0f)
        assertTrue(aiDiceRollingCycles(elapsedMillis = 12_000L) > aiDiceRollingCycles(elapsedMillis = 3_000L))
    }

    @Test
    fun `cabecera pone la marca al lado del menu sin espacio superior extra`() {
        assertEquals(0, menuDadoHeaderBrandTopPaddingDp())
        assertEquals(6, menuDadoHeaderBrandStartGapDp())
    }

    @Test
    fun `cabecera compensa margen interno del wordmark para alinearlo con el subtitulo`() {
        assertEquals(15, menuDadoWordmarkVisualStartInsetDp())
    }

    @Test
    fun `cabecera reduce el bloque de marca un quince por ciento`() {
        assertEquals(44, menuDadoHeaderSymbolSizeDp())
        assertEquals(34, menuDadoHeaderWordmarkHeightDp())
        assertEquals(14, menuDadoHeaderSubtitleFontSizeSp())
    }

    @Test
    fun `cabecera acerca el subtitulo al wordmark`() {
        assertEquals(-5, menuDadoHeaderSubtitleTopOffsetDp())
    }

    @Test
    fun `resultado del dado abre el detalle normal del menu`() {
        val result = FoodMenu(
            id = 8L,
            name = "Ravioli",
            mealType = MealType.LUNCH,
            description = "Ravioli con salsa"
        )

        assertTrue(menuShouldOpenDetailFromDiceResult(result))
        assertFalse(menuShouldOpenDetailFromDiceResult(null))
        assertEquals(8L, menuDetailMenuIdAfterDiceResult(currentDetailMenuId = null, result = result))
        assertEquals(4L, menuDetailMenuIdAfterDiceResult(currentDetailMenuId = 4L, result = null))
    }

    @Test
    fun `contenido del boton del dado queda centrado en pantallas anchas`() {
        assertEquals(Alignment.Center, contextualDiceButtonContentAlignment())
        assertEquals(280, contextualDiceButtonTextMaxWidthDp())
    }

    @Test
    fun `eliminar menu requiere confirmacion antes de borrar`() {
        val menu = FoodMenu(
            id = 12L,
            name = "Tostada",
            mealType = MealType.BREAKFAST,
            description = "Pan con tomate"
        )

        assertEquals(12L, menuDeleteConfirmationMenuIdAfterDeleteClick(menu))
        assertNull(menuDeleteConfirmationMenuIdAfterDismiss())
        assertEquals(R.string.delete_menu_confirmation_title, menuDeleteConfirmationTitleRes())
        assertEquals(R.string.delete_menu_confirmation_message, menuDeleteConfirmationMessageRes())
    }

    @Test
    fun `onboarding focuses on creating the first menu`() {
        val steps = onboardingSteps()

        assertEquals(1, steps.size)
        assertEquals(R.string.onboarding_activation_title, steps.single().titleRes)
        assertEquals(R.string.onboarding_activation_body, steps.single().bodyRes)
    }

    @Test
    fun `recent menu uses greatest creation timestamp`() {
        val old = FoodMenu(
            id = 1,
            name = "Anterior",
            mealType = MealType.LUNCH,
            description = "Anterior",
            createdAt = 10
        )
        val recent = FoodMenu(
            id = 2,
            name = "Reciente",
            mealType = MealType.DINNER,
            description = "Reciente",
            createdAt = 30
        )

        assertEquals(2L, mostRecentMenu(listOf(recent.copy(createdAt = 20), old, recent))?.id)
        assertNull(mostRecentMenu(emptyList()))
    }

    @Test
    fun `acerca de la app muestra motivo creador y version`() {
        val info = com.menudado.about.MenuDadoAboutContent(
            description = "MenuDado ayuda a decidir que comer.",
            createdBy = "Rhonal A. Delgado Padilla",
            contact = "rhonal.delgado@gmail.com"
        )

        assertEquals("MenuDado ayuda a decidir que comer.", info.description)
        assertEquals("Rhonal A. Delgado Padilla", info.createdBy)
        assertEquals("rhonal.delgado@gmail.com", info.contact)
        assertEquals("1.1.0 (10)", aboutVersionLabel(versionName = "1.1.0", versionCode = 10))
    }

    @Test
    fun `acerca de la app expone aviso de salud y politica de privacidad`() {
        assertEquals(R.string.about_health_disclaimer_title, aboutHealthDisclaimerTitleRes())
        assertEquals(R.string.about_health_disclaimer_body, aboutHealthDisclaimerBodyRes())
        assertEquals(R.string.about_privacy_policy, aboutPrivacyPolicyLabelRes())
        assertEquals("https://rhon1990.github.io/menuDado/privacy-policy/", aboutPrivacyPolicyUrl())
    }
}
