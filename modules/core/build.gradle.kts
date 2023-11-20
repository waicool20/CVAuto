dependencies {
    val platforms = listOf(
        "linux-x86_64",
        "macosx-x86_64",
        "windows-x86_64"
    )
    val javacppVersion = "1.5.9"
    platforms.forEach { p ->
        api("org.bytedeco:javacpp:$javacppVersion")
        api("org.bytedeco:javacpp:$javacppVersion:$p")
        api("org.bytedeco:opencv:4.7.0-$javacppVersion")
        api("org.bytedeco:opencv:4.7.0-$javacppVersion:$p")
        api("org.bytedeco:openblas:0.3.23-$javacppVersion")
        api("org.bytedeco:openblas:0.3.23-$javacppVersion:$p")
        api("org.bytedeco:ffmpeg:6.0-$javacppVersion")
        api("org.bytedeco:ffmpeg:6.0-$javacppVersion:$p")
    }
}
