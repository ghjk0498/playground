package khg.example.playground.postgres.repository

import khg.example.playground.postgres.entity.SessionEntity
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SessionRepository : ReactiveCrudRepository<SessionEntity, String>
