/**
 * DomainResolver — fetches the live domains.json from the builds branch at runtime.
 *
 * HOW IT WORKS:
 *   1. On first provider call, resolveBlocking() downloads domains.json from GitHub.
 *   2. The result is cached in memory for 1 hour (TTL_MS).
 *   3. If the fetch fails (network error, 404, etc.) the DomainConfig fallback is used.
 *   4. The check-domains.yml workflow updates domains.json daily in the builds branch,
 *      so providers transparently pick up new domains without a rebuild or app update.
 *
 * THREADING:
 *   resolveBlocking() makes a blocking HTTP call — always call it from a
 *   suspend function via withContext(Dispatchers.IO).  Each provider wraps
 *   it in a private ensureDomain() suspend function for this reason.
 *
 * DEPENDENCIES:
 *   Only standard Java library (java.net). Safe to place in the `library` module
 *   which does not have the cloudstream dependency.
 */
object DomainResolver {

    private val cache = mutableMapOf<String, String>()
    private var lastFetchMs = 0L
    private const val TTL_MS = 3_600_000L   // re-fetch every 1 hour

    /**
     * Returns the live URL for [key] (e.g. "CINEVOOD"), falling back to
     * [default] (a DomainConfig constant) if domains.json is unreachable.
     *
     * Must be called from a background thread or inside withContext(Dispatchers.IO).
     */
    fun resolveBlocking(key: String, default: String): String {
        val now = System.currentTimeMillis()
        if (cache.isNotEmpty() && now - lastFetchMs < TTL_MS) {
            return cache[key] ?: default
        }
        return try {
            val conn = java.net.URL(DomainConfig.DOMAINS_JSON_URL).openConnection()
                    as java.net.HttpURLConnection
            conn.connectTimeout = 6_000
            conn.readTimeout    = 6_000
            conn.setRequestProperty("User-Agent", "CloudStreamPlugin/1.0")
            conn.setRequestProperty("Cache-Control", "no-cache")

            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().readText()
                parseJson(text).also {
                    cache.clear()
                    cache.putAll(it)
                    lastFetchMs = now
                }[key] ?: default
            } else {
                default
            }
        } catch (_: Exception) {
            default  // Network error, timeout, 404 — fall back silently
        }
    }

    /** Invalidate the cache so the next call re-fetches immediately. */
    fun invalidate() {
        cache.clear()
        lastFetchMs = 0L
    }

    // ── Minimal JSON parser ────────────────────────────────────────────────────
    // Parses a flat {"KEY":"value",...} object without external JSON libraries.
    // The domains.json produced by check-domains.yml always matches this shape.
    private fun parseJson(json: String): Map<String, String> =
        Regex(""""(\w+)"\s*:\s*"([^"\\]+)"""")
            .findAll(json)
            .associate { it.groupValues[1] to it.groupValues[2] }
}
