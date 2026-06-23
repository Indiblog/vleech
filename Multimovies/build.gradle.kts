// Ported from: https://github.com/phisher98/cloudstream-extensions-phisher/tree/master/MultiMoviesProvider
// Original author: Phisher98

version = 50

cloudstream {
    description = "Indian Multi-language HD Provider — Bollywood, Hollywood, South Indian, Anime"
    authors = listOf("indiblog", "Phisher98")
    status      = 1
    tvTypes     = listOf("Movie", "TvSeries", "Anime")
    language    = "hi"
    iconUrl     = "https://raw.githubusercontent.com/LikDev-256/likdev256-tamil-providers/master/MultiMoviesProvider/icon.png"
}

android {
    namespace = "com.example"
}

dependencies {
    implementation(project(":library"))
}
