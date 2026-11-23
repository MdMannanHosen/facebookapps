package com.mannanhosen.gymflow.core.util

/**
 * A lightweight result type used by the data layer.
 * Success carries data; Failure carries an [AppError] with a user-safe message.
 */
sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class Failure(val error: AppError) : NetworkResult<Nothing>

    @Suppress("UNCHECKED_CAST")
    fun getOrNull(): T? = (this as? Success<T>)?.data

    companion object {
        fun <T> success(data: T): NetworkResult<T> = Success(data)
        fun failure(error: AppError): NetworkResult<Nothing> = Failure(error)
    }
}

/**
 * Application-level error taxonomy. Each error maps to a user-facing string
 * resource and an optional developer diagnostic. Sensitive data is never logged.
 */
sealed class AppError(open val message: String, open val cause: Throwable? = null) {
    data class Network(override val message: String, override val cause: Throwable? = null) : AppError(message, cause)
    data class Timeout(override val message: String, override val cause: Throwable? = null) : AppError(message, cause)
    data class Unauthorized(override val message: String, override val cause: Throwable? = null) : AppError(message, cause)
    data class Forbidden(override val message: String, override val cause: Throwable? = null) : AppError(message, cause)
    data class NotFound(override val message: String, override val cause: Throwable? = null) : AppError(message, cause)
    data class Validation(override val message: String, override val cause: Throwable? = null) : AppError(message, cause)
    data class Server(override val message: String, override val cause: Throwable? = null) : AppError(message, cause)
    data class Unknown(override val message: String, override val cause: Throwable? = null) : AppError(message, cause)
}