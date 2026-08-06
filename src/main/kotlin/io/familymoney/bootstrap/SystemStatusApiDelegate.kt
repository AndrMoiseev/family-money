package io.familymoney.bootstrap

import io.familymoney.generated.api.SystemApiDelegate
import io.familymoney.generated.api.SystemApiResponses.GetSystemStatusApiResponse
import io.familymoney.generated.api.model.SystemStatus
import ru.tinkoff.kora.common.Component

@Component
class SystemStatusApiDelegate : SystemApiDelegate {
    override fun getSystemStatus(): GetSystemStatusApiResponse =
        GetSystemStatusApiResponse(
            content = SystemStatus(status = SystemStatus.StatusEnum.UP),
        )
}
