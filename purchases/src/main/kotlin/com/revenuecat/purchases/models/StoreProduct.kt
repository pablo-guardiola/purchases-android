package com.revenuecat.purchases.models

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.utils.pricePerMonth
import com.revenuecat.purchases.utils.pricePerWeek
import com.revenuecat.purchases.utils.pricePerYear
import java.util.Locale

/**
 * Represents an in-app product's or subscription's listing details.
 */
interface StoreProduct {
    /**
     * The product ID.
     * Google INAPP: "<productId>"
     * Google Sub: "<productId:basePlanID>"
     * Amazon INAPP: "<sku>"
     * Amazon Sub: "<termSku>"
     */
    val id: String

    /**
     * Type of product. One of [ProductType].
     */
    val type: ProductType

    /**
     * Price information for a non-subscription product.
     * Base plan price for a Google subscription.
     * Term price for an Amazon subscription.
     * For Google subscriptions, use SubscriptionOption's pricing phases for offer pricing.
     */
    val price: Price

    /**
     * Title of the product.
     *
     * If you are using Google subscriptions with multiple base plans, this title
     * will be the same for every subscription duration (monthly, yearly, etc) as
     * base plans don't have their own titles. Google suggests using the duration
     * as a way to title base plans.
     */
    val title: String

    /**
     * The description of the product.
     */
    val description: String

    /**
     * Subscription period.
     *
     * Note: Returned only for Google subscriptions. Null for Amazon or for INAPP products.
     */
    val period: Period?

    /**
     * Contains all [SubscriptionOption]s. Null for Amazon or for INAPP products.
     */
    val subscriptionOptions: SubscriptionOptions?

    /**
     * The default [SubscriptionOption] that will be used when purchasing and not specifying a different option.
     * Null for INAPP products.
     */
    val defaultOption: SubscriptionOption?

    /**
     * Contains only data that is required to make the purchase.
     */
    val purchasingData: PurchasingData

    /**
     * The offering ID this `StoreProduct` was returned from.
     *
     * Null if not using RevenueCat offerings system, or if fetched directly via `Purchases.getProducts`
     */
    val presentedOfferingIdentifier: String?

    /**
     * The sku of the StoreProduct
     */
    @Deprecated(
        "Replaced with id",
        ReplaceWith("id"),
    )
    val sku: String

    /**
     * For internal RevenueCat use.
     *
     * Creates a copy of this `StoreProduct` with the specified `offeringId` set.
     */
    fun copyWithOfferingId(offeringId: String): StoreProduct

    /**
     * Null for INAPP products. The price of the [StoreProduct] in the given locale in a weekly recurrence.
     * This means that, for example, if the period is monthly, the price will be divided by 4.
     * It uses a currency formatter to format the price in the given locale.
     * Note that this value may be an approximation.
     * For Google subscriptions, this value will use the basePlan to calculate the value.
     * @param locale Locale to use for formatting the price. Default is the system default locale.
     */
    fun pricePerWeek(locale: Locale = Locale.getDefault()): Price? {
        return period?.let { price.pricePerWeek(it, locale) }
    }

    /**
     * Null for INAPP products. The price of the [StoreProduct] in the given locale in a monthly recurrence.
     * This means that, for example, if the period is annual, the price will be divided by 12.
     * It uses a currency formatter to format the price in the given locale.
     * Note that this value may be an approximation.
     * For Google subscriptions, this value will use the basePlan to calculate the value.
     * @param locale Locale to use for formatting the price. Default is the system default locale.
     */
    fun pricePerMonth(locale: Locale = Locale.getDefault()): Price? {
        return period?.let { price.pricePerMonth(it, locale) }
    }

    /**
     * Null for INAPP products. The price of the [StoreProduct] in the given locale in a yearly recurrence.
     * This means that, for example, if the period is monthly, the price will be multiplied by 12.
     * It uses a currency formatter to format the price in the given locale.
     * Note that this value may be an approximation.
     * For Google subscriptions, this value will use the basePlan to calculate the value.
     * @param locale Locale to use for formatting the price. Default is the system default locale.
     */
    fun pricePerYear(locale: Locale = Locale.getDefault()): Price? {
        return period?.let { price.pricePerYear(it, locale) }
    }

    /**
     * Null for INAPP products. The price of the [StoreProduct] in the given locale in a monthly recurrence.
     * This means that, for example, if the period is annual, the price will be divided by 12.
     * It uses a currency formatter to format the price in the given locale.
     * Note that this value may be an approximation.
     * For Google subscriptions, this value will use the basePlan to calculate the value.
     * @param locale Locale to use for formatting the price. Default is the system default locale.
     */
    fun formattedPricePerMonth(locale: Locale = Locale.getDefault()): String? {
        return pricePerMonth(locale)?.formatted
    }
}
