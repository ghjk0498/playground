package khg.example.playground.docker.service

import khg.example.playground.docker.config.DockerProperties
import khg.example.playground.docker.config.PostgresDockerProperties
import khg.example.playground.docker.config.RedisDockerProperties
import khg.example.playground.docker.enums.DockerContainerType
import org.springframework.stereotype.Service

@Service
class ConfigService(
    private val postgresDockerProperties: PostgresDockerProperties,
    private val redisDockerProperties: RedisDockerProperties,
) {
    fun getConfig(dockerContainerType: DockerContainerType) : DockerProperties {
        return when (dockerContainerType) {
            DockerContainerType.POSTGRES -> postgresDockerProperties
            DockerContainerType.REDIS -> redisDockerProperties
        }
    }
}