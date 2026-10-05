package academy.backend.market_pulse.service;

import academy.backend.market_pulse.model.Quote;

import java.util.Optional;

// TODO: запросы — в очередь Queue<String> (ArrayDeque, offer/poll — O(1) на обоих концах, дешевле
//  LinkedList); полученные котировки — в кеш Map<String, Quote>, чтобы повторный запрос не ходил
//  к источнику снова. Разделение «положить запрос» (request) и «обработать» (processAll) —
//  заготовка под асинхронную обработку.
public class QuoteService {

    private final QuoteSource source;

    public QuoteService(QuoteSource source) {
        this.source = source;
    }

    public void request(String ticker) {
        throw new UnsupportedOperationException("request: положить тикер в очередь запросов");
    }

    public void processAll() {
        throw new UnsupportedOperationException("processAll: разобрать очередь, заполнить кеш");
    }

    public Optional<Quote> cached(String ticker) {
        throw new UnsupportedOperationException("cached: отдать котировку из кеша");
    }
}
