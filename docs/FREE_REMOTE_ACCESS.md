# FG Machines RCK — Free Remote Access

Version 1.5.0 supports two no-subscription remote access paths. Both keep the MTTL-W01 control session local: the strip connects to FG Machines RCK on TCP 10086, while remote clients use the authenticated RCK API on TCP 18086 through a private ZeroTier path.

## Mode A — ZeroTier on the controller phone

Install and join ZeroTier on the Android phone that runs FG Machines RCK. In **Free Remote Access**, select the phone mode and enter the controller phone's private ZeroTier address. RCK builds an endpoint such as:

`http://10.x.x.x:18086`

Create a Device Share token/code after this profile is saved. The generated Share Code will prefer the ZeroTier endpoint.

## Mode B — ZeroTier/OpenWrt router gateway

Run ZeroTier on the site's router/access point and route the site's LAN through the private overlay. In RCK, select router gateway mode and enter the controller phone's LAN address, for example:

`http://192.168.10.120:18086`

The ZeroTier network must have a managed route for that LAN subnet via the router gateway. No public port forwarding is needed.

### Hardware note

The LG GAPM-7100 shown in the project lab reports RTL8198C rev B, 128 MiB RAM and Linux 3.10.90. It is treated as an experimental OpenWrt target. Do not flash a generic RTL8198C image. Use only an image verified for the exact board/partition layout and keep a recovery path before writing flash.

## Security boundary

- Remote API: TCP 18086 with Bearer token and device-scoped roles.
- MTTL controller: TCP 10086 stays local.
- Plain HTTP is accepted only for trusted private/VPN addresses.
- Public HTTP endpoints are rejected.
- VPS/Cloud remains optional and is not required by either free mode.
