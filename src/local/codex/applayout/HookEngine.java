package local.codex.applayout;

import android.content.Context;
import android.content.res.Resources;
import java.lang.reflect.*;
import java.util.*;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

final class HookEngine {
    static final class WidthClass {
        final Class<?> type;
        final Constructor<?> constructor;
        final Field width, height;
        WidthClass(Class<?> type) throws Exception {
            this.type = type;
            constructor = type.getDeclaredConstructor(int.class, int.class); constructor.setAccessible(true);
            Object probe = constructor.newInstance(600, 480); Field w = null, h = null;
            for (Field f : type.getDeclaredFields()) if (!Modifier.isStatic(f.getModifiers()) && f.getType() == int.class) {
                f.setAccessible(true);
                if (f.getInt(probe) == 600) w = f;
                if (f.getInt(probe) == 480) h = f;
            }
            if (w == null || h == null || !probe.toString().startsWith("WindowSizeClass(minWidthDp=")) throw new IllegalArgumentException("Not a standard width class");
            width = w; height = h;
        }
        Object expanded(Object old) throws Exception {
            int value = width.getInt(old);
            return value >= Settings.THRESHOLD && value < 840 ? constructor.newInstance(840, height.getInt(old)) : old;
        }
    }
    static int install(Context context, XC_LoadPackage.LoadPackageParam lp, Hook.State state) {
        int rules = resourceThreshold(context, state);
        try { rules += generic(context, lp, state); }
        catch (Throwable error) { XposedBridge.log("FoldAppLayout: generic scan skipped for " + lp.packageName + ": " + error); }
        if (lp.packageName.equals("dev.dimension.flare")) {
            try {
                if (context.getPackageManager().getPackageInfo(lp.packageName, 0).getLongVersionCode() == 5653) {
                    XposedBridge.hookAllConstructors(XposedHelpers.findClass("b3h", lp.classLoader), new XC_MethodHook() {
                        @Override protected void beforeHookedMethod(MethodHookParam p) {
                            if (state.enabled && p.args.length == 1 && p.args[0] instanceof Float && Math.abs((Float) p.args[0] - 320f) < 0.01f) p.args[0] = 280f;
                        }
                    });
                    rules++;
                }
            } catch (Throwable error) { XposedBridge.log("FoldAppLayout: Flare adapter skipped: " + error); }
        }
        if (lp.packageName.equals("app.mihon")) {
            try {
                if (context.getPackageManager().getPackageInfo(lp.packageName, 0).getLongVersionCode() == 38) {
                    Class<?> composer = resolve("Landroidx/compose/runtime/Composer;", lp.classLoader);
                    Class<?> gapComposer = resolve("Landroidx/compose/runtime/GapComposer;", lp.classLoader);
                    Class<?> local = resolve("Landroidx/compose/runtime/ProvidableCompositionLocal;", lp.classLoader);
                    Method consume = gapComposer.getDeclaredMethod("consume", local);
                    Object configurationLocal = XposedHelpers.getStaticObjectField(
                            resolve("Landroidx/compose/ui/platform/AndroidCompositionLocals_androidKt;", lp.classLoader), "LocalConfiguration");
                    XposedHelpers.findAndHookMethod("eu.kanade.presentation.util.WindowSizeKt", lp.classLoader,
                            "isTabletUi", composer, new XC_MethodHook() {
                                @Override protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                                    if (!state.enabled || p.hasThrowable()) return;
                                    // Read the real Compose window configuration without altering its dimensions.
                                    android.content.res.Configuration configuration = (android.content.res.Configuration)
                                            consume.invoke(p.args[0], configurationLocal);
                                    p.setResult(configuration.smallestScreenWidthDp >= Settings.THRESHOLD
                                            && configuration.screenWidthDp >= Settings.THRESHOLD);
                                }
                            });
                    rules++;
                }
            } catch (Throwable error) { XposedBridge.log("FoldAppLayout: Mihon adapter skipped: " + error); }
        }
        if (lp.packageName.equals("tv.danmaku.bili")) {
            try {
                if (context.getPackageManager().getPackageInfo(lp.packageName, 0).getLongVersionCode() == 9161100L) {
                    Class<?> windowClass = resolve("Landroidx/window/core/layout/WindowSizeClass;", lp.classLoader);
                    WidthClass window = new WidthClass(windowClass);
                    XposedHelpers.findAndHookMethod("kntr.common.screen.adjust.KScreenAdjustUtilsKt", lp.classLoader,
                            "isLargeLandscape", windowClass, new XC_MethodHook() {
                                @Override protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                                    if (!state.enabled || p.hasThrowable() || p.args[0] == null) return;
                                    android.content.res.Configuration c = context.getResources().getConfiguration();
                                    // The native side-by-side player also requires Bilibili's device-size gate.
                                    // Relax that layout predicate only inside a real, sufficiently wide window.
                                    if (c.smallestScreenWidthDp >= Settings.THRESHOLD && c.screenWidthDp >= Settings.THRESHOLD
                                            && window.width.getInt(p.args[0]) >= Settings.THRESHOLD
                                            && window.height.getInt(p.args[0]) >= Settings.THRESHOLD) p.setResult(true);
                                }
                            });
                    rules++;
                }
            } catch (Throwable error) { XposedBridge.log("FoldAppLayout: Bilibili adapter skipped: " + error); }
        }
        if (lp.packageName.equals("com.google.android.youtube")) {
            try {
                if (context.getPackageManager().getPackageInfo(lp.packageName, 0).getLongVersionCode() == 1561304941L) {
                    Class<?> split = resolve("Lrag;", lp.classLoader);
                    // This build has a separate watch-scene classifier; its native two-column scene is 2.
                    XposedHelpers.findAndHookMethod("rbo", lp.classLoader, "a", Object.class, Object.class, new XC_MethodHook() {
                        @Override protected void afterHookedMethod(MethodHookParam p) {
                            if (!state.enabled || p.hasThrowable() || XposedHelpers.getIntField(p.thisObject, "$cid") != 4
                                    || !(p.args[0] instanceof Context) || !Boolean.FALSE.equals(p.args[1])) return;
                            android.content.res.Configuration c = ((Context) p.args[0]).getResources().getConfiguration();
                            if (c.smallestScreenWidthDp >= Settings.THRESHOLD && c.screenWidthDp >= Settings.THRESHOLD)
                                p.setResult(Integer.valueOf(2));
                        }
                    });
                    // Relax only the native split layout predicate; keep its custom-height exclusion.
                    XposedHelpers.findAndHookMethod(split, "c", new XC_MethodHook() {
                        @Override protected void afterHookedMethod(MethodHookParam p) {
                            if (!state.enabled || p.hasThrowable()) return;
                            Context window = (Context) XposedHelpers.getObjectField(p.thisObject, "k");
                            android.content.res.Configuration c = window.getResources().getConfiguration();
                            if (c.smallestScreenWidthDp >= Settings.THRESHOLD && c.screenWidthDp >= Settings.THRESHOLD
                                    && XposedHelpers.getIntField(p.thisObject, "i") <= 0) p.setResult(true);
                        }
                    });
                    rules += 2;
                }
            } catch (Throwable error) { XposedBridge.log("FoldAppLayout: YouTube adapter skipped: " + error); }
        }
        XposedBridge.log("FoldAppLayout: " + lp.packageName + " matched " + rules + " rules; DPI unchanged");
        return rules;
    }
    static int resourceThreshold(Context context, Hook.State state) {
        Set<Integer> ids = new HashSet<>();
        int id = context.getResources().getIdentifier("min_window_width_for_expanded_window_size_class", "dimen", context.getPackageName());
        if (id != 0) ids.add(id);
        if (ids.isEmpty()) return 0;
        for (String name : new String[]{"getDimension", "getDimensionPixelSize", "getDimensionPixelOffset"}) {
            XposedHelpers.findAndHookMethod(Resources.class, name, int.class, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    if (!state.enabled || p.hasThrowable() || !ids.contains((Integer) p.args[0])) return;
                    Resources resources = (Resources) p.thisObject;
                    try {
                        if (!resources.getResourceEntryName((Integer) p.args[0]).equals("min_window_width_for_expanded_window_size_class")) return;
                        float px = Settings.THRESHOLD * resources.getDisplayMetrics().density;
                        if (p.method.getName().equals("getDimension")) p.setResult(Float.valueOf(px));
                        else p.setResult(Integer.valueOf(Math.round(px)));
                    } catch (Resources.NotFoundException ignored) {}
                }
            });
        }
        return 1;
    }
    static int generic(Context context, XC_LoadPackage.LoadPackageParam lp, Hook.State state) throws Exception {
        List<String> paths = new ArrayList<>(); paths.add(lp.appInfo.sourceDir);
        if (lp.appInfo.splitSourceDirs != null) Collections.addAll(paths, lp.appInfo.splitSourceDirs);
        DexLayoutScanner.Plan plan = DexLayoutScanner.scan(paths.toArray(new String[0]));
        Map<String, WidthClass> classes = new HashMap<>();
        for (String descriptor : plan.classes) try {
            classes.put(descriptor, new WidthClass(resolve(descriptor, lp.classLoader)));
        } catch (ReflectiveOperationException | IllegalArgumentException ignored) {}
        Set<String> hooked = new HashSet<>(); int count = 0;
        for (DexLayoutScanner.Site site : plan.sites) {
            String key = site.owner + site.name + site.args;
            if (!hooked.add(key)) continue;
            try {
                Class<?> owner = resolve(site.owner, lp.classLoader);
                Class<?>[] args = new Class<?>[site.args.size()];
                for (int i = 0; i < args.length; i++) args[i] = resolve(site.args.get(i), lp.classLoader);
                if (site.name.equals("<init>")) {
                    final WidthClass[] adapters = new WidthClass[args.length]; boolean any = false;
                    for (int i = 0; i < args.length; i++) { adapters[i] = classes.get(site.args.get(i)); any |= adapters[i] != null; }
                    if (!any) continue;
                    Constructor<?> ctor = owner.getDeclaredConstructor(args); ctor.setAccessible(true);
                    XposedBridge.hookMethod(ctor, new XC_MethodHook() {
                        @Override protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
                            if (!state.enabled) return;
                            for (int i = 0; i < adapters.length; i++) if (adapters[i] != null && p.args[i] != null) p.args[i] = adapters[i].expanded(p.args[i]);
                        }
                    }); count++;
                } else {
                    WidthClass adapter = classes.get(site.result);
                    if (adapter == null || !isLayoutFactory(args)) continue;
                    Method method = owner.getDeclaredMethod(site.name, args); method.setAccessible(true);
                    if (Modifier.isAbstract(method.getModifiers())) continue;
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                            if (state.enabled && !p.hasThrowable() && p.getResult() != null) p.setResult(adapter.expanded(p.getResult()));
                        }
                    }); count++;
                }
            } catch (ReflectiveOperationException | IllegalArgumentException ignored) {}
        }
        return count;
    }
    static boolean isLayoutFactory(Class<?>[] args) {
        int numeric = 0;
        for (Class<?> arg : args) {
            if (arg == int.class || arg == float.class) numeric++;
            if (Context.class.isAssignableFrom(arg) || arg.getName().equals("androidx.compose.runtime.Composer")) return true;
        }
        return numeric >= 2;
    }
    static Class<?> resolve(String d, ClassLoader loader) throws ClassNotFoundException {
        switch (d) {
            case "I": return int.class; case "F": return float.class; case "Z": return boolean.class;
            case "B": return byte.class; case "S": return short.class; case "C": return char.class;
            case "J": return long.class; case "D": return double.class; case "V": return void.class;
            default: return Class.forName(d.startsWith("L") ? d.substring(1, d.length() - 1).replace('/', '.') : d.replace('/', '.'), false, loader);
        }
    }
}
