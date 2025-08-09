package khg.example.playground.docker

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.redis-docker")
data class RedisDockerProperties(
    val containerName: String = "playground-redis",
    val image: String = "redis:8.2.0-alpine",
    val hostPort: Int = 6379,
    val containerPort: Int = 6379
)