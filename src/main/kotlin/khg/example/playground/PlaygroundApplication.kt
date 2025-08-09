package khg.example.playground

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@ConfigurationPropertiesScan
@SpringBootApplication
class PlaygroundApplication

fun main(args: Array<String>) {
    runApplication<PlaygroundApplication>(*args)
}
