# FG Link Gateway for KT708 / OpenWrt

This component turns a ZeroTier-capable OpenWrt access point into an always-on FG Link gateway for MTTL-W01 smart power strips.

## Architecture

Remote Android phone (FG Link)
→ ZeroTier encrypted overlay
→ KT708 / OpenWrt
→ FG Link Gateway API (TCP 18086)
→ MTTL controller service (TCP 10086)
→ MTTL-W01 power strip

The gateway implements the same remote API contract already used by `RemoteApiClient` in the Android app:
- `GET /api/v1/health`
- `GET /api/v1/devices`
- `POST /api/v1/devices/{mac}/outlets/{1..4}?state=on|off`
- `GET /api/v1/history/{mac}` (empty history in the first gateway version)

A bearer token is mandatory.

## Before the router arrives

No destructive firmware assumption is made. KT GiGA WiFi Wave2 devices exist in more than one hardware family, so the supplied `kt708-preflight.sh` detects architecture, LAN interfaces, ZeroTier state, memory and storage first.

## First boot workflow

1. Connect the KT708 to an isolated test LAN.
2. Run `gateway/scripts/kt708-preflight.sh`.
3. Record:
   - exact model / board;
   - `uname -m`;
   - LAN bridge and LAN IP;
   - ZeroTier interface and assigned ZeroTier IP;
   - free flash/RAM.
4. Join the router to the FG Link ZeroTier network and authorize it.
5. Set `api_bind` to the router's ZeroTier IP on port 18086.
6. Generate a random 32+ character bearer token and put it in `/etc/config/fg-link-gateway`.
7. Install the matching static gateway binary and init script.
8. Enable/start the service.
9. Configure/re-provision MTTL-W01 so its controller address points to the KT708 LAN IP and controller port 10086.
10. In FG Link on the remote phone, use `http://<KT708-ZeroTier-IP>:18086` with the same bearer token.
11. Test health, device list, outlet 1 ON/OFF, then all outlets individually.

## Security rules

- Do not expose TCP 18086 to the public WAN.
- Bind the API to the ZeroTier IP, not `0.0.0.0`, once the ZeroTier address is known.
- Keep TCP 10086 reachable only from the LAN used by the MTTL devices.
- Use a unique bearer token per installation.
- Do not enable cloud/VPS merely to make this gateway work; it is designed for private ZeroTier access.

## Build

```sh
cd gateway/fg-link-gateway
go test ./...
CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -trimpath -ldflags="-s -w" -o fg-link-gateway
```

The GitHub workflow builds amd64, arm64, armv7, mips and mipsle artifacts. The correct binary is selected only after the KT708 preflight identifies the actual CPU architecture.
