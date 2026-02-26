package ru.ivan.moviehub;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import ru.ivan.moviehub.server.MoviesServer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {

    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer();
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }
    @BeforeEach
    void beforeEach() {
        server.clearStore();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }



    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertEquals("application/json; charset=UTF-8",
                resp.headers().firstValue("Content-Type").orElse(""));
        assertEquals("[]", resp.body());
    }

    @Test
    void postMovies_whenValid_createsMovie() throws Exception {
        String json = "{\"title\":\"Inception\",\"year\":2010}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");
        assertEquals("application/json; charset=UTF-8",
                resp.headers().firstValue("Content-Type").orElse(""));

        assertTrue(resp.body().contains("\"id\""), "Ответ должен содержать поле id");
        assertTrue(resp.body().contains("\"title\":\"Inception\""));
        assertTrue(resp.body().contains("\"year\":2010"));

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> getResp =
                client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, getResp.statusCode());
        assertTrue(getResp.body().contains("\"title\":\"Inception\""),
                "GET /movies должен вернуть добавленный фильм");
    }

    @Test
    void postMovies_whenWrongContentType_returns415() throws Exception {
        String json = "{\"title\":\"Inception\",\"year\":2010}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, resp.statusCode());
        assertEquals("application/json; charset=UTF-8",
                resp.headers().firstValue("Content-Type").orElse(""));
        assertTrue(resp.body().contains("\"error\""));
    }

    @Test
    void postMovies_whenTitleEmpty_returns422() throws Exception {
        String json = "{\"title\":\"\",\"year\":2010}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
        assertEquals("application/json; charset=UTF-8",
                resp.headers().firstValue("Content-Type").orElse(""));

        assertTrue(resp.body().contains("\"error\""));
        assertTrue(resp.body().contains("\"details\""));
    }

    @Test
    void postMovies_whenTitleTooLong_returns422() throws Exception {
        String longTitle = "a".repeat(101);
        String json = "{\"title\":\"" + longTitle + "\",\"year\":2010}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
        assertTrue(resp.body().contains("\"details\""));
    }

    @Test
    void postMovies_whenYearTooSmall_returns422() throws Exception {
        String json = "{\"title\":\"Inception\",\"year\":1800}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
        assertTrue(resp.body().contains("\"details\""));
    }

    @Test
    void postMovies_whenSeveralValidationErrors_returnsAllDetails() throws Exception {
        String json = "{\"title\":\"\",\"year\":1800}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());

        String body = resp.body();
        assertTrue(body.contains("название не должно быть пустым"));
        assertTrue(body.contains("год должен быть между 1888"));
    }

    @Test
    void getMovieById_whenExists_returnsMovie() throws Exception {
        String json = "{\"title\":\"Inception\",\"year\":2010}";

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> postResp =
                client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, postResp.statusCode());
        assertTrue(postResp.body().contains("\"id\":"));

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        HttpResponse<String> getResp =
                client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, getResp.statusCode());
        assertEquals("application/json; charset=UTF-8",
                getResp.headers().firstValue("Content-Type").orElse(""));
        assertTrue(getResp.body().contains("\"title\":\"Inception\""));
        assertTrue(getResp.body().contains("\"year\":2010"));
    }

    @Test
    void getMovieById_whenNotFound_returns404() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/999"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode());
        assertEquals("application/json; charset=UTF-8",
                resp.headers().firstValue("Content-Type").orElse(""));
        assertTrue(resp.body().contains("\"error\""));
    }

    @Test
    void getMovieById_whenIdNotNumber_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        assertEquals("application/json; charset=UTF-8",
                resp.headers().firstValue("Content-Type").orElse(""));
        assertTrue(resp.body().contains("\"error\""));
    }

    @Test
    void deleteMovieById_whenExists_returns204_andRemovesMovie() throws Exception {
        String json = "{\"title\":\"Inception\",\"year\":2010}";

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> postResp =
                client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, postResp.statusCode());

        HttpRequest delReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .DELETE()
                .build();

        HttpResponse<String> delResp =
                client.send(delReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, delResp.statusCode());

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        HttpResponse<String> getResp =
                client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, getResp.statusCode());
    }

    @Test
    void deleteMovieById_whenNotFound_returns404() throws Exception {
        HttpRequest delReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/999"))
                .DELETE()
                .build();

        HttpResponse<String> delResp =
                client.send(delReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, delResp.statusCode());
        assertEquals("application/json; charset=UTF-8",
                delResp.headers().firstValue("Content-Type").orElse(""));
        assertTrue(delResp.body().contains("\"error\""));
    }

    @Test
    void deleteMovieById_whenIdNotNumber_returns400() throws Exception {
        HttpRequest delReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .DELETE()
                .build();

        HttpResponse<String> delResp =
                client.send(delReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, delResp.statusCode());
        assertEquals("application/json; charset=UTF-8",
                delResp.headers().firstValue("Content-Type").orElse(""));
        assertTrue(delResp.body().contains("\"error\""));
    }

    @Test
    void getMovies_whenYearQueryProvided_returnsOnlyThatYear() throws Exception {
        HttpRequest post2010 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"title\":\"A\",\"year\":2010}", StandardCharsets.UTF_8))
                .build();

        HttpRequest post2011 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"title\":\"B\",\"year\":2011}", StandardCharsets.UTF_8))
                .build();

        assertEquals(201, client.send(post2010, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).statusCode());
        assertEquals(201, client.send(post2011, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).statusCode());

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2010"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertEquals("application/json; charset=UTF-8",
                resp.headers().firstValue("Content-Type").orElse(""));

        String body = resp.body();
        assertTrue(body.contains("\"title\":\"A\""));
        assertFalse(body.contains("\"title\":\"B\""));
    }
    @Test
    void getMovies_whenYearQueryNoMatches_returnsEmptyArray() throws Exception {
        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"title\":\"A\",\"year\":2010}", StandardCharsets.UTF_8))
                .build();

        assertEquals(201, client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).statusCode());

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=1999"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertEquals("application/json; charset=UTF-8",
                resp.headers().firstValue("Content-Type").orElse(""));
        assertEquals("[]", resp.body());
    }

    @Test
    void getMovies_whenYearQueryNotNumber_returns400() throws Exception {
        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=abc"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        assertEquals("application/json; charset=UTF-8",
                resp.headers().firstValue("Content-Type").orElse(""));
        assertTrue(resp.body().contains("\"error\""));
    }

    @Test
    void unsupportedMethodOnMovies_returns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(405, resp.statusCode());
    }

    @Test
    void unsupportedMethodOnMovieById_returns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(405, resp.statusCode());
    }
}