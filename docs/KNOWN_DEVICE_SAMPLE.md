# Verified field sample

During Android field testing, one MTTL-W01 setup AP presented as `TONLY_TAP_91C0C50`. Android showed the phone associated to the setup Wi-Fi with IPv4 `192.168.1.2`, while the earlier 0.4.0 application incorrectly reported that the phone was not connected because it used a successful TCP 30300 probe as the network-detection test.

0.4.1 corrects that diagnostic and routing logic. No device serial number or full MAC address is stored here.
