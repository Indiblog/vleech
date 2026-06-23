// Ported from: https://github.com/rockhero1234/cinephile/tree/master/Cinevood
// Original author: Dilip (rockhero1234)

version = 11

cloudstream {
    description = "Bollywood, Hollywood, Regional Hindi movies and series (unstable at times)"
    authors = listOf("indiblog", "Dilip", "rockhero1234")
    status       = 1   // 0=Down 1=Ok 2=Slow 3=Beta
    tvTypes      = listOf("Movie", "TvSeries")
    language     = "hi"
    iconUrl      = "https://i.ibb.co/nMdxZgkR/8-CM3-5q-ARZYELDWg6-Erqfg-NYpo-Sdh-Yw-HRB5-CMhf-Vgg-Ygn-PMHCVYWQf-EEJMt9gwd6-EFP5t-LYgd-LAm-Zerm-KCX.png"
}

android {
    namespace = "com.example"
}

dependencies {
    // Shared domain constants
    implementation(project(":library"))
}
