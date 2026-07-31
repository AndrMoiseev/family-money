package io.familymoney.bootstrap

import java.nio.charset.StandardCharsets
import ru.tinkoff.kora.common.Component
import ru.tinkoff.kora.http.common.HttpMethod
import ru.tinkoff.kora.http.common.annotation.HttpRoute
import ru.tinkoff.kora.http.common.body.HttpBody
import ru.tinkoff.kora.http.server.common.HttpServerResponse
import ru.tinkoff.kora.http.server.common.annotation.HttpController

@Component
@HttpController
class SystemStatusController {
    @HttpRoute(method = HttpMethod.GET, path = "/api/v1/system/status")
    fun getSystemStatus(): HttpServerResponse =
        HttpServerResponse.of(
            200,
            HttpBody.json("""{"status":"UP"}""".toByteArray(StandardCharsets.UTF_8)),
        )
}
