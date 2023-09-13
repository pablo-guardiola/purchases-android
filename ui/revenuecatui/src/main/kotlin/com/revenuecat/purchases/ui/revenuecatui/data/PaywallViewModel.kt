package com.revenuecat.purchases.ui.revenuecatui.data

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewListener
import com.revenuecat.purchases.ui.revenuecatui.extensions.getActivity
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.toPaywallViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal interface PaywallViewModel {
    val state: StateFlow<PaywallViewState>

    fun selectPackage(packageToSelect: Package)

    /**
     * Purchase the selected package
     * Note: This method requires the context to be an activity or to allow reaching an activity
     */
    fun purchaseSelectedPackage(context: Context)
    fun restorePurchases()
}

internal class PaywallViewModelImpl(
    offering: Offering?,
    private val listener: PaywallViewListener?,
) : ViewModel(), PaywallViewModel {

    override val state: StateFlow<PaywallViewState>
        get() = _state.asStateFlow()
    private val initialState: PaywallViewState = offering?.toPaywallViewState() ?: PaywallViewState.Loading
    private val _state = MutableStateFlow(initialState)

    init {
        if (offering == null) {
            updateOffering()
        }
    }

    override fun selectPackage(packageToSelect: Package) {
        _state.value = when (val currentState = _state.value) {
            is PaywallViewState.Template2 -> {
                currentState.copy(selectedPackage = packageToSelect)
            }
            else -> {
                Logger.e("Unexpected state trying to select package: $currentState")
                currentState
            }
        }
    }

    override fun purchaseSelectedPackage(context: Context) {
        val activity = context.getActivity() ?: error("Activity not found")
        when (val currentState = _state.value) {
            is PaywallViewState.Template2 -> {
                purchasePackage(activity, currentState.selectedPackage)
            }
            else -> {
                Logger.e("Unexpected state trying to purchase package: $currentState")
            }
        }
    }

    override fun restorePurchases() {
        viewModelScope.launch {
            try {
                val customerInfo = Purchases.sharedInstance.awaitRestore()
                Logger.i("Restore purchases successful: $customerInfo")
            } catch (e: PurchasesException) {
                Logger.e("Error restoring purchases: $e")
            }
        }
    }

    private fun purchasePackage(activity: Activity, packageToPurchase: Package) {
        viewModelScope.launch {
            try {
                listener?.onPurchaseStarted(packageToPurchase)
                val purchaseResult = Purchases.sharedInstance.awaitPurchase(
                    PurchaseParams.Builder(activity, packageToPurchase).build(),
                )
                listener?.onPurchaseCompleted(purchaseResult.customerInfo, purchaseResult.storeTransaction)
            } catch (e: PurchasesException) {
                listener?.onPurchaseError(e.error)
            }
        }
    }

    private fun updateOffering() {
        viewModelScope.launch {
            try {
                val offerings = Purchases.sharedInstance.awaitOfferings()
                val currentOffering = offerings.current
                if (currentOffering == null) {
                    _state.value = PaywallViewState.Error("No offering or current offering")
                } else {
                    _state.value = currentOffering.toPaywallViewState()
                }
            } catch (e: PurchasesException) {
                _state.value = PaywallViewState.Error(e.toString())
            }
        }
    }
}