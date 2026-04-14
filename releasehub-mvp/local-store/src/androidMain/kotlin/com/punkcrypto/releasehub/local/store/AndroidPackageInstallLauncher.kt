package com.punkcrypto.releasehub.local.store

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.punkcrypto.releasehub.core.model.CachedArtifact
import java.io.File

class AndroidPackageInstallLauncher(
    context: Context,
) : PackageInstallLauncher {

    private val appContext = context.applicationContext

    override fun install(cachedArtifact: CachedArtifact): InstallLaunchResult {
        val file = File(cachedArtifact.absolutePath)
        if (!file.exists()) {
            return InstallLaunchResult(
                started = false,
                message = "Cached APK file is missing from storage.",
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !appContext.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${appContext.packageName}"),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(settingsIntent)
            return InstallLaunchResult(
                started = false,
                message = "Allow installs from this app, then tap install again.",
            )
        }

        val uri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            file,
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return try {
            appContext.startActivity(installIntent)
            InstallLaunchResult(
                started = true,
                message = "Installer opened for ${cachedArtifact.assetName}.",
            )
        } catch (_: ActivityNotFoundException) {
            InstallLaunchResult(
                started = false,
                message = "No package installer is available on this device.",
            )
        }
    }
}
