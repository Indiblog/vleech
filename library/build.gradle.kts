// The library module is a plain Android library that holds shared constants.
// It intentionally does NOT apply the cloudstream gradle plugin because it
// provides no Plugin class — it only contains DomainConfig.kt.
//
// The `android {}` and `kotlin-android` plugin are applied to this module
// via the root build.gradle.kts subprojects block.

android {
    namespace = "com.example.library"
}
