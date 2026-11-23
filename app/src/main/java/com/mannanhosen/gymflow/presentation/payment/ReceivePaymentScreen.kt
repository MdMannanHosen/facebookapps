package com.mannanhosen.gymflow.presentation.payment
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mannanhosen.gymflow.core.util.Money
import com.mannanhosen.gymflow.domain.model.PaymentMethod
import com.mannanhosen.gymflow.presentation.components.AmountInputField
import com.mannanhosen.gymflow.presentation.components.AppTopBar
import com.mannanhosen.gymflow.presentation.components.BannerType
import com.mannanhosen.gymflow.presentation.components.EmptyState
import com.mannanhosen.gymflow.presentation.components.GymCard
import com.mannanhosen.gymflow.presentation.components.InfoBanner
import com.mannanhosen.gymflow.presentation.components.LoadingState
import com.mannanhosen.gymflow.presentation.components.MemberListItem
import com.mannanhosen.gymflow.presentation.components.PrimaryButton
import com.mannanhosen.gymflow.presentation.components.SearchBar
import com.mannanhosen.gymflow.presentation.components.SectionHeader
import com.mannanhosen.gymflow.presentation.members.MembersViewModel
import com.mannanhosen.gymflow.presentation.util.UiEffect
import com.mannanhosen.gymflow.ui.theme.GymShape
import com.mannanhosen.gymflow.ui.theme.Spacing

@Composable
fun ReceivePaymentScreen(
    memberId: String?,
    onBack: () -> Unit,
    onPickedMember: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Crossfade(
        targetState = memberId.isNullOrBlank(),
        animationSpec = tween(durationMillis = 200),
        label = "paymentStep",
        modifier = modifier
    ) { picking ->
        if (picking) {
            MemberPicker(
                onBack = onBack,
                onPick = onPickedMember
            )
        } else {
            PaymentForm(
                onBack = onBack,
                onDone = onDone
            )
        }
    }
}

@Composable
private fun MemberPicker(
    onBack: () -> Unit,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: MembersViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Select member", showBack = true, onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.lg)
        ) {
            SearchBar(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = "Search members",
                modifier = Modifier.padding(vertical = Spacing.md)
            )
            when {
                state.isLoading -> LoadingState()
                state.members.isEmpty() -> EmptyState(
                    icon = Icons.Filled.Groups,
                    title = "No members found",
                    message = "Add a member first."
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(state.members, key = { it.member.id }) { summary ->
                        MemberListItem(summary = summary, onClick = { onPick(summary.member.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentForm(
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: PaymentViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is UiEffect.NavigateBack -> onBack()
                is UiEffect.Navigate -> onDone()
                is UiEffect.ShowMessage -> Unit
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Receive payment", showBack = true, onBack = onBack) }
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            state.memberName.isBlank() -> EmptyState(
                icon = Icons.Filled.Groups,
                title = "Member not found",
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
                GymCard {
                    Text(
                        state.memberName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Outstanding due",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            Money.withSymbol(state.dueAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (state.dueAmount > 0) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                SectionHeader(title = "Amount")
                AmountInputField(
                    value = state.amountInput,
                    onValueChange = viewModel::onAmountChange,
                    isError = state.amountError != null,
                    errorMessage = state.amountError
                )
                if (state.dueAmount > 0) {
                    TextButton(onClick = viewModel::useFullDue) {
                        Text("Use full due (${Money.withSymbol(state.dueAmount)})")
                    }
                }

                SectionHeader(title = "Payment method")
                MethodSelector(
                    selected = state.method,
                    onSelect = viewModel::onMethodChange
                )

                OutlinedTextField(
                    value = state.reference,
                    onValueChange = viewModel::onReferenceChange,
                    label = { Text("Reference / Transaction ID") },
                    singleLine = true,
                    shape = GymShape.field,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.note,
                    onValueChange = viewModel::onNoteChange,
                    label = { Text("Note (optional)") },
                    shape = GymShape.field,
                    modifier = Modifier.fillMaxWidth()
                )

                state.error?.let {
                    InfoBanner(message = it.message, type = BannerType.ERROR)
                }

                PrimaryButton(
                    text = "Collect payment",
                    onClick = viewModel::submit,
                    enabled = state.canSubmit,
                    loading = state.isSubmitting
                )
                Spacer(Modifier.height(Spacing.sm))
            }
        }
    }
}

@Composable
private fun MethodSelector(
    selected: PaymentMethod,
    onSelect: (PaymentMethod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        val methods = listOf(
            PaymentMethod.CASH to "Cash",
            PaymentMethod.BKASH to "bKash",
            PaymentMethod.NAGAD to "Nagad",
            PaymentMethod.CARD to "Card",
            PaymentMethod.BANK to "Bank"
        )
        methods.forEach { (method, label) ->
            FilterChip(
                selected = selected == method,
                onClick = { onSelect(method) },
                label = { Text(label) }
            )
        }
    }
}