package khg.example.playground.docker.controller

import khg.example.playground.docker.enums.DockerContainerType
import khg.example.playground.docker.service.DockerContainerService
import khg.example.playground.docker.service.dto.ContainerStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/docker")
class DockerController(private val dockerContainerService: DockerContainerService) {

    @GetMapping("/status")
    fun status(@RequestParam(required = true) dockerContainerType: DockerContainerType): ResponseEntity<ContainerStatus> {
        return ResponseEntity.ok(dockerContainerService.status(dockerContainerType))
    }

    @PostMapping("/start")
    fun start(@RequestParam(required = true) dockerContainerType: DockerContainerType): ResponseEntity<ContainerStatus> {
        return ResponseEntity.ok(dockerContainerService.start(dockerContainerType))
    }

    @PostMapping("/stop")
    fun stop(@RequestParam(required = true) dockerContainerType: DockerContainerType): ResponseEntity<ContainerStatus> {
        return ResponseEntity.ok(dockerContainerService.stop(dockerContainerType))
    }

    @DeleteMapping
    fun remove(@RequestParam(required = true) dockerContainerType: DockerContainerType): ResponseEntity<ContainerStatus> {
        return ResponseEntity.ok(dockerContainerService.remove(dockerContainerType))
    }
}
