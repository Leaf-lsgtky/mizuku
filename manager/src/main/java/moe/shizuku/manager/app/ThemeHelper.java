package moe.shizuku.manager.app;

import android.content.Context;
import android.util.TypedValue;
import com.google.android.material.snackbar.Snackbar;
import moe.shizuku.manager.R;

/**
 * Theming for the few remaining View-based surfaces (snackbars).
 *
 * The Compose UI is themed by {@code ThemeStore} / {@code ShizukuAppTheme}; this class only
 * resolves attributes from the already-applied Activity theme.
 */
public class ThemeHelper {

    public static void applySnackbarTheme(Context context, Snackbar snackbar) {
        snackbar.setBackgroundTint(resolveColor(context, R.attr.colorPrimaryContainer))
            .setTextColor(resolveColor(context, R.attr.colorOnSurface))
            .setActionTextColor(resolveColor(context, R.attr.colorPrimary));
    }

    private static int resolveColor(Context context, int color) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(color, typedValue, true);
        return typedValue.data;
    }
}
