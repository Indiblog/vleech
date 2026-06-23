# vleech — CloudStream Plugin Repository

> **6 streaming provider plugins in one repo, with automatic domain self-healing.**

Maintained by **[indiblog](https://github.com/indiblog)**

---

## Install in CloudStream

**Option A — Short code** *(easiest)*
> Open CloudStream → Settings → Extensions → Add Repository → paste:
```
vleech
```

**Option B — Full URL**
```
https://raw.githubusercontent.com/indiblog/vleech/builds/plugins.json
```

---

## Included Plugins

| Plugin | Language | Type | Source |
|---|---|---|---|
| **Cinevood** | Hindi | Movies · Series | Ported from [rockhero1234/cinephile](https://github.com/rockhero1234/cinephile) — original by *Dilip* |
| **HDHub4U** | Hindi | Movies · Series · Anime | Ported from [phisher98/cloudstream-extensions-phisher](https://github.com/phisher98/cloudstream-extensions-phisher) — original by *Phisher98* |
| **Piratexplay** | Hindi | Anime · Cartoon | Ported from [phisher98/cloudstream-extensions-phisher](https://github.com/phisher98/cloudstream-extensions-phisher) — original by *Phisher98* |
| **MultiMovies** | Hindi | Movies · Series · Anime | Ported from [phisher98/cloudstream-extensions-phisher](https://github.com/phisher98/cloudstream-extensions-phisher) — original by *Phisher98* |
| **Rivestream** | English | Movies · Series · Anime | New — TMDB browse + vidsrc/autoembed embeds |
| **LordFlix** | English | Movies · Series | New — TMDB browse + vidsrc/autoembed embeds |

---

## Automatic Domain Updates

Streaming sites change domains often. This repo handles that automatically:

- A **GitHub Actions workflow runs daily at 06:00 UTC**, probing all 6 provider domains
- If a domain is dead, it tries known alternatives and updates **`domains.json`** in the builds branch
- Each provider **fetches `domains.json` at runtime** and switches to the live domain — no rebuild or app update needed
- The source `DomainConfig.kt` is also patched and committed, keeping new builds in sync

You can also trigger a manual domain check from the **Actions** tab → **Auto-Update Domains** → **Run workflow**.

---

## Structure

```
vleech/
├── library/
│   └── DomainConfig.kt       ← single source of truth for all domain strings
│   └── DomainResolver.kt     ← runtime fetch of live domains.json
├── Cinevood/
├── HDHub4U/
├── Rivestream/
├── Lordflix/
├── Piratexplay/
├── Multimovies/
└── .github/workflows/
    ├── build.yml              ← builds plugins + publishes plugins.json on push
    └── check-domains.yml      ← daily domain probe + auto-update
```

To change a provider's domain manually: edit **`library/src/main/kotlin/DomainConfig.kt`** — that one file controls all providers.

---

## Credits

- **[indiblog](https://github.com/indiblog)** — repo maintainer, Rivestream and LordFlix providers, build infrastructure
- **[Phisher98](https://github.com/phisher98)** — original HDHub4U, Piratexplay, MultiMovies providers
- **[rockhero1234 / Dilip](https://github.com/rockhero1234)** — original Cinevood provider
- **[CloudStream / recloudstream](https://github.com/recloudstream/cloudstream)** — the app and plugin framework

---

## Disclaimer

This repository contains plugins that function like a browser — they fetch and link to content hosted on third-party sites. No media files are hosted here. Use responsibly and in accordance with the laws of your country.

## License

[GPL-3.0](https://www.gnu.org/licenses/gpl-3.0.html) — see the original upstream repos for their individual licenses.
