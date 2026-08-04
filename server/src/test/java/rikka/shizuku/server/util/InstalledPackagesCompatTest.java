package rikka.shizuku.server.util;

import android.content.pm.PackageInfo;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class InstalledPackagesCompatTest {

    @Test
    public void unwrapResult_null_returnsEmptyList() {
        List<PackageInfo> result = InstalledPackagesCompat.unwrapResult(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void unwrapResult_plainList_returnsSameList() {
        List<PackageInfo> input = new ArrayList<>();
        input.add(new PackageInfo());
        input.add(new PackageInfo());

        List<PackageInfo> result = InstalledPackagesCompat.unwrapResult(input);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result == input); // same reference
    }

    @Test
    public void unwrapResult_emptyList_returnsEmptyList() {
        List<PackageInfo> input = Collections.emptyList();

        List<PackageInfo> result = InstalledPackagesCompat.unwrapResult(input);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void unwrapResult_parceledListSlice_unwraps() {
        List<PackageInfo> inner = new ArrayList<>();
        inner.add(new PackageInfo());
        FakeParceledListSlice fake = new FakeParceledListSlice(inner);

        List<PackageInfo> result = InstalledPackagesCompat.unwrapResult(fake);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result == inner);
    }

    @Test
    public void unwrapResult_packageInfoList_unwraps() {
        List<PackageInfo> inner = new ArrayList<>();
        inner.add(new PackageInfo());
        FakePackageInfoList fake = new FakePackageInfoList(inner);

        List<PackageInfo> result = InstalledPackagesCompat.unwrapResult(fake);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result == inner);
    }

    @Test
    public void unwrapResult_unknownType_returnsNull() {
        Object unknown = new Object(); // no getList() method

        List<PackageInfo> result = InstalledPackagesCompat.unwrapResult(unknown);
        assertNull(result);
    }

    @Test
    public void unwrapResult_parceledListSlice_nullInner_returnsEmpty() {
        FakeParceledListSlice fake = new FakeParceledListSlice(null);

        List<PackageInfo> result = InstalledPackagesCompat.unwrapResult(fake);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void unwrapResult_packageInfoList_nullInner_returnsEmpty() {
        FakePackageInfoList fake = new FakePackageInfoList(null);

        List<PackageInfo> result = InstalledPackagesCompat.unwrapResult(fake);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void classNameCheck_parceledListSlice_matches() {
        String className = "android.content.pm.ParceledListSlice";
        assertTrue(className.startsWith(InstalledPackagesCompat.PARCELED_LIST_SLICE));
    }

    @Test
    public void classNameCheck_packageInfoList_matches() {
        String className = "android.content.pm.PackageInfoList";
        assertTrue(className.contains("PackageInfoList"));
    }

    @Test
    public void classNameCheck_arrayList_doesNotMatch() {
        String className = "java.util.ArrayList";
        boolean matches = className.startsWith(InstalledPackagesCompat.PARCELED_LIST_SLICE)
                || className.contains("PackageInfoList");
        assertTrue(!matches);
    }

    @Test
    public void unwrapResult_objectWithGetList_returnsList() {
        List<PackageInfo> inner = new ArrayList<>();
        inner.add(new PackageInfo());
        ObjectWithGetList fake = new ObjectWithGetList(inner);

        List<PackageInfo> result = InstalledPackagesCompat.unwrapResult(fake);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result == inner);
    }

    @Test
    public void unwrapResult_objectWithGetList_nullInner_returnsEmpty() {
        ObjectWithGetList fake = new ObjectWithGetList(null);

        List<PackageInfo> result = InstalledPackagesCompat.unwrapResult(fake);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Android 17 returns PackageInfoList, which extends ParceledListSlice. Unwrapping must key off
     * getList() being present rather than an exact class, or the subclass reads as unknown.
     */
    @Test
    public void unwrapResult_subclassOfParceledListSlice_unwraps() {
        List<PackageInfo> inner = new ArrayList<>();
        inner.add(new PackageInfo());
        FakePackageInfoListSubclass fake = new FakePackageInfoListSubclass(inner);

        List<PackageInfo> result = InstalledPackagesCompat.unwrapResult(fake);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result == inner);
    }

    /** An unknown shape must return null so the caller falls through to the next path. */
    @Test
    public void unwrapResult_unknownType_returnsNullSoCallerFallsThrough() {
        assertNull(InstalledPackagesCompat.unwrapResult(new Object()));
        assertNull(InstalledPackagesCompat.unwrapResult("not a package list"));
    }

    // Fake classes that mimic Android's ParceledListSlice and PackageInfoList
    // These have a getList() method like the real classes

    public static class FakeParceledListSlice {
        private final List<PackageInfo> list;
        public FakeParceledListSlice(List<PackageInfo> list) { this.list = list; }
        public List<PackageInfo> getList() { return list; }
    }

    public static class FakePackageInfoList {
        private final List<PackageInfo> list;
        public FakePackageInfoList(List<PackageInfo> list) { this.list = list; }
        public List<PackageInfo> getList() { return list; }
    }

    public static class ObjectWithGetList {
        private final List<PackageInfo> list;
        public ObjectWithGetList(List<PackageInfo> list) { this.list = list; }
        public List<PackageInfo> getList() { return list; }
    }

    /** Mirrors Android 17's PackageInfoList, which extends ParceledListSlice. */
    public static class FakePackageInfoListSubclass extends FakeParceledListSlice {
        public FakePackageInfoListSubclass(List<PackageInfo> list) { super(list); }
    }
}
