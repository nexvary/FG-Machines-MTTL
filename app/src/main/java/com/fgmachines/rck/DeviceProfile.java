package com.fgmachines.rck;

public final class DeviceProfile {
    public enum Brand { SHARP, UNIONAIR }
    public enum Protocol { SHARP_A907, SHARP_A903, SHARP_A705, MIDEA_48, CARRIER_64 }

    public static String protocolLabel(Protocol protocol) {
        switch (protocol) {
            case MIDEA_48: return "Midea 48-bit";
            case CARRIER_64: return "Carrier AC64";
            case SHARP_A903: return "Sharp A903";
            case SHARP_A705: return "Sharp A705";
            default: return "Sharp A907";
        }
    }
}
