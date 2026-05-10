package com.nexora.app.domain.model

data class SelectedFile(
    val name: String,
    val type: String, // VIDEO, AUDIO, DOCUMENT, IMAGE
    val mimeType: String,
    val size: Long,
    val uri: String
)
