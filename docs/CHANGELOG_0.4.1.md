# FG Machines RCK 0.4.1

- Fixes false “not connected to strip setup Wi-Fi” failures on Android when the phone is actually connected to `TONLY_TAP_*`/`ONLY_TAP_*`.
- Selects the Android Wi-Fi transport independently of cellular/default Internet routing.
- Prefers the known MTTL setup subnet and gateway, with route-based gateway discovery.
- Separates Wi-Fi-association detection from TCP 30300 service probing.
- Adds retry logic and clearer diagnostics when the setup service itself is unavailable.
- Temporarily binds the application process to the strip Wi-Fi during sequential provisioning and releases it afterward.
