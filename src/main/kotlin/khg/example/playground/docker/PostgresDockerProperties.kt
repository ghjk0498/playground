package khg.example.playground.docker

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.postgres-docker")
data class PostgresDockerProperties(
    val containerName: String = "playground-postgres",
    val image: String = "postgres:17.5-alpine",
    val hostPort: Int = 5432,
    val containerPort: Int = 5432,
    val username: String = "postgres",
    val password: String = "postgres",
    val database: String = "appdb"
)
