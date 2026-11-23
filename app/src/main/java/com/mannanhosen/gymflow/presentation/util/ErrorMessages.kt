package com.mannanhosen.gymflow.presentation.util

import androidx.annotation.StringRes
import com.mannanhosen.gymflow.R
import com.mannanhosen.gymflow.core.util.AppError

/** Maps an [AppError] to a user-facing string resource. */
@StringRes
fun AppError.messageRes(): Int = when (this) {
    is AppError.Network -> R.string.error_network
    is AppError.Timeout -> R.string.error_timeout
    is AppError.Unauthorized -> R.string.auth_session_expired
    is AppError.Forbidden -> R.string.error_unauthorized
    is AppError.NotFound -> R.string.error_not_found
    is AppError.Validation -> R.string.error_generic
    is AppError.Server -> R.string.error_server
    is AppError.Unknown -> R.string.error_unknown
}