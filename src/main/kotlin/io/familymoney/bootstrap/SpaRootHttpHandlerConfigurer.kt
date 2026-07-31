package io.familymoney.bootstrap

import io.undertow.server.HttpHandler
import io.undertow.util.Headers
import io.undertow.util.HttpString
import io.undertow.util.Methods
import ru.tinkoff.kora.common.Component
import ru.tinkoff.kora.http.server.undertow.HttpHandlerConfigurer
import java.nio.ByteBuffer

@Component
class SpaRootHttpHandlerConfigurer(
    private val resources: SpaResources,
) : HttpHandlerConfigurer {
    override fun configure(next: HttpHandler): HttpHandler =
        HttpHandler { exchange ->
            if (exchange.requestMethod != Methods.GET || exchange.requestPath != ROOT_PATH) {
                next.handleRequest(exchange)
                return@HttpHandler
            }

            val index = resources.load(SpaController.INDEX_RESOURCE)
            if (index == null) {
                next.handleRequest(exchange)
                return@HttpHandler
            }

            exchange.statusCode = HTTP_OK
            exchange.responseHeaders.put(Headers.CONTENT_TYPE, "text/html; charset=utf-8")
            exchange.responseHeaders.put(Headers.CACHE_CONTROL, "no-cache")
            exchange.responseHeaders.put(X_CONTENT_TYPE_OPTIONS, "nosniff")
            exchange.responseSender.send(ByteBuffer.wrap(index))
        }

    private companion object {
        const val ROOT_PATH = "/"
        const val HTTP_OK = 200
        val X_CONTENT_TYPE_OPTIONS = HttpString("X-Content-Type-Options")
    }
}
