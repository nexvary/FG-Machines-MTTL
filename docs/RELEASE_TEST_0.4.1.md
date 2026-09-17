# 0.4.1 hardware test

Hardware validation target:

- Android is manually connected to an MTTL setup AP and receives a `192.168.1.x` address.
- Cellular may remain present as Android's validated Internet transport.
- FG Machines RCK must still select the Wi-Fi transport, resolve the setup gateway, and contact TCP 30300.
- A TCP 30300 failure must be reported as a setup-service failure, not as a false Wi-Fi-disconnected error.
- On successful provisioning, the process network binding must be released before the phone hotspot is restarted.

CI can validate compilation, lint and unit tests. The final transport/provisioning check requires the physical MTTL-W01.
