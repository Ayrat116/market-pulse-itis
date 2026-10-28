package academy.backend.market_pulse.demo;

import academy.backend.market_pulse.dictionary.InstrumentType;
import academy.backend.market_pulse.factory.InstrumentFactories;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Instrument;
import academy.backend.market_pulse.model.Quote;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

/**
 * {@code Stream.iterate} и {@code Stream.generate} — источники, у которых по умолчанию нет конца:
 * элементы вычисляются по требованию, а не читаются из готовой коллекции. Без явной границы
 * (`limit`, предикат `hasNext`, `takeWhile`) конвейер не остановится сам — в отличие от
 * {@code list.stream()}, где источник конечен изначально.
 *
 * <p>Реальная задача, для которой это нужно, а не просто языковое упражнение — постраничный
 * перебор внешнего сервиса: {@link #fetchAllPages()}. Любой реальный HTTP API (в том числе
 * будущий T-Invest API) отдаёт данные страницами — неизвестно заранее, сколько их будет, конец
 * определяется по ответу (пустая страница), а не по счётчику.
 */
public class StreamGeneratorsDemo {

    public static void main(String[] args) {
        System.out.println("=============== iterate(seed, next) + limit ===============");
        simpleCounter();

        System.out.println("=============== iterate(seed, hasNext, next) — без limit ===============");
        boundedCounter();

        System.out.println("=============== generate(supplier) + limit ===============");
        syntheticQuotes();

        System.out.println("=============== задача: постраничный перебор внешнего сервиса ===============");
        fetchAllPages();
    }

    /**
     * Классический бесконечный {@code iterate}: следующий элемент считается из предыдущего, конца
     * без {@code limit} нет вовсе — забытый {@code limit} здесь означает зависший процесс, а не
     * ошибку компиляции.
     */
    private static void simpleCounter() {
        List<Integer> powersOfTwo = Stream.iterate(1, n -> n * 2)
                .limit(6)
                .toList();
        System.out.println("  " + powersOfTwo);
    }

    /**
     * {@code iterate(seed, hasNext, next)} (JDK 9+) — тот же конвейер, но с условием остановки
     * прямо в источнике, без отдельного {@code limit}: читается как классический {@code for}
     * (`for (int n = 1; n <= 10; n += 3)`), но остаётся лениво вычисляемым стримом. Не подходит
     * для {@link #fetchAllPages()} ниже: {@code hasNext} там нужно было бы знать ДО похода за
     * следующей страницей — а узнать это можно только сходив за ней.
     */
    private static void boundedCounter() {
        List<Integer> sequence = Stream.iterate(1, n -> n <= 10, n -> n + 3)
                .toList();
        System.out.println("  " + sequence);
    }

    /**
     * {@code generate(supplier)} — тоже бесконечный источник, но, в отличие от {@code iterate},
     * элементы друг с другом никак не связаны: каждый — независимый вызов {@code supplier}.
     * Подходит там, где нужен поток независимых случайных/внешних значений (здесь — синтетические
     * котировки для нагрузочного сценария, как в {@code StreamParallelBenchmark}, но через стрим,
     * а не ручной цикл с {@code for}).
     */
    private static void syntheticQuotes() {
        Instrument stock = InstrumentFactories.create(InstrumentType.STOCK, "SBER", "Сбербанк", Currency.RUB);
        Random random = new Random(42);

        List<Quote> synthetic = Stream.generate(() -> {
                    BigDecimal price = BigDecimal.valueOf(100 + random.nextInt(100));
                    BigDecimal change = BigDecimal.valueOf(random.nextInt(2000) - 1000, 2);
                    return new Quote(stock, price, change);
                })
                .limit(5)
                .toList();
        synthetic.forEach(quote -> System.out.println("  " + quote));
    }

    /**
     * Постраничный перебор внешнего сервиса: {@link #fetchPage(int)} эмулирует ответ API —
     * возвращает до {@value #PAGE_SIZE} котировок на странице, пустой список — признак конца
     * (как делает большинство реальных REST API, в том числе постраничные ответы T-Invest API).
     *
     * <p>{@code Stream.iterate(0, page -> page + 1)} — номера страниц, бесконечно; {@code map} —
     * поход за каждой страницей; {@code takeWhile(page -> !page.isEmpty())} останавливает
     * конвейер на первой пустой странице, не вычисляя следующий номер вовсе (короткое замыкание);
     * {@code flatMap(List::stream)} разворачивает страницы (список списков) в один плоский стрим
     * котировок.
     */
    private static void fetchAllPages() {
        // NOTICE: клиент не знает заранее, сколько страниц отдаст сервер (в реальности это и
        // есть весь смысл пагинации) — узнаёт постфактум, по первой пустой странице.
        List<Quote> all = Stream.iterate(0, page -> page + 1)
                .map(StreamGeneratorsDemo::fetchPage)
                .takeWhile(page -> !page.isEmpty())
                .flatMap(List::stream)
                .toList();

        System.out.println("  всего котировок (размер последней страницы сервер не раскрывает заранее): "
                + all.size());
        all.forEach(quote -> System.out.println("    " + quote));
    }

    private static final int PAGE_SIZE = 2;
    private static final int TOTAL_ON_SERVER = 5; // не кратно PAGE_SIZE — последняя страница неполная

    /**
     * Эмуляция ответа внешнего сервиса: «сервер» хранит {@value #TOTAL_ON_SERVER} котировок,
     * отдаёт их по {@value #PAGE_SIZE} за раз. Запрос страницы, которой уже нет, — пустой список,
     * а не ошибка (так же ведёт себя большинство постраничных REST API).
     */
    private static List<Quote> fetchPage(int pageNumber) {
        Instrument stock = InstrumentFactories.create(InstrumentType.STOCK, "TCKR" + pageNumber, "Тест", Currency.RUB);
        int from = pageNumber * PAGE_SIZE;
        if (from >= TOTAL_ON_SERVER) {
            return List.of();
        }
        int to = Math.min(from + PAGE_SIZE, TOTAL_ON_SERVER);

        List<Quote> page = new ArrayList<>();
        for (int i = from; i < to; i++) {
            page.add(new Quote(stock, BigDecimal.valueOf(100 + i), BigDecimal.valueOf(i)));
        }
        return page;
    }
}
