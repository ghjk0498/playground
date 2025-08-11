package khg.example.playground.docker.service.dto

data class CommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
) {
    val success: Boolean get() = exitCode == 0
}
