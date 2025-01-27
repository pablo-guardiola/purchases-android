package com.revenuecat.purchases.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class LocaleExtensionsTest {

    @Test
    fun `toLocale - converts valid locale tags to correct locale`() {
        assertThat("en".toLocale()).isEqualTo(Locale.ENGLISH)
        assertThat("en-US".toLocale()).isEqualTo(Locale.US)
        assertThat("es".toLocale()).isEqualTo(Locale("es"))
        assertThat("es-es".toLocale()).isEqualTo(Locale("es", "ES"))
    }

    @Test
    fun `toLocale - converts underscore formatted locale string to locale`() {
        assertThat("en_US".toLocale()).isEqualTo(Locale.US)
        assertThat("es_es".toLocale()).isEqualTo(Locale("es", "ES"))
    }

    @Test
    fun `toLocale - invalid locale strings return undefined locale`() {
        assertThat("invalidLocale".toLocale().toLanguageTag()).isEqualTo("und")
    }

    @Test
    fun `convertToCorrectlyFormattedLocale - converts valid locale to same locale`() {
        val originalLocale = Locale.US
        val convertedLocale = originalLocale.convertToCorrectlyFormattedLocale()
        assertThat(convertedLocale).isEqualTo(originalLocale)
    }

    @Test
    fun `convertToCorrectlyFormattedLocale - converts invalid locale to correct locale`() {
        val originalLocale = Locale("es-es")
        val convertedLocale = originalLocale.convertToCorrectlyFormattedLocale()
        assertThat(convertedLocale).isNotEqualTo(originalLocale)
        assertThat(convertedLocale).isEqualTo(Locale("es", "ES"))
    }

    @Test
    fun `sharedLanguageCodeWith - returns true when locales share language code`() {
        val locale = Locale("es", "ES")
        val otherLocale = Locale("es", "AR")
        assertThat(locale.sharedLanguageCodeWith(otherLocale)).isTrue
    }

    @Test
    fun `sharedLanguageCodeWith - returns false when locales don't share language code`() {
        val locale = Locale("es", "ES")
        val otherLocale = Locale("en", "ES")
        assertThat(locale.sharedLanguageCodeWith(otherLocale)).isFalse
    }
}
