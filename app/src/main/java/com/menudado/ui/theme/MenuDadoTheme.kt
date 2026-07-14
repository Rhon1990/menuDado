package com.menudado.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object MenuDadoColors {
    val BrandGreen = Color(0xFF2F765D)
    val HeaderGreen = BrandGreen
    val DeepGreen = BrandGreen
    val SelectionGreen = Color(0xFFE7F0EB)
    val ActionTerracotta = Color(0xFFD66548)
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

object MenuDadoUiTokens {
    val CardRadius = 24.dp
    val ControlRadius = 16.dp
    val NavigationRadius = 20.dp
    val MinimumTouchTarget = 48.dp
    val SpacingScale = listOf(8.dp, 12.dp, 16.dp, 24.dp, 32.dp)
}

private val ColorScheme = lightColorScheme(
    primary = MenuDadoColors.BrandGreen,
    onPrimary = Color.White,
    secondary = MenuDadoColors.ActionTerracotta,
    onSecondary = Color.White,
    tertiary = MenuDadoColors.EggYellow,
    error = MenuDadoColors.Tomato,
    background = MenuDadoColors.Background,
    onBackground = MenuDadoColors.Ink,
    surface = MenuDadoColors.Surface,
    onSurface = MenuDadoColors.Ink,
    outline = MenuDadoColors.OutlineBrown.copy(alpha = 0.32f)
)

private val MenuDadoTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 32.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Black
    ),
    headlineMedium = TextStyle(
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Black
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Bold
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Bold
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold
    ),
    labelSmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Bold
    )
)

private val MenuDadoShapes = Shapes(
    extraSmall = RoundedCornerShape(MenuDadoUiTokens.ControlRadius),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(MenuDadoUiTokens.ControlRadius),
    large = RoundedCornerShape(MenuDadoUiTokens.CardRadius),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MenuDadoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = MenuDadoTypography,
        shapes = MenuDadoShapes,
        content = content
    )
}
