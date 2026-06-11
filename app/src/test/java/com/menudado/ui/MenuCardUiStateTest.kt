package com.menudado.ui

import com.menudado.R
import com.menudado.domain.FoodMenu
import com.menudado.domain.HealthAnalysis
import com.menudado.domain.HealthStatus
import com.menudado.domain.MealType
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
        assertEquals("Completa tu perfil", steps[0].title)
        assertTrue(steps[0].body.contains("perfil alimentario"))
        assertTrue(steps[0].body.contains("adulto"))
        assertTrue(steps[0].body.contains("nino"))
        assertTrue(steps[0].body.contains("bebe"))
        assertTrue(steps[0].body.contains("embarazo"))
        assertTrue(steps[0].body.contains("alergias"))
        assertTrue(steps[0].body.contains("condiciones de salud"))
        assertEquals("Guarda tus menus", steps[1].title)
        assertEquals("Personaliza con IA", steps[2].title)
        assertTrue(steps[2].body.contains("20 ayudas de IA"))
        assertTrue(steps[2].body.contains("9 am"))
        assertEquals("Lanza el dado", steps[3].title)
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

        assertEquals("Acerca de la app", info.title)
        assertEquals("Rhonal A. Delgado Padilla", info.creator)
        assertEquals("rhonal.delgado@gmail.com", info.contact)
        assertEquals("Version 1.0.0", info.version)
        assertEquals(true, info.reason.contains("no sabes que comer"))
    }
}
