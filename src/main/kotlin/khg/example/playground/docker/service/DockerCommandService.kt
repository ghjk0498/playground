package khg.example.playground.docker.service

import khg.example.playground.docker.service.dto.CommandResult
import org.springframework.stereotype.Service
import java.nio.charset.Charset
import java.time.Duration
import java.util.concurrent.TimeUnit

@Service
class DockerCommandService {
    fun run(vararg args: String, timeout: Duration = Duration.ofSeconds(30)): CommandResult {
        val pb = ProcessBuilder(*args)
        pb.redirectErrorStream(false)
        val process = pb.start()
        val stdout = process.inputStream.readBytes().toString(Charset.defaultCharset())
        val stderr = process.errorStream.readBytes().toString(Charset.defaultCharset())
        if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            process.destroyForcibly()
            return CommandResult(124, stdout, "Timed out after ${timeout.seconds}s")
        }
        return CommandResult(process.exitValue(), stdout.trim(), stderr.trim())
    }

    fun exists(containerName: String): Boolean {
        // docker inspect returns non-zero exit when the container does not exist
        val r = run("docker", "inspect", containerName)
        return r.success
    }

    fun isRunning(containerName: String): Boolean {
        val r = run("docker", "inspect", "-f", "{{.State.Running}}", containerName)
        if (!r.success) return false
        return r.stdout.trim().equals("true", ignoreCase = true)
    }

    fun containerId(containerName: String): String? {
        val r = run("docker", "inspect", "-f", "{{.Id}}", containerName)
        return if (r.success) r.stdout.trim() else null
    }
}