package ru.ivan.moviehub.server;

import com.sun.net.httpserver.HttpServer;
import ru.ivan.moviehub.http.MoviesHandler;
import ru.ivan.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private final HttpServer server;
    private final MoviesStore store;

    public MoviesServer() {
        try {
            store = new MoviesStore();
            server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/movies", new MoviesHandler(store));
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP-сервер", e);
        }
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public void clearStore() {
        store.clear();
    }
}