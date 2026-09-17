# FG Machines RCK 0.5.0 setup assistant

The setup screen now treats network topology as an explicit choice instead of assuming one-phone hotspot operation.

## Supported setup plans

1. **Router Wi-Fi (recommended)** — save the controller phone IPv4 while it is on the 2.4 GHz router, join the strip setup AP, provision the router credentials and controller IPv4, then reconnect the controller phone to the router.
2. **Two phones, no router (recommended)** — Phone A keeps a 2.4 GHz hotspot and FG Machines RCK running. Phone B joins the strip setup AP and provisions Phone A hotspot credentials plus Phone A controller IPv4.
3. **One phone hotspot (experimental)** — retained only for Android devices that can perform the required Wi-Fi/hotspot transition reliably.

The physical MTTL-W01 still requires hardware validation after CI. Android CI validates unit tests, lint, and APK compilation.
