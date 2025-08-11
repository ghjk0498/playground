package khg.example.playground.postgres.controller

import khg.example.playground.postgres.config.PostgresConnectionProperties
import khg.example.playground.postgres.service.PostgresSessionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/postgres")
class PostgresSessionController(
    private val service: PostgresSessionService,
    private val props: PostgresConnectionProperties
) {
    data class GenerateResponse(
        val host: String,
        val port: Int,
        val database: String,
        val total: Int,
        val inserted: Long,
        val elapsedMs: Long,
        val message: String? = null
    )

    @PostMapping("/generate")
    fun generate(@RequestParam(name = "n", required = false, defaultValue = "10000") n: Int): Mono<ResponseEntity<GenerateResponse>> {
        return service.generate(n)
            .map { result ->
                val body = GenerateResponse(
                    host = props.host,
                    port = props.port,
                    database = props.database,
                    total = result.total,
                    inserted = result.inserted,
                    elapsedMs = result.elapsedMs,
                    message = result.message
                )
                val status = if (result.message == null) HttpStatus.OK else HttpStatus.SERVICE_UNAVAILABLE
                ResponseEntity.status(status).body(body)
            }
    }
}
