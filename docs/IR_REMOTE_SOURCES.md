# FG Link IR Remote Sources and Verification

The **Remote** module is intentionally limited to air conditioners and fans. TVs,
displays, projectors, home-theater equipment, receivers and TV boxes are outside
the product scope.

## Air-conditioner protocol sources

The initial Sharp / Midea / Carrier implementation was derived from the
user-supplied `Abu-Omar-Universal-AC-Remote-v4.0-ELECTRIC` project. The Sharp
A907/A903/A705 families, Midea 48-bit and Carrier AC64 encoders were integrated
as protocol engines rather than as a list of guessed model codes.

Additional protocol framing and checksum behavior was independently
cross-checked against the MIT-licensed `xMasterX/flipper_ac` implementation and
its upstream public protocol references. FG Link currently includes protocol
engines/candidates for:

- Sharp A907 / A903 / A705
- Midea 48-bit
- Carrier AC64
- Gree YAW1F / YBOFB / YX1FSF
- LG / LG2 28-bit
- Samsung AC
- Haier YR-W02
- Toshiba Generic / WA-TH0x
- TCL112
- Kelon168 / Hisense

For rebadged/OEM products such as some UnionAir, Fresh and other regional
brands, **Smart IR Scan** tests candidate protocol families one at a time and
stores a profile only after the user confirms that the physical appliance
actually responded.

## Fan sources

Fan control is handled separately from A/C state protocols. FG Link only embeds
fan button codes that came from public, concrete remote datasets:

- Kanazawa Tower Fan: `rdevz-ph/IR-Remote` (MIT), NEC 38 kHz.
- Atomberg BLDC Fan: `Mobasheera/Atomberg-BLDC-IR-Remote` (MIT), NEC 38 kHz.

Fresh fan model names are included as discovery targets because replacement
remotes are difficult to obtain, but **no unverified Fresh IR code is embedded**.
Fresh targets currently include Smart Remote 16, Shabah Remote 18, Classic
Remote 2026 and Top Remote 16. Smart Scan may associate a verified public
profile only when the physical fan responds and the user confirms it.

## Safety and verification policy

- The app checks for an Android Consumer IR emitter before transmitting.
- Candidate scans require physical confirmation before persistence.
- Unsupported buttons stay disabled/unavailable rather than sending guessed
  codes.
- Protocol unit tests validate carrier frequency, payload construction and
  basic pattern integrity; physical hardware validation remains distinct from
  CI verification.
