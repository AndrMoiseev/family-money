package io.familymoney.bootstrap

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class SpaControllerTest {
    private val controller = SpaController(SpaResources())

    @Test
    fun `serves the application index`() {
        val response = controller.index()

        assertEquals(200, response.code())
        assertEquals("text/html; charset=utf-8", requireNotNull(response.body()).contentType())
        assertEquals("no-cache", response.headers().getFirst("cache-control"))
        assertTrue(response.bodyText().contains("<div id=\"app\"></div>"))
    }

    @Test
    fun `falls back to the application index for a client route`() {
        val response = controller.resource(SpaRequestPath("/portfolio/overview"))

        assertEquals(200, response.code())
        assertEquals("text/html; charset=utf-8", requireNotNull(response.body()).contentType())
        assertEquals("no-cache", response.headers().getFirst("cache-control"))
    }

    @Test
    fun `does not hide missing API routes or resources`() {
        assertEquals(404, controller.resource(SpaRequestPath("/api")).code())
        assertEquals(404, controller.resource(SpaRequestPath("/api/v1/missing")).code())
        assertEquals(404, controller.resource(SpaRequestPath("/assets/missing")).code())
        assertEquals(404, controller.resource(SpaRequestPath("/assets/missing.js")).code())
    }

    @Test
    fun `rejects unsafe resource paths`() {
        assertEquals(404, controller.resource(SpaRequestPath("/../application.conf")).code())
        assertEquals(404, controller.resource(SpaRequestPath("/assets\\application.js")).code())
    }

    private fun ru.tinkoff.kora.http.server.common.HttpServerResponse.bodyText(): String {
        val content = requireNotNull(requireNotNull(body()).getFullContentIfAvailable()).duplicate()
        val bytes = ByteArray(content.remaining())
        content.get(bytes)
        return String(bytes, StandardCharsets.UTF_8)
    }
}
