package academy.backend.market_pulse.repository.store.impl;

import academy.backend.market_pulse.repository.store.WatchlistStore;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.Set;

public class WatchlistStoreSkeleton implements WatchlistStore {

    @Override
    public void save(Path path, Set<String> tickers) {
        try {
            Path parent = path.getParent();
            Path tempFile = parent != null ? Files.createTempFile(parent, "watchlist", ".tmp") : Files.createTempFile("watchlist", ".tmp");
            Files.write(tempFile, tickers, StandardCharsets.UTF_8);
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось сохранить watchlist", e);
        }
    }

    @Override
    public Set<String> load(Path path) {
        if (Files.notExists(path)) {
            return new LinkedHashSet<>();
        }
        try {
            return new LinkedHashSet<>(Files.readAllLines(path, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось загрузить watchlist", e);
        }
    }
}