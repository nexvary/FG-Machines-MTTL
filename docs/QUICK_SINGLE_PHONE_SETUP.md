# Quick single-phone setup

1. Turn on the phone hotspot on 2.4 GHz and use **Save hotspot IP** in FG Machines RCK.
2. Turn the hotspot off.
3. Put the MTTL-W01 into setup mode and join its `TONLY_TAP_*` / `ONLY_TAP_*` Wi-Fi.
4. Return to FG Machines RCK and press the green continue button.
5. FG Machines RCK selects the actual Wi-Fi transport even if Android keeps cellular as its default network, finds the strip gateway, and provisions through TCP 30300.
6. When the app opens hotspot settings, turn the same 2.4 GHz hotspot back on and wait for the strip to connect to the phone controller on TCP 10086.
