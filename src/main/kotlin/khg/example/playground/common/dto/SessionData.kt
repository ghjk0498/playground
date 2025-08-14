package khg.example.playground.common.dto

import java.time.LocalDateTime
import java.util.UUID

data class SessionData(
    val sessionId: UUID,
    val userId: Long,
    val userName: String,
    val loginTime: LocalDateTime,
    val lastActivity: LocalDateTime,
    val permissions: List<String>,
)
