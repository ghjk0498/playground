package khg.example.playground.docker

import khg.example.playground.docker.config.PostgresDockerProperties
import khg.example.playground.docker.config.RedisDockerProperties

fun PostgresDockerProperties.toDockerArgs(containerName: String, portMapping: String): List<String> {
    return listOf(
        "docker", "run", "-d",
        "--name", containerName,
        "-p", portMapping,
        "-e", "POSTGRES_USER=${username}",
        "-e", "POSTGRES_PASSWORD=${password}",
        "-e", "POSTGRES_DB=${database}",
        image,
    )
}

fun RedisDockerProperties.toDockerArgs(containerName: String, portMapping: String): List<String> {
    return listOf(
        "docker", "run", "-d",
        "--name", containerName,
        "-p", portMapping,
        image
    )
}