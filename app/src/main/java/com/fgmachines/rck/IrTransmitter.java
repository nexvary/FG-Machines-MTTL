package com.fgmachines.rck;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.ConsumerIrManager;

public final class IrTransmitter {
    private final ConsumerIrManager ir;
    private final Context appContext;
    private String lastDiagnostic = "not probed";

    public IrTransmitter(Context context) {
        appContext = context.getApplicationContext();
        ConsumerIrManager temp = null;
        try {
            temp = (ConsumerIrManager) appContext.getSystemService(Context.CONSUMER_IR_SERVICE);
        } catch (Throwable error) {
            lastDiagnostic = "service error: " + error.getClass().getSimpleName();
        }
        ir = temp;
    }

    public boolean available() {
        if (ir == null) {
            lastDiagnostic = "ConsumerIrManager unavailable";
            return false;
        }
        try {
            boolean ok = ir.hasIrEmitter();
            lastDiagnostic = ok ? "IR emitter detected" : "hasIrEmitter=false";
            return ok;
        } catch (Throwable error) {
            lastDiagnostic = "hasIrEmitter error: " + error.getClass().getSimpleName();
            return false;
        }
    }

    public boolean permissionGranted() {
        try {
            return appContext.checkCallingOrSelfPermission("android.permission.TRANSMIT_IR")
                    == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable error) {
            lastDiagnostic = "permission check error: " + error.getClass().getSimpleName();
            return false;
        }
    }

    public String frequencySummary() {
        if (!available()) return "IR unavailable";
        try {
            ConsumerIrManager.CarrierFrequencyRange[] ranges = ir.getCarrierFrequencies();
            if (ranges == null || ranges.length == 0) return "IR detected";
            for (ConsumerIrManager.CarrierFrequencyRange range : ranges) {
                if (range != null && 38000 >= range.getMinFrequency()
                        && 38000 <= range.getMaxFrequency()) {
                    return "38 kHz supported";
                }
            }
            return "38 kHz not reported";
        } catch (Throwable error) {
            return "IR detected";
        }
    }

    public String diagnostic() {
        return lastDiagnostic;
    }

    public void send(IrSignal signal) {
        if (signal == null) throw new IllegalArgumentException("signal=null");
        if (!available()) throw new IllegalStateException("IR emitter unavailable");
        if (!permissionGranted()) throw new SecurityException("TRANSMIT_IR permission not granted");
        int[] pattern = signal.pattern();
        if (pattern == null || pattern.length < 4) throw new IllegalStateException("Invalid IR pattern");
        for (int value : pattern) if (value <= 0) throw new IllegalStateException("Invalid IR timing");
        try {
            ir.transmit(signal.carrierHz(), pattern);
            lastDiagnostic = "transmit ok: " + signal.protocolName();
        } catch (Throwable error) {
            lastDiagnostic = "transmit error: " + error.getClass().getSimpleName();
            throw new IllegalStateException(lastDiagnostic, error);
        }
    }
}
