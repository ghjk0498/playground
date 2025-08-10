package khg.example.playground.postgres

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.postgres")
data class PostgresConnectionProperties(
    val host: String = "localhost",
    val port: Int = 5432,
    val database: String = "appdb",
    val username: String = "postgres",
    val password: String = "postgres"
)
