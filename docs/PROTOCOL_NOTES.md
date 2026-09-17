# MTTL-W01 protocol notes

## Verified public findings

The current Android implementation intentionally separates **device discovery** from **relay control**.

Public reverse-engineering work on the MTTL-W01 reports:

- The controller is a Realtek RTL8711AF running FreeRTOS.
- The device exposes a local TCP service on port `30300` in its Wi-Fi/AP environment.
- Port `30300` is used by the original mobile application, but a complete packet/command format has not been published in the referenced work.
- Relay state is logged over UART in the form `Update relay state, overall,r1,r2,r3,r4` where `1` means ON and `0` means OFF.
- Physical relay control was reproduced by isolating the LED-I2C bus and driving the power board with an ESP-01; this requires hardware modification and is not the default FG Machines RCK path.

Reference: https://hackaday.io/project/202043-lg-smart-power-plug-mttl-w01

## FG Machines RCK strategy

1. Discover MTTL-W01 candidates by probing TCP 30300 on the local /24 network.
2. Permit manual IP entry and a non-destructive TCP-connect probe.
3. Keep outlet toggles disabled until a relay command frame is verified on real hardware.
4. Add a protocol adapter once the LAN packet format is captured/confirmed.
5. Only consider an ESP hardware bridge as an optional fallback; it must never be presented as a software-only update.

## Next verification target

Capture the original/compatible app traffic during:

- device provisioning,
- outlet 1 ON/OFF,
- outlet 2 ON/OFF,
- outlet 3 ON/OFF,
- outlet 4 ON/OFF,
- status refresh.

A verified capture will allow implementation of the local transport without guessed mains-control commands.
