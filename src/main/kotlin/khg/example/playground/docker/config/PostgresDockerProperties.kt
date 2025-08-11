package khg.example.playground.docker.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.postgres-docker")
data class PostgresDockerProperties (
    override val containerName: String = "playground-postgres",
    override val image: String = "postgres:17.5-alpine",
    override val hostPort: Int = 5432,
    override val containerPort: Int = 5432,
    val database: String = "appdb",
    val username: String = "postgres",
    val password: String = "postgres",
) : DockerProperties
