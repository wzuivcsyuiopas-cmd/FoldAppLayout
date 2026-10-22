package local.codex.applayout;

public final class ScannerTest {
    public static void main(String[] args) throws Exception {
        DexLayoutScanner.Plan plan = DexLayoutScanner.scan(new String[]{args[0]});
        require(plan.classes.size() == 1 && plan.classes.contains("Lprobe/ObfuscatedWindow;"), "semantic class discovery");
        require(plan.sites.stream().anyMatch(s -> s.name.equals("classify")), "width factory discovery");
        require(plan.sites.stream().anyMatch(s -> s.name.equals("fromWindow") && s.args.contains("Landroid/content/Context;")), "context factory discovery");
        require(HookEngine.isLayoutFactory(new Class<?>[]{android.content.Context.class}), "context factory support");
        require(HookEngine.isLayoutFactory(new Class<?>[]{float.class, float.class}), "numeric factory support");
        require(!HookEngine.isLayoutFactory(new Class<?>[]{String.class}), "unrelated factory exclusion");
        require(plan.sites.stream().anyMatch(s -> s.name.equals("<init>") && s.owner.endsWith("$Wrapper;")), "adaptive wrapper discovery");
        HookEngine.WidthClass adapter = new HookEngine.WidthClass(probe.ObfuscatedWindow.class);
        for (int width : new int[]{0, 393, 599, 600, 697, 839, 840, 1200}) {
            probe.ObfuscatedWindow input = new probe.ObfuscatedWindow(width, 480);
            probe.ObfuscatedWindow output = (probe.ObfuscatedWindow) adapter.expanded(input);
            int expected = width >= 600 && width < 840 ? 840 : width;
            require(output.a == expected && output.b == 480, "width boundary " + width);
            if (expected == width) require(output == input, "unchanged object " + width);
        }
        DexLayoutScanner.forEachDex(new String[]{args[1]}, dex -> {
            for (int i = 0; i < dex.classCount; i++) {
                String type = dex.types[dex.u32(dex.classOff + i * 32)];
                require(type.startsWith("Llocal/codex/applayout/"), "no bundled API/third-party implementation: " + type);
            }
        });
        System.out.println("PASS: semantic discovery, factory/wrapper discovery, 8 width boundaries, height preservation");
    }
    static void require(boolean value, String test) { if (!value) throw new AssertionError(test); }
}
