package org.openinsectid.app.data

data class AlphaResponse(
    val status: String?,
    val response: AlphaInnerResponse?
)

data class AlphaInnerResponse(
    val response: String?
)
