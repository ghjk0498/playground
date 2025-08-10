package khg.example.playground.postgres.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("sessions")
data class SessionEntity(
    @Id
    @Column("session_id")
    val sessionId: String,

    @Column("user_id")
    val userId: Long?,

    @Column("session_data")
    val sessionData: String?,

    @Column("created_at")
    val createdAt: LocalDateTime?,

    @Column("expires_at")
    val expiresAt: LocalDateTime?,

    @Column("last_accessed")
    val lastAccessed: LocalDateTime?,
)