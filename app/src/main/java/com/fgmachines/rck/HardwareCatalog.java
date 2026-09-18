package com.fgmachines.rck;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Product-level hardware catalog. This intentionally separates sticker/certification
 * identity from transport support so a device is never treated as MTTL just because
 * it is a smart power strip.
 */
public final class HardwareCatalog {
    public static final String SOURCE_DAWON_ENERGY =
            "https://eep.energy.or.kr/electricity/elec_view_234.aspx?no=234170061";
    public static final String SOURCE_DAWON_ZWAVE =
            "https://products.z-wavealliance.org/z-wave-product/power-manager-5/";

    public enum Transport {
        MTTL_LOCAL_TCP,
        Z_WAVE_GATEWAY
    }

    public enum SupportStatus {
        VERIFIED_LOCAL_CONTROL,
        VERIFIED_ZWAVE_PROFILE_GATEWAY_REQUIRED
    }

    public static final Profile MTTL_W01 = new Profile(
            "mttl_w01",
            "LG U+ / TONLY",
            "MTTL-W01",
            Arrays.asList(
                    "MTTL-W01",
                    "HU04139-17002A",
                    "HU04139-17002B",
                    "HU04139-17002C",
                    "HU04139-17002D",
                    "HU04139-17002E",
                    "TONLY_TAP_",
                    "ONLY_TAP_"
            ),
            Transport.MTTL_LOCAL_TCP,
            SupportStatus.VERIFIED_LOCAL_CONTROL,
            4,
            2,
            0,
            0,
            0,
            "",
            "",
            -1,
            -1,
            -1,
            "",
            ""
    );

    /**
     * The photographed unit identifies itself as MTD-01 while the radio/certification
     * marking and Korean standby-power registration use PM-M130-ZW. We treat these as
     * aliases for the same Dawon DNS hardware family, but we do not copy a unit serial.
     */
    public static final Profile DAWON_MTD_01 = new Profile(
            "dawon_mtd_01",
            "Dawon DNS",
            "MTD-01 / PM-M130-ZW",
            Arrays.asList(
                    "MTD-01",
                    "PM-M130-ZW",
                    "MSIP-CMM-DAW-PM-M130-ZW",
                    "JH04151-17006",
                    "다원디엔에스"
            ),
            Transport.Z_WAVE_GATEWAY,
            SupportStatus.IDENTIFIED_GATEWAY_REQUIRED,
            -1,
            2,
            250,
            16,
            3500,
            "DC 5V / 2A total, 2 USB ports",
            SOURCE_DAWON_ENERGY,
            0x018C,
            0x0042,
            0x0007,
            "KR 920.90 / 921.70 / 923.10 MHz",
            "Switch Binary v1; Meter v3; Security S0"
    );

    private static final List<Profile> ALL = Collections.unmodifiableList(
            Arrays.asList(MTTL_W01, DAWON_MTD_01)
    );

    private HardwareCatalog() {}

    public static List<Profile> all() {
        return ALL;
    }

    public static Profile identifyFromLabel(String text) {
        if (text == null) return null;
        String normalized = normalize(text);
        if (normalized.isEmpty()) return null;

        // Match strong, product-specific signatures first.
        for (Profile profile : ALL) {
            for (String alias : profile.aliases) {
                String candidate = normalize(alias);
                if (!candidate.isEmpty() && normalized.contains(candidate)) {
                    return profile;
                }
            }
        }
        return null;
    }

    private static String normalize(String value) {
        return value == null ? "" : value
                .trim()
                .toUpperCase(Locale.US)
                .replace('–', '-')
                .replace('—', '-');
    }

    public static final class Profile {
        public final String id;
        public final String manufacturer;
        public final String displayModel;
        public final List<String> aliases;
        public final Transport transport;
        public final SupportStatus supportStatus;
        public final int verifiedAcOutletCount;
        public final int usbPortCount;
        public final int ratedVac;
        public final int ratedA;
        public final int maxPowerW;
        public final String usbRating;
        public final String sourceUrl;
        public final int zwaveManufacturerId;
        public final int zwaveProductTypeId;
        public final int zwaveProductId;
        public final String zwaveFrequencyPlan;
        public final String zwaveCommandClasses;

        Profile(String id,
                String manufacturer,
                String displayModel,
                List<String> aliases,
                Transport transport,
                SupportStatus supportStatus,
                int verifiedAcOutletCount,
                int usbPortCount,
                int ratedVac,
                int ratedA,
                int maxPowerW,
                String usbRating,
                String sourceUrl,
                int zwaveManufacturerId,
                int zwaveProductTypeId,
                int zwaveProductId,
                String zwaveFrequencyPlan,
                String zwaveCommandClasses) {
            this.id = id;
            this.manufacturer = manufacturer;
            this.displayModel = displayModel;
            this.aliases = Collections.unmodifiableList(aliases);
            this.transport = transport;
            this.supportStatus = supportStatus;
            this.verifiedAcOutletCount = verifiedAcOutletCount;
            this.usbPortCount = usbPortCount;
            this.ratedVac = ratedVac;
            this.ratedA = ratedA;
            this.maxPowerW = maxPowerW;
            this.usbRating = usbRating == null ? "" : usbRating;
            this.sourceUrl = sourceUrl == null ? "" : sourceUrl;
            this.zwaveManufacturerId = zwaveManufacturerId;
            this.zwaveProductTypeId = zwaveProductTypeId;
            this.zwaveProductId = zwaveProductId;
            this.zwaveFrequencyPlan = zwaveFrequencyPlan == null ? "" : zwaveFrequencyPlan;
            this.zwaveCommandClasses = zwaveCommandClasses == null ? "" : zwaveCommandClasses;
        }

        public boolean requiresGateway() {
            return transport == Transport.Z_WAVE_GATEWAY;
        }
    }
}
