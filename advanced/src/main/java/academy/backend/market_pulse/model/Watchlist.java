package academy.backend.market_pulse.model;

import academy.backend.market_pulse.repository.InstrumentRepository;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Список тикеров, которые отслеживает пользователь. Уникальность и быстрая проверка «отслеживается
 * ли» обеспечиваются {@link LinkedHashSet}: O(1) на add/contains, порядок добавления сохраняется.
 * Тикеры нормализуются к верхнему регистру.
 */
// NOTICE: акцент при показе — LinkedHashSet вместо HashSet/TreeSet по тем же причинам, что и
// LinkedHashMap в репозитории: нужен порядок добавления, а не хеш-порядок или сортировка.
public class Watchlist {

    private final Set<String> tickers = new LinkedHashSet<>();

    public boolean add(String ticker) {
        return tickers.add(ticker.toUpperCase());
    }

    public boolean contains(String ticker) {
        return tickers.contains(ticker.toUpperCase());
    }

    public Set<String> tickers() {
        // NOTICE: наружу отдаём неизменяемое представление — Collections.unmodifiableSet, а не
        // копию и не сам tickers, — вызывающий код не может незаметно мутировать внутреннее поле.
        return Collections.unmodifiableSet(tickers);
    }

    /**
     * Делистинг инструмента (см. {@link InstrumentRepository#remove}) оставляет в watchlist
     * «висячий» тикер — отслеживается то, чего больше нет в репозитории. Прибирает такие тикеры,
     * удаляя их прямо по ходу обхода через {@link Iterator#remove()}: прямой
     * {@code tickers.remove(ticker)} внутри for-each уронил бы {@code ConcurrentModificationException}.
     * Возвращает удалённые тикеры.
     */
    // NOTICE: ключевой пример fail-fast — показать, что for-each с tickers.remove() внутри
    // падает с ConcurrentModificationException, а Iterator.remove() — безопасная альтернатива.
    public Set<String> prune(InstrumentRepository repository) {
        Set<String> removed = new LinkedHashSet<>();
        Iterator<String> it = tickers.iterator();
        while (it.hasNext()) {
            String ticker = it.next();
            if (repository.findByTicker(ticker).isEmpty()) {
                it.remove();
                removed.add(ticker);
            }
        }
        return removed;
    }
}
