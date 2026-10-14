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
// NOTICE: акцент при показе — разделение request()/processAll(). В однопоточном CLI это выглядит
// избыточно, но это именно та точка, в которую позже встанет асинхронная обработка (отдельный
// поток/пул разбирает очередь, не блокируя вызывающего).
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
            // NOTICE: проверка кеша ДО обращения к source — повторный запрос того же тикера
            // не ходит к источнику снова, это и есть смысл кеша.
            if (cache.containsKey(ticker)) {
                continue;
            }
            source.fetch(ticker).ifPresent(quote -> cache.put(ticker, quote));
        }
    }

    public Optional<Quote> cached(String ticker) {
        return Optional.ofNullable(cache.get(ticker.toUpperCase()));
    }

    /**
     * Удобная обёртка «запросить и сразу получить»: кладёт запрос в очередь, обрабатывает её и
     * отдаёт котировку из кеша. Нужна командам, которым котировка по конкретному тикеру требуется
     * прямо сейчас, а не отложенно ({@code compare}).
     */
    public Optional<Quote> quoteFor(String ticker) {
        request(ticker);
        processAll();
        return cached(ticker);
    }
}
