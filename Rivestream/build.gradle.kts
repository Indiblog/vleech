// Provider for: https://www.rivestream.app
// ARCHITECTURE NOTE: Rivestream is a React/Next.js SPA — it renders entirely client-side
// and returns no scrapable HTML for content listings. This provider uses the TMDB API for
// metadata (search, browse, detail) and constructs embed URLs pointing to the Rivestream
// watch path. If Rivestream's internal embed mechanism changes, update loadLinks() only.

version = 1

cloudstream {
    description = "Rivestream — Movies, TV, Anime, K-Drama via TMDB browse"
    authors     = listOf("indiblog")
    status      = 3   // Beta — SPA site, loadLinks requires JS embed resolution
    tvTypes     = listOf("Movie", "TvSeries", "Anime")
    language    = "en"
    iconUrl     = "https://www.rivestream.app/images/MetaImage.jpg"
}

android {
    namespace = "com.example"
}

dependencies {
    implementation(project(":library"))
}
