package khg.example.playground.redis

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
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
}
