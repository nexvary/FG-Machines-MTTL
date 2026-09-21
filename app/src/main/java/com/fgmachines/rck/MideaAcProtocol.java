package com.fgmachines.rck;

import java.util.Arrays;

public final class MideaAcProtocol implements IrSignal {
    public enum Mode { COOL, DRY, AUTO, HEAT, FAN }
    public enum Fan { AUTO, LOW, MED, HIGH }

    public static final int CARRIER_HZ = 38000;
    public static final int MIN_TEMP_C = 17;
    public static final int MAX_TEMP_C = 30;
    private static final int HDR_MARK = 4480, HDR_SPACE = 4480, BIT_MARK = 560,
            ONE_SPACE = 1680, ZERO_SPACE = 560, MIN_GAP = 5600, PAIR_GAP = 100000;
    private long state = 0xA1826FFFFF62L;

    public MideaAcProtocol power(boolean on) {
        setByte(4, (getByte(4) & 0x7F) | (on ? 0x80 : 0));
        return this;
    }
    public MideaAcProtocol temp(int c) {
        c = Math.max(MIN_TEMP_C, Math.min(MAX_TEMP_C, c));
        setByte(3, (getByte(3) & 0xE0) | (c - MIN_TEMP_C));
        return this;
    }
    public MideaAcProtocol mode(Mode m) {
        int v = 2;
        if (m == Mode.COOL) v = 0;
        else if (m == Mode.DRY) v = 1;
        else if (m == Mode.HEAT) v = 3;
        else if (m == Mode.FAN) v = 4;
        setByte(4, (getByte(4) & 0xF8) | v);
        return this;
    }
    public MideaAcProtocol fan(Fan f) {
        int v = f == Fan.LOW ? 1 : f == Fan.MED ? 2 : f == Fan.HIGH ? 3 : 0;
        setByte(4, (getByte(4) & 0xE7) | (v << 3));
        return this;
    }
    public MideaAcProtocol sleep(boolean on) {
        setByte(4, (getByte(4) & 0xBF) | (on ? 0x40 : 0));
        return this;
    }
    public MideaAcProtocol swingToggle() {
        state = 0xA201FFFFFF7CL;
        return this;
    }
    private int getByte(int i) { return (int) ((state >> (i * 8)) & 0xFF); }
    private void setByte(int i, int v) {
        long mask = 0xFFL << (i * 8);
        state = (state & ~mask) | (((long) v & 0xFFL) << (i * 8));
    }
    private static int reverse8(int x) {
        x &= 255;
        int r = 0;
        for (int i = 0; i < 8; i++) r = (r << 1) | ((x >> i) & 1);
        return r;
    }
    public static int calcChecksum(long s) {
        int sum = 0;
        long t = s;
        for (int i = 0; i < 5; i++) {
            t >>>= 8;
            sum += reverse8((int) (t & 255));
        }
        return reverse8((256 - (sum & 255)) & 255);
    }
    public long raw() {
        setByte(0, calcChecksum(state));
        return state & 0xFFFFFFFFFFFFL;
    }
    public static boolean validChecksum(long s) { return ((int) s & 255) == calcChecksum(s); }
    @Override public int carrierHz() { return CARRIER_HZ; }
    @Override public String protocolName() { return "MIDEA-48"; }
    @Override public int[] pattern() {
        long d = raw();
        int[] out = new int[202];
        int k = 0;
        for (int phase = 0; phase < 2; phase++) {
            out[k++] = HDR_MARK;
            out[k++] = HDR_SPACE;
            for (int by = 5; by >= 0; by--) {
                int b = (int) ((d >> (by * 8)) & 255);
                for (int bit = 7; bit >= 0; bit--) {
                    out[k++] = BIT_MARK;
                    out[k++] = ((b & (1 << bit)) != 0 ? ONE_SPACE : ZERO_SPACE);
                }
            }
            out[k++] = BIT_MARK;
            out[k++] = MIN_GAP;
            d = (~d) & 0xFFFFFFFFFFFFL;
        }
        out[k++] = PAIR_GAP;
        return Arrays.copyOf(out, k);
    }
}
