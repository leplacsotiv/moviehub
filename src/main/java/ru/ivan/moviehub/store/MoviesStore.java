package ru.ivan.moviehub.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class MoviesStore {

    private final AtomicLong seq = new AtomicLong(1);
    private final Map<Long, Movie> movies = new HashMap<>();

    public List<Movie> findAll() {
        return new ArrayList<>(movies.values());
    }

    public Movie create(String title, int year) {
        long id = seq.getAndIncrement();
        Movie movie = new Movie(id, title, year);
        movies.put(id, movie);
        return movie;
    }

    public Movie findById(long id) {
        return movies.get(id);
    }

    public boolean deleteById(long id) {
        return movies.remove(id) != null;
    }

    public void clear() {
        movies.clear();
        seq.set(1);
    }

    public record Movie(long id, String title, int year) {}
}