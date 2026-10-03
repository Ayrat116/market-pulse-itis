package academy.backend.market_pulse.repository;

import academy.backend.market_pulse.exception.DuplicateTickerException;
import academy.backend.market_pulse.model.Bond;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Etf;
import academy.backend.market_pulse.model.Instrument;
import academy.backend.market_pulse.model.Stock;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

public class InMemoryInstrumentRepository implements InstrumentRepository {

    private final Instrument[] instruments = new Instrument[100];
    private int size = 0;

    public InMemoryInstrumentRepository() {
        // Стартовые данные для демонстрации CLI: без них каждый запуск начинается с пустого списка.
        add(new Stock("SBER", "Сбер", Currency.RUB, "Финансы", new BigDecimal("6.5")));
        add(new Bond("OFZ26233", "ОФЗ 26233", Currency.RUB, new BigDecimal("8.5"), 2036));
        add(new Etf("FXUS", "FinEx FXUS", Currency.USD, "S&P 500"));
    }

    @Override
    public void add(Instrument instrument) {
        // NOTICE: дубликат ловится тем же findByTicker(...).isPresent(), которым уже пользуется
        // ListCommand — без него повторное add с тем же тикером молча портило бы данные.
        if (findByTicker(instrument.getTicker()).isPresent()) {
            throw new DuplicateTickerException(instrument.getTicker());
        }
        instruments[size++] = instrument;
    }

    @Override
    public Optional<Instrument> findByTicker(String ticker) {
        for (Instrument instrument : this) {
            if (instrument.getTicker().equalsIgnoreCase(ticker)) {
                return Optional.of(instrument);
            }
        }
        return Optional.empty();
    }

    @Override
    public Iterator<Instrument> iterator() {
        return new Iterator<>() {
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < size;
            }

            @Override
            public Instrument next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return instruments[cursor++];
            }
        };
    }
}
