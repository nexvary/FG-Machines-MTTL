# Open-source MTTL setup research

FG Machines RCK uses independently implemented local-control logic informed by public protocol research.

Useful public references reviewed during the 0.4.x work include:

- `ttaengz/mttl-w01-matterbridge`: documents stock-firmware local TCP control, TCP 10086 for the device-to-controller session, and a setup utility that writes a local controller/server IPv4 plus Wi-Fi credentials through the strip setup AP.
- `af950833/mttl_w01`: documents the MTTL-W01 setup AP and alternative provisioning behavior, including the `192.168.1.1:30300` setup endpoint.

The Android implementation does not assume that Android's default network is Wi-Fi. During sequential setup it explicitly selects the active Wi-Fi transport because Android can keep cellular as the validated default route when the strip AP has no Internet connection.
