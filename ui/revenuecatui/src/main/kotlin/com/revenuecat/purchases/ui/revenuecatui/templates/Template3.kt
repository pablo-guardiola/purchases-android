package com.revenuecat.purchases.ui.revenuecatui.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.InternalPaywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.UIConstant
import com.revenuecat.purchases.ui.revenuecatui.composables.Footer
import com.revenuecat.purchases.ui.revenuecatui.composables.IconImage
import com.revenuecat.purchases.ui.revenuecatui.composables.Markdown
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferDetails
import com.revenuecat.purchases.ui.revenuecatui.composables.PaywallIcon
import com.revenuecat.purchases.ui.revenuecatui.composables.PaywallIconName
import com.revenuecat.purchases.ui.revenuecatui.composables.PurchaseButton
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.isInFullScreenMode
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.selectedLocalization
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData

private object Template3UIConstants {
    val iconCornerRadius = 8.dp
    val iconSize = 70.dp
    val featureIconSize = 35.dp
    val iconPadding = 5.dp
    val featureSpacing = 40.dp
}

@Composable
internal fun Template3(
    state: PaywallState.Loaded,
    viewModel: PaywallViewModel,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (state.isInFullScreenMode) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        horizontal = UIConstant.defaultHorizontalPadding,
                        vertical = UIConstant.defaultVerticalSpacing,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(UIConstant.defaultVerticalSpacing, Alignment.Top),
            ) {
                Template3MainContent(state)
            }
        }

        Spacer(modifier = Modifier.height(UIConstant.defaultVerticalSpacing))

        OfferDetails(state = state, color = state.templateConfiguration.getCurrentColors().text2)
        PurchaseButton(state, viewModel)
        Footer(templateConfiguration = state.templateConfiguration, viewModel = viewModel)
    }
}

@Composable
private fun ColumnScope.Template3MainContent(state: PaywallState.Loaded) {
    IconImage(
        uri = state.templateConfiguration.images.iconUri,
        maxWidth = Template3UIConstants.iconSize,
        iconCornerRadius = Template3UIConstants.iconCornerRadius,
        childModifier = Modifier.padding(top = UIConstant.defaultVerticalSpacing),
    )
    val localizedConfig = state.selectedLocalization
    val colors = state.templateConfiguration.getCurrentColors()
    Markdown(
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        text = localizedConfig.title,
        color = colors.text1,
    )
    Spacer(modifier = Modifier.weight(1f))
    Features(
        features = localizedConfig.features,
        colors = colors,
    )
    Spacer(modifier = Modifier.weight(1f))
}

@Composable
private fun Features(
    features: List<PaywallData.LocalizedConfiguration.Feature>,
    colors: TemplateConfiguration.Colors,
) {
    if (features.isEmpty()) return

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(Template3UIConstants.featureSpacing, Alignment.Top),
    ) {
        features.forEach { feature ->
            Feature(
                feature = feature,
                colors = colors,
            )
        }
    }
}

@Composable
private fun Feature(
    feature: PaywallData.LocalizedConfiguration.Feature,
    colors: TemplateConfiguration.Colors,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UIConstant.defaultHorizontalPadding)
            .padding(top = Template3UIConstants.iconPadding * 2)
            .semantics(mergeDescendants = true) {},
    ) {
        feature.iconID?.let { PaywallIconName.fromValue(it) }?.let { icon ->
            Box(
                modifier = Modifier
                    .size(Template3UIConstants.featureIconSize)
                    .clip(CircleShape)
                    .background(colors.accent2),
            ) {
                PaywallIcon(
                    icon = icon,
                    tintColor = colors.accent1,
                    modifier = Modifier.padding(Template3UIConstants.iconPadding),
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(start = UIConstant.defaultHorizontalPadding),
        ) {
            Markdown(
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                text = feature.title,
                color = colors.text1,
            )
            feature.content?.let { content ->
                Markdown(
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Start,
                    text = content,
                    color = colors.text2,
                )
            }
        }
    }
}

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
@Preview(locale = "en-rUS", showBackground = true)
@Preview(locale = "es-rES", showBackground = true)
@Preview(showBackground = true, device = Devices.NEXUS_7)
@Preview(showBackground = true, device = Devices.NEXUS_10)
@Composable
private fun Template3Preview() {
    InternalPaywall(
        options = PaywallOptions.Builder(dismissRequest = {}).build(),
        viewModel = MockViewModel(offering = TestData.template3Offering),
    )
}

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
@Preview(showBackground = true)
@Composable
private fun Template3FooterPreview() {
    InternalPaywall(
        options = PaywallOptions.Builder(dismissRequest = {}).build(),
        viewModel = MockViewModel(mode = PaywallMode.FOOTER, offering = TestData.template3Offering),
    )
}

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
@Preview(showBackground = true)
@Composable
private fun Template3CondensedFooterPreview() {
    InternalPaywall(
        options = PaywallOptions.Builder(dismissRequest = {}).build(),
        viewModel = MockViewModel(mode = PaywallMode.FOOTER_CONDENSED, offering = TestData.template3Offering),
    )
}
