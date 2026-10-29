package academy.backend.market_pulse.repository.store.impl;

import academy.backend.market_pulse.dictionary.InstrumentType;
import academy.backend.market_pulse.factory.InstrumentFactories;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Instrument;
import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.repository.store.QuoteFileStore;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class QuoteFileStoreSkeleton implements QuoteFileStore {

    @Override
    public void save(Path path, List<Quote> quotes) {
        List<String> lines = new ArrayList<>();
        for (Quote q : quotes) {
            lines.add(String.join(";",
                    q.getInstrument().getType().name(),
                    q.getInstrument().getTicker(),
                    q.getInstrument().getName(),
                    q.getInstrument().getCurrency().name(),
                    q.getPrice().toPlainString(),
                    q.getChangePercent().toPlainString()
            ));
        }
        try {
            Path parent = path.getParent();
            Path tempFile = parent != null ? Files.createTempFile(parent, "quotes", ".tmp") : Files.createTempFile("quotes", ".tmp");
            Files.write(tempFile, lines, StandardCharsets.UTF_8);
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось сохранить quotes", e);
        }
    }

    @Override
    public List<Quote> load(Path path) {
        if (Files.notExists(path)) {
            return new ArrayList<>();
        }
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            List<Quote> quotes = new ArrayList<>();
            for (String line : lines) {
                if (line.isBlank()) continue;
                String[] parts = line.split(";");
                Instrument instrument = InstrumentFactories.create(
                        InstrumentType.valueOf(parts[0]), parts[1], parts[2], Currency.valueOf(parts[3])
                );
                quotes.add(new Quote(instrument, new BigDecimal(parts[4]), new BigDecimal(parts[5])));
            }
            return quotes;
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось загрузить quotes", e);
        }
    }
}