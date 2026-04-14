package com.punkcrypto.releasehub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.punkcrypto.releasehub.app.ReleaseHubApp
import com.punkcrypto.releasehub.app.ReleaseStoreDependencies
import com.punkcrypto.releasehub.local.store.AndroidApkCacheStore
import com.punkcrypto.releasehub.local.store.AndroidPackageInstallLauncher
import com.punkcrypto.releasehub.local.store.ApkCacheStore
import com.punkcrypto.releasehub.local.store.PackageInstallLauncher
import com.punkcrypto.releasehub.sdk.github.GitHubReleaseSdk
import com.punkcrypto.releasehub.sdk.github.KtorGitHubReleaseSdk
import com.punkcrypto.releasehub.sdk.github.createGitHubHttpClient

class MainActivity : ComponentActivity() {

    private val dependencies: ReleaseStoreDependencies by lazy {
        AndroidReleaseStoreDependencies(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReleaseHubApp(dependencies = dependencies)
        }
    }
}

private class AndroidReleaseStoreDependencies(
    activity: ComponentActivity,
) : ReleaseStoreDependencies {

    private val httpClient = createGitHubHttpClient()
    private val appContext = activity.applicationContext

    override val gitHubSdk: GitHubReleaseSdk = KtorGitHubReleaseSdk(httpClient)
    override val apkCacheStore: ApkCacheStore = AndroidApkCacheStore(
        context = appContext,
        httpClient = httpClient,
    )
    override val packageInstallLauncher: PackageInstallLauncher = AndroidPackageInstallLauncher(appContext)
}
