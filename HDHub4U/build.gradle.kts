// Ported from: https://github.com/phisher98/cloudstream-extensions-phisher/tree/master/HDhub4u
// Original author: Phisher98

version = 49

cloudstream {
    authors = listOf("indiblog", "Phisher98")
    status   = 1
    tvTypes  = listOf("Movie", "TvSeries", "Anime")
    language = "hi"
    iconUrl  = "https://raw.githubusercontent.com/phisher98/TVVVV/refs/heads/main/Icons/HDHUB.png"
}

android {
    namespace = "com.example"
}

dependencies {
    implementation(project(":library"))
}
