package com.mannanhosen.gymflow.presentation.payment

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mannanhosen.gymflow.core.util.AppClock
import com.mannanhosen.gymflow.core.util.AppError
import com.mannanhosen.gymflow.core.util.Money
import com.mannanhosen.gymflow.core.util.NetworkResult
import com.mannanhosen.gymflow.domain.model.Payment
import com.mannanhosen.gymflow.domain.model.PaymentStatus
import com.mannanhosen.gymflow.domain.repository.PaymentRepository
import com.mannanhosen.gymflow.domain.usecase.RefundError
import com.mannanhosen.gymflow.domain.usecase.RefundPayment
import com.mannanhosen.gymflow.domain.usecase.RefundValidation
import com.mannanhosen.gymflow.presentation.components.AmountInputField
import com.mannanhosen.gymflow.presentation.components.AppTopBar
import com.mannanhosen.gymflow.presentation.components.DestructiveButton
import com.mannanhosen.gymflow.presentation.components.EmptyState
import com.mannanhosen.gymflow.presentation.components.ErrorState
import com.mannanhosen.gymflow.presentation.components.GymBottomSheet
import com.mannanhosen.gymflow.presentation.components.LoadingState
import com.mannanhosen.gymflow.presentation.components.PaymentListItem
import com.mannanhosen.gymflow.ui.theme.Spacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class PaymentHistoryUiState(
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val payments: List<Payment> = emptyList(),
    /** Payment whose detail sheet is open. */
    val selected: Payment? = null,
    val refundAmount: String = "",
    val refundReason: String = "",
    val refundError: String? = null,
    val isRefunding: Boolean = false,
    val actionSuccess: String? = null
)

/**
 * Sum of successful payments that count against the same membership as [payment].
 * Memberships keep payments separate, so a refund only frees up its own term.
 */
internal fun paidFor(payments: List<Payment>, payment: Payment): Double {
    val membershipId = payment.membershipId
    return payments
        .filter { it.isSuccessful && (membershipId == null || it.membershipId == membershipId) }
        .sumOf { it.amount }
}

/** Sum of refunds already issued against the same membership as [payment]. */
internal fun refundedFor(payments: List<Payment>, payment: Payment): Double {
    val membershipId = payment.membershipId
    return payments
        .filter {
            it.status == PaymentStatus.REFUNDED &&
                (membershipId == null || it.membershipId == membershipId)
        }
        .sumOf { it.amount }
}

/** What is still refundable for [payment]; never negative. */
internal fun refundableFor(payments: List<Payment>, payment: Payment): Double =
    (paidFor(payments, payment) - refundedFor(payments, payment)).coerceAtLeast(0.0)

