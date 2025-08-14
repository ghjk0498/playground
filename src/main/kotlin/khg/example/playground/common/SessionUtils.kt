package khg.example.playground.common

import khg.example.playground.common.dto.SessionData
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID.randomUUID
import kotlin.random.Random

fun generateRandomSessionData(n: Int): List<SessionData> {
    val now = Instant.now()
    return List(n) {
        val loginOffsetSec = Random.nextLong(0, 7L * 24 * 3600)
        val loginTime = now.minusSeconds(loginOffsetSec)
        val activityOffsetSec = Random.nextLong(0, 6 * 3600L)
        val lastActivity = loginTime.plusSeconds(activityOffsetSec)
        val permissions = buildList {
            if (Random.nextBoolean()) add("read")
            if (Random.nextBoolean()) add("write")
        }

        SessionData(
            sessionId = randomUUID(),
            userId = it.toLong(),
            userName = (1..10).map { ('a'..'z').random() }.joinToString(""),
            loginTime = LocalDateTime.ofInstant(loginTime, ZoneId.systemDefault()),
            lastActivity = LocalDateTime.ofInstant(lastActivity, ZoneId.systemDefault()),
            permissions = permissions
        )
    }
}