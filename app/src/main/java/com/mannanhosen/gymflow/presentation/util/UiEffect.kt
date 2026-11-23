package com.mannanhosen.gymflow.presentation.util

import androidx.annotation.StringRes

/** One-off UI effects emitted by ViewModels (navigation, snackbars). */
sealed interface UiEffect {
    data class Navigate(val route: String, val popUpTo: String? = null) : UiEffect
    data object NavigateBack : UiEffect
    data class ShowMessage(@StringRes val messageRes: Int) : UiEffect
}