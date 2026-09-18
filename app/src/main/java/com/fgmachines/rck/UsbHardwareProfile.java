package com.fgmachines.rck;

/**
 * Hardware capability policy for the known MTTL-W01 design.
 *
 * Public reverse-engineering evidence shows four relay channels on the Power PCB.
 * The two USB-A connectors live on a separate charger PCB and no independent USB
 * relay/control channel has been verified. Therefore the app must never invent
 * channel 5/6 commands.
 */
public final class UsbHardwareProfile {
    private UsbHardwareProfile() {}

    public static final int USB_PORT_COUNT = 2;
    public static final int VERIFIED_RELAY_CHANNEL_COUNT = 4;

    public enum ControlMode {
        SHARED_CHARGER_NO_VERIFIED_SWITCH,
        INDEPENDENT_CONTROL_VERIFIED
    }

    public static ControlMode modeForModel(String model) {
        if (model == null) return ControlMode.SHARED_CHARGER_NO_VERIFIED_SWITCH;
        String normalized = model.trim().toUpperCase();
        if ("MTTL-W01".equals(normalized) || "LGUTAP".equals(normalized)) {
            return ControlMode.SHARED_CHARGER_NO_VERIFIED_SWITCH;
        }
        return ControlMode.SHARED_CHARGER_NO_VERIFIED_SWITCH;
    }

    public static boolean canSendIndependentUsbCommand(String model, int usbPort) {
        if (usbPort < 1 || usbPort > USB_PORT_COUNT) return false;
        return modeForModel(model) == ControlMode.INDEPENDENT_CONTROL_VERIFIED;
    }

    public static String evidenceSummary() {
        return "MTTL-W01: four verified relay channels; USB is a separate charger board with no verified independent relay/control command.";
    }
}
