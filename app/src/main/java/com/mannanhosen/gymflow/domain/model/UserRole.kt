package com.mannanhosen.gymflow.domain.model

/** The three application roles. */
enum class UserRole {
    OWNER,
    STAFF,
    MEMBER;

    companion object {
        fun from(value: String?): UserRole =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEMBER
    }
}