package com.punkcrypto.releasehub.sdk.github

import com.punkcrypto.releasehub.core.model.RepositorySummary
import com.punkcrypto.releasehub.core.model.ReleaseSummary

interface GitHubReleaseSdk {
    suspend fun loadFeaturedRepositories(): List<RepositorySummary>
    suspend fun searchRepositories(query: String): List<RepositorySummary>
    suspend fun loadReleases(owner: String, repository: String): List<ReleaseSummary>
}

data class GitHubSdkConfig(
    val token: String? = null,
    val featuredQuery: String = "topic:android stars:>1500 archived:false",
)
