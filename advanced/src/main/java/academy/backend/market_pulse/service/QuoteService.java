package academy.backend.market_pulse.service;

import academy.backend.market_pulse.model.Quote;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

/**
 * Обработка пользовательских запросов котировок. Запросы копятся в очереди {@link Queue} и
 * обрабатываются по порядку поступления (FIFO); полученные котировки складываются в кеш {@link Map},
 * чтобы повторный запрос не ходил к источнику снова. Разделение «положить запрос» и «обработать» —
 * заготовка под асинхронную обработку.
 */
public class QuoteService {

    private final QuoteSource source;
    // NOTICE: ArrayDeque — реализация Queue общего назначения на кольцевом буфере, offer/poll —
    // O(1) на обоих концах, накладных расходов меньше, чем у LinkedList.
    private final Queue<String> pending = new ArrayDeque<>();
    private final Map<String, Quote> cache = new HashMap<>();

    public QuoteService(QuoteSource source) {
        this.source = source;
    }

    public void request(String ticker) {
        pending.offer(ticker.toUpperCase());
    }

    public void processAll() {
        while (!pending.isEmpty()) {
            String ticker = pending.poll();
            if (cache.containsKey(ticker)) {
                continue;
            }
            source.fetch(ticker).ifPresent(quote -> cache.put(ticker, quote));
        }
    }

    public Optional<Quote> cached(String ticker) {
        return Optional.ofNullable(cache.get(ticker.toUpperCase()));
    }
}
