package khg.example.playground.postgres.service

import org.flywaydb.core.Flyway
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Value

@Service
class FlywayMigrationService(
    @Value("\${spring.flyway.url}")
    private val jdbcUrl: String,
    
    @Value("\${spring.flyway.user}")
    private val username: String,
    
    @Value("\${spring.flyway.password}")
    private val password: String
) {
    
    fun migrateDatabase() {
        val flyway = Flyway.configure()
            .dataSource(jdbcUrl, username, password)
            .load()
            
        // Execute the migration
        val migrationResult = flyway.migrate()
        
        // Log migration results
        println("[INFO] Flyway migration completed. ${migrationResult.migrationsExecuted} migrations executed.")
    }
}