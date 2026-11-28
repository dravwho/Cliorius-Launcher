# Security

Report vulnerabilities privately via a GitHub Issue or by contacting the maintainer.
Please do not publish exploits before a fix is available.

The launcher never stores your Microsoft password. Sign-in happens on the official
login.live.com page inside an isolated WebView; only an access token is kept, encrypted
in the app's private storage, and it can be revoked at any time from
account.microsoft.com → Privacy → Apps and services.
