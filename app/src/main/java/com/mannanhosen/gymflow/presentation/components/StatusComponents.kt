package com.mannanhosen.gymflow.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mannanhosen.gymflow.core.util.Money
import com.mannanhosen.gymflow.domain.model.MembershipStatus
import com.mannanhosen.gymflow.domain.model.PaymentStatus
import com.mannanhosen.gymflow.ui.theme.GymShape
import com.mannanhosen.gymflow.ui.theme.LocalStatusColors
import com.mannanhosen.gymflow.ui.theme.Spacing

private data class StatusStyle(
    val label: String,
    val icon: ImageVector,
    val container: Color,
    val content: Color
)

/** Generic status pill with an icon and label — never color-only. */
@Composable
fun StatusPill(
    text: String,
    icon: ImageVector,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(container, GymShape.chip)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun membershipStyle(status: MembershipStatus): StatusStyle {
    val c = LocalStatusColors.current
    return when (status) {
        MembershipStatus.ACTIVE -> StatusStyle("Active", Icons.Filled.CheckCircle, c.success.copy(alpha = 0.14f), c.success)
        MembershipStatus.EXPIRING_SOON -> StatusStyle("Expiring", Icons.Filled.Schedule, c.warning.copy(alpha = 0.18f), c.warning)
        MembershipStatus.EXPIRED -> StatusStyle("Expired", Icons.Filled.ErrorOutline, c.error.copy(alpha = 0.14f), c.error)
        MembershipStatus.FROZEN -> StatusStyle(
            "Frozen", Icons.Filled.PauseCircle,
            MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant
        )
        MembershipStatus.PENDING -> StatusStyle("Pending", Icons.Filled.Info, c.info.copy(alpha = 0.14f), c.info)
    }
}

@Composable
private fun paymentStyle(status: PaymentStatus): StatusStyle {
    val c = LocalStatusColors.current
    return when (status) {
        PaymentStatus.SUCCESS -> StatusStyle("Successful", Icons.Filled.CheckCircle, c.success.copy(alpha = 0.14f), c.success)
        PaymentStatus.PENDING -> StatusStyle("Pending", Icons.Filled.Schedule, c.warning.copy(alpha = 0.18f), c.warning)
        PaymentStatus.FAILED -> StatusStyle("Failed", Icons.Filled.ErrorOutline, c.error.copy(alpha = 0.14f), c.error)
        PaymentStatus.REFUNDED -> StatusStyle("Refunded", Icons.Filled.Info, c.info.copy(alpha = 0.14f), c.info)
    }
}

@Composable
fun MembershipStatusChip(status: MembershipStatus, modifier: Modifier = Modifier) {
    val s = membershipStyle(status)
    StatusPill(s.label, s.icon, s.container, s.content, modifier)
}

@Composable
fun PaymentStatusChip(status: PaymentStatus, modifier: Modifier = Modifier) {
    val s = paymentStyle(status)
    StatusPill(s.label, s.icon, s.container, s.content, modifier)
}

/** Badge that highlights an outstanding balance. */
@Composable
fun DueAmountBadge(amount: Double, modifier: Modifier = Modifier) {
    if (amount <= 0.0) return
    val c = LocalStatusColors.current
    StatusPill(
        text = "Due ${Money.withSymbol(amount)}",
        icon = Icons.Filled.ErrorOutline,
        container = c.error.copy(alpha = 0.14f),
        content = c.error,
        modifier = modifier
    )
}

/** Small colored dot + label for inline lists. */
@Composable
fun InlineStatusLabel(text: String, dotColor: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, RoundedCornerShape(50))
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}