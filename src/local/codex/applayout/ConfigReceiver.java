package local.codex.applayout;

import android.content.*;
import android.os.*;
import java.util.Arrays;

/** Explicit, identity-sharing broadcasts avoid package-visibility and world-readable-pref workarounds. */
public final class ConfigReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String pkg = intent.getStringExtra("package");
        if (!isOrderedBroadcast() || !Settings.CONFIG_ACTION.equals(intent.getAction()) || pkg == null) return;
        String[] packages = context.getPackageManager().getPackagesForUid(getSentFromUid());
        if (packages == null || !Arrays.asList(packages).contains(pkg)) return;
        Bundle result = new Bundle();
        result.putBoolean("enabled", Settings.enabled(context, pkg));
        result.putInt("threshold", Settings.THRESHOLD);
        result.putInt("moduleUid", context.getApplicationInfo().uid);
        setResultExtras(result);
        setResultCode(1);
        String status = intent.getStringExtra("status");
        if (status != null) {
            if (status.length() > 240) status = status.substring(0, 240);
            Settings.prefs(context).edit().putString("status:" + pkg, status).apply();
        }
    }
}
