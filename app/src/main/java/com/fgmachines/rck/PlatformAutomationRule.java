package com.fgmachines.rck;

import java.util.EnumSet;
import java.util.Set;

/**
 * Protocol-neutral automation descriptor. Execution can stay local while rules
 * target any future driver that exposes the required capabilities.
 */
public final class PlatformAutomationRule {
    public enum TriggerType {
        MANUAL,
        TIME,
        DEVICE_STATE,
        SENSOR_THRESHOLD
    }

    public enum ActionType {
        SWITCH_CHANNEL,
        APPLY_SCENE,
        NOTIFY
    }

    public final String id;
    public final String name;
    public final TriggerType triggerType;
    public final ActionType actionType;
    public final String targetDeviceId;
    public final int targetChannel;

    public PlatformAutomationRule(String id, String name, TriggerType triggerType,
                                  ActionType actionType, String targetDeviceId, int targetChannel) {
        this.id = id == null ? "" : id.trim();
        this.name = name == null ? "" : name.trim();
        this.triggerType = triggerType;
        this.actionType = actionType;
        this.targetDeviceId = targetDeviceId == null ? "" : targetDeviceId.trim();
        this.targetChannel = targetChannel;
    }

    public Set<SmartDevice.Capability> requiredCapabilities() {
        if (actionType == ActionType.SWITCH_CHANNEL) {
            return EnumSet.of(SmartDevice.Capability.SWITCH);
        }
        if (actionType == ActionType.APPLY_SCENE) {
            return EnumSet.of(SmartDevice.Capability.SCENES);
        }
        return EnumSet.noneOf(SmartDevice.Capability.class);
    }

    public boolean isCompatible(SmartDevice device) {
        if (device == null || !device.id.equals(targetDeviceId)) return false;
        if (!device.capabilities.containsAll(requiredCapabilities())) return false;
        return actionType != ActionType.SWITCH_CHANNEL
                || (targetChannel >= 1 && targetChannel <= device.channelCount);
    }
}
