package com.punkcrypto.releasehub.app

import com.punkcrypto.releasehub.core.model.CachedArtifact
import com.punkcrypto.releasehub.core.model.ReleaseAsset
import com.punkcrypto.releasehub.core.model.ReleaseSummary
import com.punkcrypto.releasehub.core.model.RepositorySummary
import com.punkcrypto.releasehub.local.store.PackageInstallLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReleaseHubController(
    private val repository: ReleaseCatalogRepository,
    private val installer: PackageInstallLauncher,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _uiState = MutableStateFlow(ReleaseHubUiState())
    val uiState: StateFlow<ReleaseHubUiState> = _uiState.asStateFlow()

    private var initialLoadCompleted = false

    fun onStart() {
        if (initialLoadCompleted) return
        initialLoadCompleted = true
        refreshFeatured()
        refreshLibrary()
    }

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun selectTab(tab: MainTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
        if (tab == MainTab.Library) {
            refreshLibrary()
        }
    }

    fun search() {
        val query = _uiState.value.query.trim()
        scope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingBrowse = true,
                message = null,
            )
            runCatching {
                if (query.isBlank()) repository.loadFeatured() else repository.search(query)
            }.onSuccess { items ->
                _uiState.value = _uiState.value.copy(
                    repositories = items,
                    isLoadingBrowse = false,
                )
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoadingBrowse = false,
                    message = throwable.message ?: "Unable to search repositories.",
                )
            }
        }
    }

    fun refreshFeatured() {
        scope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingBrowse = true,
                message = null,
            )
            runCatching { repository.loadFeatured() }
                .onSuccess { items ->
                    _uiState.value = _uiState.value.copy(
                        repositories = items,
                        isLoadingBrowse = false,
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingBrowse = false,
                        message = throwable.message ?: "Unable to load featured repositories.",
                    )
                }
        }
    }

    fun openRepository(repositorySummary: RepositorySummary) {
        _uiState.value = _uiState.value.copy(
            selectedRepository = repositorySummary,
            releases = emptyList(),
            isLoadingReleases = true,
            message = null,
        )

        scope.launch {
            runCatching { repository.loadReleases(repositorySummary) }
                .onSuccess { releases ->
                    _uiState.value = _uiState.value.copy(
                        releases = releases,
                        isLoadingReleases = false,
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingReleases = false,
                        message = throwable.message ?: "Unable to load releases.",
                    )
                }
        }
    }

    fun closeRepository() {
        _uiState.value = _uiState.value.copy(
            selectedRepository = null,
            releases = emptyList(),
            isLoadingReleases = false,
        )
    }

    fun installAsset(
        repositorySummary: RepositorySummary,
        release: ReleaseSummary,
        asset: ReleaseAsset,
    ) {
        scope.launch {
            _uiState.value = _uiState.value.withProgress(asset.id, 0f)
            runCatching {
                val cached = repository.findCachedAsset(asset.id)
                    ?: repository.cacheAsset(
                        repository = repositorySummary,
                        release = release,
                        asset = asset,
                    ) { progress ->
                        _uiState.value = _uiState.value.withProgress(asset.id, progress)
                    }

                installer.install(cached)
            }.onSuccess { result ->
                _uiState.value = _uiState.value
                    .withoutProgress(asset.id)
                    .copy(message = result.message ?: if (result.started) "Installer opened." else "Install was not started.")
                refreshLibrary()
            }.onFailure { throwable ->
                _uiState.value = _uiState.value
                    .withoutProgress(asset.id)
                    .copy(message = throwable.message ?: "Unable to install asset.")
            }
        }
    }

    fun reinstall(cachedArtifact: CachedArtifact) {
        val result = installer.install(cachedArtifact)
        _uiState.value = _uiState.value.copy(
            message = result.message ?: if (result.started) "Installer opened." else "Install was not started.",
        )
    }

    fun refreshLibrary() {
        scope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingLibrary = true,
            )
            runCatching { repository.cachedArtifacts() }
                .onSuccess { items ->
                    _uiState.value = _uiState.value.copy(
                        cachedArtifacts = items,
                        isLoadingLibrary = false,
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingLibrary = false,
                        message = throwable.message ?: "Unable to refresh library.",
                    )
                }
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun close() {
        scope.cancel()
    }
}

data class ReleaseHubUiState(
    val selectedTab: MainTab = MainTab.Browse,
    val query: String = "",
    val repositories: List<RepositorySummary> = emptyList(),
    val selectedRepository: RepositorySummary? = null,
    val releases: List<ReleaseSummary> = emptyList(),
    val cachedArtifacts: List<CachedArtifact> = emptyList(),
    val isLoadingBrowse: Boolean = false,
    val isLoadingReleases: Boolean = false,
    val isLoadingLibrary: Boolean = false,
    val downloadProgress: Map<Long, Float> = emptyMap(),
    val message: String? = null,
)

enum class MainTab {
    Browse,
    Library,
}

private fun ReleaseHubUiState.withProgress(assetId: Long, progress: Float): ReleaseHubUiState {
    return copy(downloadProgress = downloadProgress.toMutableMap().apply { this[assetId] = progress })
}

private fun ReleaseHubUiState.withoutProgress(assetId: Long): ReleaseHubUiState {
    return copy(downloadProgress = downloadProgress.toMutableMap().apply { remove(assetId) })
}
