package khg.example.playground.redis

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/redis")
class RedisConnectionController(
    private val service: LettuceRedisService,
    private val props: RedisConnectionProperties
) {
    data class PingResponse(
        val host: String,
        val port: Int,
        val database: Int,
        val ok: Boolean,
        val message: String,
        val elapsedMs: Long
    )

    data class GenerateResponse(
        val host: String,
        val port: Int,
        val database: Int,
        val total: Int,
        val success: Int,
        val failed: Int,
        val elapsedMs: Long,
        val message: String? = null
    )

    data class DeleteSessionsResponse(
        val host: String,
        val port: Int,
        val database: Int,
        val pattern: String,
        val matched: Long,
        val deleted: Long,
        val elapsedMs: Long,
        val message: String? = null
    )

    @GetMapping("/ping")
    fun ping(): ResponseEntity<PingResponse> {
        val result = service.ping()
        val body = PingResponse(
            host = props.host,
            port = props.port,
            database = props.database,
            ok = result.ok,
            message = result.message,
            elapsedMs = result.elapsedMs
        )
        val status = if (result.ok) HttpStatus.OK else HttpStatus.SERVICE_UNAVAILABLE
        return ResponseEntity.status(status).body(body)
    }

    @PostMapping("/generate")
    fun generate(@RequestParam(name = "n", required = false, defaultValue = "10000") n: Int): ResponseEntity<GenerateResponse> {
        val count = if (n <= 0) 10000 else n
        val result = service.generateSessions(count, ttlSeconds = 3600)
        val body = GenerateResponse(
            host = props.host,
            port = props.port,
            database = props.database,
            total = result.total,
            success = result.success,
            failed = result.failed,
            elapsedMs = result.elapsedMs,
            message = result.message
        )
        val status = if (result.message == null) HttpStatus.OK else HttpStatus.SERVICE_UNAVAILABLE
        return ResponseEntity.status(status).body(body)
    }

    @DeleteMapping("/sessions")
    fun deleteAllSessions(@RequestParam(name = "pattern", required = false, defaultValue = "session:*") pattern: String): ResponseEntity<DeleteSessionsResponse> {
        val result = service.deleteAllSessions(pattern)
        val body = DeleteSessionsResponse(
            host = props.host,
            port = props.port,
            database = props.database,
            pattern = pattern,
            matched = result.matched,
            deleted = result.deleted,
            elapsedMs = result.elapsedMs,
            message = result.message
        )
        val status = if (result.message == null) HttpStatus.OK else HttpStatus.SERVICE_UNAVAILABLE
        return ResponseEntity.status(status).body(body)
    }
}
