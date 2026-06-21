// Ported from: https://github.com/phisher98/cloudstream-extensions-phisher/tree/master/Piratexplay
// Original author: Phisher98

version = 2

cloudstream {
    description = "Anime and Cartoon in Hindi"
    authors = listOf("indiblog", "Phisher98")
    status      = 1
    tvTypes     = listOf("AnimeMovie", "Anime", "Cartoon")
    language    = "hi"
    iconUrl     = "https://raw.githubusercontent.com/phisher98/TVVVV/refs/heads/main/Icons/piratexplay.png"
}

android {
    namespace = "com.example"
}

dependencies {
    implementation(project(":library"))
}
