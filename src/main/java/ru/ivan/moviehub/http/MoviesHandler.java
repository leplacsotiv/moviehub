package ru.ivan.moviehub.http;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class MoviesHandler extends BaseHttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();

        if ("GET".equalsIgnoreCase(method)) {
            sendJson(ex, 200, "[]");
            return;
        }

        ex.sendResponseHeaders(405, -1);
        ex.close();
    }
}