package local.codex.applayout;

import android.app.*;
import android.content.*;
import android.os.*;
import java.lang.ref.WeakReference;
import java.util.*;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class Hook implements IXposedHookLoadPackage {
    static final class State {
        volatile boolean enabled;
        boolean installed;
        int rules;
        int moduleUid = -1;
        WeakReference<Activity> activity = new WeakReference<>(null);
    }
    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (lp.packageName.equals(Settings.PACKAGE) || lp.packageName.equals("android") || lp.packageName.equals("com.android.systemui")) return;
        final State state = new State();
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                Application app = (Application) param.thisObject;
                Context context = (Context) param.args[0];
                app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                    public void onActivityResumed(Activity a) { state.activity = new WeakReference<>(a); }
                    public void onActivityCreated(Activity a, Bundle b) {}
                    public void onActivityStarted(Activity a) {}
                    public void onActivityPaused(Activity a) {}
                    public void onActivityStopped(Activity a) {}
                    public void onActivitySaveInstanceState(Activity a, Bundle b) {}
                    public void onActivityDestroyed(Activity a) { if (state.activity.get() == a) state.activity.clear(); }
                });
                Runnable reload = () -> {
                    boolean previous = state.enabled;
                    try {
                        Bundle config = ConfigClient.read(context);
                        if (config == null) throw new IllegalStateException("Configuration request timed out");
                        state.moduleUid = config.getInt("moduleUid", -1);
                        state.enabled = config.getBoolean("enabled");
                        if (state.enabled && !state.installed) {
                            state.installed = true;
                            state.rules = HookEngine.install(context, lp, state);
                        }
                        ConfigClient.report(context, !state.enabled ? "已关闭" : state.rules > 0 ? "已匹配 " + state.rules + " 项布局规则" : "未找到可用布局入口");
                    } catch (Throwable error) {
                        state.enabled = false;
                        XposedBridge.log("FoldAppLayout: config unavailable for " + lp.packageName + ": " + error);
                    }
                    if (previous != state.enabled) {
                        Activity activity = state.activity.get();
                        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) activity.recreate();
                    }
                };
                reload.run();
                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override public void onReceive(Context c, Intent i) {
                        if (state.moduleUid >= 0 && getSentFromUid() == state.moduleUid) reload.run();
                    }
                };
                IntentFilter filter = new IntentFilter(Settings.RELOAD_ACTION);
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
            }
        });
    }
}
