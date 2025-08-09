package khg.example.playground.docker

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/docker/redis")
class RedisDockerController(
    private val docker: DockerCommandService,
    private val props: RedisDockerProperties
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
                    StatusResponse(containerName,
                        exists = true,
                        running = true,
                        containerId = id,
                        message = "Container already running"
                    )
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
            // Create and run a new container bound to configured ports
            val portMapping = "${props.hostPort}:${props.containerPort}"
            val r = docker.run(
                "docker", "run", "-d",
                "--name", containerName,
                "-p", portMapping,
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
                StatusResponse(containerName,
                    exists = false,
                    running = false,
                    containerId = null,
                    message = "Container not found"
                )
            )
        }
        val running = docker.isRunning(containerName)
        if (!running) {
            val id = docker.containerId(containerName)
            return ResponseEntity.ok(
                StatusResponse(containerName,
                    exists = true,
                    running = false,
                    containerId = id,
                    message = "Container already stopped"
                )
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
                StatusResponse(containerName,
                    exists = false,
                    running = false,
                    containerId = null,
                    message = "Container not found"
                )
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
