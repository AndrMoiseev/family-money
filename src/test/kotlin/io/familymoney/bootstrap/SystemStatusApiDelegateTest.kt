package io.familymoney.bootstrap

import io.familymoney.generated.api.model.SystemStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SystemStatusApiDelegateTest {
    @Test
    fun `returns the generated system status response`() {
        val response = SystemStatusApiDelegate().getSystemStatus()

        assertEquals(SystemStatus.StatusEnum.UP, response.content.status)
    }
}
