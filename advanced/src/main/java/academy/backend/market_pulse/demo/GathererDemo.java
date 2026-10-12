package academy.backend.market_pulse.demo;

import academy.backend.market_pulse.gather.MovingAverageGatherer;
import academy.backend.market_pulse.gather.SlidingSpreadGatherer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Gatherers;

/**
 * Все встроенные фабрики {@code java.util.stream.Gatherers} рядом с кастомным {@code Gatherer}
 * (семинар 6, материал «Gatherer API»). {@code HistoryCommand} использует только
 * {@link MovingAverageGatherer} — этот демо-класс показывает, чем он отличается от штатной
 * альтернативы {@code windowSliding}, и разбирает ещё три встроенных фабрики, которых в реальном
 * коде проекта нет.
 */
public class GathererDemo {

    public static void main(String[] args) {
        List<BigDecimal> prices = List.of(
                new BigDecimal("244.00"), new BigDecimal("246.50"), new BigDecimal("245.00"),
                new BigDecimal("248.00"), new BigDecimal("250.00"), new BigDecimal("249.50"),
                new BigDecimal("251.20"));

        System.out.println("=============== Gatherers.windowSliding(3) — встроенный ===============");
        windowSliding(prices);

        System.out.println("=============== MovingAverageGatherer.movingAverage(3) — кастомный ===============");
        customMovingAverage(prices);

        System.out.println("=============== Gatherers.windowFixed(2) — неперекрывающиеся пакеты ===============");
        windowFixed(prices);

        System.out.println("=============== Gatherers.scan — нарастающий итог ===============");
        scan(prices);

        System.out.println("=============== Gatherers.mapConcurrent(3) — ограниченная конкурентность ===============");
        mapConcurrent(List.of("SBER", "GAZP", "LKOH", "AAPL", "FXUS"));

        System.out.println("=============== SlidingSpreadGatherer.spread(3) — кастомный, реальная задача ===============");
        slidingSpread(prices);
    }

    /**
     * Встроенный {@code windowSliding} сначала материализует каждое окно как отдельный
     * {@code List<BigDecimal>} — дальше нужен ещё один {@code map}, чтобы посчитать среднее.
     * Все {@code k} окон одновременно существуют в памяти как объекты списков.
     */
    private static void windowSliding(List<BigDecimal> prices) {
        prices.stream()
                .gather(Gatherers.windowSliding(3))
                .map(window -> average(window))
                .forEach(sma -> System.out.println("  окно -> среднее = " + sma));
    }

    /**
     * {@link MovingAverageGatherer} считает ровно тот же результат за один проход конвейера, без
     * промежуточного {@code map}: окно и сумма — внутреннее состояние {@code integrator}, среднее
     * считается и испускается сразу, как только окно заполнено (см. материал «Gatherer API»).
     */
    private static void customMovingAverage(List<BigDecimal> prices) {
        prices.stream()
                .gather(MovingAverageGatherer.movingAverage(3))
                .forEach(sma -> System.out.println("  SMA = " + sma));
    }

    /**
     * {@code windowFixed(n)} режет поток на НЕперекрывающиеся пакеты по {@code n} элементов — в
     * отличие от {@code windowSliding}, где соседние окна делят между собой часть элементов.
     * Последний пакет может быть короче {@code n} (как последняя страница в постраничном переборе —
     * см. {@code StreamGeneratorsDemo}). Типичное применение — батчевая обработка: отправить
     * элементы во внешний сервис пачками по {@code n}, а не по одному и не все разом.
     */
    private static void windowFixed(List<BigDecimal> prices) {
        prices.stream()
                .gather(Gatherers.windowFixed(2))
                .forEach(batch -> System.out.println("  пакет = " + batch));
    }

    /**
     * {@code scan(initial, scanner)} — как {@code reduce}, но испускает КАЖДОЕ промежуточное
     * состояние, а не только финальное: здесь — «растущий итог» суммы цен вместо одной общей
     * суммы. Промежуточная операция (конвейер продолжается), а не терминальная.
     */
    private static void scan(List<BigDecimal> prices) {
        prices.stream()
                .gather(Gatherers.scan(() -> BigDecimal.ZERO, BigDecimal::add))
                .forEach(runningTotal -> System.out.println("  нарастающий итог = " + runningTotal));
    }

    /**
     * {@code mapConcurrent(n, mapper)} — тот же {@code map}, но до {@code n} вызовов {@code mapper}
     * одновременно; полезно, когда {@code mapper} сам по себе медленный и блокирующий (поход в
     * сеть), а не арифметика на CPU — ровно то, чем в будущем станет обращение к T-Invest API
     * (семинар 7). Ниже — тот же вызов «медленного внешнего сервиса» обычным {@code map} и через
     * {@code mapConcurrent(3)}: при пяти запросах по ~150 мс обычный {@code map} делает их строго
     * по одному (~750 мс), {@code mapConcurrent(3)} — до трёх одновременно (~300 мс).
     */
    private static void mapConcurrent(List<String> tickers) {
        long sequentialMs = time(() -> tickers.stream()
                .map(GathererDemo::fetchFromSlowExternalService)
                .toList());
        System.out.println("  обычный map:      ~" + sequentialMs + " мс");

        long concurrentMs = time(() -> tickers.stream()
                .gather(Gatherers.mapConcurrent(3, GathererDemo::fetchFromSlowExternalService))
                .toList());
        System.out.println("  mapConcurrent(3): ~" + concurrentMs + " мс");
    }

    private static long time(Supplier<List<String>> action) {
        long start = System.nanoTime();
        List<String> result = action.get();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println("    -> " + result);
        return elapsedMs;
    }

    /** Эмуляция похода в сеть — реального сервиса пока нет (придёт на семинаре 7). */
    private static String fetchFromSlowExternalService(String ticker) {
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return ticker + ":OK";
    }

    /**
     * {@link SlidingSpreadGatherer} — разница между max и min в скользящем окне из 3 цен подряд.
     * Та самая «реальная задача» из материала «Gatherer API»: ни {@code map}, ни {@code filter},
     * ни {@code reduce} сами по себе не выразят «окно из последних N значений» — нужно состояние
     * между соседними элементами конвейера, то есть ровно то, для чего и нужен Gatherer API.
     */
    private static void slidingSpread(List<BigDecimal> prices) {
        prices.stream()
                .gather(SlidingSpreadGatherer.spread(3))
                .forEach(spread -> System.out.println("  разброс (max - min) = " + spread));
    }

    private static BigDecimal average(List<BigDecimal> window) {
        return window.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(window.size()), 2, RoundingMode.HALF_UP);
    }
}
