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
    public static final String PREF_ALERT_POWER_W = "alert_power_w";
    public static final String PREF_ALERT_TEMP_C = "alert_temp_c";
    public static final String PREF_ALERT_DAILY_ENERGY_KWH = "alert_daily_energy_kwh";

    private ControllerHub hub;
    private LocalAutomationEngine automationEngine;
    private FleetStore fleetStore;
    private HistoryStore historyStore;
    private AccessControlStore accessStore;
    private LocalApiServer localApiServer;
    private UsbDiscoveryStore usbDiscoveryStore;
    private final Map<String, String> lastAlertKeyByMac = new HashMap<>();
    private final Set<String> connectedMacs = ConcurrentHashMap.newKeySet();

    @Override public void onCreate() {
        super.onCreate();
        createChannels();
        hub = ControllerHub.get(this);
        fleetStore = new FleetStore(this);
        historyStore = new HistoryStore(this);
        accessStore = new AccessControlStore(this);
        usbDiscoveryStore = new UsbDiscoveryStore(this);
        localApiServer = new LocalApiServer(hub, fleetStore, historyStore, accessStore);
        automationEngine = new LocalAutomationEngine(this, hub);
        automationEngine.start();
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
            localApiServer.start();
        } catch (IOException error) {
            postAlert(getString(R.string.controller_service_error), safeMessage(error), ALERT_BASE_ID + 99);
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        if (hub != null) hub.removeListener(this);
        if (automationEngine != null) automationEngine.close();
        if (localApiServer != null) localApiServer.close();
        if (historyStore != null) historyStore.close();
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
        String mac = FleetStore.normalizeMac(bootInfo.mac);
        connectedMacs.add(mac);
        lastAlertKeyByMac.remove(mac);
        if (fleetStore != null) fleetStore.register(mac, bootInfo.firmwareVersion, System.currentTimeMillis());
        if (historyStore != null) historyStore.recordEvent(
                mac, 0, "connected", remoteAddress, System.currentTimeMillis());
        updateControllerNotification();
    }

    @Override public void onDeviceDisconnected(String mac) {
        String key = FleetStore.normalizeMac(mac);
        if (!key.isEmpty()) connectedMacs.remove(key);
        if (historyStore != null && !key.isEmpty()) historyStore.recordEvent(
                key, 0, "disconnected", "", System.currentTimeMillis());
        updateControllerNotification();
        postAlert(getString(R.string.strip_offline_alert_title),
                getString(R.string.strip_offline_alert_body), ALERT_BASE_ID + 1);
    }

    @Override public void onOutletState(String mac, MttlProtocol.OutletState state) {
        if (historyStore != null && state != null) {
            historyStore.recordEvent(mac, state.outlet, "relay_state",
                    state.on ? "on" : "off", System.currentTimeMillis());
        }
    }

    @Override public void onTelemetry(String mac, MttlProtocol.Telemetry telemetry) {
        if (automationEngine != null) automationEngine.onTelemetry(mac, telemetry);
        long now = System.currentTimeMillis();
        String key = FleetStore.normalizeMac(mac);
        if (fleetStore != null && !key.isEmpty()) fleetStore.register(key, "", now);
        if (historyStore != null) historyStore.recordTelemetry(key, telemetry, now);

        double totalPower = 0.0;
        String event = null;
        int hottest = Integer.MIN_VALUE;
        boolean overload = false;
        boolean overheat = false;
        for (MttlProtocol.OutletTelemetry outlet : telemetry.outlets) {
            totalPower += Math.max(0.0, outlet.powerW);
            hottest = Math.max(hottest, outlet.temperatureC);
            overload |= outlet.overloadProtection;
            overheat |= outlet.overheatProtection;
            if (!"00".equals(outlet.eventCode)) event = outlet.eventCode;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        double powerThreshold = Math.max(1.0,
                Double.longBitsToDouble(prefs.getLong(PREF_ALERT_POWER_W,
                        Double.doubleToRawLongBits(3000.0))));
        int tempThreshold = prefs.getInt(PREF_ALERT_TEMP_C, 0);
        double energyThreshold = Double.longBitsToDouble(
                prefs.getLong(PREF_ALERT_DAILY_ENERGY_KWH, Double.doubleToRawLongBits(0.0)));

        String alertKey = null;
        String title = null;
        String body = null;
        if (event != null || overload || overheat) {
            String code = event == null ? (overload ? "OVERLOAD" : "OVERHEAT") : event;
            alertKey = "event:" + code;
            title = getString(R.string.protection_alert_title);
            body = getString(R.string.protection_alert_body, code);
        } else if (tempThreshold > 0 && hottest >= tempThreshold) {
            alertKey = "temp:high";
            title = getString(R.string.temperature_alert_title);
            body = getString(R.string.temperature_alert_body, hottest, tempThreshold);
        } else if (totalPower > powerThreshold) {
            alertKey = "power:high";
            title = getString(R.string.high_load_alert_title);
            body = getString(R.string.high_load_alert_body, totalPower);
        } else if (energyThreshold > 0.0 && historyStore != null) {
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
            calendar.set(java.util.Calendar.MINUTE, 0);
            calendar.set(java.util.Calendar.SECOND, 0);
            calendar.set(java.util.Calendar.MILLISECOND, 0);
            double today = historyStore.summary(key, calendar.getTimeInMillis()).energyDeltaKWh;
            if (today >= energyThreshold) {
                alertKey = "energy:daily";
                title = getString(R.string.energy_alert_title);
                body = getString(R.string.energy_alert_body, today, energyThreshold);
            }
        }

        if (alertKey == null) {
            lastAlertKeyByMac.remove(key);
            return;
        }
        String previous = lastAlertKeyByMac.get(key);
        if (alertKey.equals(previous)) return;
        lastAlertKeyByMac.put(key, alertKey);
        if (historyStore != null) historyStore.recordEvent(
                key, 0, "alert", alertKey, now);
        int id = ALERT_BASE_ID + Math.abs(key.hashCode() % 500);
        postAlert(title, body, id);
    }

    @Override public void onProtocolFrame(String mac, String frame) {
        if (usbDiscoveryStore == null) return;
        long now = System.currentTimeMillis();
        if (usbDiscoveryStore.recordUnknownFrame(mac, frame, now) && historyStore != null) {
            String safe = frame == null ? "" : frame.replace('\r', ' ').replace('\n', ' ').trim();
            if (safe.length() > 512) safe = safe.substring(0, 512);
            historyStore.recordEvent(mac, 0, "usb_discovery_frame", safe, now);
        }
    }

    @Override public void onError(String message, Throwable error) { }

    private static String safeMessage(Throwable error) {
        if (error == null) return "unknown";
        String value = error.getMessage();
        return value == null || value.trim().isEmpty()
                ? error.getClass().getSimpleName() : value;
    }
}
