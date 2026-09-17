# Why 0.4.0 failed on the field phone

The phone was visibly associated with the strip setup AP and had IPv4 `192.168.1.2`. Version 0.4.0 nevertheless reported that the phone was not connected.

The cause was diagnostic coupling: `findSetupWifiNetwork()` only returned a Wi-Fi network if a TCP connection to the setup port succeeded during the same detection step. On Android, the strip AP has no Internet and cellular may remain the default validated transport. A failed/slow setup-port probe therefore became a false "not connected" diagnosis.

Version 0.4.1 separates these concerns: first select the real Wi-Fi transport from Android link properties, then bind to it, then probe the setup service with retries and report a distinct error if that service is unavailable.
