package academy.backend.market_pulse.repository;

import academy.backend.market_pulse.exception.DuplicateTickerException;
import academy.backend.market_pulse.model.Bond;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Etf;
import academy.backend.market_pulse.model.Instrument;
import academy.backend.market_pulse.model.Stock;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Реализация {@link InstrumentRepository} поверх {@link LinkedHashMap}: ключ — тикер в верхнем
 * регистре, поиск и проверка дубликата — {@code O(1)} вместо линейного перебора по массиву.
 * {@code LinkedHashMap}, а не {@code HashMap} — ради сохранения порядка добавления в выводе
 * {@code list}.
 */
public class InMemoryInstrumentRepository implements InstrumentRepository {

    private final Map<String, Instrument> instruments = new LinkedHashMap<>();

    public InMemoryInstrumentRepository() {
        // Стартовые данные для демонстрации CLI: без них каждый запуск начинается с пустого списка.
        add(new Stock("SBER", "Сбер", Currency.RUB, "Финансы", new BigDecimal("6.5")));
        add(new Bond("OFZ26233", "ОФЗ 26233", Currency.RUB, new BigDecimal("8.5"), 2036));
        add(new Etf("FXUS", "FinEx FXUS", Currency.USD, "S&P 500"));
    }

    @Override
    public void add(Instrument instrument) {
        // NOTICE: ключ нормализован к верхнему регистру и при записи, и при чтении — у Map
        // равенство ключей строгое (String.hashCode/equals чувствительны к регистру), в отличие
        // от прежнего поиска по массиву через equalsIgnoreCase.
        String key = instrument.getTicker().toUpperCase();
        if (instruments.containsKey(key)) {
            throw new DuplicateTickerException(instrument.getTicker());
        }
        instruments.put(key, instrument);
    }

    @Override
    public Optional<Instrument> findByTicker(String ticker) {
        return Optional.ofNullable(instruments.get(ticker.toUpperCase()));
    }

    @Override
    public boolean remove(String ticker) {
        return instruments.remove(ticker.toUpperCase()) != null;
    }

    @Override
    public Set<String> removeIf(Predicate<Instrument> filter) {
        Set<String> removed = new LinkedHashSet<>();
        // NOTICE: удаление идёт через сам итератор (fail-fast) — заранее неизвестно, сколько и
        // какие записи подойдут под filter, а прямой instruments.remove(key) внутри этого же
        // обхода уронил бы ConcurrentModificationException.
        Iterator<Map.Entry<String, Instrument>> it = instruments.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Instrument> entry = it.next();
            if (filter.test(entry.getValue())) {
                it.remove();
                removed.add(entry.getKey());
            }
        }
        return removed;
    }

    @Override
    public Iterator<Instrument> iterator() {
        return instruments.values().iterator();
    }
}
