package local.codex.applayout;

import android.app.BroadcastOptions;
import android.content.*;
import android.os.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

final class ConfigClient {
    static final HandlerThread thread = new HandlerThread("FoldLayoutConfig");
    static { thread.start(); }
    static Bundle options() {
        return BroadcastOptions.makeBasic().setShareIdentityEnabled(true).toBundle();
    }
    static Intent request(Context context) {
        return new Intent(Settings.CONFIG_ACTION)
                .setComponent(new ComponentName(Settings.PACKAGE, Settings.PACKAGE + ".ConfigReceiver"))
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND).putExtra("package", context.getPackageName());
    }
    static Bundle read(Context context) throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(1);
        AtomicReference<Bundle> response = new AtomicReference<>();
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent intent) {
                if (getResultCode() == 1) response.set(getResultExtras(false));
                ready.countDown();
            }
        };
        context.sendOrderedBroadcast(request(context), null, options(), receiver,
                new Handler(thread.getLooper()), 0, null, null);
        ready.await(3, TimeUnit.SECONDS);
        return response.get();
    }
    static void report(Context context, String status) {
        context.sendOrderedBroadcast(request(context).putExtra("status", status), null, options(), null,
                null, 0, null, null);
    }
}
