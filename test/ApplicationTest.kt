import com.example.module
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class ApplicationTest {

    @Test
    fun testRequests() = withTestApplication(Application::module) {
        with(handleRequest(HttpMethod.Post, "/highscore")) {
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
        with(handleRequest(HttpMethod.Get, "/highscores")) {
            assertTrue(requestHandled)
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }
}