@HiltViewModel
class PaymentHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val paymentRepository: PaymentRepository,
    private val refundPayment: RefundPayment
) : ViewModel() {

    private val memberId: String = savedStateHandle["memberId"] ?: ""

    /** Regenerated after every successful refund so a new refund is never deduped. */
    private var refundKey: String = UUID.randomUUID().toString()

    private val _uiState = MutableStateFlow(PaymentHistoryUiState())
    val uiState: StateFlow<PaymentHistoryUiState> = _uiState.asStateFlow()

    init { load() }

    fun retry() = load()

    fun openPayment(payment: Payment) = _uiState.update {
        it.copy(
            selected = payment,
            refundAmount = "",
            refundReason = "",
            refundError = null
        )
    }

    fun closePayment() = _uiState.update {
        it.copy(selected = null, refundError = null, refundAmount = "", refundReason = "")
    }

    fun onRefundAmountChange(value: String) =
        _uiState.update { it.copy(refundAmount = value, refundError = null) }

    fun onRefundReasonChange(value: String) =
        _uiState.update { it.copy(refundReason = value) }

    fun clearMessages() = _uiState.update { it.copy(actionSuccess = null) }

    fun submitRefund() {
        val state = _uiState.value
        val payment = state.selected ?: return
        if (state.isRefunding) return

        when (
            val validation = refundPayment(
                paymentId = payment.id,
                amountInput = state.refundAmount,
                reason = state.refundReason,
                paidAmount = paidFor(state.payments, payment),
                alreadyRefunded = refundedFor(state.payments, payment),
                idempotencyKey = refundKey
            )
        ) {
            is RefundValidation.Invalid -> _uiState.update {
                it.copy(refundError = messageFor(validation.error))
            }

            is RefundValidation.Valid -> {
                _uiState.update { it.copy(isRefunding = true, refundError = null) }
                viewModelScope.launch {
                    when (val result = paymentRepository.refundPayment(validation.input)) {
                        is NetworkResult.Success -> {
                            refundKey = UUID.randomUUID().toString()
                            _uiState.update {
                                it.copy(
                                    isRefunding = false,
                                    selected = null,
                                    refundAmount = "",
                                    refundReason = "",
                                    actionSuccess = "Payment refunded"
                                )
                            }
                            load(silent = true)
                        }

                        is NetworkResult.Failure -> _uiState.update {
                            it.copy(isRefunding = false, refundError = result.error.message)
                        }
                    }
                }
            }
        }
    }

    private fun load(silent: Boolean = false) {
        if (!silent) _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = paymentRepository.getPaymentHistory(memberId)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        payments = result.data,
                        error = null,
                        selected = it.selected?.let { selected ->
                            result.data.firstOrNull { p -> p.id == selected.id }
                        }
                    )
                }

                is NetworkResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    private fun messageFor(error: RefundError): String = when (error) {
        RefundError.PAYMENT_REQUIRED -> "Select a payment first"
        RefundError.AMOUNT_REQUIRED -> "Enter a refund amount"
        RefundError.AMOUNT_NOT_POSITIVE -> "Amount must be greater than zero"
        RefundError.EXCEEDS_REFUNDABLE -> "Amount exceeds the refundable balance"
    }
}

@Composable
fun PaymentHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaymentHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.actionSuccess) {
        val message = state.actionSuccess ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessages()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = "Payment history",
                showBack = true,
                onBack = onBack
            )
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))

            state.error != null -> ErrorState(
                message = state.error?.message ?: "Failed to load",
                onRetry = viewModel::retry,
                modifier = Modifier.padding(padding)
            )

            state.payments.isEmpty() -> EmptyState(
                icon = Icons.Filled.ReceiptLong,
                title = "No payments yet",
                message = "Payment records will appear here.",
                modifier = Modifier.padding(padding)
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(state.payments, key = { it.id }) { payment ->
                    PaymentListItem(
                        payment = payment,
                        onClick = { viewModel.openPayment(payment) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    GymBottomSheet(
        visible = state.selected != null,
        onDismiss = viewModel::closePayment,
        title = "Payment details"
    ) {
        val payment = state.selected ?: return@GymBottomSheet

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(
                Money.withSymbol(payment.amount),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${payment.method.name} · ${payment.status.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            payment.paidAt?.let {
                Text(
                    AppClock.formatDateTime(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            payment.reference?.takeIf { it.isNotBlank() }?.let {
                Text(
                    "Reference: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val refundable = refundableFor(state.payments, payment)
            if (payment.isSuccessful && refundable > 0.0) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "Refundable: ${Money.withSymbol(refundable)}",
                    style = MaterialTheme.typography.labelLarge
                )
                AmountInputField(
                    value = state.refundAmount,
                    onValueChange = viewModel::onRefundAmountChange,
                    label = "Refund amount",
                    isError = state.refundError != null,
                    errorMessage = state.refundError,
                    enabled = !state.isRefunding
                )
                OutlinedTextField(
                    value = state.refundReason,
                    onValueChange = viewModel::onRefundReasonChange,
                    label = { Text("Reason (optional)") },
                    enabled = !state.isRefunding,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Spacing.xs))
                DestructiveButton(
                    text = if (state.isRefunding) "Refunding…" else "Refund payment",
                    enabled = !state.isRefunding && state.refundAmount.isNotBlank(),
                    onClick = viewModel::submitRefund,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (payment.status == PaymentStatus.REFUNDED) {
                Text(
                    "This payment has already been refunded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}