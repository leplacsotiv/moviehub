package ru.ivan.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.ivan.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MoviesHandler extends BaseHttpHandler {

    private static final Gson gson = new Gson();

    private static final String ERR_VALIDATION = "Ошибка валидации";
    private static final String ERR_NOT_FOUND = "Фильм не найден";
    private static final String ERR_BAD_ID = "Некорректный ID";
    private static final String ERR_BAD_YEAR_QUERY = "Некорректный параметр запроса — 'year'";
    private static final String ERR_BAD_JSON = "Некорректный JSON";
    private static final String ERR_UNSUPPORTED_CT = "Неподдерживаемый Content-Type";

    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod().toUpperCase(java.util.Locale.ROOT);

        switch (method) {
            case "GET" -> handleGet(ex);
            case "POST" -> handlePost(ex);
            case "DELETE" -> handleDelete(ex);
            default -> {
                ex.sendResponseHeaders(405, -1);
                ex.close();
            }
        }
    }

    private void handleGet(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();

        if ("/movies".equals(path)) {
            String query = ex.getRequestURI().getQuery();
            if (query == null || query.isBlank()) {
                sendJson(ex, 200, gson.toJson(store.findAll()));
                return;
            }

            String yearStr = null;
            for (String part : query.split("&")) {
                int eq = part.indexOf('=');
                String key = eq >= 0 ? part.substring(0, eq) : part;
                String val = eq >= 0 ? part.substring(eq + 1) : "";
                if ("year".equals(key)) {
                    yearStr = val;
                    break;
                }
            }

            if (yearStr == null) {
                sendJson(ex, 400, gson.toJson(Map.of("error", ERR_BAD_YEAR_QUERY)));
                return;
            }

            int year;
            try {
                year = Integer.parseInt(yearStr);
            } catch (NumberFormatException e) {
                sendJson(ex, 400, gson.toJson(Map.of("error", ERR_BAD_YEAR_QUERY)));
                return;
            }

            List<MoviesStore.Movie> filtered = new ArrayList<>();
            for (MoviesStore.Movie m : store.findAll()) {
                if (m.year() == year) {
                    filtered.add(m);
                }
            }

            sendJson(ex, 200, gson.toJson(filtered));
            return;
        }

        String prefix = "/movies/";
        if (path.startsWith(prefix)) {
            String idStr = path.substring(prefix.length());
            long id;
            try {
                id = Long.parseLong(idStr);
            } catch (NumberFormatException e) {
                sendJson(ex, 400, gson.toJson(Map.of("error", ERR_BAD_ID)));
                return;
            }

            MoviesStore.Movie found = store.findById(id);
            if (found == null) {
                sendJson(ex, 404, gson.toJson(Map.of("error", ERR_NOT_FOUND)));
                return;
            }

            sendJson(ex, 200, gson.toJson(found));
            return;
        }

        sendJson(ex, 404, gson.toJson(Map.of("error", ERR_NOT_FOUND)));
    }

    private void handlePost(HttpExchange ex) throws IOException {
        String contentType = ex.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase().startsWith("application/json")) {
            sendJson(ex, 415, gson.toJson(Map.of("error", ERR_UNSUPPORTED_CT)));
            return;
        }

        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        CreateMovieRequest req;
        try {
            req = gson.fromJson(body, CreateMovieRequest.class);
        } catch (JsonSyntaxException e) {
            sendJson(ex, 400, gson.toJson(Map.of("error", ERR_BAD_JSON)));
            return;
        }

        List<String> details = new ArrayList<>();

        String title = req.title() == null ? "" : req.title().trim();
        if (title.isEmpty()) {
            details.add("название не должно быть пустым");
        }
        if (title.length() > 100) {
            details.add("название должно быть не длиннее 100 символов");
        }

        int year = req.year();
        int maxYear = Year.now().getValue() + 1;
        if (year < 1888 || year > maxYear) {
            details.add("год должен быть между 1888 и " + maxYear);
        }

        if (!details.isEmpty()) {
            sendJson(ex, 422, gson.toJson(Map.of(
                    "error", ERR_VALIDATION,
                    "details", details
            )));
            return;
        }

        MoviesStore.Movie created = store.create(title, year);
        sendJson(ex, 201, gson.toJson(created));
    }

    private void handleDelete(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String prefix = "/movies/";

        if (!path.startsWith(prefix)) {
            sendJson(ex, 404, gson.toJson(Map.of("error", ERR_NOT_FOUND)));
            return;
        }

        String idStr = path.substring(prefix.length());
        long id;
        try {
            id = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            sendJson(ex, 400, gson.toJson(Map.of("error", ERR_BAD_ID)));
            return;
        }

        boolean deleted = store.deleteById(id);
        if (!deleted) {
            sendJson(ex, 404, gson.toJson(Map.of("error", ERR_NOT_FOUND)));
            return;
        }

        sendNoContent(ex);
    }

    private record CreateMovieRequest(String title, int year) { }
}