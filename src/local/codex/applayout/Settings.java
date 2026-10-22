package local.codex.applayout;

import android.content.Context;
import android.content.SharedPreferences;

final class Settings {
    static final String PACKAGE = "local.codex.applayout";
    static final String CONFIG_ACTION = PACKAGE + ".CONFIG";
    static final String RELOAD_ACTION = PACKAGE + ".RELOAD";
    static final int THRESHOLD = 600;
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("layout", Context.MODE_PRIVATE); }
    static boolean enabled(Context c, String pkg) {
        return prefs(c).getBoolean("enabled:" + pkg, false);
    }
}
