# FG Machines RCK

Android local controller and interoperability project for the LG U+ / TCL / TONLY **MTTL-W01 family** of smart power strips.

## Current Android baseline — 1.5.0

- FG Machines black / electric-blue / neon-green / metallic-silver visual identity.
- Arabic, English, Turkish, Spanish and German with persistent in-app language selection.
- Correct RTL layout direction for Arabic.
- Compatibility catalog based on model, certificate revision, setup SSID, boot signature and firmware family.
- Product-level hardware catalog now identifies the Dawon DNS `MTD-01 / PM-M130-ZW` family separately from MTTL. It is routed to a Z-Wave gateway/controller path rather than the MTTL TCP scanner; direct phone-only control is not claimed until physical Z-Wave inclusion is verified.
- Setup-service diagnostics for TCP `30300`.
- Local TCP controller on port `10086` for compatible MTTL firmware.
- Validated parser for `bootinfo`, four-channel `getinfo` telemetry and outlet state events.
- Independent outlet commands `1..4` are enabled only after a peer identifies itself with a valid MTTL `lgutap` boot frame.
- USB1/USB2 are represented explicitly as charging ports, but independent USB switching remains disabled because only four AC relay channels are verified. Passive USB hardware discovery records unknown inbound frames without sending guessed channel 5/6 commands.
- Periodic local status polling and live relay-state synchronization.
- Live energy dashboard showing per-outlet and total power, reported energy, temperature, protection/event codes and manual refresh.
- Local EGP/kWh tariff setting with estimated energy cost.
- Current estimate derived from live power at nominal 220 V and clearly labelled as an estimate.
- Voltage is intentionally not fabricated: stock firmware does not expose a verified live-voltage field through the currently implemented controller protocol.
- Setup assistant now explicitly requires 2.4 GHz WPA2-Personal for the target hotspot/router, based on physical MTTL-W01 validation.
- A foreground controller service keeps TCP 10086 available after leaving the UI, preserving the local MTTL session while the phone remains the controller/hotspot.
- Physical FG validation on an MTTL-W01 running firmware `1.0.66-0.1.54` confirmed that all four AC outlets remain controllable over the local Wi-Fi LAN with the router WAN/Internet link disconnected. The UI now hides the strip's ephemeral TCP source port and shows the stable local path to controller TCP `10086`.
- 1.3.9 Automation UX replaces manual numeric typing with wheel pickers for auto-off, low-consumption cutoff, power-limit cutoff and Away Mode intervals; schedule times use a native time picker.
- 1.4.0 Professional UI Refresh introduces a calmer black/navy surface system, compact brand header, semantic connection colors, stateful per-outlet cards, clearer picker affordances, and a consistent selected-state bottom navigation.
- 1.5.0 Free Remote Access adds two zero-subscription remote paths: ZeroTier directly on the controller phone, or a ZeroTier/OpenWrt router gateway that routes the site LAN. Both reuse the authenticated local API on TCP 18086 and keep MTTL control local on TCP 10086.
- Free remote profiles deliberately accept only private/VPN HTTP endpoints. Public Internet HTTP is rejected; no port-forwarding is required.
- Device Share Codes prefer the configured free remote endpoint, so a client can import one code and use the same authenticated device scope over ZeroTier.
- Router gateway flashing is not automated by the Android app. LG GAPM-7100 / RTL8198C rev B remains an experimental hardware target and requires a board-verified OpenWrt image before any flash is attempted.
- Outlet cards now visually react to ON/OFF state while preserving the verified MTTL local control path; offline cards are visually de-emphasized rather than left looking active.
- The dashboard connection panel is condensed to user-facing LOCAL status information while keeping the technical TCP 10086 path available in the detail line.
- Numeric automation values are range-gated in the UI and again when saving: auto-off 1–720 min, low-consumption 1–100 W for 1–120 min, and power cutoff 100–3500 W.
- Local controller reliability now remembers whether the controller service is wanted, restores it after device reboot/app update, keeps the foreground service detached from the UI task, and retries TCP 10086 startup through a bounded background watchdog.
- Local Android notifications can alert on strip disconnects, non-zero protection/event codes, and live loads above 3,000 W.
- Per-outlet local automation: auto-off timers, standby low-power cutoff, watt-limit cutoff and ON/OFF schedules stored on the controller phone and executed by the foreground controller service.- Local Away Mode can randomly toggle only explicitly selected outlets inside a configured time window, with no cloud dependency.- Per-outlet local runtime statistics track observed ON time and relay transition count on the controller phone.- Event history now labels automation, Away Mode, relay, scene and alert events more clearly and shows a longer recent-event list.
- Schedule day modes are explicit: every day, Sun–Thu, or Fri–Sat. Time values use 24-hour HH:mm format.
- Multi-device fleet registry with persistent device names, rooms, selected device, firmware, last-seen and connection uptime.
- Room filtering turns room metadata into an actual fleet organization layer instead of a single free-text label.
- Local SQLite telemetry/event history with daily, weekly and monthly energy summaries plus an in-app power sparkline.
- Appliance Diagnostics includes ten guided, source-aware flows across Samsung washers, Roborock S7, Xiaomi Mi Robot Vacuum-Mop and LG air conditioners. The offline diagnostic catalog additionally supports Sharp air conditioners using Sharp's official main/sub error-code list and general external-check guidance.
- Diagnostics can display a read-only **live electrical context** from the selected MTTL-W01 outlet: relay state, live power, energy, temperature, overload, overheat and firmware event code. These measurements are evidence only and are not presented as a failure probability.
- Configurable alerts for high load, temperature and daily energy, while firmware protection/event codes remain authoritative.
- Per-outlet automation also supports automatic cutoff above a user-defined watt threshold.
- Mandatory Setup Guard blocks provisioning until 2.4 GHz + WPA2-Personal is confirmed and the WPA2 passphrase is structurally valid.
- Authenticated local HTTP API on port `18086` with View / Control / Admin sharing roles; only token hashes are stored on the controller. Device Sharing tokens can now be scoped to one selected MTTL-W01 instead of exposing the whole fleet.
- Device Share Codes package the LAN/VPN endpoint, bearer token, role and selected-device scope so another FG Machines RCK phone can import access without a cloud account.
- Local in-app Voice Control supports safe outlet commands in Arabic, English, Turkish, Spanish and German. The app requests offline Android speech recognition when available; turning all four outlets ON requires explicit confirmation.
- Cloud / Remote Control is VPS-ready: a Device Share Code can be used now on LAN/VPN, while a future HTTPS VPS can use the same client contract. No cloud service is claimed until the VPS/backend exists.
- Home Assistant custom integration under `home_assistant/custom_components/fg_machines_rck` exposes each outlet as an independent switch plus power, energy and temperature sensors.
- Local-first behavior remains the default: outlet control, automation, history and alerts do not require a subscription or external cloud.
- Emergency fleet shutdown shortcuts can turn off every outlet in the selected room or all currently connected strips, with an explicit confirmation gate.
- History can be exported to CSV through Android's Storage Access Framework without requesting broad storage permissions.
- The local API now accepts clients only from loopback, private LAN/link-local ranges, IPv6 unique-local ranges and private CGNAT/VPN space.
- Remote Control rejects public plain-HTTP endpoints; public endpoints must use HTTPS while private LAN/VPN HTTP remains supported.
- Local per-device scenes store the four outlet states as reusable presets and require confirmation before applying.
- Historical energy cost is calculated for today, the current week and current month from locally recorded kWh and the user tariff.
- GitHub Actions release gate: unit tests + Android lint + isolated debug APK build.
- Production-package APKs use a **private stable signing key** supplied only through local environment variables or GitHub Actions secrets; signing keys and passwords are never committed to this public repository.
- Debug CI builds now use the separate package ID `com.fgmachines.rck.debug`, so a runner-generated debug certificate can never block or impersonate updates to the production package `com.fgmachines.rck`.

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

