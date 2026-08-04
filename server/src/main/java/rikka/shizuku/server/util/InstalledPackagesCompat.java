package rikka.shizuku.server.util;

import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.ServiceManager;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * Reads the installed-package list across platform versions.
 *
 * <p>Android 17 changed {@code IPackageManager.getInstalledPackages} to return the new
 * {@code PackageInfoList} instead of {@code ParceledListSlice}. The return type is part of the
 * JVM method descriptor, so code linked against the old stub fails with {@link NoSuchMethodError}
 * (or {@link ClassCastException}) rather than a checked exception. Everything here is resolved by
 * name and parameter types only, and unwrapped by capability, so both shapes work.
 */
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

    public static List<PackageInfo> getInstalledPackages(long flags, int userId) throws ReflectiveOperationException {
        // Path 1: privileged IPackageManager. This is the only path that can enumerate packages
        // for an arbitrary user, so it must be tried first -- the context PackageManager below
        // silently ignores userId and would return the wrong user's list.
        try {
            Object pm = getPackageManager();
            Method method = findGetInstalledPackages(pm);
            if (method != null) {
                Object result = method.getParameterTypes()[0] == long.class
                        ? invoke(method, pm, flags, userId)
                        : invoke(method, pm, (int) flags, userId);
                List<PackageInfo> unwrapped = unwrapResult(result);
                if (unwrapped != null) return unwrapped;
            }
        } catch (Throwable e) {
            // NoSuchMethodError / ClassCastException land here on signature drift.
            Log.d(TAG, "Hidden IPackageManager.getInstalledPackages failed, trying context PackageManager", e);
        }

        // Path 2: context PackageManager.getInstalledPackagesAsUser.
        try {
            Object pm = getContextPackageManager();
            Method method = findMethod(pm, "getInstalledPackagesAsUser");
            if (method != null) {
                Object result = method.getParameterTypes()[0] == long.class
                        ? invoke(method, pm, flags, userId)
                        : invoke(method, pm, (int) flags, userId);
                List<PackageInfo> unwrapped = unwrapResult(result);
                if (unwrapped != null) return unwrapped;
            }
        } catch (Throwable e) {
            Log.d(TAG, "getInstalledPackagesAsUser failed, trying public API", e);
        }

        // Path 3: public PackageManager.getInstalledPackages as a last resort. Current user only.
        try {
            Object pm = getContextPackageManager();
            Method method = findMethod(pm, "getInstalledPackages");
            if (method != null) {
                Object result = method.getParameterTypes()[0] == long.class
                        ? invoke(method, pm, flags)
                        : invoke(method, pm, (int) flags);
                List<PackageInfo> unwrapped = unwrapResult(result);
                if (unwrapped != null) return unwrapped;
            }
        } catch (Throwable e) {
            Log.e(TAG, "Public getInstalledPackages also failed", e);
        }

        return Collections.emptyList();
    }

    /**
     * Finds {@code getInstalledPackages(flags, userId)}, preferring the long-flags overload on
     * Android 13+ where the extra flag bits live above the int range.
     */
    private static Method findGetInstalledPackages(Object pm) {
        Method intFlags = null;
        for (Method method : pm.getClass().getMethods()) {
            if (!"getInstalledPackages".equals(method.getName())) continue;
            Class<?>[] types = method.getParameterTypes();
            if (types.length != 2 || types[1] != int.class) continue;
            if (types[0] == long.class && Build.VERSION.SDK_INT >= ANDROID_13) return method;
            if (types[0] == int.class) intFlags = method;
        }
        return intFlags;
    }

    private static Method findMethod(Object receiver, String name) {
        Method intFlags = null;
        for (Method method : receiver.getClass().getMethods()) {
            if (!name.equals(method.getName())) continue;
            Class<?>[] types = method.getParameterTypes();
            if (types.length == 0) continue;
            if (types[0] == long.class && Build.VERSION.SDK_INT >= ANDROID_13) return method;
            if (types[0] == int.class) intFlags = method;
        }
        return intFlags;
    }

    /**
     * @return the contained list, or null if {@code result} is a shape we don't understand
     * (the caller then falls through to the next path).
     */
    @SuppressWarnings("unchecked")
    static List<PackageInfo> unwrapResult(Object result) {
        if (result == null) {
            return Collections.emptyList();
        }

        if (result instanceof List) {
            return (List<PackageInfo>) result;
        }

        // ParceledListSlice, and its Android 17 subclass PackageInfoList, both expose getList().
        try {
            Object list = result.getClass().getMethod("getList").invoke(result);
            return list == null ? Collections.emptyList() : (List<PackageInfo>) list;
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            Log.w(TAG, "Failed to unwrap " + result.getClass().getName(), e);
            return null;
        }

        Log.w(TAG, "Unsupported getInstalledPackages return type: " + result.getClass().getName());
        return null;
    }

    /**
     * Resolves IPackageManager.
     *
     * <p>Prefers rikka.hidden.compat.Services, whose binder is wrapped by ShizukuBinderWrapper in
     * the manager process -- going straight to ServiceManager there would yield an unprivileged
     * binder that cannot enumerate other users. Falls back to ServiceManager for the server
     * process and for release builds where R8 may have renamed Services away.
     */
    private static Object getPackageManager() throws ReflectiveOperationException {
        try {
            Class<?> servicesClass = Class.forName("rikka.hidden.compat.Services");
            java.lang.reflect.Field field = servicesClass.getDeclaredField("packageManager");
            field.setAccessible(true);
            Object binder = field.get(null);
            if (binder != null) {
                Object pm = binder.getClass().getMethod("get").invoke(binder);
                if (pm != null) return pm;
            }
        } catch (Throwable e) {
            Log.d(TAG, "rikka.hidden.compat.Services unavailable, using ServiceManager", e);
        }

        IBinder binder = ServiceManager.getService("package");
        if (binder == null) {
            throw new IllegalStateException("package service is not available");
        }
        Class<?> stub = Class.forName("android.content.pm.IPackageManager$Stub");
        return stub.getMethod("asInterface", IBinder.class).invoke(null, binder);
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
            method.setAccessible(true);
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
