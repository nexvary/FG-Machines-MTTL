# FG Machines RCK compatibility matrix

This matrix separates **retail model**, **certificate revision**, **firmware family** and
**protocol signature**. A certificate suffix or firmware build is not treated as a new product model.

| Family / evidence | Status | FG Machines RCK handling |
|---|---|---|
| MTTL-W01 | Confirmed retail model | Primary supported family |
| HU04139-17002A | Observed MTTL-W01 safety revision | Catalogued; hardware validation pending |
| HU04139-17002B | Observed MTTL-W01 safety revision | Catalogued; matches an Egypt-market unit |
| HU04139-17002C | Observed MTTL-W01 safety revision | Catalogued; hardware validation pending |
| HU04139-17002D | Observed MTTL-W01 safety revision | Catalogued; hardware validation pending |
| HU04139-17002E | Observed MTTL-W01 safety revision | Catalogued; hardware validation pending |
| Firmware 1.0.66 | Stock/original reference | Protocol family recognised |
| Firmware 1.0.68 | Public patched/local variant | Protocol family recognised |
| Firmware 1.0.106 | Public custom telemetry variant | Protocol family recognised |
| Firmware 1.0.110 | Public standalone local variant | Protocol family recognised |
| `TONLY_TAP_*` setup AP | Confirmed family signature | Recognised; `LGU_<suffix>` password derivation |
| `ONLY_TAP_*` setup AP | Public provisioner signature | Recognised; `LGU_<suffix>` password derivation |
| Boot model `lgutap` | Confirmed runtime protocol identity | Required before outlet commands are enabled |
| Unknown sticker + valid `lgutap` boot frame | Experimental family match | Can be detected by capabilities, not claimed as a verified retail model |
| MTTL-W02 / MTTL-W03 | No credible evidence located in current review | Not claimed or hard-coded |

## Protocol capability profile

A device can enter the experimental MTTL-family path when its behavior matches the protocol
signatures rather than relying only on a sticker:

- setup AP uses a known `*_TAP_*` signature;
- setup endpoint is available at `192.168.1.1:30300`;
- normal operation connects outward to controller TCP `10086`;
- boot frame identifies model `lgutap` and has matching MAC/client ID;
- state responds to `up:getinfo:all`;
- outlets acknowledge `up:onoff:<1..4>:on|off`.

This approach is intentionally conservative: a MAC OUI or an open port alone is never sufficient to
enable mains switching.

## Public projects reviewed

- `ttaengz/mttl-w01-matterbridge` — direct local TCP controller, provisioning and Matter bridge.
- `af950833/mttl_w01` — local server, Android provisioner, firmware/telemetry work and Home Assistant integration.
- `HW-YUN/MTTL-W01-local` — real-device local stack and firmware observations.
- `sosohage2/mttl-w01-standalone-110` — standalone 1.0.110 firmware/backend and local OTA behavior.
- `omarKmekkawy/Korean_LG_TCL_MTTL-w01_power-strip` — hardware reverse engineering and revision-A evidence.
- `ommeq/voltra-home-assistant` — Voltra/Home Assistant ecosystem reference.
- Hackaday `LG Smart Power Plug MTTL-W01` — RTL8711AF, FreeRTOS, UART/I2C and physical relay research.

## Licensing rule

Protocol facts and interoperability behavior are reimplemented independently. Source files from
projects without an explicit compatible license are not copied into FG Machines RCK. Third-party
credits remain documented even when only protocol observations are used.
