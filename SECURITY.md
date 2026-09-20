# Security and private-development policy

FG Link is developed as a private-source project. Official compiled releases may be distributed free of charge, but the proprietary application, gateway and backend source code are not intended for public redistribution.

## Secrets

Never commit production credentials, signing material or private network credentials.

Keep these outside the repository:
- Android signing keystores and passwords.
- VPS production `.env` files.
- Database passwords.
- JWT secrets and token peppers.
- ZeroTier network IDs, API tokens and controller credentials when they are installation-specific.
- Device Share Codes and bearer tokens.

The repository intentionally tracks only example configuration with placeholder values.

## Reporting

If a credential is accidentally committed, revoke/rotate it first, then remove it from current files and repository history as appropriate. Treat any credential that was present in public Git history as compromised.
