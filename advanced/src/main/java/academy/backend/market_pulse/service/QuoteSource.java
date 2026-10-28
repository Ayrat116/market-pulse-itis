package academy.backend.market_pulse.service;

import academy.backend.market_pulse.model.Quote;

import lombok.RequiredArgsConstructor;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Заглушка внешнего источника котировок: отдаёт заранее заготовленные данные по тикеру, без
 * реального сетевого взаимодействия. Полноценный сетевой клиент придёт на смену позже.
 */
@RequiredArgsConstructor
public class QuoteSource {

    private final Map<String, Quote> data;

    public Optional<Quote> fetch(String ticker) {
        return Optional.ofNullable(data.get(ticker.toUpperCase()));
    }

    /**
     * Все доступные котировки — «снимок рынка» для агрегатных команд ({@code stats}, {@code movers}).
     * Неизменяемое представление, чтобы вызывающий код не мог поменять внутренние
     * данные заглушки.
     */
    public Collection<Quote> all() {
        return Collections.unmodifiableCollection(data.values());
    }
}
