package com.mannanhosen.gymflow.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Central shape scale. Consistent rounded corners across the app.
 * Cards use 16dp, buttons 28dp (pill-like), inputs 12dp.
 */
val GymFlowShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Named shape tokens for semantic use. */
object GymShape {
    val card = RoundedCornerShape(16.dp)
    val button = RoundedCornerShape(28.dp)
    val field = RoundedCornerShape(12.dp)
    val chip = RoundedCornerShape(50)
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
}