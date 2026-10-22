package local.codex.applayout;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public final class MainActivity extends Activity {
    final List<App> apps = new ArrayList<>(), visible = new ArrayList<>();
    String query = ""; boolean selectedOnly;
    AppAdapter adapter; TextView count;
    static final int INK = 0xff22232b, MUTED = 0xff666875, ACCENT = 0xff6750a4;
    static final class App {
        String label, pkg; ApplicationInfo info;
        App(String label, ApplicationInfo info) { this.label = label; this.info = info; pkg = info.packageName; }
    }
    int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    TextView text(String value, int size, int color) {
        TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color); return v;
    }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved); getWindow().setStatusBarColor(0xfff7f7fb); getWindow().setNavigationBarColor(0xfff7f7fb);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        LinearLayout root = new LinearLayout(this); root.setOrientation(1); root.setBackgroundColor(0xfff7f7fb);
        root.setPadding(dp(20), dp(18), dp(20), 0); setContentView(root);
        root.addView(text("大屏布局", 29, INK));
        TextView subtitle = text("选择应用，提前触发大屏布局，保留字号与缩放", 14, MUTED);
        subtitle.setPadding(0, dp(8), 0, dp(16)); root.addView(subtitle);
        LinearLayout card = new LinearLayout(this); card.setOrientation(1); card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundColor(0xffeae4f5); root.addView(card);
        card.addView(text("840 → 600dp", 22, ACCENT));
        card.addView(text("宽度达到 600dp 时尝试触发大屏布局；外屏和小窗按实际宽度判断。", 14, INK));
        TextView note = text("首次启动会识别布局接口。自定义布局不一定支持，结果会显示在应用下方。", 12, MUTED);
        note.setPadding(0, dp(7), 0, 0); card.addView(note);
        LinearLayout actions = new LinearLayout(this); actions.setGravity(Gravity.CENTER_VERTICAL);
        count = text("正在读取应用…", 13, MUTED); actions.addView(count, new LinearLayout.LayoutParams(0, dp(50), 1));
        Button selected = new Button(this); selected.setText("只看已选"); selected.setTextSize(12);
        selected.setOnClickListener(v -> { selectedOnly = !selectedOnly; selected.setText(selectedOnly ? "显示全部" : "只看已选"); filter(); }); actions.addView(selected);
        Button help = new Button(this); help.setText("启用说明"); help.setTextSize(12); help.setOnClickListener(v -> help()); actions.addView(help); root.addView(actions);
        EditText search = new EditText(this); search.setSingleLine(true); search.setHint("搜索应用名称或包名"); search.setTextSize(15);
        search.setInputType(android.text.InputType.TYPE_CLASS_TEXT); root.addView(search);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int n, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int n) { query = s.toString().toLowerCase(Locale.ROOT); filter(); }
            public void afterTextChanged(Editable e) {}
        });
        ListView list = new ListView(this); list.setDividerHeight(0); adapter = new AppAdapter(); list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        new Thread(() -> {
            List<App> loaded = new ArrayList<>(); PackageManager pm = getPackageManager();
            for (ApplicationInfo info : pm.getInstalledApplications(0)) {
                if (info.packageName.equals(getPackageName()) || info.packageName.equals("android") || info.packageName.equals("com.android.systemui")) continue;
                boolean launcher = pm.getLaunchIntentForPackage(info.packageName) != null;
                if (launcher || Settings.enabled(this, info.packageName)) {
                    loaded.add(new App(info.loadLabel(pm).toString(), info));
                }
            }
            loaded.sort(Comparator.comparing(a -> a.label, String.CASE_INSENSITIVE_ORDER));
            runOnUiThread(() -> { apps.clear(); apps.addAll(loaded); filter(); });
        }, "AppList").start();
    }
    @Override protected void onResume() { super.onResume(); if (adapter != null) filter(); }
    void filter() {
        visible.clear(); int enabled = 0;
        for (App app : apps) {
            boolean checked = Settings.enabled(this, app.pkg); if (checked) enabled++;
            if ((!selectedOnly || checked) && (app.label.toLowerCase(Locale.ROOT).contains(query) || app.pkg.toLowerCase(Locale.ROOT).contains(query))) visible.add(app);
        }
        count.setText("已选 " + enabled + " 个应用"); adapter.notifyDataSetChanged();
    }
    void help() {
        new AlertDialog.Builder(this).setTitle("启用应用布局")
                .setMessage("1. 在 Vector / LSPosed 中启用本模块。\n2. 在框架作用域中授权你要调整的应用。\n3. 回到这里勾选应用，然后重新打开它。\n\n勾选后只改变布局规则，不改 DPI。已在作用域内运行的应用会收到设置更新；未运行的应用下次启动时读取。\n\n“未找到可用布局入口”表示应用使用了其他实现，不能保证通用规则有效。取消勾选即可恢复。")
                .setPositiveButton("知道了", null).show();
    }
    final class AppAdapter extends BaseAdapter {
        public int getCount() { return visible.size(); }
        public Object getItem(int i) { return visible.get(i); }
        public long getItemId(int i) { return visible.get(i).pkg.hashCode(); }
        public View getView(int position, View recycled, ViewGroup parent) {
            App app = visible.get(position);
            LinearLayout row = new LinearLayout(MainActivity.this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(4), dp(13), dp(2), dp(13));
            ImageView icon = new ImageView(MainActivity.this); icon.setImageDrawable(app.info.loadIcon(getPackageManager())); row.addView(icon, new LinearLayout.LayoutParams(dp(38), dp(38)));
            LinearLayout labels = new LinearLayout(MainActivity.this); labels.setOrientation(1); labels.setPadding(dp(12), 0, dp(8), 0);
            labels.addView(text(app.label, 16, INK));
            TextView pkg = text(app.pkg, 11, MUTED); pkg.setSingleLine(); pkg.setEllipsize(TextUtils.TruncateAt.END); labels.addView(pkg);
            String status = Settings.prefs(MainActivity.this).getString("status:" + app.pkg, "尚未运行 · 请同时授权框架作用域");
            TextView state = text(status, 12, Settings.enabled(MainActivity.this, app.pkg) ? ACCENT : MUTED); labels.addView(state);
            row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
            Switch toggle = new Switch(MainActivity.this); toggle.setContentDescription("启用 " + app.label + " 大屏布局"); toggle.setChecked(Settings.enabled(MainActivity.this, app.pkg));
            toggle.setOnCheckedChangeListener((v, checked) -> {
                boolean saved = Settings.prefs(MainActivity.this).edit().putBoolean("enabled:" + app.pkg, checked).remove("status:" + app.pkg).commit();
                if (!saved) { Toast.makeText(MainActivity.this, "保存失败，请重试", Toast.LENGTH_LONG).show(); return; }
                sendBroadcast(new Intent(Settings.RELOAD_ACTION).setPackage(app.pkg), null, ConfigClient.options());
                Toast.makeText(MainActivity.this, checked ? "已保存：请确认 Vector 作用域已授权，再打开应用" : "已关闭：重新打开应用恢复布局", Toast.LENGTH_SHORT).show();
                filter();
            });
            row.addView(toggle); row.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked())); return row;
        }
    }
}
