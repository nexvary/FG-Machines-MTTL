# Single-phone setup network fix (0.4.1)

The Android sequential setup path must not use successful TCP/30300 probing as proof that the phone is connected to the strip setup Wi-Fi.

On current Android versions, a no-Internet Wi-Fi such as `TONLY_TAP_*` can coexist with cellular as the default validated network. The application therefore:

1. Finds the active Android `TRANSPORT_WIFI` network first.
2. Scores link properties, preferring the MTTL setup subnet (`192.168.1.x`) and gateway (`192.168.1.1`).
3. Temporarily binds the process to that Wi-Fi transport.
4. Discovers the setup gateway from Android route information, falling back to `192.168.1.1`.
5. Probes TCP 30300 separately with retries.
6. Sends the documented local provisioning commands only after the setup service is reachable.
7. Removes the process binding when provisioning finishes or fails.

This prevents the misleading “not connected to strip setup Wi-Fi” result when Android is actually associated with the strip AP but the initial setup-port probe failed.
