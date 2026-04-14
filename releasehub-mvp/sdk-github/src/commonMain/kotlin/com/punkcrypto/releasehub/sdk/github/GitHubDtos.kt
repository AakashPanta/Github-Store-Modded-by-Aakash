package com.punkcrypto.releasehub.sdk.github

import com.punkcrypto.releasehub.core.model.AssetKind
import com.punkcrypto.releasehub.core.model.RepositoryOwner
import com.punkcrypto.releasehub.core.model.RepositorySummary
import com.punkcrypto.releasehub.core.model.ReleaseAsset
import com.punkcrypto.releasehub.core.model.ReleaseSummary
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RepositorySearchResponseDto(
    val items: List<RepositoryDto> = emptyList(),
)

@Serializable
internal data class RepositoryDto(
    val id: Long,
    val name: String,
    @SerialName("full_name")
    val fullName: String,
    val owner: OwnerDto,
    val description: String? = null,
    @SerialName("stargazers_count")
    val stars: Long = 0,
    val language: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("html_url")
    val htmlUrl: String,
)

@Serializable
internal data class OwnerDto(
    val login: String,
    @SerialName("avatar_url")
    val avatarUrl: String,
    @SerialName("html_url")
    val htmlUrl: String,
)

@Serializable
internal data class ReleaseDto(
    val id: Long,
    @SerialName("tag_name")
    val tagName: String,
    val name: String? = null,
    val body: String? = null,
    @SerialName("published_at")
    val publishedAt: String? = null,
    val draft: Boolean = false,
    @SerialName("prerelease")
    val preRelease: Boolean = false,
    val assets: List<ReleaseAssetDto> = emptyList(),
)

@Serializable
internal data class ReleaseAssetDto(
    val id: Long,
    val name: String,
    @SerialName("content_type")
    val contentType: String? = null,
    val size: Long = 0,
    @SerialName("download_count")
    val downloadCount: Long = 0,
    @SerialName("browser_download_url")
    val browserDownloadUrl: String,
)

internal fun RepositoryDto.toModel(): RepositorySummary = RepositorySummary(
    id = id,
    name = name,
    fullName = fullName,
    owner = RepositoryOwner(
        login = owner.login,
        avatarUrl = owner.avatarUrl,
        profileUrl = owner.htmlUrl,
    ),
    description = description,
    stars = stars,
    language = language,
    updatedAt = updatedAt,
    htmlUrl = htmlUrl,
)

internal fun ReleaseDto.toModel(): ReleaseSummary = ReleaseSummary(
    id = id,
    tagName = tagName,
    name = name,
    body = body,
    publishedAt = publishedAt,
    isDraft = draft,
    isPreRelease = preRelease,
    assets = assets.map { it.toModel() },
)

internal fun ReleaseAssetDto.toModel(): ReleaseAsset = ReleaseAsset(
    id = id,
    name = name,
    contentType = contentType,
    sizeBytes = size,
    downloadCount = downloadCount,
    downloadUrl = browserDownloadUrl,
    kind = when {
        name.endsWith(".apk", ignoreCase = true) -> AssetKind.Apk
        contentType?.contains("android.package-archive", ignoreCase = true) == true -> AssetKind.Apk
        else -> AssetKind.Other
    },
)
