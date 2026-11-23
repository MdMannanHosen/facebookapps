package com.mannanhosen.gymflow.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 8dp-based spacing system. Prefer these tokens instead of magic numbers.
 * Use 4 / 8 / 12 / 16 / 24 / 32 for most layouts.
 */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

/** Minimum touch target sizes for accessibility. */
object TouchTarget {
    val minimum = 48.dp
    val primaryAction = 56.dp
}

/** Corner radius tokens (dp) that mirror GymShape for use in modifiers. */
object Radius {
    val field = 12.dp
    val card = 16.dp
    val button = 28.dp
    val chip = 50.dp
}