package com.punkcrypto.releasehub.sdk.github

import com.punkcrypto.releasehub.core.model.RepositorySummary
import com.punkcrypto.releasehub.core.model.ReleaseSummary
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter

class KtorGitHubReleaseSdk(
    private val client: HttpClient,
    private val config: GitHubSdkConfig = GitHubSdkConfig(),
) : GitHubReleaseSdk {

    override suspend fun loadFeaturedRepositories(): List<RepositorySummary> {
        return searchRepositories(config.featuredQuery)
    }

    override suspend fun searchRepositories(query: String): List<RepositorySummary> {
        val normalizedQuery = query.ifBlank { config.featuredQuery }

        val response = runCatching {
            client.get("search/repositories") {
                parameter("q", normalizedQuery)
                parameter("sort", "stars")
                parameter("order", "desc")
                parameter("per_page", 20)
                config.token?.takeIf { it.isNotBlank() }?.let { token ->
                    header("Authorization", "Bearer $token")
                }
            }.body<RepositorySearchResponseDto>()
        }.getOrElse { throwable ->
            throw throwable.toReadableException()
        }

        return response.items.map { it.toModel() }
    }

    override suspend fun loadReleases(owner: String, repository: String): List<ReleaseSummary> {
        return runCatching {
            client.get("repos/$owner/$repository/releases") {
                parameter("per_page", 25)
                config.token?.takeIf { it.isNotBlank() }?.let { token ->
                    header("Authorization", "Bearer $token")
                }
            }.body<List<ReleaseDto>>()
        }.getOrElse { throwable ->
            throw throwable.toReadableException()
        }.map { it.toModel() }
    }
}

private fun Throwable.toReadableException(): Throwable = when (this) {
    is ClientRequestException -> {
        if (response.status.value == 403) {
            IllegalStateException(
                "GitHub API rate limit reached. Add a token later or wait for the limit to reset.",
                this,
            )
        } else {
            IllegalStateException("GitHub request failed with ${response.status.value}.", this)
        }
    }

    else -> this
}
