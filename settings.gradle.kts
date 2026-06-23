rootProject.name = "CloudstreamPlugins"

// Auto-include every directory that contains a build.gradle.kts file.
// This covers the `library` shared module as well as all 6 plugin modules.
// Add module names to `disabled` to exclude them from the build.
val disabled = listOf<String>()

File(rootDir, ".").eachDir { dir ->
    if (!disabled.contains(dir.name) && File(dir, "build.gradle.kts").exists()) {
        include(dir.name)
    }
}

fun File.eachDir(block: (File) -> Unit) {
    listFiles()?.filter { it.isDirectory }?.forEach { block(it) }
}
