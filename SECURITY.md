# Security Policy

## Supported versions

Only the latest release receives security fixes.

| Version | Supported |
| --- | --- |
| 6.x | Yes |
| 5.x and older | No |

## Reporting a vulnerability

Please **do not** open a public issue for security problems. Instead, contact the
maintainer privately through the GitHub profile
[@dravwho](https://github.com/dravwho) and include:

- A description of the issue and its impact.
- Steps to reproduce, or a proof of concept.
- The launcher version and Android version you tested on.

You can expect an acknowledgement within a few days. Once a fix is ready it will
be released and, with your permission, you will be credited in the changelog.

## How Cliorius Launcher handles your credentials

- **Microsoft accounts.** The password is typed into the official
  `login.live.com` page inside an isolated WebView. The launcher never sees or
  stores it. Only the resulting access token is kept, in the app's private
  storage, and it can be revoked at any time from
  `account.microsoft.com` → *Privacy* → *Apps and services*.
- **Ely.by / Yggdrasil accounts.** Credentials are sent only to the
  authentication server you selected. The resulting session token is stored
  locally.
- **Offline accounts.** No credentials exist; a local UUID is generated.
- **No telemetry.** The launcher does not send analytics, device identifiers or
  usage data anywhere. Network requests go only to Mojang, Modrinth, the
  authentication server you chose, and the mirror you selected for downloads.

## Scope

Reports about the following are in scope:

- Credential or token leakage.
- Path traversal or arbitrary file write through mod, version or asset handling.
- Remote code execution through downloaded content.
- Bypass of the PIN app lock.

Issues in upstream projects (Pojav Glow-Worm, gl4es, Mesa, LWJGL) should be
reported to those projects as well.
