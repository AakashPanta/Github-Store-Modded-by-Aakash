package com.punkcrypto.releasehub.app

import com.punkcrypto.releasehub.local.store.ApkCacheStore
import com.punkcrypto.releasehub.local.store.PackageInstallLauncher
import com.punkcrypto.releasehub.sdk.github.GitHubReleaseSdk

interface ReleaseStoreDependencies {
    val gitHubSdk: GitHubReleaseSdk
    val apkCacheStore: ApkCacheStore
    val packageInstallLauncher: PackageInstallLauncher
}
