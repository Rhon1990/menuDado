package com.menudado.ui

import com.menudado.BuildConfig
import com.menudado.R
import com.menudado.data.toEntity
import com.menudado.domain.FoodMenu
import com.menudado.domain.HealthAnalysis
import com.menudado.domain.HealthStatus
import com.menudado.domain.MealType
import com.menudado.domain.MenuAudience
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
            imageUri = "/local/menu-images/pasta.jpg"
        )

        assertEquals("/local/menu-images/pasta.jpg", menu.toEntity().toDomain().imageUri)
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
        assertEquals(42, menuPhotoActionButtonSizeDp())
        assertEquals(8, menuPhotoActionButtonInsetDp())
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
        assertEquals(".fileprovider", menuPhotoFileProviderAuthoritySuffix())
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
    fun `solo crea secciones de carrusel para publicos con menus`() {
        val menus = listOf(
            FoodMenu(id = 1L, name = "Adulto", mealType = MealType.BREAKFAST, audience = MenuAudience.ADULT, description = "A"),
            FoodMenu(id = 2L, name = "Bebe", mealType = MealType.LUNCH, audience = MenuAudience.BABY, description = "B")
        )

        assertEquals(
            listOf(MenuAudience.ADULT, MenuAudience.BABY),
            menuCarouselAudiencesWithMenus(menus)
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
    fun `drawer deja visible parte de la pantalla en moviles`() {
        assertEquals(256, menuDadoDrawerWidthDp(screenWidthDp = 320))
        assertEquals(288, menuDadoDrawerWidthDp(screenWidthDp = 360))
        assertEquals(304, menuDadoDrawerWidthDp(screenWidthDp = 412))
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
    fun `modal de resultado usa proporciones premium para movil`() {
        assertEquals(92, resultDialogMaxHeightPercent())
        assertEquals(14, resultDialogHorizontalPaddingDp())
        assertEquals(184, resultDialogHeroHeightDp())
        assertEquals(4, resultDialogTitleMaxLines())
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
