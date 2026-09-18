# FG Machines RCK

Android local controller and interoperability project for the LG U+ / TCL / TONLY **MTTL-W01 family** of smart power strips.

## Current Android baseline — 0.8.0

- FG Machines black / electric-blue / neon-green / metallic-silver visual identity.
- Arabic, English, Turkish, Spanish and German with persistent in-app language selection.
- Correct RTL layout direction for Arabic.
- Compatibility catalog based on model, certificate revision, setup SSID, boot signature and firmware family.
- Setup-service diagnostics for TCP `30300`.
- Local TCP controller on port `10086` for compatible MTTL firmware.
- Validated parser for `bootinfo`, four-channel `getinfo` telemetry and outlet state events.
- Independent outlet commands `1..4` are enabled only after a peer identifies itself with a valid MTTL `lgutap` boot frame.
- Periodic local status polling and live relay-state synchronization.
- Live energy dashboard showing per-outlet and total power, reported energy, temperature, protection/event codes and manual refresh.
- Local EGP/kWh tariff setting with estimated energy cost.
- Current estimate derived from live power at nominal 220 V and clearly labelled as an estimate.
- Voltage is intentionally not fabricated: stock firmware does not expose a verified live-voltage field through the currently implemented controller protocol.
- Setup assistant now explicitly requires 2.4 GHz WPA2-Personal for the target hotspot/router, based on physical MTTL-W01 validation.
- A foreground controller service keeps TCP 10086 available after leaving the UI, preserving the local MTTL session while the phone remains the controller/hotspot.
- Local Android notifications can alert on strip disconnects, non-zero protection/event codes, and live loads above 3,000 W.
- GitHub Actions release gate: unit tests + Android lint + debug APK build.

## Compatibility catalog

The public material reviewed so far consistently identifies the retail product as `MTTL-W01`, with multiple KC/safety revisions rather than separate W02/W03 retail models.

Observed certificate revisions:

- `HU04139-17002A`
- `HU04139-17002B`
- `HU04139-17002C`
- `HU04139-17002D`
- `HU04139-17002E`

Observed firmware families:

- `1.0.66`
- `1.0.68`
- `1.0.106`
- `1.0.110`

Known setup SSID prefixes include `TONLY_TAP_` and `ONLY_TAP_`. FG Machines RCK also uses the runtime boot signature (`lgutap`) so future rebrands using the same protocol can be identified experimentally without falsely claiming an unverified sticker model is supported.

## Network architecture

There are two different local roles:

1. **Setup / provisioning:** the strip exposes an AP and local endpoint at `192.168.1.1:30300`.
2. **Normal operation:** after provisioning, compatible firmware connects outward to the configured controller on TCP `10086`. FG Machines RCK now implements that controller endpoint on Android.

Normal controller commands include:

```text
up:getinfo:all
up:onoff:1:on
up:onoff:1:off
...
up:onoff:4:on
up:onoff:4:off
```

See `docs/PROTOCOL_NOTES.md` for the research evidence, firmware distinctions and validation gates.

## Safety and validation

This project controls mains-powered hardware. FG Machines RCK does not send outlet commands to an unidentified TCP peer. A compatible device must first supply a structurally valid boot identity with matching MAC/client ID and the expected MTTL boot model.

Protocol support is tracked at two levels:

- **Open-source verified:** independently corroborated by public implementations.
- **FG hardware verified:** exercised against the exact physical hardware revision being tested.

The current software implementation has passed automated build/lint gates; physical-unit validation is a separate gate.

## Build

```bash
gradle --no-daemon clean testDebugUnitTest lintDebug assembleDebug
```

Or open the repository in Android Studio and build the `app` module.

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```
