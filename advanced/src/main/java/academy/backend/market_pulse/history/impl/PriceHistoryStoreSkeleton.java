package academy.backend.market_pulse.history.impl;

import academy.backend.market_pulse.history.PriceHistoryStore;
import academy.backend.market_pulse.history.PricePoint;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

public class PriceHistoryStoreSkeleton implements PriceHistoryStore {

    @Override
    public Path append(Path directory, PricePoint point) {
        try {
            Files.createDirectories(directory);
            String fileName = point.ticker() + "_" + UUID.randomUUID() + ".txt";
            Path file = directory.resolve(fileName);

            String content = point.timestamp().toEpochMilli() + ";" + point.price().toPlainString();
            Files.writeString(file, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);

            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось записать историю о цене", e);
        }
    }

    @Override
    public List<PricePoint> load(Path directory, String ticker) {
        if (Files.notExists(directory)) {
            return new ArrayList<>();
        }
        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                    .filter(p -> p.getFileName().toString().startsWith(ticker + "_"))
                    .map(this::readPoint)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(PricePoint::timestamp))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось загрузить историю цен", e);
        }
    }

    private PricePoint readPoint(Path file) {
        try {
            String[] parts = Files.readString(file, StandardCharsets.UTF_8).split(";");
            String ticker = file.getFileName().toString().split("_")[0];
            return new PricePoint(ticker, Instant.ofEpochMilli(Long.parseLong(parts[0])), new BigDecimal(parts[1]));
        } catch (Exception e) {
            return null;
        }
    }
}