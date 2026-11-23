package com.mannanhosen.gymflow.domain.model

/**
 * Configurable membership plan. Fees and durations are data, not hardcoded rules,
 * so new plans can be added without app changes.
 */
data class MembershipPlan(
    val id: String,
    val name: String,
    val durationMonths: Int,
    val fee: Double,
    val description: String? = null,
    val isActive: Boolean = true
) {
    val displayDuration: String
        get() = if (durationMonths % 12 == 0) "${durationMonths / 12} yr" else "$durationMonths mo"
}