/**
 * DomainConfig — THE SINGLE SOURCE OF TRUTH for all provider domain names.
 *
 * These constants serve as compile-time fallbacks only.
 * At runtime, DomainResolver fetches the live `domains.json` from the builds branch
 * of this repository and overrides these values when a domain has moved.
 *
 * TO ENABLE AUTOMATIC UPDATES:
 *   1. Fork / push this repo to GitHub.
 *   2. Set GITHUB_REPO below to "your-username/your-repo-name".
 *   3. The check-domains.yml workflow will update domains.json daily — no code change needed.
 *
 * RULES:
 *   - Fallback values in this file are updated by the check-domains.yml workflow
 *     when a definitive new domain is confirmed stable (belt-and-suspenders).
 *   - Never hardcode domain strings directly inside a provider file.
 */
object DomainConfig {

    /**
     * Set this to YOUR GitHub repo ("username/repo") after pushing.
     * The check-domains.yml workflow writes domains.json to the builds branch of this repo.
     */
    const val GITHUB_REPO = "indiblog/vleech"

    /** URL of the live domain registry maintained by the scheduled workflow. */
    const val DOMAINS_JSON_URL =
        "https://raw.githubusercontent.com/$GITHUB_REPO/builds/domains.json"

    // ── Fallback domains (overridden at runtime by DomainResolver) ───────────
    const val CINEVOOD    = "https://cinevood.art"
    const val HDHUB4U     = "https://hdhub4u.com"
    const val RIVESTREAM  = "https://www.rivestream.app"
    const val LORDFLIX    = "https://lordflix.org"
    const val PIRATEXPLAY = "http://piratexplay.cc"
    const val MULTIMOVIES = "https://multimovies.autos"
}
