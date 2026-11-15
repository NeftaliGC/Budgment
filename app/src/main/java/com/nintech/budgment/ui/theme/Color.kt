package com.nintech.budgment.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta base
val DarkGreen = Color(0xFF002B24)
val CastletonGreen = Color(0xFF0B5D44)
val PineGreen = Color(0xFF01796F)
val JungleGreen = Color(0xFF2AA679)
val Aquamarine = Color(0xFF8EE5C1)
val MintCream = Color(0xFFF3FFF9)
val Night = Color(0xFF0D1111)
val FaluRed = Color(0xFF7A1613)

// Acentos
val HighlightDark = Color(0xFFFF9800)
val Success = Color(0xFF2ECC71)
val BurntOrange = Color(0xFF9A4507) // interacción en tema claro: 6.3:1 sobre MintCream, 5.7:1 sobre CardBgLight
val SuccessDark = Color(0xFF18794E)
val ErrorDark = Color(0xFFFFB4AB) // error en tema oscuro: 11.2:1 sobre Night, 8.1:1 sobre CardBgDark

// Texto secundario (sustituye a onSurface con opacidad)
val MutedTextLight = Color(0xFF3F5650) // 7.7:1 sobre MintCream, 6.9:1 sobre CardBgLight
val MutedTextDark = Color(0xFFA9C9BC) // 10.7:1 sobre Night, 7.7:1 sobre CardBgDark

// Tarjetas
val CardBgLight = Color(0xFFE3F4ED)
val CardBgDark = Color(0xFF12332C)

// Contornos
val MutedLight = Color(0xFF2B6B63)
val OutlineVariantLight = Color(0xFFBFD8CE)
val OutlineVariantDark = Color(0xFF2F4A43)

// Contenedores de acento
val PrimaryContainerLight = Color(0xFFBFEBD9)
val SecondaryContainerLight = Color(0xFFC5EBDB)
val SecondaryContainerDark = Color(0xFF1F4A3F)
val TertiaryContainerLight = Color(0xFFFFDCC2)
val OnTertiaryContainerLight = Color(0xFF3A1800)
val TertiaryContainerDark = Color(0xFF5C2E00)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)
val ErrorContainerDark = Color(0xFF93000A)

// Superficies elevadas (menús, diálogos, hojas)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFEDFAF4)
val SurfaceContainerLight = Color(0xFFE8F7F0)
val SurfaceContainerHighLight = Color(0xFFE3F4ED)
val SurfaceContainerHighestLight = Color(0xFFDCEFE7)
val SurfaceContainerLowestDark = Color(0xFF080B0B)
val SurfaceContainerLowDark = Color(0xFF101A18)
val SurfaceContainerDark = Color(0xFF12211E)
val SurfaceContainerHighDark = Color(0xFF172B27)
val SurfaceContainerHighestDark = Color(0xFF1C3630)

// Chrome estructural: headers, hero, bottom nav — SIEMPRE estos colores, sin importar el tema
val ChromeBackground = DarkGreen
val ChromeContent = MintCream
val ChromeAccent = HighlightDark // elemento activo sobre el chrome: 7.1:1 sobre DarkGreen
val ChromeIndicator = Night.copy(alpha = 0.6f)