3. **Local API / Home Assistant:** the controller phone exposes an authenticated API on TCP `18086` for trusted LAN/VPN clients. Access tokens are created in the app and only their SHA-256 hashes are retained.
4. **Device Sharing / Remote Control:** another FG Machines RCK installation can import a scoped Device Share Code and use the same API over a trusted LAN/private VPN. A future HTTPS VPS can preserve this client contract. Direct public exposure of the phone's plain HTTP port is not recommended.

Normal controller commands include:

```text
up:getinfo:all
up:onoff:1:on
up:onoff:1:off
...
up:onoff:4:on
up:onoff:4:off
```

See `docs/PROTOCOL_NOTES.md` for the research evidence, firmware distinctions and validation gates. See `docs/CLOUD_VPS_CONTRACT.md` for the Device Sharing and future VPS/cloud boundary.

## Safety and validation

This project controls mains-powered hardware. FG Machines RCK does not send outlet commands to an unidentified TCP peer. A compatible device must first supply a structurally valid boot identity with matching MAC/client ID and the expected MTTL boot model.

Protocol support is tracked at two levels:

- **Open-source verified:** independently corroborated by public implementations.
- **FG hardware verified:** exercised against the exact physical hardware revision being tested.

The current software implementation has passed automated build/lint gates; physical-unit validation is a separate gate.

## Build

```bash
gradle --no-daemon clean testDebugUnitTest lintDebug assembleDebug

# Stable release (requires the four FG_RCK signing environment variables)
gradle --no-daemon assembleRelease
```

Or open the repository in Android Studio and build the `app` module.

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```
