package com.fgmachines.rck;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

/**
 * Best-effort restoration of the local controller after a phone reboot or app update.
 * The service is restarted only if the user previously ran the controller.
 */
public final class ControllerBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return;
        }
        boolean wanted = context.getSharedPreferences(
                MttlControllerService.PREFS, Context.MODE_PRIVATE)
                .getBoolean(MttlControllerService.PREF_CONTROLLER_WANTED, false);
        if (!wanted) return;

        Intent service = new Intent(context, MttlControllerService.class);
        service.setAction(MttlControllerService.ACTION_START);
        try {
            ContextCompat.startForegroundService(context, service);
        } catch (RuntimeException ignored) {
            // Some Android/vendor builds can defer background foreground-service starts.
            // Opening FG Machines RCK starts the controller again immediately.
        }
    }
}
