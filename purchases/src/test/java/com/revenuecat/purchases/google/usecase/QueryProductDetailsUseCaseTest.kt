package com.revenuecat.purchases.google.usecase

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.google.productId
import com.revenuecat.purchases.google.productList
import com.revenuecat.purchases.google.productType
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.utils.mockProductDetails
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class QueryProductDetailsUseCaseTest: BaseBillingUseCaseTest() {

    private lateinit var mockDetailsList: List<ProductDetails>

    private var storeProducts: List<StoreProduct>? = null

    @Before
    override fun setup() {
        super.setup()
        mockDetailsList = listOf(mockProductDetails())
    }

    @Test
    fun whenProductDetailsIsEmptyPassAnEmptyListToTheListener() {
        mockEmptyProductDetailsResponse()

        val productIDs = setOf("product_a")

        var receivedList: List<StoreProduct>? = null
        wrapper.queryProductDetailsAsync(
            ProductType.SUBS,
            productIDs, {
                receivedList = it
            }, {
                AssertionsForClassTypes.fail("shouldn't be an error")
            })

        assertThat(receivedList).isNotNull
        assertThat(receivedList!!.size).isZero
    }

    @Test
    fun `product type defaults to INAPP when querying product details`() {
        val slot = slot<QueryProductDetailsParams>()
        every {
            mockClient.queryProductDetailsAsync(
                capture(slot),
                any()
            )
        } just Runs

        val productIDs = setOf("product_a")

        wrapper.queryProductDetailsAsync(
            ProductType.UNKNOWN,
            productIDs,
            {
                this@QueryProductDetailsUseCaseTest.storeProducts = it
            }, {
                AssertionsForClassTypes.fail("shouldn't be an error")
            })

        assertThat(slot.isCaptured).isTrue
        assertThat(slot.captured.productList[0].productType).isEqualTo(BillingClient.ProductType.INAPP)
    }

    @Test
    fun `queryProductDetails filters empty productIds before querying BillingClient`() {
        val productIdsSet = setOf("abcd", "", "1", "")

        val slot = slot<QueryProductDetailsParams>()
        every {
            mockClient.queryProductDetailsAsync(capture(slot), any())
        } just Runs

        wrapper.queryProductDetailsAsync(
            ProductType.SUBS,
            productIdsSet,
            {}, {
                AssertionsForClassTypes.fail("shouldn't be an error")
            })

        assertThat(slot.captured).isNotNull
        val queryProductDetailsParamsProductList = slot.captured.productList
        val queriedProductIds = queryProductDetailsParamsProductList.map { it.productId }
        assertThat(queriedProductIds).isEqualTo(productIdsSet.filter { it.isNotEmpty() })
    }

    @Test
    fun `queryProductDetails with empty list returns empty list and does not query BillingClient`() {
        wrapper.queryProductDetailsAsync(
            ProductType.SUBS,
            emptySet(),
            {
                assertThat(it).isEmpty()
            }, {
                AssertionsForClassTypes.fail("shouldn't be an error")
            })

        verify(exactly = 0) {
            mockClient.queryProductDetailsAsync(any(), any())
        }
    }

    @Test
    fun `queryProductDetails with only empty productIds returns empty list and does not query BillingClient`() {
        wrapper.queryProductDetailsAsync(
            ProductType.SUBS,
            setOf("", ""),
            {
                assertThat(it).isEmpty()
            }, {
                AssertionsForClassTypes.fail("shouldn't be an error")
            })

        verify(exactly = 0) {
            mockClient.queryProductDetailsAsync(any(), any())
        }
    }

    @Test
    fun `queryProductDetailsAsync only calls one response when BillingClient responds twice`() {
        var numCallbacks = 0

        val slot = slot<ProductDetailsResponseListener>()
        every {
            mockClient.queryProductDetailsAsync(
                any(),
                capture(slot)
            )
        } answers {
            slot.captured.onProductDetailsResponse(billingClientOKResult, emptyList())
            slot.captured.onProductDetailsResponse(billingClientOKResult, emptyList())
        }

        wrapper.queryProductDetailsAsync(
            ProductType.SUBS,
            setOf("asdf", "asdf"),
            {
                Thread.sleep(200)
                numCallbacks++
            }, {
                numCallbacks++
            })

        assertThat(numCallbacks).isEqualTo(1)
    }

    @Test
    fun `queryProductDetailsAsync only calls one response when BillingClient responds twice in separate threads`() {
        val numCallbacks = AtomicInteger(0)

        val slot = slot<ProductDetailsResponseListener>()
        val lock = CountDownLatch(3)
        every {
            mockClient.queryProductDetailsAsync(
                any(),
                capture(slot)
            )
        } answers {
            Thread {
                slot.captured.onProductDetailsResponse(billingClientOKResult, emptyList())
                lock.countDown()
            }.start()

            Thread {
                slot.captured.onProductDetailsResponse(billingClientOKResult, emptyList())
                lock.countDown()
            }.start()
        }

        wrapper.queryProductDetailsAsync(
            ProductType.SUBS,
            setOf("asdf"),
            {
                // ensuring we don't hit an edge case where numCallbacks doesn't increment before the final assert
                numCallbacks.incrementAndGet()
                lock.countDown()
            }, {
                AssertionsForClassTypes.fail("shouldn't be an error")
            })

        lock.await()
        assertThat(lock.count).isEqualTo(0)

        assertThat(numCallbacks.get()).isEqualTo(1)
    }

    // region retries

    @Test
    fun `If service is disconnected, re-executeRequestOnUIThread`() {
        val slot = slot<ProductDetailsResponseListener>()
        val queryProductDetailsStubbing = every {
            mockClient.queryProductDetailsAsync(
                any(),
                capture(slot)
            )
        }
        val productIDs = setOf("product_a")
        var receivedList: List<StoreProduct>? = null
        var timesExecutedInMainThread = 0
        val useCase = QueryProductDetailsUseCase(
            QueryProductDetailsUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
                productIDs,
                ProductType.SUBS,
            ),
            { received ->
                receivedList = received
            },
            { _ ->
                fail("shouldn't be an error")
            },
            withConnectedClient = {
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = {
                timesExecutedInMainThread++

                queryProductDetailsStubbing answers {
                    if (timesExecutedInMainThread == 0) {
                        slot.captured.onProductDetailsResponse(
                            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED.buildResult(),
                            emptyList()
                        )
                    } else {
                        slot.captured.onProductDetailsResponse(
                            BillingClient.BillingResponseCode.OK.buildResult(),
                            mockDetailsList
                        )
                    }
                }

                it.invoke(null)
            },
        )

        useCase.run()

        assertThat(receivedList).isNotNull
        assertThat(receivedList!!.size).isOne
    }

    @Test
    fun `If service returns NETWORK_ERROR, re-execute a max of 3 times`() {
        val slot = slot<ProductDetailsResponseListener>()
        val queryProductDetailsStubbing = every {
            mockClient.queryProductDetailsAsync(
                any(),
                capture(slot)
            )
        }
        val productIDs = setOf("product_a")
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = QueryProductDetailsUseCase(
            QueryProductDetailsUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
                productIDs,
                ProductType.SUBS,
            ),
            { _ ->
                fail("shouldn't be success")
            },
            { error ->
                receivedError = error
            },
            withConnectedClient = {
                timesRetried++
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = {
                queryProductDetailsStubbing answers {
                    slot.captured.onProductDetailsResponse(
                        BillingClient.BillingResponseCode.NETWORK_ERROR.buildResult(),
                        emptyList()
                    )
                }

                it.invoke(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(4) // First attempt plus 3 retries
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.NetworkError)
    }

    @Test
    fun `If service returns SERVICE_UNAVAILABLE, re-execute a max of 3 times`() {
        val slot = slot<ProductDetailsResponseListener>()
        val queryProductDetailsStubbing = every {
            mockClient.queryProductDetailsAsync(
                any(),
                capture(slot)
            )
        }
        val productIDs = setOf("product_a")
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = QueryProductDetailsUseCase(
            QueryProductDetailsUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
                productIDs,
                ProductType.SUBS,
            ),
            { _ ->
                fail("shouldn't be success")
            },
            { error ->
                receivedError = error
            },
            withConnectedClient = {
                timesRetried++
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = {
                queryProductDetailsStubbing answers {
                    slot.captured.onProductDetailsResponse(
                        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult(),
                        emptyList()
                    )
                }

                it.invoke(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(4) // First attempt plus 3 retries
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `If service returns ERROR, re-execute a max of 3 times`() {
        val slot = slot<ProductDetailsResponseListener>()
        val queryProductDetailsStubbing = every {
            mockClient.queryProductDetailsAsync(
                any(),
                capture(slot)
            )
        }
        val productIDs = setOf("product_a")
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = QueryProductDetailsUseCase(
            QueryProductDetailsUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
                productIDs,
                ProductType.SUBS,
            ),
            { _ ->
                fail("shouldn't be success")
            },
            { error ->
                receivedError = error
            },
            withConnectedClient = {
                timesRetried++
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = {
                queryProductDetailsStubbing answers {
                    slot.captured.onProductDetailsResponse(
                        BillingClient.BillingResponseCode.ERROR.buildResult(),
                        emptyList()
                    )
                }

                it.invoke(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(4) // First attempt plus 3 retries
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `If service returns ITEM_UNAVAILABLE, doesn't retry`() {
        val slot = slot<ProductDetailsResponseListener>()
        val queryProductDetailsStubbing = every {
            mockClient.queryProductDetailsAsync(
                any(),
                capture(slot)
            )
        }
        val productIDs = setOf("product_a")
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = QueryProductDetailsUseCase(
            QueryProductDetailsUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
                productIDs,
                ProductType.SUBS,
            ),
            { _ ->
                fail("shouldn't be success")
            },
            { error ->
                receivedError = error
            },
            withConnectedClient = {
                timesRetried++
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = {
                queryProductDetailsStubbing answers {
                    slot.captured.onProductDetailsResponse(
                        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE.buildResult(),
                        emptyList()
                    )
                }

                it.invoke(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(1)
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.ProductNotAvailableForPurchaseError)
    }

    // endregion retries

    private fun mockEmptyProductDetailsResponse() {
        val slot = slot<ProductDetailsResponseListener>()
        every {
            mockClient.queryProductDetailsAsync(
                any(),
                capture(slot)
            )
        } answers {
            slot.captured.onProductDetailsResponse(billingClientOKResult, emptyList())
        }
    }

}