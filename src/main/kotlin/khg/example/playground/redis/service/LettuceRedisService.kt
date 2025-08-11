package khg.example.playground.redis.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanCursor
import io.lettuce.core.api.StatefulRedisConnection
import khg.example.playground.redis.config.RedisConnectionProperties
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.random.Random

@Service
class LettuceRedisService(
    private val props: RedisConnectionProperties,
    private val objectMapper: ObjectMapper
) {
    data class PingResult(
        val ok: Boolean,
        val message: String,
        val elapsedMs: Long
    )

    data class GenerateResult(
        val total: Int,
        val success: Int,
        val failed: Int,
        val elapsedMs: Long,
        val message: String? = null
    )

    data class DeleteSessionsResult(
        val matched: Long,
        val deleted: Long,
        val elapsedMs: Long,
        val message: String? = null
    )

    private fun buildUri(): RedisURI {
        val uri = RedisURI.builder()
            .withHost(props.host)
            .withPort(props.port)
            .withDatabase(props.database)
            .withTimeout(Duration.ofSeconds(5))
        if (!props.username.isNullOrBlank()) {
            uri.withAuthentication(props.username, props.password ?: "")
        } else if (!props.password.isNullOrBlank()) {
            uri.withPassword(props.password.toCharArray())
        }
        return uri.build()
    }

    fun ping(): PingResult {
        val start = System.nanoTime()
        var client: RedisClient? = null
        var connection: StatefulRedisConnection<String, String>? = null
        return try {
            client = RedisClient.create(buildUri())
            connection = client.connect()
            val pong = connection.sync().ping()
            val elapsed = (System.nanoTime() - start) / 1_000_000
            PingResult(ok = (pong.equals("PONG", ignoreCase = true)), message = pong, elapsedMs = elapsed)
        } catch (e: Exception) {
            val elapsed = (System.nanoTime() - start) / 1_000_000
            PingResult(ok = false, message = e.message ?: e.toString(), elapsedMs = elapsed)
        } finally {
            try { connection?.close() } catch (_: Exception) {}
            try { client?.shutdown() } catch (_: Exception) {}
        }
    }

    fun generateSessions(n: Int, ttlSeconds: Long = 3600L): GenerateResult {
        val start = System.nanoTime()
        var client: RedisClient? = null
        var connection: StatefulRedisConnection<String, String>? = null
        return try {
            client = RedisClient.create(buildUri())
            connection = client.connect()
            val commands = connection.sync()

            val formatter = DateTimeFormatter.ISO_INSTANT
            var success = 0
            var failed = 0

            repeat(n) {
                try {
                    val sessionId = UUID.randomUUID().toString()
                    val key = "session:$sessionId"

                    val now = Instant.now()
                    val loginOffsetSec = Random.nextLong(0, 7L * 24 * 3600)
                    val loginTime = now.minusSeconds(loginOffsetSec)
                    val activityOffsetSec = Random.nextLong(0, 6 * 3600L)
                    val lastActivity = loginTime.plusSeconds(activityOffsetSec)

                    val permissionsPool = listOf("read", "write")
                    val perm = if (Random.nextBoolean()) permissionsPool else listOf("read")

                    val valueMap = mapOf(
                        "user_id" to RandomStringUtils.randomNumeric(5),
                        "username" to ("user_" + RandomStringUtils.randomAlphanumeric(6).lowercase()),
                        "login_time" to formatter.format(loginTime.atOffset(ZoneOffset.UTC)),
                        "last_activity" to formatter.format(lastActivity.atOffset(ZoneOffset.UTC)),
                        "permissions" to perm
                    )
                    val json = objectMapper.writeValueAsString(valueMap)

                    commands.setex(key, ttlSeconds, json)
                    success++
                } catch (_: Exception) {
                    failed++
                }
            }
            val elapsed = (System.nanoTime() - start) / 1_000_000
            GenerateResult(total = n, success = success, failed = failed, elapsedMs = elapsed)
        } catch (e: Exception) {
            val elapsed = (System.nanoTime() - start) / 1_000_000
            GenerateResult(total = n, success = 0, failed = n, elapsedMs = elapsed, message = e.message ?: e.toString())
        } finally {
            try { connection?.close() } catch (_: Exception) {}
            try { client?.shutdown() } catch (_: Exception) {}
        }
    }

    fun deleteAllSessions(pattern: String = "session:*", batchSize: Int = 500): DeleteSessionsResult {
        val start = System.nanoTime()
        var client: RedisClient? = null
        var connection: StatefulRedisConnection<String, String>? = null
        return try {
            client = RedisClient.create(buildUri())
            connection = client.connect()
            val commands = connection.sync()

            var cursor: ScanCursor = ScanCursor.INITIAL
            val scanArgs = ScanArgs.Builder.matches(pattern).limit(1000)
            var matched = 0L
            var deleted = 0L
            val buffer = mutableListOf<String>()

            do {
                val scan = commands.scan(cursor, scanArgs)
                val keys = scan.keys
                matched += keys.size
                for (k in keys) {
                    buffer.add(k)
                    if (buffer.size >= batchSize) {
                        deleted += commands.del(*buffer.toTypedArray())
                        buffer.clear()
                    }
                }
                cursor = ScanCursor.of(scan.cursor)
                if (scan.isFinished) {
                    cursor.setFinished(true)
                }
            } while (!cursor.isFinished)

            if (buffer.isNotEmpty()) {
                deleted += commands.del(*buffer.toTypedArray())
                buffer.clear()
            }

            val elapsed = (System.nanoTime() - start) / 1_000_000
            DeleteSessionsResult(matched = matched, deleted = deleted, elapsedMs = elapsed)
        } catch (e: Exception) {
            val elapsed = (System.nanoTime() - start) / 1_000_000
            DeleteSessionsResult(matched = 0, deleted = 0, elapsedMs = elapsed, message = e.message ?: e.toString())
        } finally {
            try { connection?.close() } catch (_: Exception) {}
            try { client?.shutdown() } catch (_: Exception) {}
        }
    }
}
