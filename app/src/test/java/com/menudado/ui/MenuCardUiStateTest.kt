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
    fun `barra inferior de navegacion usa el verde de cabecera`() {
        assertEquals(MenuDadoColors.HeaderGreen, menuDadoNavigationBarScrimColor())
    }

    @Test
    fun `verdes principales usan un unico verde MenuDado`() {
        assertEquals(MenuDadoColors.BrandGreen, MenuDadoColors.HeaderGreen)
        assertEquals(MenuDadoColors.BrandGreen, MenuDadoColors.DeepGreen)
        assertEquals(MenuDadoColors.BrandGreen, menuActionSheetContainerColor())
        assertEquals(MenuDadoColors.BrandGreen, menuDadoNavigationBarScrimColor())
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
    fun `drawer deja visible parte de la pantalla en moviles`() {
        assertEquals(256, menuDadoDrawerWidthDp(screenWidthDp = 320))
        assertEquals(288, menuDadoDrawerWidthDp(screenWidthDp = 360))
        assertEquals(304, menuDadoDrawerWidthDp(screenWidthDp = 412))
    }

    @Test
    fun `drawer usa colores y formas de MenuDado`() {
        assertEquals(MenuDadoColors.Surface, menuDadoDrawerContainerColor())
        assertEquals(MenuDadoColors.BrandGreen, menuDadoDrawerSelectedContainerColor())
        assertEquals(Color.White, menuDadoDrawerSelectedTextColor())
        assertEquals(MenuDadoColors.Ink, menuDadoDrawerUnselectedTextColor())
        assertEquals(8, menuDadoDrawerItemCornerRadiusDp())
    }

    @Test
    fun `generacion IA muestra overlay bloqueante con dado de carga`() {
        assertEquals(R.drawable.dado_loading, aiGenerationLoadingImageRes())
        assertEquals(R.string.ai_generation_loading_title, aiGenerationLoadingTitleRes())
        assertEquals(R.string.ai_generation_loading_message, aiGenerationLoadingMessageRes())
        assertEquals(MenuDadoColors.DeepGreen.copy(alpha = 0.72f), aiGenerationLoadingOverlayColor())
        assertEquals(MenuDadoColors.Surface, aiGenerationLoadingCardColor())
        assertEquals(true, aiGenerationLoadingBlocksTouches())
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
        assertEquals(61, menuDadoHeaderSymbolSizeDp())
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
    fun `onboarding explica los usos principales y recuerda el perfil alimentario`() {
        val steps = onboardingSteps()

        assertEquals(4, steps.size)
        assertEquals(R.string.onboarding_profile_title, steps[0].titleRes)
        assertEquals(R.string.onboarding_profile_body, steps[0].bodyRes)
        assertEquals(R.string.onboarding_menus_title, steps[1].titleRes)
        assertEquals(R.string.onboarding_menus_body, steps[1].bodyRes)
        assertEquals(R.string.onboarding_ai_title, steps[2].titleRes)
        assertEquals(R.string.onboarding_ai_body, steps[2].bodyRes)
        assertEquals(R.string.onboarding_dice_title, steps[3].titleRes)
        assertEquals(R.string.onboarding_dice_body, steps[3].bodyRes)
    }

    @Test
    fun `onboarding permite avanzar y retroceder con swipe horizontal`() {
        assertEquals(1, onboardingStepAfterSwipe(currentStep = 0, stepCount = 4, dragAmount = -72f))
        assertEquals(2, onboardingStepAfterSwipe(currentStep = 3, stepCount = 4, dragAmount = 72f))
        assertEquals(0, onboardingStepAfterSwipe(currentStep = 0, stepCount = 4, dragAmount = 72f))
        assertEquals(3, onboardingStepAfterSwipe(currentStep = 3, stepCount = 4, dragAmount = -72f))
        assertEquals(1, onboardingStepAfterSwipe(currentStep = 1, stepCount = 4, dragAmount = 20f))
    }

    @Test
    fun `acerca de la app muestra motivo creador y version`() {
        val info = aboutAppInfo()

        assertEquals("Rhonal A. Delgado Padilla", info.creator)
        assertEquals("rhonal.delgado@gmail.com", info.contact)
    }
}
