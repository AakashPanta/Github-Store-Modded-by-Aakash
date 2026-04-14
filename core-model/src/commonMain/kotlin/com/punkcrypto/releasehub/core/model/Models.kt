package com.punkcrypto.releasehub.core.model

import kotlinx.serialization.Serializable

@Serializable
data class RepositoryOwner(
    val login: String,
    val avatarUrl: String,
    val profileUrl: String,
)

@Serializable
data class RepositorySummary(
    val id: Long,
    val name: String,
    val fullName: String,
    val owner: RepositoryOwner,
    val description: String?,
    val stars: Long,
    val language: String?,
    val updatedAt: String?,
    val htmlUrl: String,
)

@Serializable
data class ReleaseSummary(
    val id: Long,
    val tagName: String,
    val name: String?,
    val body: String?,
    val publishedAt: String?,
    val isDraft: Boolean,
    val isPreRelease: Boolean,
    val assets: List<ReleaseAsset>,
) {
    val displayTitle: String
        get() = name?.takeIf { it.isNotBlank() } ?: tagName

    val apkAssets: List<ReleaseAsset>
        get() = assets.filter { it.kind == AssetKind.Apk }
}

@Serializable
data class ReleaseAsset(
    val id: Long,
    val name: String,
    val contentType: String?,
    val sizeBytes: Long,
    val downloadCount: Long,
    val downloadUrl: String,
    val kind: AssetKind,
)

@Serializable
enum class AssetKind {
    Apk,
    Other,
}

@Serializable
data class CachedArtifact(
    val assetId: Long,
    val assetName: String,
    val repositoryFullName: String,
    val releaseName: String,
    val absolutePath: String,
    val sizeBytes: Long,
    val downloadedAtEpochMillis: Long,
)

fun Long.asCompactCount(): String = when {
    this >= 1_000_000 -> "${(this / 100_000) / 10.0}M"
    this >= 1_000 -> "${(this / 100) / 10.0}K"
    else -> toString()
}

fun Long.asReadableSize(): String {
    if (this <= 0L) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        this >= gb -> "${((this / gb) * 100).toInt() / 100.0} GB"
        this >= mb -> "${((this / mb) * 100).toInt() / 100.0} MB"
        this >= kb -> "${((this / kb) * 100).toInt() / 100.0} KB"
        else -> "$this B"
    }
}
