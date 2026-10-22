package probe;

/** Deliberately unrelated package/name: scanner must use semantics, not an app allowlist. */
public final class ObfuscatedWindow {
    public final int a, b;
    public ObfuscatedWindow(int width, int height) { a = width; b = height; }
    public static ObfuscatedWindow classify(int width, int height) {
        return new ObfuscatedWindow(width >= 840 ? 840 : width >= 600 ? 600 : 0, height >= 480 ? 480 : 0);
    }
    public static ObfuscatedWindow fromWindow(android.content.Context window) {
        return new ObfuscatedWindow(697, 785);
    }
    @Override public String toString() { return "WindowSizeClass(minWidthDp=" + a + ", minHeightDp=" + b + ")"; }
    public static final class Wrapper {
        final ObfuscatedWindow window;
        public Wrapper(ObfuscatedWindow window) { this.window = window; }
    }
}
