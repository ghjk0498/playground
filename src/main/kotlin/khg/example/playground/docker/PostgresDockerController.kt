package khg.example.playground.docker

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/docker/postgres")
class PostgresDockerController(
    private val docker: DockerCommandService,
    private val props: PostgresDockerProperties
) {
    data class StatusResponse(
        val name: String,
        val exists: Boolean,
        val running: Boolean,
        val containerId: String?,
        val message: String? = null
    )

    @GetMapping("/status")
    fun status(): ResponseEntity<StatusResponse> {
        val exists = docker.exists(props.containerName)
        val running = if (exists) docker.isRunning(props.containerName) else false
        val id = if (exists) docker.containerId(props.containerName) else null
        return ResponseEntity.ok(
            StatusResponse(
                name = props.containerName,
                exists = exists,
                running = running,
                containerId = id,
                message = if (!exists) "Container not found" else null
            )
        )
    }

    @PostMapping("/start")
    fun start(): ResponseEntity<StatusResponse> {
        val containerName = props.containerName
        val exists = docker.exists(containerName)
        if (exists) {
            val running = docker.isRunning(containerName)
            if (running) {
                val id = docker.containerId(containerName)
                return ResponseEntity.ok(
                    StatusResponse(containerName, true, true, id, "Container already running")
                )
            }
            val r = docker.run("docker", "start", containerName)
            val nowRunning = docker.isRunning(containerName)
            val id = docker.containerId(containerName)
            val status = if (r.success && nowRunning) HttpStatus.OK else HttpStatus.INTERNAL_SERVER_ERROR
            val msg = if (r.success && nowRunning) "Container started" else (r.stderr.ifBlank { r.stdout })
            return ResponseEntity.status(status).body(
                StatusResponse(containerName, true, nowRunning, id, msg)
            )
        } else {
            val portMapping = "${props.hostPort}:${props.containerPort}"
            val r = docker.run(
                "docker", "run", "-d",
                "--name", containerName,
                "-p", portMapping,
                "-e", "POSTGRES_USER=${props.username}",
                "-e", "POSTGRES_PASSWORD=${props.password}",
                "-e", "POSTGRES_DB=${props.database}",
                props.image
            )
            val existsNow = docker.exists(containerName)
            val runningNow = docker.isRunning(containerName)
            val id = docker.containerId(containerName)
            val status = if (r.success && existsNow) HttpStatus.CREATED else HttpStatus.INTERNAL_SERVER_ERROR
            val msg = if (r.success) "Container created${if (runningNow) " and running" else ""}" else (r.stderr.ifBlank { r.stdout })
            return ResponseEntity.status(status).body(
                StatusResponse(containerName, existsNow, runningNow, id, msg)
            )
        }
    }

    @PostMapping("/stop")
    fun stop(): ResponseEntity<StatusResponse> {
        val containerName = props.containerName
        val exists = docker.exists(containerName)
        if (!exists) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                StatusResponse(containerName, false, false, null, "Container not found")
            )
        }
        val running = docker.isRunning(containerName)
        if (!running) {
            val id = docker.containerId(containerName)
            return ResponseEntity.ok(
                StatusResponse(containerName, true, false, id, "Container already stopped")
            )
        }
        val r = docker.run("docker", "stop", containerName)
        val nowRunning = docker.isRunning(containerName)
        val id = docker.containerId(containerName)
        val status = if (r.success && !nowRunning) HttpStatus.OK else HttpStatus.INTERNAL_SERVER_ERROR
        val msg = if (r.success && !nowRunning) "Container stopped" else (r.stderr.ifBlank { r.stdout })
        return ResponseEntity.status(status).body(
            StatusResponse(containerName, true, nowRunning, id, msg)
        )
    }

    @DeleteMapping
    fun remove(): ResponseEntity<StatusResponse> {
        val containerName = props.containerName
        val exists = docker.exists(containerName)
        if (!exists) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                StatusResponse(containerName, false, false, null, "Container not found")
            )
        }
        if (docker.isRunning(containerName)) {
            docker.run("docker", "stop", containerName)
        }
        val rm = docker.run("docker", "rm", containerName)
        val existsNow = docker.exists(containerName)
        val status = if (rm.success && !existsNow) HttpStatus.NO_CONTENT else HttpStatus.INTERNAL_SERVER_ERROR
        val msg = if (rm.success && !existsNow) "Container removed" else (rm.stderr.ifBlank { rm.stdout })
        return ResponseEntity.status(status).body(
            StatusResponse(containerName, existsNow, false, null, msg)
        )
    }
}
