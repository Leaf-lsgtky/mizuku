package rikka.shizuku.server.util;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

public final class InstalledPackagesCompat {

    private static final String TAG = "InstalledPackagesCompat";
    private static final int ANDROID_13 = 33;
    static final String PARCELED_LIST_SLICE = "android.content.pm.ParceledListSlice";

    private InstalledPackagesCompat() {
    }

    public static List<PackageInfo> getInstalledPackagesNoThrow(long flags, int userId) {
        try {
            List<PackageInfo> packages = getInstalledPackages(flags, userId);
            return packages == null ? Collections.emptyList() : packages;
        } catch (Throwable e) {
            Log.w(TAG, "getInstalledPackages failed", e);
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    public static List<PackageInfo> getInstalledPackages(long flags, int userId) throws ReflectiveOperationException {
        // Path 1: try getInstalledPackagesAsUser via context PackageManager
        try {
            Object packageManager = getContextPackageManager();
            Method method = packageManager.getClass().getMethod("getInstalledPackagesAsUser", int.class, int.class);
            Object result = invoke(method, packageManager, (int) flags, userId);
            List<PackageInfo> unwrapped = unwrapResult(result);
            if (unwrapped != null) return unwrapped;
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            Log.d(TAG, "getInstalledPackagesAsUser failed, falling back to hidden API", e);
        }

        // Path 2: try hidden IPackageManager.getInstalledPackages
        try {
            Object packageManager = getPackageManager();
            Method method;
            Object result;

            if (Build.VERSION.SDK_INT >= ANDROID_13) {
                method = packageManager.getClass().getMethod("getInstalledPackages", long.class, int.class);
                result = invoke(method, packageManager, flags, userId);
            } else {
                method = packageManager.getClass().getMethod("getInstalledPackages", int.class, int.class);
                result = invoke(method, packageManager, (int) flags, userId);
            }

            List<PackageInfo> unwrapped = unwrapResult(result);
            if (unwrapped != null) return unwrapped;
            return Collections.emptyList();
        } catch (NoSuchMethodException e) {
            Log.d(TAG, "Hidden IPackageManager.getInstalledPackages not found, falling back to public API", e);
        } catch (Exception e) {
            Log.d(TAG, "Hidden IPackageManager.getInstalledPackages failed, falling back to public API", e);
        }

        // Path 3: public PackageManager.getInstalledPackages(int) as last resort
        try {
            Object contextPm = getContextPackageManager();
            Method method = contextPm.getClass().getMethod("getInstalledPackages", int.class);
            Object result = invoke(method, contextPm, (int) flags);
            List<PackageInfo> unwrapped = unwrapResult(result);
            if (unwrapped != null) return unwrapped;
            return Collections.emptyList();
        } catch (Exception e) {
            Log.e(TAG, "Public getInstalledPackages(int) also failed", e);
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    static List<PackageInfo> unwrapResult(Object result) {
        if (result == null) {
            return Collections.emptyList();
        }

        if (result instanceof List) {
            return (List<PackageInfo>) result;
        }

        String resultClassName = result.getClass().getName();
        if (resultClassName.startsWith(PARCELED_LIST_SLICE) || resultClassName.contains("PackageInfoList")) {
            try {
                Object list = result.getClass().getMethod("getList").invoke(result);
                return list == null ? Collections.emptyList() : (List<PackageInfo>) list;
            } catch (Exception e) {
                Log.w(TAG, "Failed to unwrap " + resultClassName, e);
                return null;
            }
        }

        Log.w(TAG, "Unsupported getInstalledPackages return type: " + resultClassName);
        return null;
    }

    private static Object getPackageManager() throws ReflectiveOperationException {
        Class<?> servicesClass = Class.forName("rikka.hidden.compat.Services");
        var field = servicesClass.getDeclaredField("packageManager");
        field.setAccessible(true);
        Object service = field.get(null);
        return service.getClass().getMethod("get").invoke(service);
    }

    private static Object getContextPackageManager() throws ReflectiveOperationException {
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
        if (activityThread != null) {
            Object application = activityThreadClass.getMethod("getApplication").invoke(activityThread);
            if (application != null) {
                return application.getClass().getMethod("getPackageManager").invoke(application);
            }
        }

        activityThread = activityThreadClass.getMethod("systemMain").invoke(null);
        Object systemContext = activityThreadClass.getMethod("getSystemContext").invoke(activityThread);
        return systemContext.getClass().getMethod("getPackageManager").invoke(systemContext);
    }

    private static Object invoke(Method method, Object receiver, Object... args) throws ReflectiveOperationException {
        try {
            return method.invoke(receiver, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ReflectiveOperationException) {
                throw (ReflectiveOperationException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw e;
        }
    }
}
