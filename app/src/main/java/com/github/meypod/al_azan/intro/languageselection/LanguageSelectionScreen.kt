package com.github.meypod.al_azan.intro.languageselection

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.SupportedLanguages
import com.github.meypod.al_azan.core.presentation.components.BottomSelect
import com.github.meypod.al_azan.core.presentation.components.TertiaryButton
import com.github.meypod.al_azan.core.presentation.drawCurvedTopPatternedBackground
import com.github.meypod.al_azan.core.presentation.rememberPatternImageBitmap
import com.github.meypod.al_azan.intro.IntroUiAction
import com.github.meypod.al_azan.intro.components.IntroSkipButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionScreen(
    uiState: LanguageSelectionUiState,
    onAction: (LanguageSelectionUiAction) -> Unit,
    onIntroAction: (IntroUiAction) -> Unit,
) {
    val patternImage = rememberPatternImageBitmap(R.drawable.pattern)
    val selectedLanguage =
        remember(uiState.selectedLocale) {
            SupportedLanguages.firstOrNull { it.value == uiState.selectedLocale } ?: SupportedLanguages.first()
        }

    val mosquePainter = painterResource(id = R.drawable.mosque)

    Scaffold(
        containerColor = colorResource(R.color.intro_background),
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(30.dp))
            Text(
                text = stringResource(R.string.welcome),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Column(
                modifier = Modifier.weight(0.55f),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    modifier = Modifier
                        .offset(y = 25.dp)
                        .graphicsLayer { clip = false },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .drawWithCache {
                                val center = Offset(x = size.width / 2f, y = size.height * 0.5f)
                                val radius = size.height * 0.8f
                                val brush = Brush.radialGradient(
                                    colorStops =
                                        arrayOf(
                                            0.05f to Color(0x66139554),
                                            0.34f to Color(0x3365C088),
                                            1.0f to Color(0x1A00585A),
                                        ),
                                    center = center,
                                    radius = radius,
                                )
                                onDrawBehind {
                                    drawCircle(
                                        brush = brush,
                                        radius = radius,
                                        center = center,
                                    )
                                }
                            },
                    )
                    Image(
                        painter = mosquePainter,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(x = 6.dp, y = 6.dp)
                            .blur(8.dp),
                        contentScale = ContentScale.FillHeight,
                        colorFilter = ColorFilter.tint(Color.Black.copy(alpha = 0.5f), BlendMode.SrcIn),
                    )
                    Image(
                        painter = mosquePainter,
                        contentDescription = null,
                        contentScale = ContentScale.FillHeight,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Column(
                modifier =
                    Modifier
                        .drawCurvedTopPatternedBackground(
                            pattern = patternImage,
                            backgroundColor = colorResource(R.color.intro_curve_background),
                        )
                        .fillMaxWidth()
                        .weight(0.45f)
                        .padding(top = 50.dp, bottom = paddingValues.calculateBottomPadding()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.choose_language),
                    style = MaterialTheme.typography.titleMedium.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = Offset(0f, 2f),
                            blurRadius = 8f,
                        ),
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,

                )
                Spacer(modifier = Modifier.height(12.dp))
                BottomSelect(
                    options = SupportedLanguages,
                    optionKey = { it.value },
                    optionLabel = { it.label },
                    selectedKey = selectedLanguage.value,
                    onSelect = { onAction(LanguageSelectionUiAction.OnLanguageSelected(it.value)) },
                    searchable = true,
                    colors = ButtonDefaults.outlinedButtonColors().copy(
                        contentColor = Color.White,
                    ),
                )
                Spacer(modifier = Modifier.height(40.dp))
                TertiaryButton(
                    onClick = { onIntroAction(IntroUiAction.OnNextClick) },
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp),
                        text = stringResource(R.string.get_started),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                IntroSkipButton { onIntroAction(IntroUiAction.OnSkipClick) }
            }
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF00585A,
)
@Preview(
    showBackground = true,
    backgroundColor = 0xFF00585A,
    device = Devices.TABLET,
)
@Preview(
    showBackground = true,
    backgroundColor = 0xFF00585A,
    device = Devices.DESKTOP,
)
@Composable
private fun LanguageSelectionScreenPreview() {
    AlAzanTheme {
        LanguageSelectionScreen(
            uiState = LanguageSelectionUiState(
                selectedLocale = "en",
            ),
            onAction = {},
            onIntroAction = {},
        )
    }
}
