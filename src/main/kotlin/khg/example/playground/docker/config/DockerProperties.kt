package khg.example.playground.docker.config

interface DockerProperties {
    val containerName: String
    val image: String
    val hostPort: Int
    val containerPort: Int
}