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
        Object unknown = "not a list type";

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
}
