package com.tks.videophotobookv3.model

import java.util.UUID

data class ArKeyPair(
    val id: String = UUID.randomUUID().toString(),
    val markerUri: String,
    val videoUri: String,
    val physicalWidth: Float = 0.1f // 物理的な横幅（メートル、デフォルト0.1m = 10cm）
)
