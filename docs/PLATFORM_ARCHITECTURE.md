# FG Link Smart Home Platform Architecture

FG Link is being restructured from a single-product MTTL controller into a local-first smart-home platform.

## Core rule

Protocol-specific code must live behind a `DeviceDriver`. UI, room organization, sharing and future automation should work with `SmartDevice` and capabilities instead of hard-coding a specific transport.

## Current production driver

`MttlDeviceDriver` wraps the already verified local MTTL control path:

- Device family: MTTL-W01
- Controller transport: TCP 10086
- Local API / sharing: TCP 18086
- Four verified AC switch channels
- Power, energy and temperature telemetry
- Scenes, local automation and ZeroTier-based remote access remain available

The MTTL protocol implementation itself is not rewritten by this refactor.

## Platform layers

1. **SmartDevice** — protocol-neutral device identity, category, support level, room, channel count and capabilities.
2. **DeviceDriver** — protocol adapter contract for switching, refresh and online state.
3. **DeviceDriverRegistry** — resolves a device to its driver without adding branches throughout MainActivity.
4. **PlatformDeviceStore** — persistent generic registry for future non-MTTL devices.
5. **SmartHomePlatform** — facade that merges the legacy MTTL fleet with future protocol-neutral devices.
6. **PlatformAutomationRule** — first protocol-neutral automation descriptor using capabilities instead of product names.

## Product families reserved by the architecture

- FG Smart Relay
- FG IR Link
- FG Sensor Hub
- FG Energy Monitor
- FG Room Controller
- FG Gateway
- FG Smart Panel

These entries are architecture targets only. A `PLANNED` support level is not a claim that the hardware is already supported.

## Migration strategy

Existing MTTL behavior stays operational while control entry points move gradually through `SmartHomePlatform`. New hardware should be added by implementing a driver and registering devices in `PlatformDeviceStore`, not by copying the MTTL screens or creating a separate application.

The next structural migrations are:

- generic telemetry model,
- generic automation trigger/condition/action execution,
- room-centric device UI,
- scenes spanning multiple driver types,
- driver-specific setup plugins,
- optional gateway services for MQTT / ESPHome / Tasmota / OpenBeken-class integrations where protocol support is verified.
