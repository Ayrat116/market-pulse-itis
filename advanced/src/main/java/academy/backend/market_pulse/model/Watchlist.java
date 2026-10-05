package academy.backend.market_pulse.model;

import academy.backend.market_pulse.repository.InstrumentRepository;

import java.util.LinkedHashSet;
import java.util.Set;

// TODO: реализовать поверх LinkedHashSet<String> — O(1) на add/contains, порядок добавления
//  сохраняется. Тикеры нормализовать к верхнему регистру.
public class Watchlist {

    private final Set<String> tickers = new LinkedHashSet<>();

    public boolean add(String ticker) {
        throw new UnsupportedOperationException("add: реализовать поверх LinkedHashSet");
    }

    public boolean contains(String ticker) {
        throw new UnsupportedOperationException("contains: реализовать поверх LinkedHashSet");
    }

    public Set<String> tickers() {
        // TODO: отдавать наружу неизменяемое представление — Collections.unmodifiableSet(tickers).
        throw new UnsupportedOperationException("tickers: реализовать поверх LinkedHashSet");
    }

    /**
     * Делистинг инструмента (см. {@link InstrumentRepository#remove}) оставляет в watchlist
     * «висячий» тикер — отслеживается то, чего больше нет в репозитории.
     */
    public Set<String> prune(InstrumentRepository repository) {
        // TODO: прибрать такие тикеры, удаляя их прямо по ходу обхода через Iterator.remove():
        //  прямой tickers.remove(ticker) внутри for-each уронит ConcurrentModificationException.
        //  Вернуть удалённые тикеры.
        throw new UnsupportedOperationException("prune: реализовать через Iterator.remove()");
    }
}
