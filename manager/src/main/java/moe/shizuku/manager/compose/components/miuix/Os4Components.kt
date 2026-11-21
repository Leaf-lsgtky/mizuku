package moe.shizuku.manager.compose.components.miuix

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.rememberTextMeasurer
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassNavigationBar
import top.yukonga.miuix.kmp.glass.GlassNavigationBarDefaults
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import moe.shizuku.manager.R
import moe.shizuku.manager.compose.theme.LocalThemeState
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.ChevronBackward
import top.yukonga.miuix.kmp.icon.glass.Close
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Backdrop available to page-local Glass dialogs, sheets, and dropdowns. */
val LocalPageBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/** The stock indicator requires bounded constraints, so measure labels before sizing its capsule. */
@Composable
fun ContentSizedGlassNavigationBar(
    items: List<GlassNavigationItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop?,
) {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val textMeasurer = rememberTextMeasurer()
    val labelSize = with(density) {
        if (fontScale >= 1.6f) GlassNavigationBarDefaults.LargeLabelSize.toSp()
        else GlassNavigationBarDefaults.LabelSize.toSp()
    }
    val labelStyle = MiuixTheme.textStyles.footnote2.copy(fontSize = labelSize)
    val widestLabel = items.maxOfOrNull { item ->
        textMeasurer.measure(item.label.orEmpty(), style = labelStyle, maxLines = 1, softWrap = false).size.width
    } ?: 0
    val slotWidth = maxOf(
        76.dp,
        with(density) { widestLabel.toDp() } + GlassNavigationBarDefaults.IndicatorOverhang * 2,
    )
    val contentWidth = slotWidth * items.size + GlassNavigationBarDefaults.ContentPaddingHorizontal * 2
    val safePadding = WindowInsets.displayCutout.union(WindowInsets.navigationBars)
        .only(WindowInsetsSides.Horizontal).asPaddingValues()
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val availableWidth = (maxWidth - safePadding.calculateLeftPadding(direction) -
            safePadding.calculateRightPadding(direction)).coerceAtLeast(0.dp)
        GlassNavigationBar(
            items = items,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            backdrop = backdrop,
            modifier = Modifier.width(contentWidth.coerceAtMost(availableWidth)),
        )
    }
}

/** Compact titles preserve content height on wide and short windows. */
@Composable
fun isOs4WideWindow(): Boolean {
    val size = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val width = with(density) { size.width.toDp() }
    val height = with(density) { size.height.toDp() }
    return width >= 840.dp || (width >= 600.dp && height < width * 1.2f)
}

/**
 * OS4-style secondary page shell. The outer Scaffold owns Glass popup rendering while the inner
 * page Scaffold owns its bar and content padding. Only the page body is recorded for the bar;
 * popup content and the bar itself stay outside that recording.
 */
@Composable
fun Os4Scaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    closeAction: Boolean = false,
    showTopBar: Boolean = true,
    topBarAlpha: () -> Float = { 1f },
    topBarContentModifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues, ScrollBehavior, Modifier) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val wideWindow = isOs4WideWindow()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val surfaceColor = MiuixTheme.colorScheme.surface
    val effectsEnabled = LocalThemeState.current.enableBlur && isRuntimeShaderSupported()
    val pageBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val contentBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val sampledPageBackdrop = pageBackdrop.takeIf { effectsEnabled }
    val sampledContentBackdrop = contentBackdrop.takeIf { effectsEnabled }
    val contentModifier = Modifier
        .then(if (effectsEnabled) Modifier.layerBackdrop(contentBackdrop) else Modifier)
        .windowInsetsPadding(
            WindowInsets.displayCutout
                .union(WindowInsets.navigationBars)
                .only(WindowInsetsSides.Horizontal),
        )

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        containerColor = surfaceColor,
    ) {
        CompositionLocalProvider(LocalPageBackdrop provides sampledPageBackdrop) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize().then(
                    if (effectsEnabled) Modifier.layerBackdrop(pageBackdrop) else Modifier,
                ),
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (showTopBar) GlassTopAppBar(
                            title = title,
                            modifier = Modifier
                                .graphicsLayer { alpha = topBarAlpha() }
                                .layout { measurable, constraints ->
                                    val bar = measurable.measure(constraints)
                                    layout(bar.width, bar.height) {
                                        if (topBarAlpha() > 0f) bar.placeRelative(0, 0)
                                    }
                                },
                            contentModifier = topBarContentModifier,
                            expandable = !wideWindow,
                            backdrop = sampledContentBackdrop,
                            progressiveBlur = effectsEnabled,
                            titleBlurRadius = if (effectsEnabled) {
                                GlassTopAppBarDefaults.TitleBlurRadius
                            } else {
                                0.dp
                            },
                            scrollBehavior = scrollBehavior,
                            actions = { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions) },
                            navigationIcon = {
                                GlassIconButton(onClick = onBack) {
                                    Icon(
                                        imageVector = if (closeAction) MiuixIcons.Glass.Close
                                        else MiuixIcons.Glass.ChevronBackward,
                                        contentDescription = stringResource(
                                            if (closeAction) R.string.about_dialog_button_close
                                            else R.string.navigation_back,
                                        ),
                                        modifier = Modifier.size(28.dp).graphicsLayer {
                                            scaleX = if (!closeAction && isRtl) -1f else 1f
                                        },
                                    )
                                }
                            },
                        )
                    },
                    popupHost = {},
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
                ) { paddingValues ->
                    content(paddingValues, scrollBehavior, contentModifier)
                }
            }
        }
    }
}
