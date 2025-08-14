package khg.example.playground.postgres.service

import khg.example.playground.common.generateRandomSessionData
import khg.example.playground.postgres.config.PostgresConnectionProperties
import khg.example.playground.postgres.entity.SessionEntity
import khg.example.playground.postgres.repository.SessionRepository
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class PostgresSessionService(
    private val repository: SessionRepository,
    private val props: PostgresConnectionProperties,
    private val databaseClient: DatabaseClient,
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

        val entities = generateRandomSessionData(n)
            .map {
                SessionEntity(
                    sessionId = it.sessionId,
                    userId = it.userId,
                    userName = it.userName,
                    loginTime = it.loginTime,
                    lastActivity = it.lastActivity,
                    permissions = it.permissions,
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

    data class TruncateResult(
        val host: String,
        val port: Int,
        val database: String,
        val success: Boolean,
        val elapsedMs: Long,
        val message: String? = null
    )

    fun truncate(): Mono<TruncateResult> {
        val start = System.nanoTime()
        
        return databaseClient.sql("TRUNCATE TABLE sessions")
            .fetch()
            .rowsUpdated()
            .map { 
                val elapsed = (System.nanoTime() - start) / 1_000_000
                TruncateResult(props.host, props.port, props.database, true, elapsed)
            }
            .onErrorResume { e ->
                val elapsed = (System.nanoTime() - start) / 1_000_000
                Mono.just(
                    TruncateResult(
                        props.host,
                        props.port,
                        props.database,
                        false,
                        elapsed,
                        e.message ?: e.toString()
                    )
                )
            }
    }
}
