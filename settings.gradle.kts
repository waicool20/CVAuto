rootProject.name = "cvauto"

for (module in arrayOf("core", "desktop", "android")) {
    include(":modules:$module")
}
