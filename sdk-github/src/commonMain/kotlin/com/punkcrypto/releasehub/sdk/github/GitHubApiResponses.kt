package com.punkcrypto.releasehub.sdk.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRepositorySearchResponse(
    @SerialName("items")
    val items: List<RepositoryDto> = emptyList()
)

typealias GitHubReleaseDto = ReleaseDto
