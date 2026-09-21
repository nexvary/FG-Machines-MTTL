package com.fgmachines.rck;

import java.util.Arrays;

public final class SharpAcProtocol implements IrSignal {
    public enum Model { A907, A705, A903 }
    public enum Mode { AUTO, HEAT, COOL, DRY, FAN }
    public enum Fan { AUTO, MIN, MED, HIGH, MAX }

    public static final int CARRIER_HZ = 38000;
    public static final int MIN_TEMP_C = 15;
    public static final int MAX_TEMP_C = 30;
    private static final int HDR_MARK = 3800, HDR_SPACE = 1900, BIT_MARK = 470,
            ZERO_SPACE = 500, ONE_SPACE = 1400, GAP = 100000;

    private final byte[] state = {(byte) 0xAA, 0x5A, (byte) 0xCF, 0x10, 0x00, 0x01, 0x00, 0x00,
            0x08, (byte) 0x80, 0x00, (byte) 0xE0, 0x01};
    private Model model = Model.A907;
    private int savedTemp = MIN_TEMP_C;
    private Mode savedMode = Mode.AUTO;
    private Fan savedFan = Fan.AUTO;

    public SharpAcProtocol() { model(Model.A907); }

    public SharpAcProtocol model(Model value) {
        model = value == null ? Model.A907 : value;
        setBit(4, 4, model != Model.A907);
        setBit(11, 4, model != Model.A907);
        mode(savedMode);
        return this;
    }

    public SharpAcProtocol power(boolean on, boolean previouslyOn) {
        setPowerSpecial(on ? (previouslyOn ? 3 : 1) : 2);
        state[10] = 0x00;
        setBit(6, 3, false);
        return this;
    }

    public SharpAcProtocol temp(int c) {
        savedTemp = Math.max(MIN_TEMP_C, Math.min(MAX_TEMP_C, c));
        int nativeMode = state[6] & 0x03;
        if (nativeMode == 0 || nativeMode == 3) {
            state[4] = 0;
            return this;
        }
        state[4] = (byte) (model == Model.A705 ? 0xD0 : 0xC0);
        state[4] = (byte) ((state[4] & 0xF0) | (savedTemp - MIN_TEMP_C));
        state[10] = 0x04;
        clearPowerSpecial();
        return this;
    }

    public SharpAcProtocol mode(Mode requested) {
        Mode effective = requested == null ? Mode.AUTO : requested;
        if (effective == Mode.HEAT && model != Model.A907) effective = Mode.FAN;
        if (effective == Mode.FAN && model == Model.A907) effective = Mode.AUTO;
        int v;
        switch (effective) {
            case HEAT: v = 1; break;
            case COOL: v = 2; break;
            case DRY: v = 3; break;
            case FAN:
            case AUTO:
            default: v = 0;
        }
        state[6] = (byte) ((state[6] & 0xFC) | v);
        savedMode = effective;
        if (effective == Mode.AUTO || effective == Mode.DRY) setFanInternal(Fan.AUTO, false);
        temp(savedTemp);
        state[10] = 0x00;
        clearPowerSpecial();
        return this;
    }

    public SharpAcProtocol fan(Fan fan) { return setFanInternal(fan, true); }

    private SharpAcProtocol setFanInternal(Fan fan, boolean save) {
        Fan use = fan == null ? Fan.AUTO : fan;
        int v;
        if (model == Model.A705 || model == Model.A903) {
            switch (use) {
                case MIN: v = 3; break;
                case MED:
                case HIGH: v = 5; break;
                case MAX: v = 7; break;
                default: v = 2;
            }
        } else {
            switch (use) {
                case MIN: v = 4; break;
                case MED: v = 3; break;
                case HIGH: v = 5; break;
                case MAX: v = 7; break;
                default: v = 2;
            }
        }
        state[6] = (byte) ((state[6] & 0x8F) | (v << 4));
        if (save) savedFan = use;
        state[10] = 0x05;
        clearPowerSpecial();
        return this;
    }

    public SharpAcProtocol swingToggle() {
        state[8] = (byte) ((state[8] & 0xF8) | 0x07);
        state[10] = 0x06;
        return this;
    }

    private void clearPowerSpecial() { setPowerSpecial(((state[5] >>> 4) & 0xF) & 3); }
    private void setPowerSpecial(int v) { state[5] = (byte) ((state[5] & 0x0F) | ((v & 0xF) << 4)); }
    private void setBit(int i, int bit, boolean value) {
        if (value) state[i] = (byte) (state[i] | (1 << bit));
        else state[i] = (byte) (state[i] & ~(1 << bit));
    }

    public static int calcChecksum(byte[] bytes) {
        if (bytes == null || bytes.length < 2) throw new IllegalArgumentException("state too short");
        int x = 0;
        for (int i = 0; i < bytes.length - 1; i++) x ^= bytes[i] & 0xFF;
        x ^= bytes[bytes.length - 1] & 0x0F;
        x ^= (x >>> 4) & 0x0F;
        return x & 0x0F;
    }

    public static boolean validChecksum(byte[] bytes) {
        return bytes != null && bytes.length >= 2
                && (((bytes[bytes.length - 1] >>> 4) & 0x0F) == calcChecksum(bytes));
    }

    public byte[] raw() {
        byte[] bytes = Arrays.copyOf(state, state.length);
        int checksum = calcChecksum(bytes);
        bytes[12] = (byte) ((bytes[12] & 0x0F) | (checksum << 4));
        return bytes;
    }

    @Override public int carrierHz() { return CARRIER_HZ; }
    @Override public String protocolName() { return "SHARP-104/" + model; }

    @Override public int[] pattern() {
        byte[] bytes = raw();
        int[] pattern = new int[2 + bytes.length * 16 + 2];
        int k = 0;
        pattern[k++] = HDR_MARK;
        pattern[k++] = HDR_SPACE;
        for (byte b : bytes) {
            int u = b & 0xFF;
            for (int bit = 0; bit < 8; bit++) {
                pattern[k++] = BIT_MARK;
                pattern[k++] = ((u & (1 << bit)) != 0 ? ONE_SPACE : ZERO_SPACE);
            }
        }
        pattern[k++] = BIT_MARK;
        pattern[k++] = GAP;
        return Arrays.copyOf(pattern, k);
    }
}
