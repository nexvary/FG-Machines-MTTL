package com.fgmachines.rck;

public final class DeviceProfile {
    public enum Brand {
        SHARP, UNIONAIR, MIDEA, CARRIER, GREE, LG, SAMSUNG, HAIER,
        TOSHIBA, TCL, HISENSE, FRESH, OTHER
    }
    public enum Protocol {
        SHARP_A907, SHARP_A903, SHARP_A705, MIDEA_48, CARRIER_64,
        GREE_YAW1F, GREE_YBOFB, GREE_YX1FSF,
        LG_28, LG2_28, SAMSUNG_AC, HAIER_YRW02,
        TOSHIBA_GENERIC, TOSHIBA_WA_TH0X, TCL112, KELON168
    }

    public static String brandLabel(Brand brand) {
        switch (brand) {
            case UNIONAIR: return "UnionAir";
            case MIDEA: return "Midea";
            case CARRIER: return "Carrier";
            case GREE: return "Gree";
            case LG: return "LG";
            case SAMSUNG: return "Samsung";
            case HAIER: return "Haier";
            case TOSHIBA: return "Toshiba";
            case TCL: return "TCL";
            case HISENSE: return "Hisense";
            case FRESH: return "Fresh";
            case OTHER: return "Other";
            default: return "Sharp";
        }
    }

    public static String protocolLabel(Protocol p) {
        switch (p) {
            case MIDEA_48: return "Midea 48-bit";
            case CARRIER_64: return "Carrier AC64";
            case GREE_YAW1F: return "Gree YAW1F";
            case GREE_YBOFB: return "Gree YBOFB";
            case GREE_YX1FSF: return "Gree YX1FSF";
            case LG_28: return "LG 28-bit";
            case LG2_28: return "LG2 28-bit";
            case SAMSUNG_AC: return "Samsung AC";
            case HAIER_YRW02: return "Haier YR-W02";
            case TOSHIBA_GENERIC: return "Toshiba Generic";
            case TOSHIBA_WA_TH0X: return "Toshiba WA-TH0x";
            case TCL112: return "TCL112";
            case KELON168: return "Kelon168 / Hisense";
            case SHARP_A903: return "Sharp A903";
            case SHARP_A705: return "Sharp A705";
            default: return "Sharp A907";
        }
    }
}
