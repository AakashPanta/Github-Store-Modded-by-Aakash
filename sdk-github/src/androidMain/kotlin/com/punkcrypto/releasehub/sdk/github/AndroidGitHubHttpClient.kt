package com.punkcrypto.releasehub.sdk.github

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createGitHubHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }
        )
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 120000
        connectTimeoutMillis = 30000
        socketTimeoutMillis = 120000
    }

    defaultRequest {
        url("https://api.github.com/")
        header(HttpHeaders.Accept, "application/vnd.github+json")
        header("X-GitHub-Api-Version", "2022-11-28")
    }
}
