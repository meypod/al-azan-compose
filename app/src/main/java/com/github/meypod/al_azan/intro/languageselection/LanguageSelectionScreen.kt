package com.github.meypod.al_azan.intro.languageselection

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.scale
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.SupportedLanguages
import com.github.meypod.al_azan.core.presentation.drawCurvedTopPatternedBackground
import com.github.meypod.al_azan.intro.IntroSkipButton
import com.github.meypod.al_azan.intro.IntroUiAction

private val IntroBackgroundColor = Color(0xFF00585A)
private val IntroCurveBackgroundColor = Color(0xFF006663)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionScreen(
    uiState: LanguageSelectionUiState,
    onAction: (LanguageSelectionUiAction) -> Unit,
    onIntroAction: (IntroUiAction) -> Unit,
) {
    val resources = LocalResources.current
    val containerSize = LocalWindowInfo.current.containerDpSize
    val density = LocalDensity.current
    val patternImage = remember(containerSize) {
        val original = BitmapFactory.decodeResource(resources, R.drawable.pattern)
        val sizeDp = if (containerSize.width >= 600.dp) 150 else 140
        val sizePx = (sizeDp * density.density).toInt().coerceAtLeast(1)
        original.scale(sizePx, sizePx).asImageBitmap()
    }
    var showLanguageSheet by rememberSaveable { mutableStateOf(false) }
    val selectedLanguage =
        remember(uiState.selectedLocale) {
            SupportedLanguages.firstOrNull { it.value == uiState.selectedLocale } ?: SupportedLanguages.first()
        }

    Column(
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
                            val radius = size.height * 0.6f
                            val brush = Brush.radialGradient(
                                colorStops =
                                    arrayOf(
                                        0.05f to Color(0x66139554),
                                        0.54f to Color(0x3365C088),
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
                    painter = painterResource(id = R.drawable.mosque),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(x = 6.dp, y = 6.dp)
                        .blur(8.dp),
                    contentScale = ContentScale.FillHeight,
                    colorFilter = ColorFilter.tint(Color.Black.copy(alpha = 0.5f), BlendMode.SrcIn),
                )
                Image(
                    painter = painterResource(id = R.drawable.mosque),
                    contentDescription = null,
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Column(
            modifier =
                Modifier
                    .drawCurvedTopPatternedBackground(pattern = patternImage, backgroundColor = IntroCurveBackgroundColor)
                    .fillMaxWidth()
                    .weight(0.45f)
                    .padding(top = 50.dp),
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
                color = Color.White,

                )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = selectedLanguage.label,
                onValueChange = {},
                readOnly = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                enabled = false,
                trailingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.baseline_arrow_drop_down_24),
                        contentDescription = null,
                    )
                },
                colors =
                    TextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        disabledContainerColor = Color.Transparent,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        disabledIndicatorColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f),
                    ),
                modifier =
                    Modifier
                        .widthIn(max = 290.dp)
                        .fillMaxWidth()
                        .clickable { showLanguageSheet = true },
                shape = RoundedCornerShape(10.dp),
            )
            if (showLanguageSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showLanguageSheet = false },
                ) {
                    Text(
                        text = stringResource(R.string.choose_language),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        items(SupportedLanguages) { language ->
                            DropdownMenuItem(
                                text = { Text(text = language.label) },
                                onClick = {
                                    onAction(LanguageSelectionUiAction.OnLanguageSelected(language.value))
                                    showLanguageSheet = false
                                },
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = { onIntroAction(IntroUiAction.OnGetStartedClick) },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                shape = RoundedCornerShape(40.dp),
                modifier =
                    Modifier
                        .height(56.dp),
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    text = stringResource(R.string.get_started),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            IntroSkipButton { onIntroAction(IntroUiAction.OnSkipClick) }
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
