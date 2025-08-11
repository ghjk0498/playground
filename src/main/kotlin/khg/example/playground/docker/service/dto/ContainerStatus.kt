package khg.example.playground.docker.service.dto

data class ContainerStatus(
    val name: String,
    val exists: Boolean,
    val running: Boolean,
    val containerId: String?,
    val message: String? = null
)
