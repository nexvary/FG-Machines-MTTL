package com.fgmachines.rck;

import android.content.Context;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Process-wide owner of the local MTTL controller. A foreground service keeps
 * this object alive while the UI can attach/detach listeners safely.
 */
public final class ControllerHub implements Closeable {
    private static volatile ControllerHub instance;

    private final CopyOnWriteArraySet<MttlControllerServer.Listener> listeners =
            new CopyOnWriteArraySet<>();
    private final Map<String, DeviceSnapshot> snapshots = new ConcurrentHashMap<>();
    private final MttlControllerServer server;
    private volatile boolean started;
    private volatile String activeMac;

    private ControllerHub(Context context) {
        server = new MttlControllerServer(new MttlControllerServer.Listener() {
            @Override public void onListening(int port) {
                for (MttlControllerServer.Listener listener : listeners) listener.onListening(port);
            }

            @Override public void onDeviceConnected(MttlProtocol.BootInfo bootInfo, String remoteAddress) {
                String key = bootInfo.mac.toUpperCase();
                DeviceSnapshot snapshot = snapshots.computeIfAbsent(key, unused -> new DeviceSnapshot());
                snapshot.bootInfo = bootInfo;
                snapshot.remoteAddress = remoteAddress;
                snapshot.connected = true;
                snapshot.connectedSince = System.currentTimeMillis();
                activeMac = key;
                for (MttlControllerServer.Listener listener : listeners) {
                    listener.onDeviceConnected(bootInfo, remoteAddress);
                }
            }

            @Override public void onDeviceDisconnected(String mac) {
                String key = mac == null ? "" : mac.toUpperCase();
                DeviceSnapshot snapshot = snapshots.get(key);
                if (snapshot != null) snapshot.connected = false;
                if (key.equalsIgnoreCase(activeMac == null ? "" : activeMac)) {
                    activeMac = null;
                    for (Map.Entry<String, DeviceSnapshot> entry : snapshots.entrySet()) {
                        if (entry.getValue().connected) {
                            activeMac = entry.getKey();
                            break;
                        }
                    }
                }
                for (MttlControllerServer.Listener listener : listeners) listener.onDeviceDisconnected(mac);
            }

            @Override public void onOutletState(String mac, MttlProtocol.OutletState state) {
                for (MttlControllerServer.Listener listener : listeners) listener.onOutletState(mac, state);
            }

            @Override public void onTelemetry(String mac, MttlProtocol.Telemetry telemetry) {
                DeviceSnapshot snapshot = snapshots.computeIfAbsent(mac.toUpperCase(), unused -> new DeviceSnapshot());
                snapshot.telemetry = telemetry;
                for (MttlControllerServer.Listener listener : listeners) listener.onTelemetry(mac, telemetry);
            }

            @Override public void onProtocolFrame(String mac, String frame) {
                for (MttlControllerServer.Listener listener : listeners) listener.onProtocolFrame(mac, frame);
            }

            @Override public void onError(String message, Throwable error) {
                for (MttlControllerServer.Listener listener : listeners) listener.onError(message, error);
            }
        });
    }

    public static ControllerHub get(Context context) {
        ControllerHub local = instance;
        if (local == null) {
            synchronized (ControllerHub.class) {
                local = instance;
                if (local == null) {
                    local = new ControllerHub(context.getApplicationContext());
                    instance = local;
                }
            }
        }
        return local;
    }

    public synchronized void start() throws IOException {
        if (started) return;
        server.start();
        started = true;
    }

    public boolean isRunning() {
        return started && server.isRunning();
    }

    public void addListener(MttlControllerServer.Listener listener, boolean replay) {
        if (listener == null) return;
        listeners.add(listener);
        if (replay) replay(listener);
    }

    public void removeListener(MttlControllerServer.Listener listener) {
        if (listener != null) listeners.remove(listener);
    }

    private void replay(MttlControllerServer.Listener listener) {
        if (isRunning()) listener.onListening(ModelCatalog.CONTROLLER_PORT);
        for (Map.Entry<String, DeviceSnapshot> entry : snapshots.entrySet()) {
            DeviceSnapshot snapshot = entry.getValue();
            if (!snapshot.connected || snapshot.bootInfo == null) continue;
            listener.onDeviceConnected(snapshot.bootInfo,
                    snapshot.remoteAddress == null ? "" : snapshot.remoteAddress);
            if (snapshot.telemetry != null) listener.onTelemetry(entry.getKey(), snapshot.telemetry);
        }
    }

    public String activeMac() {
        return activeMac;
    }

    public List<DeviceState> connectedStates() {
        List<DeviceState> out = new ArrayList<>();
        for (Map.Entry<String, DeviceSnapshot> entry : snapshots.entrySet()) {
            DeviceSnapshot snapshot = entry.getValue();
            if (!snapshot.connected || snapshot.bootInfo == null) continue;
            out.add(toState(entry.getKey(), snapshot));
        }
        out.sort((a, b) -> a.mac.compareToIgnoreCase(b.mac));
        return out;
    }

    public DeviceState state(String mac) {
        String key = FleetStore.normalizeMac(mac);
        DeviceSnapshot snapshot = snapshots.get(key);
        return snapshot == null || snapshot.bootInfo == null ? null : toState(key, snapshot);
    }

    public boolean isConnected(String mac) {
        DeviceSnapshot snapshot = snapshots.get(FleetStore.normalizeMac(mac));
        return snapshot != null && snapshot.connected;
    }

    private static DeviceState toState(String mac, DeviceSnapshot snapshot) {
        return new DeviceState(
                mac,
                snapshot.bootInfo.model,
                snapshot.bootInfo.firmwareVersion,
                snapshot.remoteAddress == null ? "" : snapshot.remoteAddress,
                snapshot.connected,
                snapshot.connectedSince,
                snapshot.telemetry
        );
    }

    public void setOutlet(String mac, int outlet, boolean on) throws IOException {
        server.setOutlet(mac, outlet, on);
    }

    public void refresh(String mac) throws IOException {
        server.refresh(mac);
    }

    @Override public synchronized void close() {
        if (!started) return;
        server.close();
        started = false;
        activeMac = null;
        snapshots.clear();
    }

    private static final class DeviceSnapshot {
        volatile boolean connected;
        volatile long connectedSince;
        volatile MttlProtocol.BootInfo bootInfo;
        volatile String remoteAddress;
        volatile MttlProtocol.Telemetry telemetry;
    }

    public static final class DeviceState {
        public final String mac;
        public final String model;
        public final String firmwareVersion;
        public final String remoteAddress;
        public final boolean connected;
        public final long connectedSince;
        public final MttlProtocol.Telemetry telemetry;

        DeviceState(String mac, String model, String firmwareVersion, String remoteAddress,
                    boolean connected, long connectedSince, MttlProtocol.Telemetry telemetry) {
            this.mac = mac;
            this.model = model;
            this.firmwareVersion = firmwareVersion;
            this.remoteAddress = remoteAddress;
            this.connected = connected;
            this.connectedSince = connectedSince;
            this.telemetry = telemetry;
        }
    }
}
