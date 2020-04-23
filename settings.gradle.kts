rootProject.name = "cvauto"

for(module in arrayOf("core", "desktop", "android")) {
    include(":modules:${rootProject.name}-$module")
}

includeBuild("./BoofCV")


