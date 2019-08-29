package com.example

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.*
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.gson.gson
import io.ktor.html.respondHtml
import io.ktor.http.content.resource
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.post
import kotlinx.coroutines.async
import kotlinx.html.*
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import io.ktor.routing.get
import javax.naming.AuthenticationException

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    initDB()
    install(StatusPages) {
        exception<Throwable> { e ->
            call.respondText(
                e.localizedMessage,
                ContentType.Text.Plain,
                HttpStatusCode.InternalServerError
            )
        }
        exception<AuthenticationException> {
            call.respond(HttpStatusCode.Unauthorized)
        }
    }
    install(ContentNegotiation) {
        gson()
    }
    install(Authentication) {
        basic(name = "userAuth") {
            validate { (name, password) ->
                if (name == "marcel" && password == "enter") {
                    UserIdPrincipal(name)
                }
                else if (name == "peter" && password == "enter") {
                    UserIdPrincipal(name)
                }
                else {
                    null
                }
            }
        }
    }
    install(Routing) {
        authenticate("userAuth") {
            post("highscore") {
                val userIdPrincipal = call.authentication.principal as UserIdPrincipal
                val request = call.receive<HighscoreRequest>()
                val entityId =
                    transaction {
                        HighScoreDao.insertAndGetId {
                            it[highscore] = request.highscore
                            it[username] = userIdPrincipal.name
                        }
                    }
                call.respond(HighscoreResponse(entityId.value, request.highscore, userIdPrincipal.name))
            }
        }

        resource("styles.css", "styles.css")

        get("/highscores") {
            call.respondHtml {
                head {
                    link(rel = "stylesheet", href = "/styles.css", type = "text/css")
                    title { "Highscores" }
                }
                body {
                    table {
                        thead {
                            tr {
                                th { text("User") }
                                th {
                                    text("Highscore") }
                            }
                        }
                        for (highscoreResponse in getHighscoreList())
                            tr {
                                td {
                                    text(highscoreResponse.username)
                                }
                                td {
                                    text(highscoreResponse.highscore)
                                }
                            }
                    }
                }
            }
        }
    }
}

data class HighscoreResponse(val id: Int, val highscore: Int, val username: String)
data class HighscoreRequest(val highscore: Int)

fun initDB() {
    val config = HikariConfig()
    config.driverClassName = "org.h2.Driver"
    config.jdbcUrl = "jdbc:h2:mem:test"
    config.validate()
    val ds = HikariDataSource(config)
    Database.connect(ds)

    transaction {
        SchemaUtils.create(HighScoreDao)
    }

}

object HighScoreDao : IntIdTable("highscore") {
    val highscore = integer("highscore")
    val username = text("username")
}


private fun getHighscoreList(): List<HighscoreResponse> {
    val highscores: ArrayList<HighscoreResponse> = arrayListOf()
    transaction {
        HighScoreDao.selectAll().orderBy(HighScoreDao.highscore to SortOrder.DESC)
            .map { row ->
                highscores.add(toMessageResponse(row))
            }
    }
    return highscores
}

private fun toMessageResponse(row: ResultRow): HighscoreResponse {
    return HighscoreResponse(
        id = row[HighScoreDao.id].value,
        highscore = row[HighScoreDao.highscore],
        username = row[HighScoreDao.username]
    )
}