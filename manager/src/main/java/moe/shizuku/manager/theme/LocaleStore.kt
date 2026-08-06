package moe.shizuku.manager.theme

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import moe.shizuku.manager.ShizukuSettings
import rikka.material.app.LocaleDelegate
import java.util.Locale

/**
 * Language selection.
 *
 * Uses [AppCompatDelegate.setApplicationLocales], which applies the new locale to running
 * activities immediately instead of leaving stale strings until the next cold start.
 */
object LocaleStore {

    private const val SYSTEM = "SYSTEM"

    /** The persisted tag, or [SYSTEM] when following the system language. */
    val currentTag: String
        get() = ShizukuSettings.getPreferences()
            .getString(ShizukuSettings.Keys.KEY_LANGUAGE, SYSTEM)
            .takeUnless { it.isNullOrEmpty() } ?: SYSTEM

    /**
     * Applies the persisted language. Call once at startup so an activity launched directly
     * (not via the launcher) still gets the right locale.
     */
    fun initialize() {
        apply(currentTag, persist = false)
    }

    fun setLocale(tag: String) = apply(tag, persist = true)

    private fun apply(tag: String, persist: Boolean) {
        val isSystem = tag == SYSTEM || tag.isEmpty()

        LocaleDelegate.defaultLocale =
            if (isSystem) LocaleDelegate.systemLocale else Locale.forLanguageTag(tag)

        AppCompatDelegate.setApplicationLocales(
            if (isSystem) LocaleListCompat.getEmptyLocaleList()
            else LocaleListCompat.forLanguageTags(tag)
        )

        if (persist) {
            ShizukuSettings.getPreferences().edit {
                putString(ShizukuSettings.Keys.KEY_LANGUAGE, tag)
            }
        }
    }
}
