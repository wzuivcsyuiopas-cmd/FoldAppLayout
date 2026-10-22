package local.codex.applayout;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Finds AndroidX WindowSizeClass by its semantic toString marker, including R8-renamed classes. */
public final class DexLayoutScanner {
    public static final class Site {
        public String owner, name;
        public final List<String> args = new ArrayList<>();
        public String result;
    }
    public static final class Plan {
        public final Set<String> classes = new LinkedHashSet<>();
        public final List<Site> sites = new ArrayList<>();
    }
    public static Plan scan(String[] apks) throws IOException {
        Plan plan = new Plan();
        forEachDex(apks, d -> plan.classes.addAll(d.windowClasses()));
        if (!plan.classes.isEmpty()) forEachDex(apks, d -> {
            for (int i = 0; i < d.methodCount; i++) {
                Site site = d.method(i);
                boolean factory = plan.classes.contains(site.result) && !site.name.equals("<init>");
                boolean wrapper = site.name.equals("<init>") && !plan.classes.contains(site.owner)
                        && site.args.stream().anyMatch(plan.classes::contains);
                if (factory || wrapper) plan.sites.add(site);
            }
        });
        return plan;
    }
    interface Consumer { void accept(Dex d) throws IOException; }
    static void forEachDex(String[] apks, Consumer consumer) throws IOException {
        for (String apk : apks) try (ZipFile zip = new ZipFile(apk)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                try (InputStream in = zip.getInputStream(entry)) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    byte[] buffer = new byte[16384]; int n;
                    while ((n = in.read(buffer)) != -1) bytes.write(buffer, 0, n);
                    consumer.accept(new Dex(bytes.toByteArray()));
                }
            }
        }
    }
    static final class Dex {
        final byte[] b;
        final String[] strings, types;
        final int methodCount, methodOff, protoOff, classCount, classOff;
        Dex(byte[] bytes) throws IOException {
            b = bytes;
            if (b.length < 112 || b[0] != 'd' || b[1] != 'e' || b[2] != 'x') throw new IOException("Unsupported DEX");
            strings = new String[u32(56)]; int stringOff = u32(60);
            for (int i = 0; i < strings.length; i++) {
                int[] pos = {u32(stringOff + i * 4)}; leb(pos); int start = pos[0], end = start;
                while (end < b.length && b[end] != 0) end++;
                strings[i] = new String(b, start, end - start, StandardCharsets.UTF_8);
            }
            types = new String[u32(64)]; int typeOff = u32(68);
            for (int i = 0; i < types.length; i++) types[i] = strings[u32(typeOff + i * 4)];
            protoOff = u32(76); methodCount = u32(88); methodOff = u32(92);
            classCount = u32(96); classOff = u32(100);
        }
        int u16(int p) { return (b[p] & 255) | ((b[p + 1] & 255) << 8); }
        int u32(int p) { return u16(p) | (u16(p + 2) << 16); }
        int leb(int[] p) {
            int result = 0;
            for (int shift = 0; shift < 35; shift += 7) {
                int x = b[p[0]++] & 255; result |= (x & 127) << shift;
                if ((x & 128) == 0) return result;
            }
            throw new IllegalArgumentException("Invalid ULEB128");
        }
        Site method(int id) {
            int at = methodOff + id * 8, proto = protoOff + u16(at + 2) * 12;
            Site site = new Site(); site.owner = types[u16(at)]; site.name = strings[u32(at + 4)];
            site.result = types[u32(proto + 4)]; int args = u32(proto + 8);
            if (args != 0) for (int i = 0, n = u32(args); i < n; i++) site.args.add(types[u16(args + 4 + i * 2)]);
            return site;
        }
        Set<String> windowClasses() {
            Set<String> result = new LinkedHashSet<>(); Set<Integer> markers = new HashSet<>();
            for (int i = 0; i < strings.length; i++) if (strings[i].startsWith("WindowSizeClass(minWidthDp=")) markers.add(i);
            if (markers.isEmpty()) return result;
            for (int c = 0; c < classCount; c++) {
                int data = u32(classOff + c * 32 + 24); if (data == 0) continue;
                int[] p = {data}; int sf = leb(p), inf = leb(p), dm = leb(p), vm = leb(p);
                for (int f = 0; f < sf + inf; f++) { leb(p); leb(p); }
                for (int group : new int[]{dm, vm}) {
                    int method = 0;
                    for (int m = 0; m < group; m++) {
                        method += leb(p); leb(p); int code = leb(p);
                        if (code == 0 || !this.method(method).name.equals("toString")) continue;
                        int count = u32(code + 12), insns = code + 16;
                        for (int j = 0; j < count; j++) {
                            int op = u16(insns + j * 2) & 255, id = -1;
                            if (op == 0x1a && j + 1 < count) id = u16(insns + (j + 1) * 2);
                            if (op == 0x1b && j + 2 < count) id = u32(insns + (j + 1) * 2);
                            if (markers.contains(id)) { result.add(types[u32(classOff + c * 32)]); break; }
                        }
                    }
                }
            }
            return result;
        }
    }
}
