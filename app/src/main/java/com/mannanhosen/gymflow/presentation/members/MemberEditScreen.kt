package com.mannanhosen.gymflow.presentation.members

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mannanhosen.gymflow.presentation.components.AppTopBar
import com.mannanhosen.gymflow.presentation.components.ConfirmationDialog
import com.mannanhosen.gymflow.presentation.components.DatePickerField
import com.mannanhosen.gymflow.presentation.components.DestructiveButton
import com.mannanhosen.gymflow.presentation.components.ErrorState
import com.mannanhosen.gymflow.presentation.components.LoadingState
import com.mannanhosen.gymflow.presentation.components.PhoneInputField
import com.mannanhosen.gymflow.presentation.components.PrimaryButton
import com.mannanhosen.gymflow.presentation.util.UiEffect
import com.mannanhosen.gymflow.ui.theme.GymShape
import com.mannanhosen.gymflow.ui.theme.Spacing

@Composable
fun MemberEditScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemberEditViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is UiEffect.NavigateBack -> onBack()
                is UiEffect.Navigate -> onSaved()
                is UiEffect.ShowMessage -> snackbarHostState.showSnackbar("Done")
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = if (state.isEditing) "Edit member" else "Add member",
                showBack = true,
                onBack = onBack
            )
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))

            state.error != null && !state.isEditing -> ErrorState(
                message = state.error?.message ?: "Failed to load member",
                onRetry = onBack,
                modifier = Modifier.padding(padding)
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                OutlinedTextField(
                    value = state.fullName,
                    onValueChange = viewModel::onFullNameChange,
                    label = { Text("Full name") },
                    isError = state.nameError != null,
                    supportingText = {
                        state.nameError?.let { Text(it) }
                    },
                    singleLine = true,
                    shape = GymShape.field,
                    modifier = Modifier.fillMaxWidth()
                )

                PhoneInputField(
                    value = state.phone,
                    onValueChange = viewModel::onPhoneChange,
                    isError = state.phoneError != null,
                    errorMessage = state.phoneError
                )

                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = { Text("Email (optional)") },
                    singleLine = true,
                    shape = GymShape.field,
                    modifier = Modifier.fillMaxWidth()
                )

                DatePickerField(
                    label = "Joining date",
                    selectedDate = state.joinedAt,
                    onDateSelected = viewModel::onJoinedAtChange
                )

                Spacer(Modifier.height(Spacing.sm))
                PrimaryButton(
                    text = if (state.isEditing) "Save changes" else "Add member",
                    onClick = viewModel::save,
                    enabled = state.canSave,
                    loading = state.isSaving
                )

                if (state.isEditing) {
                    DestructiveButton(
                        text = "Delete member",
                        onClick = { showDeleteDialog = true }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmationDialog(
            title = "Delete member?",
            message = "This will remove the member and their records. This action cannot be undone.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}