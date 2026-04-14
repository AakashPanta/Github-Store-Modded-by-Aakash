package com.punkcrypto.releasehub.local.store

import android.content.Context
import com.punkcrypto.releasehub.core.model.CachedArtifact
import com.punkcrypto.releasehub.core.model.ReleaseAsset
import com.punkcrypto.releasehub.core.model.ReleaseSummary
import com.punkcrypto.releasehub.core.model.RepositorySummary
import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.contentLength
import io.ktor.client.request.get
import io.ktor.utils.io.readAvailable
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class AndroidApkCacheStore(
    context: Context,
    private val httpClient: HttpClient,
) : ApkCacheStore {

    private val appContext = context.applicationContext
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val rootDir = File(appContext.filesDir, "release_cache").apply { mkdirs() }
    private val artifactDir = File(rootDir, "artifacts").apply { mkdirs() }
    private val indexFile = File(rootDir, "index.json").apply {
        if (!exists()) {
            writeText(json.encodeToString(CacheIndex()))
        }
    }

    override suspend fun list(): List<CachedArtifact> = withContext(Dispatchers.IO) {
        val index = readIndex()
        val cleaned = index.items.filter { File(it.absolutePath).exists() }
        if (cleaned.size != index.items.size) {
            writeIndex(CacheIndex(cleaned))
        }
        cleaned.sortedByDescending { it.downloadedAtEpochMillis }
    }

    override suspend fun findByAssetId(assetId: Long): CachedArtifact? = withContext(Dispatchers.IO) {
        readIndex().items.firstOrNull { it.assetId == assetId && File(it.absolutePath).exists() }
    }

    override suspend fun cacheApk(
        repository: RepositorySummary,
        release: ReleaseSummary,
        asset: ReleaseAsset,
        onProgress: (Float) -> Unit,
    ): CachedArtifact = withContext(Dispatchers.IO) {
        findByAssetId(asset.id)?.let { return@withContext it }

        val targetFile = File(artifactDir, "${asset.id}-${asset.name.sanitizeFileName()}")
        if (targetFile.exists()) {
            val existing = CachedArtifact(
                assetId = asset.id,
                assetName = asset.name,
                repositoryFullName = repository.fullName,
                releaseName = release.displayTitle,
                absolutePath = targetFile.absolutePath,
                sizeBytes = targetFile.length(),
                downloadedAtEpochMillis = System.currentTimeMillis(),
            )
            upsert(existing)
            return@withContext existing
        }

        val response = httpClient.get(asset.downloadUrl)
        val totalBytes = response.contentLength()?.takeIf { it > 0 } ?: asset.sizeBytes
        val channel = response.bodyAsChannel()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var downloaded = 0L

        FileOutputStream(targetFile).use { output ->
            while (true) {
                val read = channel.readAvailable(buffer)
                if (read == -1) break
                if (read == 0) continue
                output.write(buffer, 0, read)
                downloaded += read
                if (totalBytes > 0) {
                    onProgress((downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f))
                }
            }
            output.flush()
        }

        val cachedArtifact = CachedArtifact(
            assetId = asset.id,
            assetName = asset.name,
            repositoryFullName = repository.fullName,
            releaseName = release.displayTitle,
            absolutePath = targetFile.absolutePath,
            sizeBytes = targetFile.length(),
            downloadedAtEpochMillis = System.currentTimeMillis(),
        )

        upsert(cachedArtifact)
        onProgress(1f)
        cachedArtifact
    }

    private fun readIndex(): CacheIndex {
        if (!indexFile.exists()) return CacheIndex()
        val raw = indexFile.readText().ifBlank { return CacheIndex() }
        return runCatching { json.decodeFromString<CacheIndex>(raw) }.getOrDefault(CacheIndex())
    }

    private fun writeIndex(index: CacheIndex) {
        indexFile.writeText(json.encodeToString(index))
    }

    private fun upsert(item: CachedArtifact) {
        val current = readIndex().items
            .filterNot { it.assetId == item.assetId }
            .toMutableList()
            .apply { add(item) }

        writeIndex(CacheIndex(current.sortedByDescending { it.downloadedAtEpochMillis }))
    }
}

@Serializable
private data class CacheIndex(
    val items: List<CachedArtifact> = emptyList(),
)

private fun String.sanitizeFileName(): String = replace(Regex("[^A-Za-z0-9._-]"), "_")
