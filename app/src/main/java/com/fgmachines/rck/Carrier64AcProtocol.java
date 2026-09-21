package com.fgmachines.rck;

import java.util.Arrays;

public final class Carrier64AcProtocol implements IrSignal {
    public enum Mode { COOL, HEAT, FAN }
    public enum Fan { AUTO, LOW, MED, HIGH }

    public static final int CARRIER_HZ = 38000;
    public static final int MIN_TEMP_C = 16;
    public static final int MAX_TEMP_C = 30;
    private static final int HDR_MARK = 8940, HDR_SPACE = 4556, BIT_MARK = 503,
            ONE_SPACE = 1736, ZERO_SPACE = 615, GAP = 100000;
    private long state = 0x109000002C2A5584L;

    private int getByte(int i) { return (int) ((state >> (i * 8)) & 255); }
    private void setByte(int i, int v) {
        long mask = 0xFFL << (i * 8);
        state = (state & ~mask) | (((long) v & 255L) << (i * 8));
    }
    public Carrier64AcProtocol temp(int c) {
        c = Math.max(MIN_TEMP_C, Math.min(MAX_TEMP_C, c));
        setByte(3, (getByte(3) & 0xF0) | (c - MIN_TEMP_C));
        return this;
    }
    public Carrier64AcProtocol power(boolean on) {
        setByte(4, (getByte(4) & 0xEF) | (on ? 0x10 : 0));
        return this;
    }
    public Carrier64AcProtocol mode(Mode m) {
        int v = m == Mode.HEAT ? 1 : m == Mode.FAN ? 3 : 2;
        setByte(2, (getByte(2) & 0xCF) | (v << 4));
        return this;
    }
    public Carrier64AcProtocol fan(Fan f) {
        int v = f == Fan.LOW ? 1 : f == Fan.MED ? 2 : f == Fan.HIGH ? 3 : 0;
        setByte(2, (getByte(2) & 0x3F) | (v << 6));
        return this;
    }
    public Carrier64AcProtocol swing(boolean on) {
        setByte(3, (getByte(3) & 0xDF) | (on ? 0x20 : 0));
        return this;
    }
    public static int calcChecksum(long s) {
        long d = s >>> 20;
        int sum = 0;
        while (d != 0) {
            sum += (int) (d & 0xF);
            d >>>= 4;
        }
        return sum & 0xF;
    }
    public long raw() {
        int b2 = getByte(2);
        b2 = (b2 & 0xF0) | calcChecksum(state);
        setByte(2, b2);
        return state;
    }
    public static boolean validChecksum(long s) { return ((s >>> 16) & 0xF) == calcChecksum(s); }
    @Override public int carrierHz() { return CARRIER_HZ; }
    @Override public String protocolName() { return "CARRIER-AC64"; }
    @Override public int[] pattern() {
        long d = raw();
        int[] p = new int[132];
        int k = 0;
        p[k++] = HDR_MARK;
        p[k++] = HDR_SPACE;
        for (int bit = 0; bit < 64; bit++) {
            p[k++] = BIT_MARK;
            p[k++] = ((d & (1L << bit)) != 0 ? ONE_SPACE : ZERO_SPACE);
        }
        p[k++] = BIT_MARK;
        p[k++] = GAP;
        return Arrays.copyOf(p, k);
    }
}
