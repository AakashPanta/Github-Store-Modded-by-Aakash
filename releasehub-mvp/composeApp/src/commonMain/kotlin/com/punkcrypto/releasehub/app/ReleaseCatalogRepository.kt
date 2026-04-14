package com.punkcrypto.releasehub.app

import com.punkcrypto.releasehub.core.model.CachedArtifact
import com.punkcrypto.releasehub.core.model.ReleaseAsset
import com.punkcrypto.releasehub.core.model.ReleaseSummary
import com.punkcrypto.releasehub.core.model.RepositorySummary
import com.punkcrypto.releasehub.local.store.ApkCacheStore
import com.punkcrypto.releasehub.sdk.github.GitHubReleaseSdk

class ReleaseCatalogRepository(
    private val sdk: GitHubReleaseSdk,
    private val cacheStore: ApkCacheStore,
) {
    suspend fun loadFeatured(): List<RepositorySummary> = sdk.loadFeaturedRepositories()

    suspend fun search(query: String): List<RepositorySummary> = sdk.searchRepositories(query)

    suspend fun loadReleases(repository: RepositorySummary): List<ReleaseSummary> {
        val owner = repository.owner.login
        return sdk.loadReleases(owner = owner, repository = repository.name)
    }

    suspend fun cachedArtifacts(): List<CachedArtifact> = cacheStore.list()

    suspend fun findCachedAsset(assetId: Long): CachedArtifact? = cacheStore.findByAssetId(assetId)

    suspend fun cacheAsset(
        repository: RepositorySummary,
        release: ReleaseSummary,
        asset: ReleaseAsset,
        onProgress: (Float) -> Unit,
    ): CachedArtifact = cacheStore.cacheApk(
        repository = repository,
        release = release,
        asset = asset,
        onProgress = onProgress,
    )
}
