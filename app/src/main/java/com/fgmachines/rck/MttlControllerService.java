package com.fgmachines.rck;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the local TCP controller available while the app is in the background.
 * This is required for reliable strip connectivity, local alerts and later
 * automation features.
 */
public final class MttlControllerService extends Service implements MttlControllerServer.Listener {
    public static final String ACTION_START = "com.fgmachines.rck.START_CONTROLLER";
    public static final String ACTION_STOP = "com.fgmachines.rck.STOP_CONTROLLER";
    private static final String CHANNEL_CONTROLLER = "fg_rck_controller";
    private static final String CHANNEL_ALERTS = "fg_rck_alerts";
    private static final int CONTROLLER_NOTIFICATION_ID = 1001;
    private static final int ALERT_BASE_ID = 2000;
    private static final String PREFS = "fg_rck_settings";
    private static final String PREF_ALERTS_ENABLED = "alerts_enabled";

    private ControllerHub hub;
    private LocalAutomationEngine automationEngine;
    private EnergyHistoryStore energyHistory;
    private final Map<String, String> lastAlertKeyByMac = new HashMap<>();
    private final Set<String> connectedMacs = ConcurrentHashMap.newKeySet();

    @Override public void onCreate() {
        super.onCreate();
        createChannels();
        hub = ControllerHub.get(this);
        automationEngine = new LocalAutomationEngine(this, hub);
        automationEngine.start();
        energyHistory = new EnergyHistoryStore(this);
        hub.addListener(this, true);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(CONTROLLER_NOTIFICATION_ID, buildControllerNotification());
        try {
            hub.start();
        } catch (IOException error) {
            postAlert(getString(R.string.controller_service_error), safeMessage(error), ALERT_BASE_ID + 99);
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        if (hub != null) hub.removeListener(this);
        if (automationEngine != null) automationEngine.close();
        super.onDestroy();
    }

    @Nullable @Override public IBinder onBind(Intent intent) {
        return null;
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = getSystemService(NotificationManager.class);
        NotificationChannel controller = new NotificationChannel(
                CHANNEL_CONTROLLER,
                getString(R.string.controller_notification_channel),
                NotificationManager.IMPORTANCE_LOW);
        controller.setDescription(getString(R.string.controller_notification_channel_desc));
        manager.createNotificationChannel(controller);

        NotificationChannel alerts = new NotificationChannel(
                CHANNEL_ALERTS,
                getString(R.string.alert_notification_channel),
                NotificationManager.IMPORTANCE_HIGH);
        alerts.setDescription(getString(R.string.alert_notification_channel_desc));
        manager.createNotificationChannel(alerts);
    }

    private Notification buildControllerNotification() {
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(
                this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        int connectedCount = connectedMacs.size();
        String text = connectedCount > 0
                ? getString(R.string.controller_notification_connected, connectedCount)
                : getString(R.string.controller_notification_waiting);
        return new NotificationCompat.Builder(this, CHANNEL_CONTROLLER)
                .setSmallIcon(R.drawable.ic_fg_logo)
                .setContentTitle(getString(R.string.controller_notification_title))
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pending)
                .build();
    }

    private void updateControllerNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(CONTROLLER_NOTIFICATION_ID, buildControllerNotification());
    }

    private boolean alertsEnabled() {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(PREF_ALERTS_ENABLED, true);
    }

    private void postAlert(String title, String body, int id) {
        if (!alertsEnabled()) return;
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(
                this, id, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ALERTS)
                .setSmallIcon(R.drawable.ic_fg_logo)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
        getSystemService(NotificationManager.class).notify(id, notification);
    }

    @Override public void onListening(int port) {
        updateControllerNotification();
    }

    @Override public void onDeviceConnected(MttlProtocol.BootInfo bootInfo, String remoteAddress) {
        connectedMacs.add(bootInfo.mac.toUpperCase());
        lastAlertKeyByMac.remove(bootInfo.mac);
        updateControllerNotification();
    }

    @Override public void onDeviceDisconnected(String mac) {
        if (mac != null) connectedMacs.remove(mac.toUpperCase());
        updateControllerNotification();
        postAlert(getString(R.string.strip_offline_alert_title),
                getString(R.string.strip_offline_alert_body), ALERT_BASE_ID + 1);
    }

    @Override public void onOutletState(String mac, MttlProtocol.OutletState state) { }

    @Override public void onTelemetry(String mac, MttlProtocol.Telemetry telemetry) {
        if (automationEngine != null) automationEngine.onTelemetry(mac, telemetry);
        if (energyHistory != null) energyHistory.record(mac, telemetry);
        double totalPower = 0.0;
        String event = null;
        int hottest = Integer.MIN_VALUE;
        for (MttlProtocol.OutletTelemetry outlet : telemetry.outlets) {
            totalPower += Math.max(0.0, outlet.powerW);
            hottest = Math.max(hottest, outlet.temperatureC);
            if (!"00".equals(outlet.eventCode)) event = outlet.eventCode;
        }

        String alertKey = null;
        String title = null;
        String body = null;
        if (event != null) {
            alertKey = "event:" + event;
            title = getString(R.string.protection_alert_title);
            body = getString(R.string.protection_alert_body, event);
        } else if (totalPower > 3000.0) {
            alertKey = "power:high";
            title = getString(R.string.high_load_alert_title);
            body = getString(R.string.high_load_alert_body, totalPower);
        }

        if (alertKey == null) {
            lastAlertKeyByMac.remove(mac);
            return;
        }
        String previous = lastAlertKeyByMac.get(mac);
        if (alertKey.equals(previous)) return;
        lastAlertKeyByMac.put(mac, alertKey);
        int id = ALERT_BASE_ID + Math.abs(mac.hashCode() % 500);
        postAlert(title, body, id);
    }

    @Override public void onProtocolFrame(String mac, String frame) { }

    @Override public void onError(String message, Throwable error) { }

    private static String safeMessage(Throwable error) {
        if (error == null) return "unknown";
        String value = error.getMessage();
        return value == null || value.trim().isEmpty()
                ? error.getClass().getSimpleName() : value;
    }
}
