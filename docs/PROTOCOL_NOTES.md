# MTTL family protocol notes

## Status

The original assumption that TCP `30300` was the normal relay-control endpoint is no longer used.
Multiple public projects now provide enough independently reviewable information to implement a
software-only local controller for the MTTL-W01 family without guessing mains-control bytes.

This repository reimplements the protocol facts independently. It does **not** copy source code
from repositories that do not publish a clear software license.

## Two distinct local transports

### 1. Setup / provisioning AP

Known setup SSIDs:

- `TONLY_TAP_XXXXXXX`
- `ONLY_TAP_XXXXXXX`

Known setup password derivation:

- `LGU_XXXXXXX`, using the suffix from the setup SSID.

Known setup endpoint:

- Address: `192.168.1.1`
- TCP port: `30300`

Two provisioning dialects are publicly documented:

1. Text command firmware:
   - `up:ip:<controller-ip>`
   - `up:connect:<home-ssid>:<home-password>`
   - `up:reboot:0`
2. Binary AP-mode firmware using header `LGAPMODE0010` and numeric commands for device info,
   Wi-Fi configuration and reboot.

FG Machines RCK keeps both dialects in the compatibility plan so that firmware differences can be
handled without treating a failed text command as proof that the device is unsupported.

## 2. Normal controller session

After provisioning, compatible firmware initiates an outbound TCP connection to the configured
controller. Public local-control implementations use TCP port `10086` for this controller service.

Identification frame:

```text
up:bootinfo:<model>;<mac>;<client-id>;<firmware>;connect
```

The documented boot model is `lgutap`. The MAC and client ID are expected to match.

State request:

```text
up:getinfo:all
```

Outlet control:

```text
up:onoff:1:on
up:onoff:1:off
...
up:onoff:4:on
up:onoff:4:off
```

Acknowledgements may echo the command. Firmware may also emit:

```text
up:event:onoff:<1..4>:<on|off>
```

The getinfo payload exposes, per channel, relay state, overload/overheat flags, instantaneous power,
cumulative energy, temperature and additional status/configuration fields. Some firmware can split a
long getinfo record across CRLF boundaries, so the FG parser permits safe bounded reassembly.

## Compatibility evidence

Public and certification material consistently identifies the retail model as `MTTL-W01`. The
following safety-certificate revisions have been observed while retaining the same model number:

- `HU04139-17002A`
- `HU04139-17002B`
- `HU04139-17002C`
- `HU04139-17002D`
- `HU04139-17002E`

Firmware families seen in public local-control work include:

- `1.0.66` — stock/original reference
- `1.0.68` — patched/direct-local variant in one public project
- `1.0.106` — custom telemetry-capable variant reported by another local-control project
- `1.0.110` — standalone local firmware with push state events and local OTA support

No credible public evidence reviewed so far establishes separate retail models named MTTL-W02 or
MTTL-W03. FG Machines RCK therefore uses capability/signature detection in addition to the sticker
model, allowing future rebrands using the same protocol to be added without falsely claiming they
are already verified.

## Discovery hints, not identity proof

Observed manufacturer MAC prefixes include `88:D0:39` and `2C:E0:32`. These are useful hints only;
OUI ownership is not sufficient to identify an MTTL device.

## Public references reviewed

- https://github.com/ttaengz/mttl-w01-matterbridge
- https://github.com/af950833/mttl_w01
- https://github.com/HW-YUN/MTTL-W01-local
- https://github.com/sosohage2/mttl-w01-standalone-110
- https://github.com/omarKmekkawy/Korean_LG_TCL_MTTL-w01_power-strip
- https://github.com/ommeq/voltra-home-assistant
- https://hackaday.io/project/202043-lg-smart-power-plug-mttl-w01

## Hardware validation gate

The protocol implementation now exists in FG Machines RCK, but support is described in two levels:

- **Open-source verified:** command/response behavior is corroborated by public implementations.
- **FG hardware verified:** behavior has been exercised against the specific physical revision under
  test and its results recorded.

Until the second gate is complete, the app accepts a device only after a valid `lgutap` boot frame,
and it never sends relay commands to an unidentified TCP peer.


## USB hardware status

The MTTL-W01 has two USB-A charging ports in addition to the four AC outlets.

As of the 1.3.1 hardware-discovery work, independent USB switching is **not verified** and FG Machines RCK deliberately does not invent channel 5/6 commands.

Evidence reviewed:

- Public reverse-engineering of the MTTL-W01 identifies four latching AC relays on the Power PCB and reports relay state as overall status + Relay1..Relay4.
- The same teardown shows a separate USB PCB with two USB-A connectors, a charger IC and likely voltage regulation, with no identified USB relay.
- The verified local protocol currently exposes relay commands and state for channels 1..4 only.

FG Machines RCK therefore treats USB1/USB2 as charger ports with no independently verified ON/OFF command. A passive **USB Hardware Discovery** window can be started from the app for a connected strip. During that window the controller records only otherwise-unparsed frames already sent by the strip; it does not transmit guessed USB commands.

External research references:
- https://hackaday.io/project/202043/logs
- https://manuals.plus/asin/B0DW45483F


## Dawon DNS MTD-01 / PM-M130-ZW

A physical unit supplied for FG testing carries these non-unique product identifiers:

- Sticker model: `MTD-01`
- Manufacturer: Dawon DNS Co., Ltd. / (주)다원디엔에스
- Safety approval: `JH04151-17006`
- Wireless conformity marking: `MSIP-CMM-DaW-PM-M130-ZW`
- AC outlet rating shown on the label: 250 V~, 16 A, 3500 W
- USB output shown on the label: DC 5 V / 2 A, two ports (shared output behavior)
- Label date: 2017.12

The unit-specific barcode/serial is intentionally not stored in this public repository.

The Korea Energy Agency standby-power database lists Dawon DNS model `PM-M130-ZW`, completed
2017-04-07, as an automatic standby-power-cutoff power strip with a wireless communication interface:
https://eep.energy.or.kr/electricity/elec_view_234.aspx?no=234170061

Dawon DNS has certified Z-Wave products using manufacturer ID `0x018C`, and its current product
history/catalog documents Z-Wave smart-plug families. Based on the `-ZW` model family and vendor
ecosystem, FG Machines RCK classifies this device into a **Z-Wave gateway/controller path** rather
than the MTTL local TCP path.

The Z-Wave Alliance certification for MTD-01 identifies manufacturer `0x018C`, product type
`0x0042`, product ID `0x0007`, Korean frequencies 920.90 / 921.70 / 923.10 MHz, and command
classes including Switch Binary v1, Meter v3 and Security S0.

FG Machines RCK 1.3.3 implements a real gateway control path for this model through the authenticated
Home Assistant REST API. The Android app can discover `switch.*` entities from a Home Assistant /
Z-Wave JS gateway, rank likely Dawon/MTD-01 entities, invoke `switch.turn_on` /
`switch.turn_off`, and read the resulting entity state.

This does not remove the physical Z-Wave-radio requirement: the gateway/controller must use a
KR-compatible Z-Wave radio. The phone itself is not treated as a Z-Wave controller. Physical
inclusion against the photographed unit remains a separate hardware-validation gate, but the
software path now sends standards-based switch commands through the gateway instead of merely
identifying the model.

References:
- https://products.z-wavealliance.org/z-wave-product/power-manager-5/
- https://eep.energy.or.kr/electricity/elec_view_234.aspx?no=234170061
- https://products.z-wavealliance.org/z-wave-product/smartplug-10a-2/
- https://dawondns.com/new/03_about_eng/
