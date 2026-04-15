package com.punkcrypto.releasehub.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.punkcrypto.releasehub.core.model.CachedArtifact
import com.punkcrypto.releasehub.core.model.ReleaseAsset
import com.punkcrypto.releasehub.core.model.ReleaseSummary
import com.punkcrypto.releasehub.core.model.RepositorySummary
import com.punkcrypto.releasehub.core.model.asCompactCount
import com.punkcrypto.releasehub.core.model.asReadableSize
import kotlinx.coroutines.launch

@Composable
fun ReleaseHubApp(
    dependencies: ReleaseStoreDependencies,
) {
    ReleaseHubTheme {
        val controller = remember(dependencies) {
            ReleaseHubController(
                repository = ReleaseCatalogRepository(
                    sdk = dependencies.gitHubSdk,
                    cacheStore = dependencies.apkCacheStore,
                ),
                installer = dependencies.packageInstallLauncher,
            )
        }

        DisposableEffect(controller) {
            onDispose { controller.close() }
        }

        LaunchedEffect(controller) {
            controller.onStart()
        }

        ReleaseHubAppContent(controller = controller)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReleaseHubAppContent(
    controller: ReleaseHubController,
) {
    val uiState by controller.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        scope.launch {
            snackbarHostState.showSnackbar(message)
            controller.consumeMessage()
        }
    }

    val inDetail = uiState.selectedRepository != null

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (inDetail) uiState.selectedRepository?.fullName.orEmpty() else "ReleaseHub MVP",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!inDetail) {
                            Text(
                                text = "Browse GitHub releases and install APKs",
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (inDetail) {
                        IconButton(onClick = controller::closeRepository) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            if (!inDetail) {
                BottomAppBar(
                    modifier = Modifier.navigationBarsPadding(),
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                ) {
                    NavigationBarItem(
                        selected = uiState.selectedTab == MainTab.Browse,
                        onClick = { controller.selectTab(MainTab.Browse) },
                        icon = { Icon(Icons.Rounded.Search, contentDescription = "Browse") },
                        label = { Text("Browse") },
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == MainTab.Library,
                        onClick = { controller.selectTab(MainTab.Library) },
                        icon = { Icon(Icons.Rounded.Inventory2, contentDescription = "Library") },
                        label = { Text("Library") },
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .safeDrawingPadding(),
            color = androidx.compose.material3.MaterialTheme.colorScheme.background,
        ) {
            when {
                uiState.selectedRepository != null -> RepositoryDetailScreen(
                    repository = uiState.selectedRepository!!,
                    releases = uiState.releases,
                    isLoading = uiState.isLoadingReleases,
                    activeDownloads = uiState.downloadProgress,
                    onInstallAsset = { release, asset ->
                        controller.installAsset(uiState.selectedRepository!!, release, asset)
                    },
                )

                uiState.selectedTab == MainTab.Library -> LibraryScreen(
                    items = uiState.cachedArtifacts,
                    isLoading = uiState.isLoadingLibrary,
                    onReinstall = controller::reinstall,
                    onRefresh = controller::refreshLibrary,
                )

                else -> BrowseScreen(
                    uiState = uiState,
                    onQueryChange = controller::onQueryChanged,
                    onSearch = controller::search,
                    onOpenRepository = controller::openRepository,
                    onRefreshFeatured = controller::refreshFeatured,
                )
            }
        }
    }
}

@Composable
private fun BrowseScreen(
    uiState: ReleaseHubUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenRepository: (RepositorySummary) -> Unit,
    onRefreshFeatured: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.size(8.dp))
        HeroCard()
        Spacer(Modifier.size(16.dp))

        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search repositories") },
            placeholder = { Text("Try: launcher, reader, browser, manga, notes") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            trailingIcon = {
                TextButton(onClick = onSearch) {
                    Text("Search")
                }
            },
        )

        Spacer(Modifier.size(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = uiState.featuredOnly,
                onClick = onRefreshFeatured,
                label = { Text("Featured") },
            )
            AssistChip(
                onClick = onRefreshFeatured,
                label = { Text("Refresh") },
                leadingIcon = {
                    Icon(Icons.Rounded.Android, contentDescription = null)
                },
            )
        }

        Spacer(Modifier.size(16.dp))

        when {
            uiState.isLoadingRepositories -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.repositories.isEmpty() -> {
                EmptyState(
                    title = "No repositories yet",
                    body = "Search GitHub repositories or refresh featured apps.",
                )
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.repositories) { repo ->
                        RepositoryCard(
                            repository = repo,
                            onClick = { onOpenRepository(repo) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    items: List<CachedArtifact>,
    isLoading: Boolean,
    onReinstall: (CachedArtifact) -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.size(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Cached APKs",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = onRefresh) {
                Text("Refresh")
            }
        }

        Spacer(Modifier.size(8.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            items.isEmpty() -> {
                EmptyState(
                    title = "No cached apps",
                    body = "Installed APK downloads will appear here for quick reinstall.",
                )
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items) { item ->
                        CachedArtifactCard(
                            item = item,
                            onReinstall = { onReinstall(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RepositoryDetailScreen(
    repository: RepositorySummary,
    releases: List<ReleaseSummary>,
    isLoading: Boolean,
    activeDownloads: Map<Long, Float>,
    onInstallAsset: (ReleaseSummary, ReleaseAsset) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.size(12.dp))
        RepositoryHeaderCard(repository)
        Spacer(Modifier.size(16.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            releases.isEmpty() -> {
                EmptyState(
                    title = "No releases found",
                    body = "This repository does not currently expose GitHub releases.",
                )
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(releases) { release ->
                        ReleaseCard(
                            release = release,
                            activeDownloads = activeDownloads,
                            onInstallAsset = { asset -> onInstallAsset(release, asset) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard() {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "GitHub releases, simplified",
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Search repositories, inspect releases, and install APK assets with one tap.",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RepositoryCard(
    repository: RepositorySummary,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Android, contentDescription = null)
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = repository.fullName,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = repository.description ?: "No description provided.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MetaChip("★ ${repository.stars.asCompactCount()}")
                    repository.language?.let { MetaChip(it) }
                    repository.updatedAt?.take(10)?.let { MetaChip("Updated $it") }
                }
            }
        }
    }
}

@Composable
private fun RepositoryHeaderCard(
    repository: RepositorySummary,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = repository.fullName,
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = repository.description ?: "No description provided.",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetaChip("★ ${repository.stars.asCompactCount()}")
                repository.language?.let { MetaChip(it) }
                MetaChip("Owner ${repository.owner.login}")
            }
        }
    }
}

@Composable
private fun ReleaseCard(
    release: ReleaseSummary,
    activeDownloads: Map<Long, Float>,
    onInstallAsset: (ReleaseAsset) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = release.displayTitle,
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = release.publishedAt?.take(10) ?: "Publish date unavailable",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (release.isPreRelease) {
                        MetaChip("Pre-release")
                    }
                    if (release.isDraft) {
                        MetaChip("Draft")
                    }
                }
            }

            release.body?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val apkAssets = release.apkAssets
            if (apkAssets.isEmpty()) {
                EmptyInlineLabel("No APK assets in this release")
            } else {
                apkAssets.forEachIndexed { index, asset ->
                    if (index > 0) {
                        HorizontalDivider()
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = asset.name,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${asset.sizeBytes.asReadableSize()} • ${asset.downloadCount.asCompactCount()} downloads",
                                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Button(onClick = { onInstallAsset(asset) }) {
                                Icon(Icons.Rounded.CloudDownload, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Install APK")
                            }
                        }

                        activeDownloads[asset.id]?.let { progress ->
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CachedArtifactCard(
    item: CachedArtifact,
    onReinstall: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = item.assetName,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = item.repositoryFullName,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetaChip(item.releaseName)
                MetaChip(item.sizeBytes.asReadableSize())
            }
            Text(
                text = "Saved at: ${item.absolutePath}",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Button(onClick = onReinstall) {
                Text("Reinstall")
            }
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(androidx.compose.material3.MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun EmptyInlineLabel(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyState(
    title: String,
    body: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = body,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
PY

      - name: Commit and push
        run: |
          set -e
          git config user.name "github-actions[bot]"
          git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
          git add -A
          if git diff --cached --quiet; then
            echo "No changes to commit."
            exit 0
          fi
          git commit -m "Fix Compose UI source file"
          git push


This code changes the action not workable
