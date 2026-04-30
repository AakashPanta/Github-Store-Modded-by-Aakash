package com.punkcrypto.releasehub.sdk.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GitHubRepositorySearchResponse(
    @SerialName("items")
    val items: List<RepositoryDto> = emptyList()
)

internal typealias GitHubReleaseDto = ReleaseDto
