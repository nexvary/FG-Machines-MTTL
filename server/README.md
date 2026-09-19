# FG Machines RCK Cloud / VPS

This directory contains the first deployable cloud backend for FG Machines RCK.

The design is intentionally **local-first**:

```
MTTL-W01
   │ local TCP 10086
   ▼
FG Machines RCK Android controller
   │ outbound HTTPS only
   ▼
FG Machines RCK Cloud / VPS
   │
   ├── account + Device Sharing
   ├── command relay
   ├── telemetry/history
   └── future Alexa / Google adapters
```

The VPS never talks directly to the MTTL-W01. The Android controller keeps the
verified local protocol and polls the VPS for authorized commands. This avoids
opening the phone's local TCP/HTTP ports to the public Internet.

## Implemented in v0.1

- Email/password accounts with Argon2 password hashing.
- Short-lived signed JWT access tokens.
- Per-controller high-entropy API key; only its HMAC hash is stored.
- Device claim/registration by MTTL MAC.
- Owner / Admin / Control / View authorization.
- Single-use, expiring Device Sharing invite codes.
- Cloud command queue for outlets 1–4.
- Outbound controller polling with bounded redelivery and command expiry.
- Controller ACK/failed status.
- Controller heartbeat and online/offline state.
- Telemetry upload and history retrieval.
- Voice-intent relay endpoint for future Alexa/Google/mobile adapters.
- Explicit confirmation gate before cloud voice ALL ON.
- Audit log foundation.
- PostgreSQL production storage.
- Caddy automatic HTTPS termination.
- Docker Compose deployment.

## Security boundary

Do **not** expose Android TCP 10086 or local HTTP 18086 to the public Internet.
The controller phone should make outbound HTTPS requests to this service.

Real credentials must never be committed. The public repository contains only
`.env.example`.

## Local development

```bash
cd server
python -m venv .venv
. .venv/bin/activate
pip install -r requirements.txt

export FGRCK_ENV=development
export FGRCK_DATABASE_URL=sqlite+pysqlite:///./fg_rck_cloud.db
uvicorn app.main:app --reload --port 8080
```

Open:

- API docs: `http://127.0.0.1:8080/docs`
- Health: `http://127.0.0.1:8080/healthz`

Run tests from the repository root:

```bash
PYTHONPATH=server pytest -q server/tests
```

## VPS deployment

Requirements:

- Ubuntu/Debian VPS with Docker Engine + Docker Compose plugin.
- A DNS A/AAAA record pointing `FGRCK_DOMAIN` to the VPS.
- TCP 80/443 allowed by the firewall.
- No public PostgreSQL port.

```bash
cd server
cp .env.example .env

# Edit .env and use independent random secrets.
# Example generator:
openssl rand -base64 48

docker compose pull
docker compose build
docker compose up -d
docker compose ps
```

Caddy obtains/renews the public TLS certificate automatically after DNS is
correct and ports 80/443 are reachable.

## Android controller integration

The next Android integration uses these outbound endpoints:

- `POST /api/v1/controllers/{id}/heartbeat`
- `POST /api/v1/controllers/{id}/telemetry`
- `GET /api/v1/controllers/{id}/commands/poll`
- `POST /api/v1/controllers/{id}/commands/{command_id}/ack`

The controller authenticates using:

```
X-Controller-Key: <controller key>
```

Client/account APIs continue under `/api/v1`. The existing Android
`RemoteApiClient` contract remains compatible for:

- `GET /api/v1/devices`
- `POST /api/v1/devices/{mac}/outlets/{1..4}?state=on|off`
- `GET /api/v1/history/{mac}?hours=24`

The Bearer value for the VPS is an account JWT rather than a LAN sharing token.

## Voice control

`POST /api/v1/voice/intent` is the neutral internal bridge. Alexa/Google
adapters can be added later without changing the MTTL controller protocol.
Vendor account linking/OAuth is not claimed or enabled yet.
