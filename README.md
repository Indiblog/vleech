# vleech — CloudStream Plugin Repo

6 streaming plugins for [CloudStream 3](https://github.com/recloudstream/cloudstream).

## Install

In CloudStream → **Settings** → **Extensions** → **Add repository**, paste:

```
https://raw.githubusercontent.com/indiblog/vleech/builds/plugins.json
```

## Plugins

| Plugin | Language | Source |
|--------|----------|--------|
| Cinevood | Hindi | cinevood.art |
| HDHub4U | Hindi | hdhub4u.com |
| Lordflix | English | lordflix.org |
| Multimovies | Hindi | multimovies.autos |
| Piratexplay | Hindi | piratexplay.cc |
| Rivestream | English | rivestream.app |

## How it works

- All plugins resolve their live domain at runtime via a shared `DomainConfig.kt` — update one file to fix all broken domains at once.
- Plugins use TMDB API for metadata where the source site is a React SPA (Lordflix, Rivestream).
- Video links are extracted via site iframes where available, with vidsrc.cc and autoembed.co as fallbacks.

## Auto-update

Plugins rebuild automatically on every push to `main`.  
Built `.cs3` files and `plugins.json` live in the [`builds`](../../tree/builds) branch.

## Domain updates

If a source site moves to a new domain, edit [`library/src/main/kotlin/DomainConfig.kt`](src/main/kotlin/DomainConfig.kt) on `main` and push — all 6 plugins pick up the change on next install/update.
