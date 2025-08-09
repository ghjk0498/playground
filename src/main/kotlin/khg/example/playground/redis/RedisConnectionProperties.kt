package khg.example.playground.redis

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.redis")
data class RedisConnectionProperties(
    val host: String = "localhost",
    val port: Int = 6379,
    val username: String? = null,
    val password: String? = null,
    val database: Int = 0,
)
