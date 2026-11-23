package com.mannanhosen.gymflow.presentation.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mannanhosen.gymflow.ui.theme.GymShape
import com.mannanhosen.gymflow.ui.theme.Spacing

/**
 * App-standard modal bottom sheet.
 *
 * Used for contextual actions (member quick actions, payment method picker,
 * notification options). Handles its own scrim, drag handle, dismissal and
 * accessibility semantics; callers only supply content.
 *
 * @param visible when false the sheet is not composed at all (no leaked state)
 * @param dismissible when false the sheet cannot be dismissed by tapping the
 *   scrim or pressing back — callers must provide an explicit action
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    if (!visible) return

    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (dismissible) onDismiss() },
        modifier = modifier.fillMaxWidth(),
        sheetState = sheetState,
        shape = GymShape.sheet,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(
                    start = Spacing.xl,
                    end = Spacing.xl,
                    bottom = Spacing.sm
                )
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        content()
    }
}