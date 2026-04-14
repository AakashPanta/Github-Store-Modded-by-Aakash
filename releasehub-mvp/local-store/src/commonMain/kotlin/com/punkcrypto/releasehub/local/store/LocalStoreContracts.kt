package com.punkcrypto.releasehub.local.store

import com.punkcrypto.releasehub.core.model.CachedArtifact
import com.punkcrypto.releasehub.core.model.ReleaseAsset
import com.punkcrypto.releasehub.core.model.ReleaseSummary
import com.punkcrypto.releasehub.core.model.RepositorySummary

interface ApkCacheStore {
    suspend fun list(): List<CachedArtifact>
    suspend fun findByAssetId(assetId: Long): CachedArtifact?
    suspend fun cacheApk(
        repository: RepositorySummary,
        release: ReleaseSummary,
        asset: ReleaseAsset,
        onProgress: (Float) -> Unit = {},
    ): CachedArtifact
}

interface PackageInstallLauncher {
    fun install(cachedArtifact: CachedArtifact): InstallLaunchResult
}

data class InstallLaunchResult(
    val started: Boolean,
    val message: String? = null,
)
