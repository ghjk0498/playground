package khg.example.playground.docker.service

import khg.example.playground.docker.config.PostgresDockerProperties
import khg.example.playground.docker.config.RedisDockerProperties
import khg.example.playground.docker.enums.DockerContainerType
import khg.example.playground.docker.service.dto.ContainerStatus
import khg.example.playground.docker.extensions.toDockerArgs
import khg.example.playground.postgres.service.FlywayMigrationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.sql.DriverManager

@Service
class DockerContainerService(
    private val dockerCommandService: DockerCommandService,
    private val configService: ConfigService,
    private val flywayMigrationService: FlywayMigrationService,
    @Value("\${spring.flyway.url}") private val flywayUrl: String,
) {
    fun status(dockerContainerType: DockerContainerType): ContainerStatus {
        val dockerProperties = configService.getConfig(dockerContainerType)
        val exists = dockerCommandService.exists(dockerProperties.containerName)
        val running = if (exists) dockerCommandService.isRunning(dockerProperties.containerName) else false
        val id = if (exists) dockerCommandService.containerId(dockerProperties.containerName) else null
        return ContainerStatus(
            name = dockerProperties.containerName,
            exists = exists,
            running = running,
            containerId = id,
            message = if (!exists) "Container not found" else null
        )
    }

    suspend fun start(dockerContainerType: DockerContainerType): ContainerStatus {
        val dockerProperties = configService.getConfig(dockerContainerType)
        val containerName = dockerProperties.containerName
        val exists = dockerCommandService.exists(containerName)
        if (exists) {
            val running = dockerCommandService.isRunning(containerName)
            if (running) {
                val id = dockerCommandService.containerId(containerName)
                return ContainerStatus(
                    name = containerName,
                    exists = true,
                    running = true,
                    containerId = id,
                    message = "Container already running"
                )
            }
            val r = dockerCommandService.run("docker", "start", containerName)
            val nowRunning = dockerCommandService.isRunning(containerName)
            val id = dockerCommandService.containerId(containerName)

            // Apply Flyway migrations if this is a PostgreSQL container and it's running
            var migrationsApplied = false
            if (dockerProperties is PostgresDockerProperties && nowRunning) {
                try {
                    waitForPostgreSQLAsync(dockerProperties)
                    flywayMigrationService.migrateDatabase()
                    migrationsApplied = true
                } catch (e: Exception) {
                    e.printStackTrace()
                    println("[ERROR] Failed to apply Flyway migrations: ${e.message}")
                }
            }

            val msg = if (r.success && nowRunning) {
                if (migrationsApplied) "Container started and Flyway migrations applied"
                else "Container started"
            } else (r.stderr.ifBlank { r.stdout })
            return ContainerStatus(
                name = containerName,
                exists = true,
                running = nowRunning,
                containerId = id,
                message = msg
            )
        } else {
            val portMapping = "${dockerProperties.hostPort}:${dockerProperties.containerPort}"
            val args = when (dockerProperties) {
                is PostgresDockerProperties -> dockerProperties.toDockerArgs(containerName, portMapping)
                is RedisDockerProperties -> dockerProperties.toDockerArgs(containerName, portMapping)
                else -> throw IllegalArgumentException("Unknown docker container type: $dockerContainerType")
            }
            val r = dockerCommandService.run(*args.toTypedArray())
            val existsNow = dockerCommandService.exists(containerName)
            val runningNow = dockerCommandService.isRunning(containerName)
            val id = dockerCommandService.containerId(containerName)

            // Apply Flyway migrations if this is a PostgreSQL container and it's running
            var migrationsApplied = false
            if (dockerProperties is PostgresDockerProperties && runningNow) {
                try {
                    waitForPostgreSQLAsync(dockerProperties, 5 * 60 * 1000)
                    flywayMigrationService.migrateDatabase()
                    migrationsApplied = true
                } catch (e: Exception) {
                    println("[ERROR] Failed to apply Flyway migrations: ${e.message}")
                }
            }

            val msg = if (r.success) {
                if (runningNow) {
                    if (migrationsApplied) "Container created, running, and Flyway migrations applied"
                    else "Container created and running"
                } else "Container created"
            } else (r.stderr.ifBlank { r.stdout })
            return ContainerStatus(
                name = containerName,
                exists = existsNow,
                running = runningNow,
                containerId = id,
                message = msg
            )
        }
    }

    private suspend fun waitForPostgreSQLAsync(
        postgresDockerProperties: PostgresDockerProperties,
        maxWaitTimeMs: Long = 30 * 1000,
        intervalMs: Long = 1000
    ): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < maxWaitTimeMs) {
            try {
                DriverManager.getConnection(
                    flywayUrl,
                    postgresDockerProperties.username,
                    postgresDockerProperties.password
                ).use { connection ->
                    connection.prepareStatement("SELECT 1").use { statement ->
                        statement.executeQuery().use { resultSet ->
                            if (resultSet.next() && resultSet.getInt(1) == 1) {
                                println("[INFO] PostgreSQL is ready!")
                                return@withContext true
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("[DEBUG] Waiting for PostgreSQL...")
            }

            delay(intervalMs) // Thread.sleep 대신 코루틴의 delay
        }

        false
    }

    fun stop(dockerContainerType: DockerContainerType): ContainerStatus {
        val dockerProperties = configService.getConfig(dockerContainerType)
        val containerName = dockerProperties.containerName
        val exists = dockerCommandService.exists(containerName)
        if (!exists) {
            return ContainerStatus(
                name = containerName,
                exists = false,
                running = false,
                containerId = null,
                message = "Container not found"
            )
        }
        val running = dockerCommandService.isRunning(containerName)
        if (!running) {
            val id = dockerCommandService.containerId(containerName)
            return ContainerStatus(
                name = containerName,
                exists = true,
                running = false,
                containerId = id,
                message = "Container already stopped"
            )
        }
        val r = dockerCommandService.run("docker", "stop", containerName)
        val nowRunning = dockerCommandService.isRunning(containerName)
        val id = dockerCommandService.containerId(containerName)
        val msg = if (r.success && !nowRunning) "Container stopped" else (r.stderr.ifBlank { r.stdout })
        return ContainerStatus(
            name = containerName,
            exists = true,
            running = nowRunning,
            containerId = id,
            message = msg
        )
    }

    fun remove(dockerContainerType: DockerContainerType): ContainerStatus {
        val dockerProperties = configService.getConfig(dockerContainerType)
        val containerName = dockerProperties.containerName
        val exists = dockerCommandService.exists(containerName)
        if (!exists) {
            return ContainerStatus(
                name = containerName,
                exists = false,
                running = false,
                containerId = null,
                message = "Container not found"
            )
        }
        if (dockerCommandService.isRunning(containerName)) {
            dockerCommandService.run("docker", "stop", containerName)
        }
        val rm = dockerCommandService.run("docker", "rm", containerName)
        val existsNow = dockerCommandService.exists(containerName)
        val msg = if (rm.success && !existsNow) "Container removed" else (rm.stderr.ifBlank { rm.stdout })
        return ContainerStatus(
            name = containerName,
            exists = existsNow,
            running = false,
            containerId = null,
            message = msg
        )
    }
}