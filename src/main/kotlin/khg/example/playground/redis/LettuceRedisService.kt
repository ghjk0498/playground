package khg.example.playground.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class LettuceRedisService(
    private val props: RedisConnectionProperties
) {
    data class PingResult(
        val ok: Boolean,
        val message: String,
        val elapsedMs: Long
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
}
