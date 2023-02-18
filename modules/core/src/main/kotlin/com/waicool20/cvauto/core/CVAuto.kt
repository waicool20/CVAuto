package com.waicool20.cvauto.core

import java.nio.file.Path
import java.nio.file.Paths

object CVAuto {
    val HOME_DIR: Path = Paths.get(System.getProperty("user.home")).resolve(".cvauto")
}