package khg.example.playground.postgres.service

import com.fasterxml.jackson.databind.ObjectMapper
import khg.example.playground.postgres.config.PostgresConnectionProperties
import khg.example.playground.postgres.entity.SessionEntity
import khg.example.playground.postgres.repository.SessionRepository
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.random.Random

@Service
class PostgresSessionService(
    private val repository: SessionRepository,
    private val props: PostgresConnectionProperties,
    private val objectMapper: ObjectMapper,
) {
    data class GenerateResult(
        val host: String,
        val port: Int,
        val database: String,
        val total: Int,
        val inserted: Long,
        val elapsedMs: Long,
        val message: String? = null
    )

    fun generate(n: Int): Mono<GenerateResult> {
        val count = if (n <= 0) 10000 else n
        val start = System.nanoTime()
        val ttlSeconds = 3600L

        val entities = Flux.range(0, count)
            .map {
                val sessionUuid = UUID.randomUUID().toString()
                val sessionId = "session:$sessionUuid"

                val now = Instant.now()
                val loginOffsetSec = Random.nextLong(0, 7L * 24 * 3600)
                val createdAt = LocalDateTime.ofInstant(now.minusSeconds(loginOffsetSec), ZoneOffset.UTC)
                val activityOffsetSec = Random.nextLong(0, 6 * 3600L)
                val lastAccessed = LocalDateTime.ofInstant(now.minusSeconds(loginOffsetSec - activityOffsetSec), ZoneOffset.UTC)
                val expiresAt = createdAt.plusSeconds(ttlSeconds)

                val userId = RandomStringUtils.randomNumeric(5).toLong()
                val username = "user_" + RandomStringUtils.randomAlphanumeric(6).lowercase()

                val sessionDataMap = mapOf(
                    "user_id" to userId.toString(),
                    "username" to username,
                    "login_time" to createdAt.atOffset(ZoneOffset.UTC).toString(),
                    "last_activity" to lastAccessed.atOffset(ZoneOffset.UTC).toString()
                )
                val sessionDataJson = objectMapper.writeValueAsString(sessionDataMap)

                SessionEntity(
                    sessionId = sessionId,
                    userId = userId,
                    sessionData = sessionDataJson,
                    createdAt = createdAt,
                    expiresAt = expiresAt,
                    lastAccessed = lastAccessed,
                )
            }

        return repository.saveAll(entities)
            .count()
            .map { savedCount ->
                val elapsed = (System.nanoTime() - start) / 1_000_000
                GenerateResult(props.host, props.port, props.database, count, savedCount, elapsed)
            }
            .onErrorResume { e ->
                val elapsed = (System.nanoTime() - start) / 1_000_000
                Mono.just(
                    GenerateResult(
                        props.host,
                        props.port,
                        props.database,
                        count,
                        0,
                        elapsed,
                        e.message ?: e.toString()
                    )
                )
            }
    }
}
