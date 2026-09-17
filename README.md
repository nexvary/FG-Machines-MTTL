# FG Machines RCK

Android controller and interoperability research project for the LG U+ / TCL **MTTL-W01** smart power strip.

## Project goals

- Arabic/English Android controller branded **FG Machines RCK**.
- Discover MTTL-W01 devices on the local Wi-Fi network.
- Probe the device's known local TCP service on port `30300` without sending unsafe or unverified relay commands.
- Provide a clean protocol-adapter boundary so verified local-control frames can be added without rewriting the UI.
- Keep cloud/Korean-subscription dependencies optional; prioritize local LAN control when technically verified.

## Hardware baseline

- Model: `MTTL-W01`
- Wi-Fi: 2.4 GHz 802.11 b/g/n
- Four individually switched AC outlets
- Local service observed by public reverse-engineering work: TCP `30300`

## Safety

This project controls mains-powered hardware. The default Android implementation does **not** transmit guessed relay-control frames. Physical modification of the power strip is outside the Android app and should only be performed by a qualified person with the device unplugged.

## Build

```bash
./gradlew assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Status

Initial Android application scaffold and LAN discovery/probe are under active development.
