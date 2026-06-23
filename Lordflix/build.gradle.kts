// Provider for: https://lordflix.org
// ARCHITECTURE NOTE: Lordflix is a React SPA — same approach as Rivestream.
// See LordflixProvider.kt for full explanation.

version = 1

cloudstream {
    description = "LordFlix — Movies and TV Shows (TMDB browse, multi-embed links)"
    authors     = listOf("indiblog")
    status      = 3  // Beta — SPA site
    tvTypes     = listOf("Movie", "TvSeries")
    language    = "en"
    iconUrl     = "https://lordflix.org/favicon.ico"
}

android {
    namespace = "com.example"
}

dependencies {
    implementation(project(":library"))
}
