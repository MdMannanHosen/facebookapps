package com.mannanhosen.gymflow.domain.usecase

import com.mannanhosen.gymflow.domain.model.StaffPermissions
import com.mannanhosen.gymflow.domain.model.UserRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckStaffPermissionTest {

    private val useCase = CheckStaffPermission()

    private val restricted = StaffPermissions(staffId = "s1")
    private val full = StaffPermissions.owner("s1")

    @Test
    fun `owner can do everything`() {
        assertTrue(useCase(UserRole.OWNER, null))
        assertTrue(useCase.canReceivePayments(UserRole.OWNER, null))
        assertTrue(useCase.canManagePlans(UserRole.OWNER, null))
    }

    @Test
    fun `member can do nothing`() {
        assertFalse(useCase(UserRole.MEMBER, full))
        assertFalse(useCase.canReceivePayments(UserRole.MEMBER, full))
    }

    @Test
    fun `staff without grants cannot receive payments`() {
        assertFalse(useCase.canReceivePayments(UserRole.STAFF, restricted))
    }

    @Test
    fun `staff with grants can receive payments`() {
        assertTrue(useCase.canReceivePayments(UserRole.STAFF, full))
        assertTrue(useCase.canManageMembers(UserRole.STAFF, full))
    }

    @Test
    fun `staff with null permissions is denied`() {
        assertFalse(useCase(UserRole.STAFF, null))
    }
}