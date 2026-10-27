package com.bytechnum.reveil.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.unit.sp

private val defaultTypography = Typography()

// Textes un peu plus grands que la valeur par défaut : l'appli est souvent lue à moitié endormi.
val ReveilTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontSize = 72.sp, lineHeight = 80.sp),
    bodyLarge = defaultTypography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = defaultTypography.bodyMedium.copy(fontSize = 16.sp, lineHeight = 22.sp),
    labelLarge = defaultTypography.labelLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
)
