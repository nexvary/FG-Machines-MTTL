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

- `HU04139-17002B`
- `HU04139-17002C`
- `HU04139-17002D`
- `HU04139-17002E`

Firmware families seen in public local-control work include:

- `1.0.66` — stock/original reference
- `1.0.68` — patched/direct-local variant in one public project
- `1.0.106` — custom telemetry-capable variant reported by another local-control project

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
- https://github.com/ommeq/voltra-home-assistant
- https://hackaday.io/project/202043-lg-smart-power-plug-mttl-w01

## Hardware validation gate

The protocol implementation now exists in FG Machines RCK, but support is described in two levels:

- **Open-source verified:** command/response behavior is corroborated by public implementations.
- **FG hardware verified:** behavior has been exercised against the specific physical revision under
  test and its results recorded.

Until the second gate is complete, the app accepts a device only after a valid `lgutap` boot frame,
and it never sends relay commands to an unidentified TCP peer.
