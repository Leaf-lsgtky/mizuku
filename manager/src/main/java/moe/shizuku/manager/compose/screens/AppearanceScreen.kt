package moe.shizuku.manager.compose.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import moe.shizuku.manager.R
import moe.shizuku.manager.theme.ColorSource
import moe.shizuku.manager.theme.ColorSpec
import moe.shizuku.manager.theme.DarkMode
import moe.shizuku.manager.theme.LocaleStore
import moe.shizuku.manager.theme.PaletteStyle
import moe.shizuku.manager.theme.PresetSeedColors
import moe.shizuku.manager.theme.ThemeStore
import moe.shizuku.manager.theme.UiStyle
import moe.shizuku.manager.theme.colorSchemeFromSeed
import rikka.shizuku.manager.ShizukuLocales
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.util.Locale

/**
 * All appearance settings in one place.
 *
 * Every control writes through [ThemeStore], whose state is observed by the theme root, so
 * changes are visible on the next frame.
 */
@Composable
fun AppearanceScreen(
    scrollBehavior: ScrollBehavior,
    scaffoldPadding: PaddingValues = PaddingValues(0.dp),
) {
    val state = ThemeStore.state

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(
            top = scaffoldPadding.calculateTopPadding(),
            bottom = scaffoldPadding.calculateBottomPadding(),
        ),
        overscrollEffect = null,
    ) {
        item { Spacer(Modifier.height(12.dp)) }

        // ---- Style: which design system, and light/dark ----
        item { SmallTitle(text = stringResource(R.string.appearance_style)) }
        item {
            SettingsCard {
                OverlayDropdownPreference(
                    title = stringResource(R.string.settings_theme_mode),
                    items = listOf(
                        stringResource(R.string.ui_style_material),
                        stringResource(R.string.ui_style_miuix),
                    ),
                    selectedIndex = state.uiStyle.value,
                    onSelectedIndexChange = { ThemeStore.setUiStyle(UiStyle.fromValue(it)) },
                )

                OverlayDropdownPreference(
                    title = stringResource(R.string.dark_theme),
                    items = listOf(
                        stringResource(R.string.follow_system),
                        stringResource(R.string.dark_theme_off),
                        stringResource(R.string.dark_theme_on),
                    ),
                    selectedIndex = state.darkMode.value,
                    onSelectedIndexChange = { ThemeStore.setDarkMode(DarkMode.fromValue(it)) },
                )

                AnimatedVisibility(visible = state.darkMode != DarkMode.Light) {
                    SwitchPreference(
                        title = stringResource(R.string.settings_black_night_theme),
                        summary = stringResource(R.string.settings_black_night_theme_summary),
                        checked = state.pureBlack,
                        onCheckedChange = { ThemeStore.setPureBlack(it) },
                    )
                }
            }
        }

        // ---- Colors ----
        item { SmallTitle(text = stringResource(R.string.appearance_colors)) }
        item {
            SettingsCard {
                val sources = buildList {
                    add(ColorSource.Default to stringResource(R.string.color_source_default))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        add(ColorSource.Wallpaper to stringResource(R.string.color_source_wallpaper))
                    }
                    add(ColorSource.Custom to stringResource(R.string.color_source_custom))
                }
                val selected = sources.indexOfFirst { it.first == state.colorSource }
                    .coerceAtLeast(0)

                OverlayDropdownPreference(
                    title = stringResource(R.string.color_source),
                    items = sources.map { it.second },
                    selectedIndex = selected,
                    onSelectedIndexChange = { ThemeStore.setColorSource(sources[it].first) },
                )

                AnimatedVisibility(visible = state.colorSource == ColorSource.Custom) {
                    SeedColorGrid(
                        selected = state.seedColor,
                        onSelect = { ThemeStore.setSeedColor(it) },
                    )
                }

                // Palette style and spec only affect generated palettes.
                AnimatedVisibility(visible = state.isSeeded) {
                    Column {
                        OverlayDropdownPreference(
                            title = stringResource(R.string.palette_style),
                            items = PaletteStyle.entries.map { it.name },
                            selectedIndex = state.paletteStyle.value,
                            onSelectedIndexChange = {
                                ThemeStore.setPaletteStyle(PaletteStyle.fromValue(it))
                            },
                        )

                        val spec2025Supported = state.paletteStyle.supportsSpec2025
                        OverlayDropdownPreference(
                            title = stringResource(R.string.color_spec),
                            // Show the spec actually in effect, not the stored one, so the
                            // fallback for styles without 2025 mappings isn't confusing.
                            summary = if (spec2025Supported) null
                            else stringResource(R.string.color_spec_2021_only),
                            items = listOf("2021", "2025"),
                            selectedIndex = state.effectiveColorSpec.value,
                            enabled = spec2025Supported,
                            onSelectedIndexChange = {
                                ThemeStore.setColorSpec(ColorSpec.fromValue(it))
                            },
                        )
                    }
                }
            }
        }

        // ---- Effects (Miuix only) ----
        if (state.isMiuix) {
            item { SmallTitle(text = stringResource(R.string.appearance_effects)) }
            item {
                SettingsCard {
                    SwitchPreference(
                        title = stringResource(R.string.settings_enable_blur),
                        summary = stringResource(R.string.settings_enable_blur_summary),
                        checked = state.enableBlur,
                        onCheckedChange = { ThemeStore.setEnableBlur(it) },
                    )

                    SwitchPreference(
                        title = stringResource(R.string.settings_floating_bottom_bar),
                        summary = stringResource(R.string.settings_floating_bottom_bar_summary),
                        checked = state.enableFloatingBottomBar,
                        onCheckedChange = { ThemeStore.setEnableFloatingBottomBar(it) },
                    )

                    AnimatedVisibility(
                        visible = state.enableFloatingBottomBar &&
                            state.enableBlur &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ) {
                        SwitchPreference(
                            title = stringResource(R.string.settings_floating_bottom_bar_blur),
                            summary = stringResource(R.string.settings_floating_bottom_bar_blur_summary),
                            checked = state.enableFloatingBottomBarBlur,
                            onCheckedChange = { ThemeStore.setEnableFloatingBottomBarBlur(it) },
                        )
                    }
                }
            }
        }

        // ---- Language ----
        item { SmallTitle(text = stringResource(R.string.appearance_language)) }
        item {
            SettingsCard {
                val tags = ShizukuLocales.LOCALES
                val displayTags = ShizukuLocales.DISPLAY_LOCALES
                val entries = displayTags.mapIndexed { index, tag ->
                    if (index == 0) stringResource(R.string.follow_system)
                    else Locale.forLanguageTag(tag).let { it.getDisplayName(it) }
                }
                val index = tags.indexOf(LocaleStore.currentTag).coerceAtLeast(0)

                OverlayDropdownPreference(
                    title = stringResource(R.string.settings_language),
                    items = entries,
                    selectedIndex = index,
                    onSelectedIndexChange = { LocaleStore.setLocale(tags[it]) },
                )
            }
        }

        item { Spacer(Modifier.navigationBarsPadding()) }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
    ) { content() }
}

/**
 * Accent swatches. Each shows a real generated palette rather than the flat seed, so the
 * preview matches what selecting it produces.
 */
@Composable
private fun SeedColorGrid(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        val columns = (maxWidth / 56.dp).toInt().coerceIn(4, 8)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PresetSeedColors.chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { seed ->
                        val argb = seed.color.toArgb()
                        SeedSwatch(
                            seed = seed.color,
                            isSelected = argb == selected,
                            onClick = { onSelect(argb) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // Keep the last row's items the same width as the others.
                    repeat(columns - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SeedSwatch(
    seed: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = remember(seed) {
        colorSchemeFromSeed(seed = seed, isDark = false, style = PaletteStyle.TonalSpot)
    }

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(scheme.primaryContainer)
                .then(
                    if (isSelected) Modifier.border(
                        width = 2.dp,
                        color = MiuixTheme.colorScheme.onBackground,
                        shape = CircleShape,
                    ) else Modifier
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(if (isSelected) 18.dp else 22.dp)
                    .clip(CircleShape)
                    .background(scheme.primary)
            )
        }
    }
}
