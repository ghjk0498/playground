package khg.example.playground.docker.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.redis-docker")
data class RedisDockerProperties(
    override val containerName: String = "playground-redis",
    override val image: String = "redis:8.2.0-alpine",
    override val hostPort: Int = 6379,
    override val containerPort: Int = 6379,
) : DockerProperties