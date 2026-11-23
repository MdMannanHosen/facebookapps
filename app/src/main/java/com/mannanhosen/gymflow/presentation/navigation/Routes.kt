package com.mannanhosen.gymflow.presentation.navigation

/** Centralized route definitions and builders. */
object Routes {
    const val LOGIN = "login"

    // Owner / staff top-level
    const val DASHBOARD = "dashboard"
    const val MEMBERS = "members"
    const val DUES = "dues"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"

    // Member top-level
    const val MEMBER_HOME = "member_home"
    const val MEMBER_MEMBERSHIP = "member_membership"
    const val MEMBER_PAYMENTS = "member_payments"
    const val MEMBER_PROFILE = "member_profile"

    // Secondary
    const val NOTIFICATIONS = "notifications"
    const val PLANS = "plans"
    const val PAY_ONLINE = "pay_online"
    const val STAFF_PERMISSIONS = "staff_permissions"

    const val MEMBER_DETAIL = "member_detail/{memberId}"
    const val MEMBER_EDIT = "member_edit?memberId={memberId}"
    const val MEMBERSHIP = "membership?memberId={memberId}&mode={mode}"
    const val RECEIVE_PAYMENT = "receive_payment?memberId={memberId}"
    const val PAYMENT_HISTORY = "payment_history/{memberId}"

    fun memberDetail(id: String) = "member_detail/$id"

    fun memberEdit(id: String? = null): String =
        if (id.isNullOrBlank()) "member_edit" else "member_edit?memberId=$id"

    fun membership(memberId: String?, renew: Boolean): String {
        val mode = if (renew) "renew" else "create"
        return if (memberId.isNullOrBlank()) "membership?mode=$mode"
        else "membership?mode=$mode&memberId=$memberId"
    }

    fun receivePayment(memberId: String? = null): String =
        if (memberId.isNullOrBlank()) "receive_payment" else "receive_payment?memberId=$memberId"

    fun paymentHistory(memberId: String) = "payment_history/$memberId"
}