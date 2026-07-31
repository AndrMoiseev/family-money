package io.familymoney.bootstrap

import ru.tinkoff.kora.common.Component
import ru.tinkoff.kora.common.Mapping
import ru.tinkoff.kora.http.common.HttpMethod
import ru.tinkoff.kora.http.common.annotation.HttpRoute
import ru.tinkoff.kora.http.common.body.HttpBody
import ru.tinkoff.kora.http.common.header.HttpHeaders
import ru.tinkoff.kora.http.server.common.HttpServerRequest
import ru.tinkoff.kora.http.server.common.HttpServerResponse
import ru.tinkoff.kora.http.server.common.annotation.HttpController
import ru.tinkoff.kora.http.server.common.handler.HttpServerRequestMapper

@Component
@HttpController
class SpaController(
    private val resources: SpaResources,
) {
    fun index(): HttpServerResponse = serveIndex()

    @HttpRoute(method = HttpMethod.GET, path = "/*")
    fun resource(
        @Mapping(SpaRequestPathMapper::class) requestedPath: SpaRequestPath,
    ): HttpServerResponse {
        val resourcePath = requestedPath.value.removePrefix("/")
        return if (!isSafePath(resourcePath) || isApiPath(resourcePath)) {
            notFound()
        } else {
            serveResourceOrIndex(resourcePath)
        }
    }

    private fun serveResourceOrIndex(resourcePath: String): HttpServerResponse {
        val resource = loadResource(resourcePath)
        return when {
            resource != null -> ok(resourcePath, resource)
            resourcePath.startsWith(ASSET_PATH_PREFIX) -> notFound()
            resourcePath.substringAfterLast('/').contains('.') -> notFound()
            else -> serveIndex()
        }
    }

    private fun serveIndex(): HttpServerResponse {
        val index = loadResource(INDEX_RESOURCE) ?: return notFound()
        return ok(INDEX_RESOURCE, index)
    }

    private fun loadResource(resourcePath: String): ByteArray? = resources.load(resourcePath)

    private fun ok(
        resourcePath: String,
        content: ByteArray,
    ): HttpServerResponse {
        val cacheControl =
            if (resourcePath == INDEX_RESOURCE) {
                "no-cache"
            } else if (resourcePath.startsWith("assets/")) {
                "public, max-age=31536000, immutable"
            } else {
                "public, max-age=3600"
            }

        return HttpServerResponse.of(
            HTTP_OK,
            HttpHeaders.of(
                "cache-control",
                cacheControl,
                "x-content-type-options",
                "nosniff",
            ),
            HttpBody.of(contentType(resourcePath), content),
        )
    }

    private fun notFound(): HttpServerResponse = HttpServerResponse.of(HTTP_NOT_FOUND)

    private fun isSafePath(resourcePath: String): Boolean =
        resourcePath.isNotBlank() &&
            '\\' !in resourcePath &&
            '\u0000' !in resourcePath &&
            resourcePath.split('/').none { segment -> segment.isBlank() || segment == "." || segment == ".." }

    private fun isApiPath(resourcePath: String): Boolean =
        resourcePath == API_PATH ||
            resourcePath.startsWith(API_PATH_PREFIX)

    private fun contentType(resourcePath: String): String =
        when (resourcePath.substringAfterLast('.', missingDelimiterValue = "")) {
            "css" -> "text/css; charset=utf-8"
            "html" -> "text/html; charset=utf-8"
            "ico" -> "image/x-icon"
            "jpeg", "jpg" -> "image/jpeg"
            "js", "mjs" -> "text/javascript; charset=utf-8"
            "json", "map" -> "application/json; charset=utf-8"
            "png" -> "image/png"
            "svg" -> "image/svg+xml"
            "webp" -> "image/webp"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            else -> "application/octet-stream"
        }

    companion object {
        const val API_PATH = "api"
        const val API_PATH_PREFIX = "api/"
        const val ASSET_PATH_PREFIX = "assets/"
        const val HTTP_NOT_FOUND = 404
        const val HTTP_OK = 200
        const val INDEX_RESOURCE = "index.html"
    }
}

@Component
class SpaResources {
    private val index = loadFromClasspath(SpaController.INDEX_RESOURCE)

    fun load(resourcePath: String): ByteArray? =
        if (resourcePath == SpaController.INDEX_RESOURCE) index else loadFromClasspath(resourcePath)

    private fun loadFromClasspath(resourcePath: String): ByteArray? =
        javaClass.classLoader
            .getResourceAsStream("static/$resourcePath")
            ?.use { it.readBytes() }
}

data class SpaRequestPath(
    val value: String,
)

class SpaRequestPathMapper : HttpServerRequestMapper<SpaRequestPath?> {
    override fun apply(request: HttpServerRequest): SpaRequestPath = SpaRequestPath(request.path())
}
