package com.menudado.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object MenuDadoColors {
    val BrandGreen = Color(0xFF2F765D)
    val HeaderGreen = Color(0xFF337551)
    val DeepGreen = Color(0xFF1F4F43)
    val Cream = Color(0xFFFFF7E7)
    val Background = Color(0xFFFFF9EC)
    val Surface = Color(0xFFFFFCF4)
    val FormSurface = Color(0xFFFFEBC7)
    val SoftSand = Color(0xFFF2DFC1)
    val OutlineBrown = Color(0xFF9A6A45)
    val Tomato = Color(0xFFE35D3E)
    val Avocado = Color(0xFF79A85B)
    val EggYellow = Color(0xFFF4B43E)
    val Ink = Color(0xFF263238)
    val MutedInk = Color(0xFF5F6B66)
    val DicePlateBackground = Color(0xFFFBF2DC)
    val DiceSideWarm = Color(0xFFE8C48D)
    val DiceLineBrown = Color(0xFFC49460)
    val DiceAccentBrown = Color(0xFFC49460)
}

private val ColorScheme = lightColorScheme(
    primary = MenuDadoColors.BrandGreen,
    onPrimary = Color.White,
    secondary = MenuDadoColors.Tomato,
    onSecondary = Color.White,
    tertiary = MenuDadoColors.EggYellow,
    background = MenuDadoColors.Background,
    onBackground = MenuDadoColors.Ink,
    surface = MenuDadoColors.Surface,
    onSurface = MenuDadoColors.Ink,
    outline = MenuDadoColors.OutlineBrown.copy(alpha = 0.32f)
)

@Composable
fun MenuDadoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        content = content
    )
}
